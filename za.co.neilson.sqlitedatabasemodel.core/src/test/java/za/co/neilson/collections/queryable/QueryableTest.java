package za.co.neilson.collections.queryable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class QueryableTest {

    private static final class Person {
        final String name;
        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    private static final Queryable.Field<Person> BY_AGE = new Queryable.Field<Person>() {
        @Override
        public Comparable<?> getFieldValue(Person p) {
            return p.age;
        }
    };

    private Person ann, bob, cat, dan;
    private Queryable<Person> people;

    @Before
    public void setUp() {
        ann = new Person("Ann", 30);
        bob = new Person("Bob", 25);
        cat = new Person("Cat", 41);
        dan = new Person("Dan", 25);
        people = Queryable.asQueryable(Arrays.asList(ann, bob, cat, dan));
    }

    private static Queryable.Matcher<Person> olderThan(final int age) {
        return new Queryable.Matcher<Person>() {
            @Override
            public boolean isMatch(Person p) {
                return p.age > age;
            }
        };
    }

    @Test
    public void asQueryable_copiesTheSourceList() {
        java.util.List<Person> source = new java.util.ArrayList<>(Arrays.asList(ann, bob));
        Queryable<Person> copy = Queryable.asQueryable(source);
        assertEquals(source, copy);
        copy.clear();
        assertEquals("source list must not be modified", 2, source.size());
    }

    @Test
    public void where_returnsMatchesInOriginalOrderAsNewQueryable() {
        Queryable<Person> older = people.where(olderThan(26));
        assertEquals(Arrays.asList(ann, cat), older);
        assertEquals("original is untouched", 4, people.size());
    }

    @Test
    public void where_noMatchGivesEmptyResult() {
        assertTrue(people.where(olderThan(100)).isEmpty());
    }

    @Test
    public void first_returnsFirstMatchOrNull() {
        assertSame(ann, people.first(olderThan(26)));
        assertNull(people.first(olderThan(100)));
    }

    @Test
    public void orderBy_sortsAscendingInPlaceAndIsStableForTies() {
        Queryable<Person> sorted = people.orderBy(BY_AGE);
        assertSame(people, sorted);
        assertEquals(Arrays.asList(bob, dan, ann, cat), sorted);
    }

    @Test
    public void orderByDescending_sortsDescendingAndIsStableForTies() {
        assertEquals(Arrays.asList(cat, ann, bob, dan), people.orderByDescending(BY_AGE));
    }

    @Test
    public void orderBy_canSortByStringField() {
        Queryable<Person> sorted = people.orderByDescending(new Queryable.Field<Person>() {
            @Override
            public Comparable<?> getFieldValue(Person p) {
                return p.name;
            }
        });
        assertEquals(Arrays.asList(dan, cat, bob, ann), sorted);
    }
}

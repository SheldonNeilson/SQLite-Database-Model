package za.co.neilson.sqlite.orm.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import za.co.neilson.sqlite.orm.OrmTestState;
import za.co.neilson.sqlite.orm.android.Entities.AllTypes;
import za.co.neilson.sqlite.orm.android.Entities.Country;
import za.co.neilson.sqlite.orm.android.Entities.Membership;
import za.co.neilson.sqlite.orm.android.Entities.Mode;
import za.co.neilson.sqlite.orm.android.Entities.Person;
import za.co.neilson.sqlite.orm.android.Entities.Tracked;

/** Full ORM round trips through {@link AndroidObjectModel} on a real SQLite database (Robolectric). */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AndroidObjectModelTest {

    private AndroidTestDatabase db;
    private AndroidTestDatabase.TestModel<Person> people;
    private AndroidTestDatabase.TestModel<AllTypes> allTypes;

    @Before
    public void setUp() throws Exception {
        OrmTestState.reset();
        db = AndroidTestDatabase.create(ApplicationProvider.getApplicationContext(), 1, Entities.ALL);
        people = db.model(Person.class);
        allTypes = db.model(AllTypes.class);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static Person person(String email, String name, String country, Integer age) {
        Person p = new Person();
        p.email = email;
        p.name = name;
        p.country = country;
        p.age = age;
        return p;
    }

    private static Membership membership(int group, int user, String role) {
        Membership m = new Membership();
        m.groupId = group;
        m.userId = user;
        m.role = role;
        return m;
    }

    private static AllTypes filled() {
        AllTypes a = new AllTypes();
        a.text = "hello";
        a.flag = true;
        a.boxedFlag = Boolean.FALSE;
        a.tiny = -5;
        a.small = 300;
        a.number = 70000;
        a.big = 5_000_000_000L;
        a.ratio = 1.5f;
        a.precise = 2.25;
        a.date = new Date(1_700_000_000_000L);
        a.calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        a.calendar.setTimeInMillis(1_600_000_000_000L);
        a.blob = new byte[]{0, 1, 2, (byte) 0xFF, (byte) 0x80};
        a.mode = Mode.B;
        a.optionalNumber = 42;
        a.optionalBig = 9_000_000_000L;
        a.optionalDouble = 0.125;
        return a;
    }

    private static void assertSameValues(AllTypes expected, AllTypes actual) {
        assertEquals(expected.text, actual.text);
        assertEquals(expected.flag, actual.flag);
        assertEquals(expected.boxedFlag, actual.boxedFlag);
        assertEquals(expected.tiny, actual.tiny);
        assertEquals(expected.small, actual.small);
        assertEquals(expected.number, actual.number);
        assertEquals(expected.big, actual.big);
        assertEquals(expected.ratio, actual.ratio, 0f);
        assertEquals(expected.precise, actual.precise, 0.0);
        assertEquals(expected.date, actual.date);
        assertEquals(expected.calendar == null ? null : expected.calendar.getTimeInMillis(), actual.calendar == null ? null : actual.calendar.getTimeInMillis());
        assertArrayEquals(expected.blob, actual.blob);
        assertEquals(expected.mode, actual.mode);
        assertEquals(expected.optionalNumber, actual.optionalNumber);
        assertEquals(expected.optionalBig, actual.optionalBig);
        assertEquals(expected.optionalDouble, actual.optionalDouble);
    }

    // ---- round trips ---------------------------------------------------------------------------------------------

    @Test
    public void roundTrip_everySupportedType() throws Exception {
        AllTypes in = filled();
        long id = allTypes.insert(in);

        AllTypes out = allTypes.getFirst("id = ?", id);

        assertNotNull(out);
        assertEquals(id, out.id);
        assertSameValues(in, out);
    }

    @Test
    public void roundTrip_nullsStayNull_andPrimitivesKeepTheirDefaults() throws Exception {
        AllTypes out = allTypes.insertAndReturnUpdated(new AllTypes());

        assertNull(out.text);
        assertNull(out.date);
        assertNull(out.calendar);
        assertNull(out.blob);
        assertNull(out.mode);
        assertNull(out.boxedFlag);
        assertNull(out.optionalNumber);
        assertNull(out.optionalBig);
        assertNull(out.optionalDouble);
        assertFalse(out.flag);
        assertEquals(0, out.number);
        assertEquals(0.0, out.precise, 0.0);
    }

    @Test
    public void roundTrip_booleansAreStoredAsOneAndZero() throws Exception {
        AllTypes in = new AllTypes();
        in.flag = true;
        in.boxedFlag = Boolean.TRUE;
        allTypes.insert(in);
        Map<String, Object> row = db.rows("SELECT flag, boxedFlag FROM AllTypes").get(0);
        assertEquals(1L, row.get("flag"));
        assertEquals(1L, row.get("boxedFlag"));

        in.id = 1;
        in.flag = false;
        in.boxedFlag = Boolean.FALSE;
        allTypes.update(in);
        AllTypes out = allTypes.getFirst("id = 1");
        assertFalse(out.flag);
        assertEquals(Boolean.FALSE, out.boxedFlag);
    }

    @Test
    public void roundTrip_datesAreEpochMillisecondsAndEnumsAreNames() throws Exception {
        allTypes.insert(filled());
        Map<String, Object> row = db.rows("SELECT date, calendar, mode, typeof(date) td FROM AllTypes").get(0);
        assertEquals(1_700_000_000_000L, row.get("date"));
        assertEquals(1_600_000_000_000L, row.get("calendar"));
        assertEquals("B", row.get("mode"));
        assertEquals("integer", row.get("td"));
    }

    @Test
    public void roundTrip_boundaryValues() throws Exception {
        AllTypes in = new AllTypes();
        in.tiny = Byte.MIN_VALUE;
        in.small = Short.MAX_VALUE;
        in.number = Integer.MIN_VALUE;
        in.big = Long.MAX_VALUE;
        in.ratio = Float.MIN_VALUE;
        in.precise = Double.MAX_VALUE;
        in.date = new Date(0);
        in.calendar = Calendar.getInstance();
        in.calendar.setTimeInMillis(-86_400_000L);
        in.optionalNumber = Integer.MAX_VALUE;
        in.optionalBig = Long.MIN_VALUE;
        in.optionalDouble = -0.5;
        in.text = "";
        in.blob = new byte[0];

        AllTypes out = allTypes.insertAndReturnUpdated(in);

        assertSameValues(in, out);
        assertEquals("empty text is not null", "", out.text);
    }

    @Test
    public void roundTrip_textWithQuotesUnicodeAndWildcards() throws Exception {
        String[] samples = {"O'Brien", "say \"hi\"", "50% off_sale", "line1\nline2\ttab", "back\\slash", "éè 中文 😀", "'; DROP TABLE Person; --", "007"};
        for (String s : samples) {
            AllTypes in = new AllTypes();
            in.text = s;
            AllTypes out = allTypes.insertAndReturnUpdated(in);
            assertEquals(s, out.text);
        }
        assertEquals("table survived", samples.length, db.count("AllTypes"));
    }

    @Test
    public void roundTrip_largeBlob() throws Exception {
        byte[] big = new byte[200_000];
        for (int i = 0; i < big.length; i++)
            big[i] = (byte) (i * 31);
        AllTypes in = new AllTypes();
        in.blob = big;
        assertArrayEquals(big, allTypes.insertAndReturnUpdated(in).blob);
    }

    // ---- insert --------------------------------------------------------------------------------------------------

    @Test
    public void insert_assignsIncreasingAutoIncrementIds_andIgnoresAClientSuppliedOne() throws Exception {
        Person a = person("a@x.com", "A", "ZA", 1);
        a.id = 77;
        assertEquals(1, people.insert(a));
        assertEquals(2, people.insert(person("b@x.com", "B", "ZA", 2)));
        assertEquals(Arrays.asList(1L, 2L), column("SELECT id FROM Person ORDER BY id"));
    }

    @Test
    public void insert_naturalAndCompositeKeysAreWrittenAsGiven() throws Exception {
        Country za = new Country();
        za.code = "ZA";
        za.name = "South Africa";
        db.model(Country.class).insert(za);
        assertEquals("ZA", db.scalar("SELECT code FROM Country"));

        db.model(Membership.class).insert(membership(3, 4, "admin"));
        Map<String, Object> row = db.rows("SELECT groupId, userId, role FROM Membership").get(0);
        assertEquals(3L, row.get("groupId"));
        assertEquals(4L, row.get("userId"));
    }

    @Test
    public void insert_constraintViolationsReturnMinusOneAndStoreNothing() throws Exception {
        assertEquals(1, people.insert(person("a@x.com", "A", "ZA", 1)));
        assertEquals("duplicate unique value", -1, people.insert(person("a@x.com", "Other", "ZA", 2)));
        assertEquals("null in a @Nullable(false) column", -1, people.insert(person("b@x.com", "B", null, 2)));
        assertEquals(1, db.count("Person"));

        db.model(Membership.class).insert(membership(1, 1, "r"));
        assertEquals("duplicate composite key", -1, db.model(Membership.class).insert(membership(1, 1, "other")));
    }

    @Test
    public void insertAll_insertsEveryObjectAndReturnsTheirIds() throws Exception {
        long[] ids = people.insertAll(Arrays.asList(person("a@x.com", "A", "ZA", 1), person("b@x.com", "B", "ZA", 2), person("c@x.com", "C", "ZA", 3)));
        assertArrayEquals(new long[]{1, 2, 3}, ids);
        assertEquals(3, db.count("Person"));
        assertEquals(0, people.insertAll(null).length);
        assertEquals(0, people.insertAll(Collections.<Person>emptyList()).length);
    }

    @Test
    public void insertAndReturnUpdated_returnsTheStoredRow() throws Exception {
        Person stored = people.insertAndReturnUpdated(person("a@x.com", "Ann", "ZA", 31));
        assertEquals(1, stored.id);
        assertEquals("Ann", stored.name);
        assertEquals(Integer.valueOf(31), stored.age);
    }

    @Test
    public void insertAllAndReturnUpdated_returnsEachStoredRowInOrder() throws Exception {
        List<Person> stored = people.insertAllAndReturnUpdated(Arrays.asList(person("a@x.com", "A", "ZA", 1), person("b@x.com", "B", "ZA", 2)));
        assertEquals(2, stored.size());
        assertEquals("A", stored.get(0).name);
        assertEquals(2, stored.get(1).id);
    }

    @Test
    public void insert_cleansTrackedInstances() throws Exception {
        Tracked t = new Tracked();
        t.id = 1;
        t.a = "x";
        t.setDirty("a", true);
        db.model(Tracked.class).insert(t);
        assertFalse(t.isDirty());
    }

    // ---- queries -------------------------------------------------------------------------------------------------

    private void seedPeople() throws Exception {
        people.insertAll(Arrays.asList(
                person("a@x.com", "Ann", "ZA", 30),
                person("b@x.com", "Bob", "ZA", 10),
                person("c@x.com", "Cy", "UK", 20),
                person("d@x.com", "Di", "UK", null)));
    }

    @Test
    public void getAll_returnsEveryRowInInsertionOrder() throws Exception {
        seedPeople();
        assertEquals(Arrays.asList("Ann", "Bob", "Cy", "Di"), names(people.getAll()));
    }

    @Test
    public void getAll_filtersWithStringAndNumericArguments() throws Exception {
        seedPeople();
        assertEquals(Arrays.asList("Ann", "Bob"), names(people.getAll("country = ?", "ZA")));
        assertEquals(Collections.singletonList("Ann"), names(people.getAll("country = ? AND age > ?", "ZA", 20)));
        assertEquals(Collections.singletonList("Bob"), names(people.getAll("age = ?", 10)));
        assertTrue(people.getAll("country = ?", "FR").isEmpty());
    }

    @Test
    public void getAll_ordersAndLimits() throws Exception {
        seedPeople();
        assertEquals(Arrays.asList("Bob", "Cy"), names(people.getAll("age IS NOT NULL", null, "age ASC", 2)));
        assertEquals(Arrays.asList("Ann", "Cy", "Bob"), names(people.getAll("age IS NOT NULL", null, "age DESC", null)));
    }

    @Test
    public void getAll_nullableColumnReadsAsNull() throws Exception {
        seedPeople();
        assertNull(people.getFirst("name = ?", new Object[]{"Di"}, null).age);
    }

    @Test
    public void getFirst_honoursOrderAndReturnsNullWhenNothingMatches() throws Exception {
        seedPeople();
        assertEquals("Bob", people.getFirst("country = ?", new Object[]{"ZA"}, "age ASC").name);
        assertEquals("Ann", people.getFirst("country = ?", new Object[]{"ZA"}, "age DESC").name);
        assertNull(people.getFirst("country = ?", "FR"));
        assertEquals("Ann", people.getFirst(null).name);
    }

    @Test
    public void getFirstRaw_andGetAllRaw_mapRowsOfACustomQuery() throws Exception {
        seedPeople();
        assertEquals("Cy", people.getFirstRaw("SELECT * FROM Person WHERE age = ?", 20).name);
        assertEquals(Arrays.asList("Cy", "Di"), names(people.getAllRaw("SELECT * FROM Person WHERE country = ? ORDER BY id", "UK")));
        assertNull(people.getFirstRaw("SELECT * FROM Person WHERE age = ?", 999));
        assertTrue(people.getAllRaw("SELECT * FROM Person WHERE age = ?", 999).isEmpty());
    }

    @Test
    public void getAllRaw_mapsPartialColumnsByName() throws Exception {
        seedPeople();
        Person p = people.getFirstRaw("SELECT name, id FROM Person WHERE id = ?", 2);
        assertEquals("Bob", p.name);
        assertEquals(2, p.id);
        assertNull("unselected columns keep their defaults", p.email);
    }

    @Test
    public void exists_andGetCountWithAndWithoutArguments() throws Exception {
        seedPeople();
        assertTrue(people.exists("country = ?", "UK"));
        assertFalse(people.exists("country = ?", "FR"));
        assertTrue(people.exists("age > 25"));
        assertTrue(people.exists(null));
        assertEquals(2, people.getCount("country = ?", "ZA"));
        assertEquals(3, people.getCount("age > ?", 5));
        assertEquals(4, people.getCount("1 = 1"));
    }

    @Test
    public void getCount_withoutAWhereClauseCountsEverything() throws Exception {
        seedPeople();
        assertEquals(4, people.getCount(null));
    }

    // ---- update --------------------------------------------------------------------------------------------------

    @Test
    public void update_writesTheRowIdentifiedByThePrimaryKeyOnly() throws Exception {
        seedPeople();
        Person changed = person("b2@x.com", "Bee", "FR", null);
        changed.id = 2;

        assertEquals(1, people.update(changed));

        Person bee = people.getFirst("id = 2");
        assertEquals("Bee", bee.name);
        assertEquals("b2@x.com", bee.email);
        assertEquals("FR", bee.country);
        assertNull(bee.age);
        assertEquals("Ann", people.getFirst("id = 1").name);
    }

    @Test
    public void update_unknownKeyAffectsNothing() throws Exception {
        Person ghost = person("g@x.com", "G", "ZA", 1);
        ghost.id = 99;
        assertEquals(0, people.update(ghost));
        assertEquals(0, db.count("Person"));
    }

    @Test
    public void update_withExplicitWhereClause() throws Exception {
        seedPeople();
        Person patch = person("a@x.com", "Renamed", "ZA", 30);
        patch.id = 1;
        assertEquals(1, people.update(patch, "email = ?", "a@x.com"));
        assertEquals("Renamed", people.getFirst("id = 1").name);
    }

    @Test
    public void update_compositeKeyMatchesBothColumns() throws Exception {
        AndroidTestDatabase.TestModel<Membership> memberships = db.model(Membership.class);
        Membership a = membership(1, 1, "member");
        Membership b = membership(1, 2, "member");
        memberships.insert(a);
        memberships.insert(b);

        b.role = "admin";
        assertEquals(1, memberships.update(b));

        assertEquals("member", memberships.getFirst("groupId = ? AND userId = ?", 1, 1).role);
        assertEquals("admin", memberships.getFirst("groupId = ? AND userId = ?", 1, 2).role);
    }

    @Test
    public void update_trackedInstanceWritesOnlyDirtyColumns_andCleanOnesAreSkipped() throws Exception {
        db.execute("INSERT INTO Tracked (id, a, b) VALUES (1, 'old-a', 'old-b')");
        AndroidTestDatabase.TestModel<Tracked> tracked = db.model(Tracked.class);
        Tracked t = new Tracked();
        t.id = 1;
        t.a = "new-a";
        t.b = "ignored-b";

        assertEquals("clean instance is not written", -1, tracked.update(t));
        assertEquals("old-a", db.scalar("SELECT a FROM Tracked"));

        t.setDirty("a", true);
        assertEquals(1, tracked.update(t));
        Map<String, Object> row = db.rows("SELECT a, b FROM Tracked").get(0);
        assertEquals("new-a", row.get("a"));
        assertEquals("b was not marked dirty", "old-b", row.get("b"));
        assertFalse(t.isDirty());
    }

    // ---- delete --------------------------------------------------------------------------------------------------

    @Test
    public void delete_removesTheRowWithThatPrimaryKey() throws Exception {
        seedPeople();
        Person a = new Person();
        a.id = 1;
        assertEquals(1, people.delete(a));
        assertEquals(Arrays.asList("Bob", "Cy", "Di"), names(people.getAll()));
    }

    @Test
    public void delete_textKeyAndCompositeKey() throws Exception {
        Country za = new Country();
        za.code = "ZA";
        db.model(Country.class).insert(za);
        assertEquals(1, db.model(Country.class).delete(za));
        assertEquals(0, db.count("Country"));

        AndroidTestDatabase.TestModel<Membership> memberships = db.model(Membership.class);
        memberships.insert(membership(1, 1, "r"));
        memberships.insert(membership(1, 2, "r"));
        assertEquals(1, memberships.delete(membership(1, 2, "r")));
        assertEquals(1, db.count("Membership"));
    }

    @Test
    public void deleteAll_variants() throws Exception {
        seedPeople();
        assertEquals(2, people.deleteAll("country = ?", "ZA"));
        assertEquals(Arrays.asList("Cy", "Di"), names(people.getAll()));

        Person cy = people.getFirst("name = ?", "Cy");
        assertEquals(1, people.deleteAll(Collections.singletonList(cy)));

        assertEquals(1, people.deleteAll());
        assertEquals(0, db.count("Person"));
        assertEquals(0, people.deleteAll());
    }

    // ---- insertOrUpdate ------------------------------------------------------------------------------------------

    @Test
    public void insertOrUpdate_insertsANewRowAndWritesTheGeneratedKeyBack() throws Exception {
        Person p = person("a@x.com", "A", "ZA", 1);
        long rowId = people.insertOrUpdate(p);
        assertEquals(1, rowId);
        assertEquals("the generated key is set on the object", 1, p.id);
        assertEquals(1, db.count("Person"));
    }

    @Test
    public void insertOrUpdate_updatesTheExistingRowAndReturnsTheAffectedCount() throws Exception {
        Person p = person("a@x.com", "A", "ZA", 1);
        people.insertOrUpdate(p);
        p.name = "Renamed";

        assertEquals(1, people.insertOrUpdate(p));

        assertEquals(1, db.count("Person"));
        assertEquals("Renamed", people.getFirst("id = 1").name);
    }

    @Test
    public void insertOrUpdate_textPrimaryKey() throws Exception {
        AndroidTestDatabase.TestModel<Country> countries = db.model(Country.class);
        Country za = new Country();
        za.code = "ZA";
        za.name = "South Africa";
        countries.insertOrUpdate(za);
        za.name = "RSA";
        countries.insertOrUpdate(za);
        assertEquals(1, db.count("Country"));
        assertEquals("RSA", db.scalar("SELECT name FROM Country"));
    }

    @Test
    public void insertOrUpdate_compositePrimaryKey() throws Exception {
        AndroidTestDatabase.TestModel<Membership> memberships = db.model(Membership.class);
        Membership m = membership(1, 2, "member");
        memberships.insertOrUpdate(m);
        m.role = "admin";
        memberships.insertOrUpdate(m);
        memberships.insertOrUpdate(membership(1, 3, "member"));
        assertEquals(2, db.count("Membership"));
        assertEquals("admin", memberships.getFirst("groupId = 1 AND userId = 2").role);
    }

    @Test
    public void insertOrUpdate_doesNotRewriteACleanTrackedRow() throws Exception {
        db.execute("INSERT INTO Tracked (id, a, b) VALUES (1, 'old-a', 'old-b')");
        Tracked t = new Tracked();
        t.id = 1;
        t.a = "new-a";
        assertEquals(1, db.model(Tracked.class).insertOrUpdate(t));
        assertEquals("old-a", db.scalar("SELECT a FROM Tracked"));
    }

    @Test
    public void insertOrUpdateAll_insertsAndUpdatesInOneBlock() throws Exception {
        Person existing = person("a@x.com", "A", "ZA", 1);
        people.insert(existing);
        existing.id = 1;
        existing.name = "A2";
        Person fresh = person("b@x.com", "B", "ZA", 2);

        people.insertOrUpdateAll(Arrays.asList(existing, fresh));

        assertEquals(Arrays.asList("A2", "B"), names(people.getAll()));
    }

    // ---- ContentValues / cursor helpers --------------------------------------------------------------------------

    @Test
    public void putNullOrValue_writesNullForNullAndTheValueOtherwise() {
        android.content.ContentValues cv = new android.content.ContentValues();
        people.putNullOrValue(cv, "s", (String) null);
        people.putNullOrValue(cv, "i", (Integer) null);
        people.putNullOrValue(cv, "d", (Date) null);
        people.putNullOrValue(cv, "b", (byte[]) null);
        people.putNullOrValue(cv, "flag", (Boolean) null);
        for (String key : Arrays.asList("s", "i", "d", "b", "flag")) {
            assertTrue(key, cv.containsKey(key));
            assertNull(key, cv.get(key));
        }

        people.putNullOrValue(cv, "s", "text");
        people.putNullOrValue(cv, "i", Integer.valueOf(3));
        people.putNullOrValue(cv, "l", Long.valueOf(4));
        people.putNullOrValue(cv, "sh", Short.valueOf((short) 5));
        people.putNullOrValue(cv, "by", Byte.valueOf((byte) 6));
        people.putNullOrValue(cv, "f", Float.valueOf(1.5f));
        people.putNullOrValue(cv, "dbl", Double.valueOf(2.5));
        people.putNullOrValue(cv, "d", new Date(1234));
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(5678);
        people.putNullOrValue(cv, "c", cal);
        people.putNullOrValue(cv, "flag", Boolean.TRUE);
        assertEquals("text", cv.getAsString("s"));
        assertEquals(Integer.valueOf(3), cv.getAsInteger("i"));
        assertEquals(Long.valueOf(4), cv.getAsLong("l"));
        assertEquals(Short.valueOf((short) 5), cv.getAsShort("sh"));
        assertEquals(Byte.valueOf((byte) 6), cv.getAsByte("by"));
        assertEquals(Float.valueOf(1.5f), cv.getAsFloat("f"));
        assertEquals(Double.valueOf(2.5), cv.getAsDouble("dbl"));
        assertEquals("dates are stored as epoch millis", Long.valueOf(1234), cv.getAsLong("d"));
        assertEquals(Long.valueOf(5678), cv.getAsLong("c"));
        assertEquals(Boolean.TRUE, cv.getAsBoolean("flag"));
    }

    @Test
    public void getValueOrDefault_returnsTheCellOnlyWhenTheColumnNameMatchesAndTheCellIsNotNull() throws Exception {
        db.execute("INSERT INTO Person (email, name, country, age) VALUES ('a@x.com', 'Ann', 'ZA', 30)");
        db.execute("INSERT INTO Person (email, name, country, age) VALUES ('b@x.com', NULL, 'ZA', NULL)");
        android.database.Cursor c = db.driver().rawQuery("SELECT name, age FROM Person ORDER BY id", new Object[0]);
        try {
            assertTrue(c.moveToFirst());
            assertEquals("Ann", people.getValueOrDefault("name", c, 0, "dflt"));
            assertEquals(Integer.valueOf(30), people.getValueOrDefault("age", c, 1, Integer.valueOf(-1)));
            assertEquals(Long.valueOf(30), people.getValueOrDefault("age", c, 1, Long.valueOf(-1)));
            assertEquals("wrong name for the column index falls back", "dflt", people.getValueOrDefault("other", c, 0, "dflt"));
            assertEquals("null column name falls back", "dflt", people.getValueOrDefault(null, c, 0, "dflt"));

            assertTrue(c.moveToNext());
            assertEquals("SQL NULL falls back", "dflt", people.getValueOrDefault("name", c, 0, "dflt"));
            assertEquals(Integer.valueOf(-1), people.getValueOrDefault("age", c, 1, Integer.valueOf(-1)));
        } finally {
            c.close();
        }
    }

    @Test
    public void insertOrUpdateAll_returnsTheNumberOfRowsAffected() throws Exception {
        int affected = people.insertOrUpdateAll(Arrays.asList(person("a@x.com", "A", "ZA", 1), person("b@x.com", "B", "ZA", 2)));
        assertEquals(2, affected);
    }

    @Test
    public void roundTrip_enumWithOverriddenToString() throws Exception {
        reopenWith(Entities.ENUMS);
        AndroidTestDatabase enumDb = db;
        Entities.Ticket t = new Entities.Ticket();
        t.level = Entities.Level.HIGH;
        Entities.Ticket back = enumDb.model(Entities.Ticket.class).insertAndReturnUpdated(t);
        assertEquals(Entities.Level.HIGH, back.level);
    }

    /** Replaces the database created in setUp with a fresh one holding a different set of entities. */
    private void reopenWith(AndroidTestDatabase.Registrar registrar) throws Exception {
        db.close();
        OrmTestState.reset();
        db = AndroidTestDatabase.create(ApplicationProvider.getApplicationContext(), 1, registrar);
    }

    private List<String> names(List<Person> list) {
        List<String> out = new ArrayList<>();
        for (Person p : list)
            out.add(p.name);
        return out;
    }

    private List<Object> column(String sql) throws Exception {
        List<Object> out = new ArrayList<>();
        for (Map<String, Object> row : db.rows(sql))
            out.add(row.values().iterator().next());
        return out;
    }
}

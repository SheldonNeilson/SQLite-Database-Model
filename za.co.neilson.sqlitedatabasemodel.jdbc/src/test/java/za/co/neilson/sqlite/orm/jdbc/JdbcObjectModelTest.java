package za.co.neilson.sqlite.orm.jdbc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.OrmTestState;
import za.co.neilson.sqlite.orm.jdbc.Entities.AllTypes;
import za.co.neilson.sqlite.orm.jdbc.Entities.Child;
import za.co.neilson.sqlite.orm.jdbc.Entities.Country;
import za.co.neilson.sqlite.orm.jdbc.Entities.Membership;
import za.co.neilson.sqlite.orm.jdbc.Entities.Mode;
import za.co.neilson.sqlite.orm.jdbc.Entities.Parent;
import za.co.neilson.sqlite.orm.jdbc.Entities.Person;
import za.co.neilson.sqlite.orm.jdbc.Entities.Tracked;

/** Insert / update / delete / transactions / relationships through {@link JdbcObjectModel}, asserted with plain SQL. */
public class JdbcObjectModelTest {

    private JdbcTestDatabase db;
    private JdbcTestDatabase.TestModel<Person> people;

    @Before
    public void setUp() throws Exception {
        OrmTestState.reset();
        db = JdbcTestDatabase.create(1, Entities.ALL);
        people = db.model(Person.class);
    }

    @After
    public void tearDown() throws Exception {
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

    private Map<String, Object> onlyRow(String sql, Object... params) throws Exception {
        List<Map<String, Object>> rows = db.memory().rows(sql, params);
        assertEquals(sql, 1, rows.size());
        return rows.get(0);
    }

    // ---- insert --------------------------------------------------------------------------------------------------

    @Test
    public void insert_storesEveryFieldInItsNativeSqliteForm() throws Exception {
        AllTypes a = new AllTypes();
        a.text = "hello";
        a.tiny = 5;
        a.small = 300;
        a.number = 70000;
        a.big = 5_000_000_000L;
        a.ratio = 1.5f;
        a.precise = 2.25;
        a.date = new Date(1_700_000_000_000L);
        a.calendar = Calendar.getInstance();
        a.calendar.setTimeInMillis(1_600_000_000_000L);
        a.mode = Mode.B;
        a.optionalNumber = null;

        long id = db.model(AllTypes.class).insert(a);

        Map<String, Object> row = onlyRow("SELECT text, tiny, small, number, big, ratio, precise, date, calendar, mode, optionalNumber, typeof(date) td FROM AllTypes");
        assertEquals(1, id);
        assertEquals("hello", row.get("text"));
        assertEquals(5, ((Number) row.get("tiny")).intValue());
        assertEquals(300, ((Number) row.get("small")).intValue());
        assertEquals(70000, ((Number) row.get("number")).intValue());
        assertEquals(5_000_000_000L, ((Number) row.get("big")).longValue());
        assertEquals(1.5, ((Number) row.get("ratio")).doubleValue(), 0.0);
        assertEquals(2.25, ((Number) row.get("precise")).doubleValue(), 0.0);
        assertEquals("dates are stored as epoch milliseconds", 1_700_000_000_000L, ((Number) row.get("date")).longValue());
        assertEquals("integer", row.get("td"));
        assertEquals(1_600_000_000_000L, ((Number) row.get("calendar")).longValue());
        assertEquals("enums are stored by name", "B", row.get("mode"));
        assertNull(row.get("optionalNumber"));
    }

    @Test
    public void insert_assignsIncreasingAutoIncrementIdsAndReturnsThem() throws Exception {
        assertEquals(1, people.insert(person("a@x.com", "A", "ZA", 1)));
        assertEquals(2, people.insert(person("b@x.com", "B", "ZA", 2)));
        assertEquals(Arrays.asList(1L, 2L), column("SELECT id FROM Person ORDER BY id"));
    }

    @Test
    public void insert_ignoresAClientSuppliedAutoIncrementValue() throws Exception {
        Person p = person("a@x.com", "A", "ZA", 1);
        p.id = 77;
        assertEquals(1, people.insert(p));
        assertEquals(Collections.singletonList(1L), column("SELECT id FROM Person"));
    }

    @Test
    public void insert_naturalKeyIsWrittenAsGiven() throws Exception {
        Country za = new Country();
        za.code = "ZA";
        za.name = "South Africa";
        db.model(Country.class).insert(za);
        assertEquals("ZA", onlyRow("SELECT code, name FROM Country").get("code"));
    }

    @Test
    public void insert_nullInNotNullColumnIsRejected() throws Exception {
        try {
            people.insert(person("a@x.com", "A", null, 1));
            fail("expected SQLException");
        } catch (SQLException e) {
            assertTrue(e.getMessage(), e.getMessage().toUpperCase().contains("NOT NULL"));
        }
        assertEquals(0, db.memory().count("Person"));
    }

    @Test
    public void insert_duplicateUniqueValueIsRejected() throws Exception {
        people.insert(person("a@x.com", "A", "ZA", 1));
        try {
            people.insert(person("a@x.com", "Other", "ZA", 2));
            fail("expected SQLException");
        } catch (SQLException e) {
            assertTrue(e.getMessage(), e.getMessage().toUpperCase().contains("UNIQUE"));
        }
        assertEquals(1, db.memory().count("Person"));
    }

    @Test
    public void insert_duplicatePrimaryKeyIsRejected() throws Exception {
        Membership m = new Membership();
        m.groupId = 1;
        m.userId = 2;
        m.role = "admin";
        db.model(Membership.class).insert(m);
        try {
            db.model(Membership.class).insert(m);
            fail("expected SQLException");
        } catch (SQLException expected) {
        }
        assertEquals(1, db.memory().count("Membership"));
    }

    @Test
    public void insertAll_insertsEveryObjectAndReturnsTheirIds() throws Exception {
        long[] ids = people.insertAll(Arrays.asList(person("a@x.com", "A", "ZA", 1), person("b@x.com", "B", "ZA", 2), person("c@x.com", "C", "ZA", 3)));
        assertArrayEquals(new long[]{1, 2, 3}, ids);
        assertEquals(3, db.memory().count("Person"));
    }

    @Test
    public void insert_cleansTrackedInstances() throws Exception {
        Tracked t = new Tracked();
        t.id = 1;
        t.a = "x";
        t.setDirty("a", true);
        db.model(Tracked.class).insert(t);
        assertFalse(t.isDirty());
        assertEquals("x", onlyRow("SELECT a FROM Tracked").get("a"));
    }

    @Test
    public void insert_booleanIsStoredAsOneOrZero() throws Exception {
        AllTypes a = new AllTypes();
        a.flag = true;
        a.boxedFlag = Boolean.FALSE;
        db.model(AllTypes.class).insert(a);
        Map<String, Object> row = onlyRow("SELECT flag, boxedFlag FROM AllTypes");
        assertEquals("1", String.valueOf(row.get("flag")));
        assertEquals("0", String.valueOf(row.get("boxedFlag")));
    }

    @Test
    public void insert_blobKeepsItsBytes() throws Exception {
        AllTypes a = new AllTypes();
        a.blob = new byte[]{1, 2, 3, (byte) 0xFF};
        db.model(AllTypes.class).insert(a);
        Object stored = onlyRow("SELECT blob FROM AllTypes").get("blob");
        assertTrue("stored as " + stored, stored instanceof byte[]);
        assertArrayEquals(new byte[]{1, 2, 3, (byte) 0xFF}, (byte[]) stored);
    }

    @Test
    public void insert_textWithApostrophe() throws Exception {
        people.insert(person("o@x.com", "O'Brien", "IE", 40));
        assertEquals("O'Brien", onlyRow("SELECT name FROM Person").get("name"));
    }

    // ---- update --------------------------------------------------------------------------------------------------

    @Test
    public void update_writesTheRowIdentifiedByThePrimaryKeyOnly() throws Exception {
        people.insert(person("a@x.com", "A", "ZA", 1));
        people.insert(person("b@x.com", "B", "ZA", 2));
        Person changed = person("b2@x.com", "Bee", "UK", null);
        changed.id = 2;

        assertEquals(1, people.update(changed));

        assertEquals("A", db.memory().scalar("SELECT name FROM Person WHERE id = 1"));
        Map<String, Object> row = onlyRow("SELECT email, name, country, age FROM Person WHERE id = 2");
        assertEquals("b2@x.com", row.get("email"));
        assertEquals("Bee", row.get("name"));
        assertEquals("UK", row.get("country"));
        assertNull("null writes SQL NULL", row.get("age"));
    }

    @Test
    public void update_unknownKeyAffectsNothing() throws Exception {
        Person ghost = person("g@x.com", "G", "ZA", 1);
        ghost.id = 99;
        assertEquals(0, people.update(ghost));
        assertEquals(0, db.memory().count("Person"));
    }

    @Test
    public void update_withExplicitWhereClause() throws Exception {
        people.insert(person("a@x.com", "A", "ZA", 1));
        people.insert(person("b@x.com", "B", "UK", 2));
        Person patch = person("a@x.com", "Renamed", "ZA", 1);
        patch.id = 1;
        assertEquals(1, people.update(patch, "email = ?", "a@x.com"));
        assertEquals("Renamed", db.memory().scalar("SELECT name FROM Person WHERE id = 1"));
    }

    @Test
    public void update_compositeKeyMatchesBothColumns() throws Exception {
        JdbcTestDatabase.TestModel<Membership> memberships = db.model(Membership.class);
        Membership a = membership(1, 1, "member");
        Membership b = membership(1, 2, "member");
        memberships.insert(a);
        memberships.insert(b);

        b.role = "admin";
        assertEquals(1, memberships.update(b));

        assertEquals("member", db.memory().scalar("SELECT role FROM Membership WHERE groupId = 1 AND userId = 1"));
        assertEquals("admin", db.memory().scalar("SELECT role FROM Membership WHERE groupId = 1 AND userId = 2"));
    }

    @Test
    public void update_writesOnlyDirtyColumnsOfTrackedInstances() throws Exception {
        db.memory().execute("INSERT INTO Tracked (id, a, b) VALUES (1, 'old-a', 'old-b')");
        Tracked t = new Tracked();
        t.id = 1;
        t.a = "new-a";
        t.b = "ignored-b";
        t.setDirty("a", true);

        assertEquals(1, db.model(Tracked.class).update(t));

        Map<String, Object> row = onlyRow("SELECT a, b FROM Tracked");
        assertEquals("new-a", row.get("a"));
        assertEquals("b was not marked dirty", "old-b", row.get("b"));
        assertFalse(t.isDirty());
    }

    // ---- delete --------------------------------------------------------------------------------------------------

    @Test
    public void delete_removesTheRowWithThatPrimaryKey() throws Exception {
        Person a = person("a@x.com", "A", "ZA", 1);
        people.insert(a);
        people.insert(person("b@x.com", "B", "ZA", 2));
        a.id = 1;
        assertEquals(1, people.delete(a));
        assertEquals(Collections.singletonList(2L), column("SELECT id FROM Person"));
    }

    @Test
    public void delete_textKeyAndCompositeKey() throws Exception {
        Country za = new Country();
        za.code = "ZA";
        db.model(Country.class).insert(za);
        assertEquals(1, db.model(Country.class).delete(za));
        assertEquals(0, db.memory().count("Country"));

        JdbcTestDatabase.TestModel<Membership> memberships = db.model(Membership.class);
        memberships.insert(membership(1, 1, "r"));
        memberships.insert(membership(1, 2, "r"));
        assertEquals(1, memberships.delete(membership(1, 2, "r")));
        assertEquals(1, db.memory().count("Membership"));
    }

    @Test
    public void deleteAll_withWhereArgumentsReturnsTheDeletedCount() throws Exception {
        people.insertAll(Arrays.asList(person("a@x.com", "A", "ZA", 10), person("b@x.com", "B", "ZA", 20), person("c@x.com", "C", "UK", 30)));
        assertEquals(2, people.deleteAll("country = ?", "ZA"));
        assertEquals(Collections.singletonList(3L), column("SELECT id FROM Person"));
    }

    @Test
    public void deleteAll_withoutArgumentsEmptiesTheTable() throws Exception {
        people.insertAll(Arrays.asList(person("a@x.com", "A", "ZA", 10), person("b@x.com", "B", "ZA", 20)));
        assertEquals(2, people.deleteAll());
        assertEquals(0, db.memory().count("Person"));
    }

    @Test
    public void deleteAll_collectionDeletesEachObject() throws Exception {
        Person a = person("a@x.com", "A", "ZA", 1);
        Person b = person("b@x.com", "B", "ZA", 2);
        people.insertAll(Arrays.asList(a, b));
        a.id = 1;
        b.id = 2;
        assertEquals(2, people.deleteAll(Arrays.asList(a, b)));
        assertEquals(0, db.memory().count("Person"));
    }

    // ---- exists / count ------------------------------------------------------------------------------------------

    @Test
    public void exists_andGetCountWithNumericArguments() throws Exception {
        people.insertAll(Arrays.asList(person("a@x.com", "A", "ZA", 10), person("b@x.com", "B", "ZA", 20)));
        assertTrue(people.exists("age >= ?", 20));
        assertFalse(people.exists("age >= ?", 21));
        assertEquals(2, people.getCount("age > ?", 5));
        assertEquals(1, people.getCount("age > ?", 15));
    }

    @Test
    public void exists_andGetCountWithoutArguments() throws Exception {
        people.insert(person("a@x.com", "A", "ZA", 10));
        assertTrue(people.exists("age = 10"));
        assertEquals(1, people.getCount("age = 10"));
    }

    // ---- insertOrUpdate ------------------------------------------------------------------------------------------

    @Test
    public void insertOrUpdate_updatesAnExistingNumericKey() throws Exception {
        people.insert(person("a@x.com", "A", "ZA", 1));
        Person p = person("a@x.com", "Renamed", "ZA", 1);
        p.id = 1;

        assertEquals("update returns the affected row count", 1, people.insertOrUpdate(p));

        assertEquals(1, db.memory().count("Person"));
        assertEquals("Renamed", db.memory().scalar("SELECT name FROM Person"));
    }

    @Test
    public void insertOrUpdate_insertsWhenTheNumericKeyIsUnknown() throws Exception {
        Person p = person("a@x.com", "A", "ZA", 1);
        p.id = 5;
        long result = people.insertOrUpdate(p);
        assertTrue("row id", result > 0);
        assertEquals(1, db.memory().count("Person"));
    }

    @Test
    public void insertOrUpdate_writesTheGeneratedKeyBackToTheObject() throws Exception {
        Person p = person("a@x.com", "A", "ZA", 1);
        people.insertOrUpdate(p);
        assertEquals(1, p.id);
    }

    @Test
    public void insertOrUpdate_textPrimaryKey() throws Exception {
        Country za = new Country();
        za.code = "ZA";
        za.name = "South Africa";
        db.model(Country.class).insertOrUpdate(za);
        za.name = "RSA";
        db.model(Country.class).insertOrUpdate(za);
        assertEquals(1, db.memory().count("Country"));
        assertEquals("RSA", db.memory().scalar("SELECT name FROM Country"));
    }

    @Test
    public void insertOrUpdateAll_commitsEveryRowInOneTransaction() throws Exception {
        Person existing = person("a@x.com", "A", "ZA", 1);
        people.insert(existing);
        existing.id = 1;
        existing.name = "A2";
        Person fresh = person("b@x.com", "B", "ZA", 2);
        fresh.id = 2;

        people.insertOrUpdateAll(Arrays.asList(existing, fresh));

        assertEquals(2, db.memory().count("Person"));
        assertEquals("A2", db.memory().scalar("SELECT name FROM Person WHERE id = 1"));
    }

    @Test
    public void insertOrUpdateAll_failureDoesNotLeaveATransactionOpen() throws Exception {
        people.insert(person("dup@x.com", "A", "ZA", 1));
        Person clash = person("dup@x.com", "B", "ZA", 2);
        clash.id = 50;
        try {
            people.insertOrUpdateAll(Collections.singletonList(clash));
            fail("expected SQLException");
        } catch (SQLException expected) {
        }
        db.getDatabaseDriverInterface().execute("begin");
        db.getDatabaseDriverInterface().execute("end");
    }

    // ---- relationships -------------------------------------------------------------------------------------------

    @Test
    public void relationship_isDiscoveredFromTheForeignKeyAnnotation() {
        ObjectModel<Child, ?, ?> children = db.getObjectModel(Child.class);
        assertEquals(1, children.getObjectModelColumn("parentId").getRelationships().size());
    }

    @Test
    public void cascadeDelete_removesDependentRowsOfTheDeletedParentsOnly() throws Exception {
        db.memory().execute("INSERT INTO Parent (id, name) VALUES (1, 'p1'), (2, 'p2')");
        db.memory().execute("INSERT INTO Child (parentId, label) VALUES (1, 'a'), (1, 'b'), (2, 'c')");

        int deleted = db.model(Parent.class).deleteAll("id = ?", 1);

        assertEquals("1 parent + 2 children", 3, deleted);
        assertEquals(Collections.singletonList("c"), column("SELECT label FROM Child"));
        assertEquals(Collections.singletonList(2L), column("SELECT id FROM Parent"));
    }

    @Test
    public void cascadeDelete_canBeDisabled() throws Exception {
        db.memory().execute("INSERT INTO Parent (id, name) VALUES (1, 'p1')");
        db.memory().execute("INSERT INTO Child (parentId, label) VALUES (1, 'a')");
        JdbcTestDatabase.TestModel<Parent> parents = db.model(Parent.class);
        parents.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);

        assertEquals(1, parents.deleteAll("id = ?", 1));
        assertEquals("orphans stay when cascade is off", 1, db.memory().count("Child"));
    }

    @Test
    public void cascadeDelete_parentWithoutChildrenJustDeletesTheParent() throws Exception {
        db.memory().execute("INSERT INTO Parent (id, name) VALUES (1, 'p1')");
        assertEquals(1, db.model(Parent.class).deleteAll("id = ?", 1));
    }

    @Test
    public void insert_parentWithChildrenAlsoSavesTheChildren() throws Exception {
        Parent parent = new Parent();
        parent.name = "p";
        Child child = new Child();
        child.label = "c";
        parent.children = new ArrayList<>(Collections.singletonList(child));

        db.model(Parent.class).insert(parent);

        assertEquals(1, db.memory().count("Child"));
        assertEquals(1L, ((Number) db.memory().scalar("SELECT parentId FROM Child")).longValue());
    }

    // ---- reads ---------------------------------------------------------------------------------------------------

    @Test
    public void getFirst_andGetAll_onAnEmptyTableReturnNothing() throws Exception {
        assertNull(people.getFirst(null));
        assertNull(people.getFirst("id = ?", 1));
        assertTrue(people.getAll().isEmpty());
        assertTrue(people.getAll("age > ?", 1).isEmpty());
    }

    @Test
    public void getFirst_returnsTheStoredEntity() throws Exception {
        people.insert(person("a@x.com", "Ann", "ZA", 31));
        Person back = people.getFirst("email = ?", new Object[]{"a@x.com"}, null);
        assertNotNull(back);
        assertEquals(1, back.id);
        assertEquals("Ann", back.name);
        assertEquals(Integer.valueOf(31), back.age);
    }

    @Test
    public void getAll_filtersOrdersAndLimits() throws Exception {
        people.insertAll(Arrays.asList(person("a@x.com", "A", "ZA", 30), person("b@x.com", "B", "ZA", 10), person("c@x.com", "C", "UK", 20)));
        List<Person> result = people.getAll("country = ?", new Object[]{"ZA"}, "age ASC", 5);
        assertEquals(2, result.size());
        assertEquals("B", result.get(0).name);
        assertEquals("A", result.get(1).name);
    }

    @Test
    public void roundTrip_everySupportedType() throws Exception {
        AllTypes a = new AllTypes();
        a.text = "hello";
        a.flag = true;
        a.boxedFlag = Boolean.FALSE;
        a.tiny = 5;
        a.small = 300;
        a.number = 70000;
        a.big = 5_000_000_000L;
        a.ratio = 1.5f;
        a.precise = 2.25;
        a.date = new Date(1_700_000_000_000L);
        a.calendar = Calendar.getInstance();
        a.calendar.setTimeInMillis(1_600_000_000_000L);
        a.blob = new byte[]{1, 2, 3};
        a.mode = Mode.B;
        a.optionalNumber = 42;
        JdbcTestDatabase.TestModel<AllTypes> model = db.model(AllTypes.class);

        AllTypes back = model.insertAndReturnUpdated(a);

        assertEquals("hello", back.text);
        assertTrue(back.flag);
        assertEquals(Boolean.FALSE, back.boxedFlag);
        assertEquals(5, back.tiny);
        assertEquals(300, back.small);
        assertEquals(70000, back.number);
        assertEquals(5_000_000_000L, back.big);
        assertEquals(1.5f, back.ratio, 0f);
        assertEquals(2.25, back.precise, 0.0);
        assertEquals(a.date, back.date);
        assertEquals(1_600_000_000_000L, back.calendar.getTimeInMillis());
        assertArrayEquals(a.blob, back.blob);
        assertEquals(Mode.B, back.mode);
        assertEquals(Integer.valueOf(42), back.optionalNumber);
    }

    @Test
    public void roundTrip_nullsStayNull() throws Exception {
        AllTypes back = db.model(AllTypes.class).insertAndReturnUpdated(new AllTypes());
        assertNull(back.text);
        assertNull(back.date);
        assertNull(back.boxedFlag);
        assertNull(back.optionalNumber);
        assertNull(back.mode);
    }

    private Membership membership(int group, int user, String role) {
        Membership m = new Membership();
        m.groupId = group;
        m.userId = user;
        m.role = role;
        return m;
    }

    private List<Object> column(String sql) throws Exception {
        List<Object> out = new ArrayList<>();
        for (Map<String, Object> row : db.memory().rows(sql)) {
            Object v = row.values().iterator().next();
            out.add(v instanceof Number && !(v instanceof Double) ? Long.valueOf(((Number) v).longValue()) : v);
        }
        return out;
    }
}

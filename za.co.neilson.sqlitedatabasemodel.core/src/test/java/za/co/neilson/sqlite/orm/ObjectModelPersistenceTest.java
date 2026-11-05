package za.co.neilson.sqlite.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;

/** Insert / update / delete / query orchestration in {@link ObjectModel}, observed through the recording driver. */
public class ObjectModelPersistenceTest {

    static class Person {
        @PrimaryKey(autoIncrement = true)
        long id;
        String name;
        Integer age;

        Person() {
        }

        Person(long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    static class Keyed {
        @PrimaryKey
        int a;
        @PrimaryKey
        String b;
        String payload;
    }

    static class NoKey {
        String value;
    }

    static class Tracked implements InstanceStateTrackable {
        @PrimaryKey
        String id;
        String text;
        boolean dirty;

        @Override
        public boolean isDirty() {
            return dirty;
        }

        @Override
        public boolean isDirty(String fieldName) {
            return dirty;
        }

        @Override
        public void setDirty(String fieldName, boolean dirty) {
            this.dirty = dirty;
        }

        @Override
        public void setClean() {
            dirty = false;
        }
    }

    static class Dept {
        @PrimaryKey(autoIncrement = true)
        long id;
        String name;
    }

    static class Employee {
        @PrimaryKey(autoIncrement = true)
        long id;
        @ForeignKey(table = "Dept", column = "id")
        long deptId;
    }

    static class Node {
        @PrimaryKey(autoIncrement = true)
        long id;
        @ForeignKey(table = "Node", column = "id")
        Long parentId;
    }

    private FakeDriver driver;
    private FakeDatabaseModel db;
    private ObjectModel<Person, FakeRow, java.util.Map<String, Object>> people;

    @Before
    public void setUp() throws Exception {
        OrmTestState.reset();
        driver = new FakeDriver();
        db = FakeDatabaseModel.createExisting(driver, 1, (d, models) -> {
            models.put(Person.class, new FakeObjectModel<Person>(d) {});
            models.put(Keyed.class, new FakeObjectModel<Keyed>(d) {});
            models.put(NoKey.class, new FakeObjectModel<NoKey>(d) {});
            models.put(Tracked.class, new FakeObjectModel<Tracked>(d) {});
            models.put(Dept.class, new FakeObjectModel<Dept>(d) {});
            models.put(Employee.class, new FakeObjectModel<Employee>(d) {});
            models.put(Node.class, new FakeObjectModel<Node>(d) {});
        });
        people = db.getObjectModel(Person.class);
    }

    // ---- delete --------------------------------------------------------------------------------------------------

    @Test
    public void delete_usesThePrimaryKeyInATransaction() throws Exception {
        driver.deleteResult = 1;
        int deleted = people.delete(new Person(7, "Ann", 30));

        assertEquals(1, deleted);
        assertEquals(Arrays.asList(
                "beginTransaction",
                "delete Person where=\"id\" = ? args=[7]",
                "setTransactionSuccessful",
                "endTransaction"), driver.events);
    }

    @Test
    public void delete_compositeKeyMatchesAllKeyColumns() throws Exception {
        Keyed k = new Keyed();
        k.a = 3;
        k.b = "x";
        db.getObjectModel(Keyed.class).delete(k);
        assertEquals("delete Keyed where=\"a\" = ? AND \"b\" = ? args=[3, x]", driver.eventsStartingWith("delete").get(0));
    }

    @Test
    public void delete_requiresAPrimaryKey() throws Exception {
        try {
            db.getObjectModel(NoKey.class).delete(new NoKey());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("primary key"));
        }
        assertTrue(driver.eventsStartingWith("delete").isEmpty());
    }

    @Test
    public void deleteAll_withoutArgumentsDeletesEveryRow() throws Exception {
        driver.deleteResult = 4;
        assertEquals(4, people.deleteAll());
        assertEquals("delete Person where=1 args=null", driver.eventsStartingWith("delete").get(0));
    }

    @Test
    public void deleteAll_collectionDeletesEachInOneBeginEndBlock() throws Exception {
        driver.deleteResult = 1;
        int total = people.deleteAll(Arrays.asList(new Person(1, "a", 1), new Person(2, "b", 2)));
        assertEquals(2, total);
        assertEquals("execute begin", driver.events.get(0));
        assertEquals("execute end", driver.events.get(driver.events.size() - 1));
        assertEquals(2, driver.eventsStartingWith("delete").size());
    }

    @Test
    public void deleteAll_endsTheTransactionEvenWhenTheDriverFails() throws Exception {
        FakeDriver failing = new FakeDriver() {
            @Override
            public int delete(String tableName, String whereClause, Object[] whereArgs) {
                throw new IllegalStateException("boom");
            }
        };
        OrmTestState.reset();
        FakeDatabaseModel failingDb = FakeDatabaseModel.createExisting(failing, 1, (d, models) ->
                models.put(Person.class, new FakeObjectModel<Person>(d) {}));
        try {
            failingDb.getObjectModel(Person.class).deleteAll("1");
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
        }
        assertEquals(Arrays.asList("beginTransaction", "endTransaction"), failing.events);
    }

    // ---- cascade delete -------------------------------------------------------------------------------------------

    @Test
    public void cascadeDelete_removesChildrenBeforeTheParent() throws Exception {
        driver.columnValuesResult = Arrays.asList("1", "2");
        db.getObjectModel(Dept.class).deleteAll("name = ?", "x");

        List<String> deletes = driver.eventsStartingWith("delete");
        assertEquals(2, deletes.size());
        assertTrue(deletes.get(0), deletes.get(0).startsWith("delete Employee where=deptId IN ("));
        assertTrue(deletes.get(0), deletes.get(0).contains("1") && deletes.get(0).contains("2"));
        assertEquals("delete Dept where=name = ? args=[x]", deletes.get(1));
    }

    @Test
    public void cascadeDelete_skippedWhenNoParentRowMatches() throws Exception {
        driver.columnValuesResult = Collections.emptyList();
        db.getObjectModel(Dept.class).deleteAll("name = ?", "none");
        assertEquals(Collections.singletonList("delete Dept where=name = ? args=[none]"), driver.eventsStartingWith("delete"));
    }

    @Test
    public void cascadeDelete_canBeSwitchedOff() throws Exception {
        driver.columnValuesResult = Arrays.asList("1");
        ObjectModel<Dept, FakeRow, java.util.Map<String, Object>> depts = db.getObjectModel(Dept.class);
        depts.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);
        depts.deleteAll("1");
        assertEquals(1, driver.eventsStartingWith("delete").size());
    }

    @Test(timeout = 5000)
    public void cascadeDelete_selfReferenceTerminatesAndExcludesTheNodeBeingDeleted() throws Exception {
        driver.columnValuesResult = Arrays.asList("1");
        db.getObjectModel(Node.class).deleteAll("id = ?", 1);

        List<String> deletes = driver.eventsStartingWith("delete");
        assertEquals("child delete then the node itself", 2, deletes.size());
        assertTrue(deletes.get(0), deletes.get(0).contains("parentId IN"));
        assertTrue("the node must not be treated as its own child: " + deletes.get(0), deletes.get(0).contains("NOT IN"));
        assertEquals("delete Node where=id = ? args=[1]", deletes.get(1));
    }

    // ---- update --------------------------------------------------------------------------------------------------

    @Test
    public void update_writesAllColumnsWhereThePrimaryKeyMatches() throws Exception {
        driver.updateResult = 1;
        assertEquals(1, people.update(new Person(7, "Ann", null)));
        assertEquals("update Person {id=7, name=Ann, age=null} where=\"id\" = ? args=[7]", driver.eventsStartingWith("update").get(0));
    }

    @Test
    public void update_requiresAPrimaryKey() throws Exception {
        try {
            db.getObjectModel(NoKey.class).update(new NoKey());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("primary key"));
        }
    }

    @Test
    public void update_skipsCleanTrackedInstances() throws Exception {
        Tracked clean = new Tracked();
        clean.id = "k";
        assertEquals(-1, db.getObjectModel(Tracked.class).update(clean));
        assertTrue(driver.eventsStartingWith("update").isEmpty());
    }

    @Test
    public void update_writesDirtyTrackedInstances() throws Exception {
        Tracked dirty = new Tracked();
        dirty.id = "k";
        dirty.dirty = true;
        assertEquals(1, db.getObjectModel(Tracked.class).update(dirty));
        assertEquals(1, driver.eventsStartingWith("update").size());
    }

    // ---- insertOrUpdate --------------------------------------------------------------------------------------------

    @Test
    public void insertOrUpdate_insertsWhenNoRowMatches_andReadsBackTheGeneratedKey() throws Exception {
        driver.existsResult = false;
        driver.insertResult = 5;
        driver.queryResult = new FakeRow("id", 5L);
        Person p = new Person(0, "Bob", 41);

        long rowId = people.insertOrUpdate(p);

        assertEquals(5, rowId);
        assertEquals("autoincrement key is left out of the insert", "insert Person {name=Bob, age=41}", driver.eventsStartingWith("insert").get(0));
        assertEquals("the new key is written back to the object", 5, p.id);
        assertTrue(driver.eventsStartingWith("query").get(0).contains("where= ROWID = ?"));
    }

    @Test
    public void insertOrUpdate_updatesWhenARowMatches() throws Exception {
        driver.existsResult = true;
        driver.updateResult = 1;
        Person p = new Person(7, "Bob", 41);

        assertEquals(1, people.insertOrUpdate(p));

        assertTrue(driver.eventsStartingWith("insert").isEmpty());
        assertEquals("exists Person where=\"id\" = ? args=[7]", driver.eventsStartingWith("exists").get(0));
        assertEquals("update Person {id=7, name=Bob, age=41} where=\"id\" = ? args=[7]", driver.eventsStartingWith("update").get(0));
    }

    @Test
    public void insertOrUpdate_doesNotRewriteACleanTrackedRow() throws Exception {
        driver.existsResult = true;
        Tracked clean = new Tracked();
        clean.id = "k";
        assertEquals(1, db.getObjectModel(Tracked.class).insertOrUpdate(clean));
        assertTrue(driver.eventsStartingWith("update").isEmpty());
    }

    @Test
    public void insertOrUpdate_requiresAPrimaryKey() throws Exception {
        try {
            db.getObjectModel(NoKey.class).insertOrUpdate(new NoKey());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("primary key"));
        }
    }

    @Test
    public void insertOrUpdate_withCustomWhereClauseTestsThatClause() throws Exception {
        driver.existsResult = true;
        people.insertOrUpdate(new Person(0, "Zed", 1), "name = ?", "Zed");
        assertEquals("exists Person where=name = ? args=[Zed]", driver.eventsStartingWith("exists").get(0));
        assertEquals("update Person {id=0, name=Zed, age=1} where=name = ? args=[Zed]", driver.eventsStartingWith("update").get(0));
    }

    @Test
    public void insertOrUpdateAll_wrapsEverythingInBeginEnd() throws Exception {
        driver.existsResult = false;
        driver.insertResult = 3;
        driver.queryResult = new FakeRow("id", 3L);
        people.insertOrUpdateAll(Arrays.asList(new Person(0, "a", 1), new Person(0, "b", 2)));

        assertEquals("execute begin", driver.events.get(0));
        assertEquals("execute end", driver.events.get(driver.events.size() - 1));
        assertEquals(2, driver.eventsStartingWith("insert").size());
    }

    @Test
    public void insertOrUpdateAll_returnsTheNumberOfRowsAffected() throws Exception {
        driver.existsResult = false;
        driver.insertResult = 3;
        driver.queryResult = new FakeRow("id", 3L);
        assertEquals(2, people.insertOrUpdateAll(Arrays.asList(new Person(0, "a", 1), new Person(0, "b", 2))));
    }

    @Test
    public void insertOrUpdateAll_closesTheTransactionWhenAnItemFails() throws Exception {
        driver.failInsert = true;
        try {
            people.insertOrUpdateAll(Arrays.asList(new Person(0, "a", 1)));
            fail("expected SQLException");
        } catch (SQLException expected) {
        }
        assertTrue("transaction must be ended or rolled back: " + driver.executed,
                driver.executed.contains("end") || driver.executed.contains("rollback"));
    }

    // ---- queries -------------------------------------------------------------------------------------------------

    @Test
    public void getFirst_queriesOneRowWithTheMappedColumnsAndBuildsTheInstance() throws Exception {
        driver.queryResult = new FakeRow("id", 9L, "name", "Cy", "age", 33);

        Person p = people.getFirst("id = ?", new Object[]{9}, "name");

        assertNotNull(p);
        assertEquals(9, p.id);
        assertEquals("Cy", p.name);
        assertEquals(Integer.valueOf(33), p.age);
        assertEquals("query Person cols=[id, name, age] where=id = ? args=[9] order=name limit=1", driver.eventsStartingWith("query").get(0));
    }

    @Test
    public void getFirst_noRowGivesNull() throws Exception {
        driver.queryResult = null;
        assertNull(people.getFirst("id = 1"));
    }

    @Test
    public void getFirst_leavesUnmatchedColumnsOutOfTheInstance() throws Exception {
        driver.queryResult = new FakeRow("name", "Only");
        Person p = people.getFirst(null);
        assertEquals("Only", p.name);
        assertEquals(0, p.id);
        assertNull(p.age);
    }

    @Test
    public void insertAndReturnUpdated_readsTheRowBackByRowId() throws Exception {
        driver.insertResult = 12;
        driver.queryResult = new FakeRow("id", 12L, "name", "Dee", "age", 5);

        Person stored = people.insertAndReturnUpdated(new Person(0, "Dee", 5));

        assertEquals(12, stored.id);
        assertEquals("query Person cols=[id, name, age] where=ROWID = ? args=[12] order=null limit=1", driver.eventsStartingWith("query").get(0));
    }

    @Test
    public void exists_andGetCountDelegateToTheDriverWithTheTableName() throws Exception {
        driver.existsResult = true;
        assertTrue(people.exists("name = ?", "x"));
        assertEquals(0, people.getCount("age > ?", 3));
        assertEquals("exists Person where=name = ? args=[x]", driver.eventsStartingWith("exists").get(0));
        assertEquals("count Person where=age > ?", driver.eventsStartingWith("count").get(0));
    }

    // ---- misc ----------------------------------------------------------------------------------------------------

    @Test
    public void castToObjectType_returnsTheSameInstance() {
        Person p = new Person();
        assertEquals(p, ObjectModel.castToObjectType(p, Person.class));
    }

    @Test
    public void castToObjectType_wrapsClassCastException() {
        try {
            ObjectModel.castToObjectType("text", Person.class);
            fail("expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof ClassCastException);
        }
    }

    @Test
    public void relationshipManagementDefaultsToFull_andFlagsAreIndependentBits() {
        assertEquals(ObjectModel.RELATIONSHIP_MANAGEMENT_FULL, people.getRelationshipManagementMode());
        int all = ObjectModel.RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED | ObjectModel.RELATIONSHIP_MANAGEMENT_INSERT_RELATED
                | ObjectModel.RELATIONSHIP_MANAGEMENT_UPDATE_RELATED | ObjectModel.RELATIONSHIP_MANAGEMENT_CASCADE_DELETE;
        assertEquals(15, all);
        assertEquals(all, ObjectModel.RELATIONSHIP_MANAGEMENT_FULL);
        assertEquals(0, ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);
        people.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED);
        assertEquals(1, people.getRelationshipManagementMode());
    }

    @Test
    public void modelsAreEqualWhenTheyMapTheSameTableAndColumns() throws Exception {
        ObjectModel<Person, FakeRow, java.util.Map<String, Object>> other = new FakeObjectModel<Person>(db) {};
        assertEquals(people, other);
        assertTrue(!people.equals(db.getObjectModel(Dept.class)));
        assertTrue(!people.equals("Person"));
    }

    @Test
    public void getDatabaseModel_failsClearlyWhenNoneWasSupplied() throws Exception {
        ObjectModel<Dept, FakeRow, java.util.Map<String, Object>> detached = new FakeObjectModel<Dept>(null) {};
        try {
            detached.getDatabaseModel();
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("not been initialized"));
        }
    }
}

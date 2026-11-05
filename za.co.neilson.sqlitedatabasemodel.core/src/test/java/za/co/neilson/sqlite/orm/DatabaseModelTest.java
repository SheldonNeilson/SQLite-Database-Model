package za.co.neilson.sqlite.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import za.co.neilson.sqlite.orm.annotations.PrimaryKey;

/** Schema creation, version upgrade and rebuild flow of {@link DatabaseModel}. */
public class DatabaseModelTest {

    static class Widget {
        @PrimaryKey(autoIncrement = true)
        long id;
        String name;
    }

    static class Gadget {
        @PrimaryKey(autoIncrement = true)
        long id;
    }

    private FakeDriver driver;

    @Before
    public void setUp() {
        OrmTestState.reset();
        driver = new FakeDriver();
    }

    private static final FakeDatabaseModel.Registrar WIDGET_AND_GADGET = (db, models) -> {
        models.put(Widget.class, new FakeObjectModel<Widget>(db) {});
        models.put(Gadget.class, new FakeObjectModel<Gadget>(db) {});
    };

    @Test
    public void newDatabase_createsEveryTableInRegistrationOrderInOneTransaction() throws Exception {
        driver.fileExists = false;
        FakeDatabaseModel db = FakeDatabaseModel.create(driver, 3, WIDGET_AND_GADGET);

        assertEquals("DatabaseInfo is registered first, then the subclass models",
                Arrays.asList("DatabaseInfo", "Widget", "Gadget"), tableNamesCreated());
        assertEquals("connect", driver.events.get(0));
        assertEquals("beginTransaction", driver.events.get(1));
        List<String> tail = driver.events.subList(driver.events.size() - 3, driver.events.size());
        assertEquals(Arrays.asList("setTransactionSuccessful", "endTransaction", "disconnect"), tail);
        assertEquals("onInsertDefaultValues is called once", 1, db.defaultValuesInserted);
        assertFalse("the connection is released after creation", driver.isConnected());
    }

    @Test
    public void newDatabase_recordsTheSchemaVersionAndCreationDate() throws Exception {
        driver.fileExists = false;
        long before = System.currentTimeMillis();
        FakeDatabaseModel.create(driver, 7, WIDGET_AND_GADGET);
        long after = System.currentTimeMillis();

        String insert = driver.eventsStartingWith("insert DatabaseInfo").get(0);
        assertTrue(insert, insert.contains("version=7"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("createdDate=(\\d+), accessedDate=(\\d+)").matcher(insert);
        assertTrue(insert, m.find());
        long created = Long.parseLong(m.group(1));
        assertTrue(created >= before && created <= after);
        assertEquals("accessed starts equal to created", created, Long.parseLong(m.group(2)));
    }

    @Test
    public void newDatabase_failedTableCreationDoesNotCommit() throws Exception {
        driver.fileExists = false;
        driver.failOnSqlContaining = "\"Gadget\"";
        FakeDatabaseModel.create(driver, 1, WIDGET_AND_GADGET);

        assertFalse(driver.events.contains("setTransactionSuccessful"));
        assertTrue(driver.events.contains("endTransaction"));
        assertTrue("connection is always released", driver.events.contains("disconnect"));
        assertTrue("no DatabaseInfo row for a half-built schema", driver.eventsStartingWith("insert").isEmpty());
    }

    @Test
    public void existingDatabaseAtCurrentVersion_isLeftAlone() throws Exception {
        driver.fileExists = true;
        driver.queryResult = new FakeRow("version", 4, "createdDate", 5L, "accessedDate", 6L);
        FakeDatabaseModel.create(driver, 4, WIDGET_AND_GADGET);

        assertTrue("no DDL", driver.executed.isEmpty());
        assertTrue(driver.eventsStartingWith("insert").isEmpty());
        assertEquals("checks the stored version, then releases the connection",
                Arrays.asList("connect", "query DatabaseInfo cols=[version, createdDate, accessedDate] where=null args=null order=null limit=1", "disconnect"),
                driver.events);
    }

    @Test
    public void existingDatabaseAtOlderVersion_dropsAndRecreatesAllTables() throws Exception {
        driver.fileExists = true;
        driver.queryResult = new FakeRow("version", 2, "createdDate", 5L, "accessedDate", 6L);
        FakeDatabaseModel db = FakeDatabaseModel.create(driver, 3, WIDGET_AND_GADGET);

        assertEquals("begin;", driver.executed.get(0));
        assertEquals("end;", driver.executed.get(driver.executed.size() - 1));
        for (String table : Arrays.asList("DatabaseInfo", "Widget", "Gadget")) {
            int drop = driver.executed.indexOf("DROP TABLE IF EXISTS \"" + table + "\";");
            assertTrue("drop " + table, drop >= 0);
            assertTrue("create after drop for " + table, driver.executed.get(drop + 1).startsWith("CREATE TABLE IF NOT EXISTS \"" + table + "\""));
        }
        assertTrue("new version row written", driver.eventsStartingWith("insert DatabaseInfo").get(0).contains("version=3"));
        assertEquals(1, db.defaultValuesInserted);
        assertTrue(driver.events.contains("disconnect"));
    }

    @Test
    public void existingDatabaseAtNewerVersion_isAlsoRebuilt() throws Exception {
        driver.fileExists = true;
        driver.queryResult = new FakeRow("version", 9, "createdDate", 5L, "accessedDate", 6L);
        FakeDatabaseModel.create(driver, 3, WIDGET_AND_GADGET);
        assertTrue("a downgrade does not silently keep an incompatible schema", driver.executed.contains("DROP TABLE IF EXISTS \"Widget\";"));
    }

    @Test
    public void upgrade_endsTheTransactionWhenCreationFails() throws Exception {
        driver.fileExists = false;
        FakeDatabaseModel db = FakeDatabaseModel.create(driver, 1, WIDGET_AND_GADGET);
        driver.executed.clear();
        driver.failOnSqlContaining = "CREATE TABLE IF NOT EXISTS \"Gadget\"";
        try {
            db.callUpgrade(1);
        } catch (java.sql.SQLException expected) {
        }
        assertTrue(driver.executed.contains("end;") || driver.executed.contains("rollback;"));
    }

    @Test
    public void rebuild_dropsAndRecreatesEvenAfterTheTablesWereAlreadyCreated() throws Exception {
        driver.fileExists = false;
        FakeDatabaseModel db = FakeDatabaseModel.create(driver, 1, WIDGET_AND_GADGET);
        driver.executed.clear();
        driver.events.clear();

        db.callRebuild();

        assertEquals("begin;", driver.executed.get(0));
        assertTrue(driver.executed.contains("DROP TABLE IF EXISTS \"Widget\";"));
        assertEquals(3, countStartingWith("CREATE TABLE IF NOT EXISTS"));
        assertEquals("end;", driver.executed.get(driver.executed.size() - 1));
        assertEquals("version row re-inserted", 1, driver.eventsStartingWith("insert DatabaseInfo").size());
    }

    @Test
    public void rebuild_continuesWithRemainingTablesWhenOneFails() throws Exception {
        driver.fileExists = false;
        FakeDatabaseModel db = FakeDatabaseModel.create(driver, 1, WIDGET_AND_GADGET);
        driver.executed.clear();
        driver.failOnSqlContaining = "DROP TABLE IF EXISTS \"Widget\"";

        db.callRebuild();

        assertTrue(driver.executed.contains("DROP TABLE IF EXISTS \"Gadget\";"));
    }

    @Test
    public void objectModelLookup_isByEntityType() throws Exception {
        FakeDatabaseModel db = FakeDatabaseModel.create(driver, 1, WIDGET_AND_GADGET);
        assertNotNull(db.getObjectModel(Widget.class));
        assertEquals(Widget.class, db.getObjectModel(Widget.class).getObjectType());
        assertNotNull("DatabaseInfo model is always present", db.getObjectModel(DatabaseInfo.class));
        assertEquals(null, db.getObjectModel(String.class));
        assertEquals(3, db.getObjectModels().size());
    }

    @Test
    public void driverIsSharedPerDatabaseModelClass() throws Exception {
        FakeDatabaseModel first = FakeDatabaseModel.create(driver, 1, WIDGET_AND_GADGET);
        FakeDriver other = new FakeDriver();
        FakeDatabaseModel second = FakeDatabaseModel.create(other, 1, WIDGET_AND_GADGET);
        assertSame(driver, first.getDatabaseDriverInterface());
        assertSame("a second instance of the same DatabaseModel class reuses the first driver", driver, second.getDatabaseDriverInterface());
    }

    @Test
    public void connectionHelpersDelegateToTheDriver() throws Exception {
        FakeDatabaseModel db = FakeDatabaseModel.createExisting(driver, 1, WIDGET_AND_GADGET);
        assertFalse(db.isConnected());
        db.connect();
        assertTrue(db.isConnected());
        db.disconnect();
        assertFalse(db.isConnected());
    }

    @Test
    public void journalModeDefaultsAndCanBeChanged() throws Exception {
        FakeDatabaseModel db = FakeDatabaseModel.createExisting(driver, 1, WIDGET_AND_GADGET);
        assertEquals(DatabaseModel.JournalMode.DEFAULT, db.getJournalMode());
        db.setJournalMode(DatabaseModel.JournalMode.WAL);
        assertEquals(DatabaseModel.JournalMode.WAL, db.getJournalMode());
    }

    @Test
    public void databaseInfoCarriesVersionAndDates() {
        DatabaseInfo info = new DatabaseInfo();
        Date d = new Date(1234);
        info.setVersion(5);
        info.setCreatedDate(d);
        info.setAccessedDate(d);
        assertEquals(5, info.getVersion());
        assertSame(d, info.getCreatedDate());
        assertSame(d, info.getAccessedDate());
    }

    private List<String> tableNamesCreated() {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^CREATE TABLE IF NOT EXISTS \"(\\w+)\"");
        for (String sql : driver.executed) {
            java.util.regex.Matcher m = p.matcher(sql);
            if (m.find())
                names.add(m.group(1));
        }
        return names;
    }

    private int countStartingWith(String prefix) {
        int n = 0;
        for (String s : driver.executed)
            if (s.startsWith(prefix))
                n++;
        return n;
    }
}

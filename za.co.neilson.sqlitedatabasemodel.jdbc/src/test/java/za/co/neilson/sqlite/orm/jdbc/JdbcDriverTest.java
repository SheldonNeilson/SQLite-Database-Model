package za.co.neilson.sqlite.orm.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import za.co.neilson.sqlite.orm.DatabaseDriverInterface;
import za.co.neilson.sqlite.orm.OrmTestState;

/** {@link JdbcSqliteDatabaseDriverInterface} against a real in-memory SQLite database. */
public class JdbcDriverTest {

    private JdbcTestDatabase db;
    private DatabaseDriverInterface<ResultSet, HashMap<String, Object>> driver;

    @Before
    public void setUp() throws Exception {
        OrmTestState.reset();
        db = JdbcTestDatabase.create(1, (d, models) -> {
        });
        driver = db.getDatabaseDriverInterface();
        db.memory().execute("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, qty INT, price DOUBLE, tag TEXT)");
    }

    @After
    public void tearDown() throws Exception {
        db.close();
    }

    private static HashMap<String, Object> values(Object... pairs) {
        HashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2)
            map.put((String) pairs[i], pairs[i + 1]);
        return map;
    }

    private void seed() throws Exception {
        db.memory().execute("INSERT INTO notes (title, qty, price, tag) VALUES ('alpha', 3, 1.5, 'x')");
        db.memory().execute("INSERT INTO notes (title, qty, price, tag) VALUES ('beta', 1, 2.5, 'y')");
        db.memory().execute("INSERT INTO notes (title, qty, price, tag) VALUES ('gamma', 2, 0.5, 'x')");
    }

    // ---- connection ----------------------------------------------------------------------------------------------

    @Test
    public void connectionLifecycle() throws Exception {
        assertFalse("DatabaseModel releases the connection after creating the schema", driver.isConnected());
        driver.connect();
        assertTrue(driver.isConnected());
        driver.connect();
        assertTrue("connect is idempotent", driver.isConnected());
        driver.disconnect();
        assertFalse(driver.isConnected());
        driver.disconnect();
        assertFalse("disconnect is idempotent", driver.isConnected());
    }

    @Test
    public void operationsConnectOnDemand() throws Exception {
        assertFalse(driver.isConnected());
        driver.execute("INSERT INTO notes (title) VALUES ('lazy')");
        assertTrue(driver.isConnected());
        assertEquals(1, db.memory().count("notes"));
    }

    @Test
    public void databaseFileIsTheConfiguredName_andResultColumnsAreOneBased() {
        assertEquals(db.memory().getName(), driver.getDatabaseFile().getName());
        assertEquals(1, driver.getFirstColumnIndex());
    }

    // ---- insert --------------------------------------------------------------------------------------------------

    @Test
    public void insert_returnsTheGeneratedRowId() throws Exception {
        assertEquals(1, driver.insert("notes", values("title", "a")));
        assertEquals(2, driver.insert("notes", values("title", "b")));
        assertEquals(10, driver.insert("notes", values("id", 10, "title", "c")));
        assertEquals(11, driver.insert("notes", values("title", "d")));
    }

    @Test
    public void insert_storesNumbersAsNumbersTextAsTextAndNullAsNull() throws Exception {
        driver.insert("notes", values("title", "widget", "qty", 5, "price", 2.5, "tag", null));
        Map<String, Object> row = db.memory().rows("SELECT title, qty, price, tag, typeof(qty) q, typeof(price) p, typeof(tag) t FROM notes").get(0);
        assertEquals("widget", row.get("title"));
        assertEquals(5, ((Number) row.get("qty")).intValue());
        assertEquals(2.5, ((Number) row.get("price")).doubleValue(), 0.0);
        assertNull(row.get("tag"));
        assertEquals("integer", row.get("q"));
        assertEquals("real", row.get("p"));
        assertEquals("null", row.get("t"));
    }

    @Test(expected = java.sql.SQLException.class)
    public void insert_unknownColumnIsReportedToTheCaller() throws Exception {
        driver.insert("notes", values("nope", 1));
    }

    // ---- update / delete -----------------------------------------------------------------------------------------

    @Test
    public void update_changesOnlyMatchingRowsAndReturnsTheCount() throws Exception {
        seed();
        assertEquals(2, driver.update("notes", values("qty", 9), "tag = ?", new Object[]{"x"}));
        assertEquals(java.util.Arrays.asList(9, 1, 9), column("SELECT qty FROM notes ORDER BY id"));
    }

    @Test
    public void update_numericWhereArgumentIsNotQuoted_nullValueBecomesSqlNull() throws Exception {
        seed();
        assertEquals(1, driver.update("notes", values("tag", null), "id = ?", new Object[]{2}));
        assertNull(db.memory().scalar("SELECT tag FROM notes WHERE id = 2"));
        assertEquals("other rows untouched", "x", db.memory().scalar("SELECT tag FROM notes WHERE id = 1"));
    }

    @Test
    public void update_withoutWhereUpdatesEveryRow() throws Exception {
        seed();
        assertEquals(3, driver.update("notes", values("qty", 0), null, null));
        assertEquals(0L, ((Number) db.memory().scalar("SELECT SUM(qty) FROM notes")).longValue());
    }

    @Test
    public void update_noMatchReturnsZero() throws Exception {
        seed();
        assertEquals(0, driver.update("notes", values("qty", 0), "id = ?", new Object[]{99}));
    }

    @Test
    public void delete_removesMatchingRowsAndReturnsTheCount() throws Exception {
        seed();
        assertEquals(2, driver.delete("notes", "tag = ?", new Object[]{"x"}));
        assertEquals(1, db.memory().count("notes"));
        assertEquals(0, driver.delete("notes", "tag = ?", new Object[]{"x"}));
    }

    @Test
    public void delete_withoutWhereClearsTheTable() throws Exception {
        seed();
        assertEquals(3, driver.delete("notes", null, null));
        assertEquals(0, db.memory().count("notes"));
    }

    // ---- query ---------------------------------------------------------------------------------------------------

    @Test
    public void query_selectsRequestedColumnsFilteredOrderedAndLimited() throws Exception {
        seed();
        ResultSet rs = driver.query("notes", new String[]{"title", "qty"}, "qty >= ?", new Object[]{1}, null, null, "qty DESC", 2);
        try {
            assertTrue(rs.next());
            assertEquals("alpha", rs.getString(1));
            assertEquals(3, rs.getInt(2));
            assertTrue(rs.next());
            assertEquals("gamma", rs.getString(1));
            assertFalse("limit 2", rs.next());
        } finally {
            rs.close();
        }
    }

    @Test
    public void query_withoutColumnsSelectsEverything() throws Exception {
        seed();
        ResultSet rs = driver.query("notes", null, "id = ?", new Object[]{2}, null, null, null, null);
        try {
            assertEquals(5, rs.getMetaData().getColumnCount());
            assertTrue(rs.next());
            assertEquals("beta", rs.getString("title"));
        } finally {
            rs.close();
        }
    }

    @Test
    public void query_stringWhereArgumentsAreQuoted() throws Exception {
        seed();
        ResultSet rs = driver.query("notes", new String[]{"id"}, "title = ? AND qty = ?", new Object[]{"gamma", 2}, null, null, null, null);
        try {
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }

    @Test
    public void query_groupByAggregates() throws Exception {
        seed();
        ResultSet rs = driver.query("notes", new String[]{"tag", "SUM(qty) AS total"}, null, null, "tag", null, "tag", null);
        try {
            assertTrue(rs.next());
            assertEquals("x", rs.getString(1));
            assertEquals(5, rs.getInt(2));
            assertTrue(rs.next());
            assertEquals("y", rs.getString(1));
            assertEquals(1, rs.getInt(2));
        } finally {
            rs.close();
        }
    }

    @Test
    public void query_noMatchGivesAnEmptyResult() throws Exception {
        ResultSet rs = driver.query("notes", null, "id = ?", new Object[]{1}, null, null, null, null);
        try {
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }

    @Test
    public void rawQuery_substitutesPositionalArguments() throws Exception {
        seed();
        ResultSet rs = ((JdbcSqliteDatabaseDriverInterface) driver).rawQuery("SELECT title FROM notes WHERE id > ? AND qty > ?", new Object[]{0, 2});
        try {
            assertTrue(rs.next());
            assertEquals("alpha", rs.getString(1));
            assertFalse(rs.next());
        } finally {
            rs.close();
        }
    }

    // ---- exists / count ------------------------------------------------------------------------------------------

    @Test
    public void exists_reflectsWhetherARowMatches() throws Exception {
        seed();
        assertTrue(driver.exists("notes", "id = ?", new Object[]{2}));
        assertFalse(driver.exists("notes", "id = ?", new Object[]{99}));
    }

    @Test
    public void getCount_countsMatchingRows() throws Exception {
        seed();
        assertEquals(2, driver.getCount("notes", "qty > ?", new Object[]{1}));
        assertEquals(0, driver.getCount("notes", "qty > ?", new Object[]{50}));
        assertEquals(3, driver.getCount("SELECT COUNT(*) FROM notes WHERE qty > ?", new Object[]{0}));
    }

    @Test
    public void isNumericType_followsQuery() {
        assertTrue(driver.isNumericType(Integer.class));
        assertFalse(driver.isNumericType(String.class));
    }

    // ---- known defects -------------------------------------------------------------------------------------------

    @Test
    public void insert_textWithApostropheRoundTrips() throws Exception {
        driver.insert("notes", values("title", "O'Brien"));
        assertEquals("O'Brien", db.memory().scalar("SELECT title FROM notes"));
    }

    @Test
    public void query_whereArgumentWithApostrophe() throws Exception {
        db.memory().execute("INSERT INTO notes (title) VALUES ('O''Brien')");
        ResultSet rs = driver.query("notes", new String[]{"id"}, "title = ?", new Object[]{"O'Brien"}, null, null, null, null);
        try {
            assertTrue(rs.next());
        } finally {
            rs.close();
        }
    }

    @Test
    public void query_whereArgumentWithDollarSign() throws Exception {
        db.memory().execute("INSERT INTO notes (title) VALUES ('cost$5')");
        assertTrue(driver.exists("notes", "title = ?", new Object[]{"cost$5"}));
    }

    @Test
    public void insert_numericLookingTextKeepsItsText() throws Exception {
        driver.insert("notes", values("title", "007"));
        assertEquals("007", db.memory().scalar("SELECT title FROM notes"));
    }

    @Test
    public void exists_andCountWithStringArguments() throws Exception {
        seed();
        assertTrue(driver.exists("notes", "title = ?", new Object[]{"beta"}));
        assertEquals(2, driver.getCount("notes", "tag = ?", new Object[]{"x"}));
    }

    @Test
    public void exists_andCountWithoutArguments() throws Exception {
        seed();
        assertTrue(driver.exists("notes", "qty > 1", null));
        assertEquals(2, driver.getCount("notes", "qty > 1", null));
    }

    @Test
    public void getCount_withoutWhereCountsAllRows() throws Exception {
        seed();
        assertEquals(3, driver.getCount("notes", null, new Object[0]));
    }

    @Test
    public void resultMetadataIsAvailable() throws Exception {
        seed();
        ResultSet rs = driver.query("notes", new String[]{"title", "qty"}, null, null, null, null, null, null);
        try {
            assertEquals(2, driver.getResultColumnCount(rs));
            assertEquals("title", driver.getResultColumnName(rs, 1));
            assertEquals("qty", driver.getResultColumnName(rs, 2));
        } finally {
            rs.close();
        }
    }

    private java.util.List<Object> column(String sql) throws Exception {
        java.util.List<Object> out = new java.util.ArrayList<>();
        for (Map<String, Object> row : db.memory().rows(sql))
            out.add(((Number) row.values().iterator().next()).intValue());
        return out;
    }
}

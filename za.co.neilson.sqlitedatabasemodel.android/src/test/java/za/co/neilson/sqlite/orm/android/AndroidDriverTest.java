package za.co.neilson.sqlite.orm.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import za.co.neilson.sqlite.orm.OrmTestState;

/** {@link AndroidSqliteDatabaseDriverInterface} against Robolectric's SQLite. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AndroidDriverTest {

    private AndroidTestDatabase db;
    private AndroidSqliteDatabaseDriverInterface driver;

    @Before
    public void setUp() throws Exception {
        OrmTestState.reset();
        db = AndroidTestDatabase.create(ApplicationProvider.getApplicationContext(), 1, (d, models) -> {
        });
        driver = db.driver();
        db.execute("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT UNIQUE, qty INT, price DOUBLE, tag TEXT)");
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static ContentValues values(Object... pairs) {
        ContentValues cv = new ContentValues();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            Object v = pairs[i + 1];
            if (v == null) cv.putNull(key);
            else if (v instanceof Integer) cv.put(key, (Integer) v);
            else if (v instanceof Double) cv.put(key, (Double) v);
            else cv.put(key, (String) v);
        }
        return cv;
    }

    private void seed() throws Exception {
        db.execute("INSERT INTO notes (title, qty, price, tag) VALUES ('alpha', 3, 1.5, 'x')");
        db.execute("INSERT INTO notes (title, qty, price, tag) VALUES ('beta', 1, 2.5, 'y')");
        db.execute("INSERT INTO notes (title, qty, price, tag) VALUES ('gamma', 2, 0.5, 'x')");
    }

    // ---- connection ----------------------------------------------------------------------------------------------

    @Test
    public void connect_opensTheDatabaseOnceAndOperationsConnectOnDemand() throws Exception {
        driver.reallyClose();
        assertFalse(driver.isConnected());
        db.execute("INSERT INTO notes (title) VALUES ('lazy')");
        assertTrue("execute reconnects", driver.isConnected());
        driver.connect();
        assertTrue("connect is idempotent", driver.isConnected());
    }

    @Test
    public void disconnect_keepsTheConnectionOpenByDesign_reallyCloseClosesIt() throws Exception {
        driver.connect();
        driver.disconnect();
        assertTrue("disconnect() is a no-op so the shared connection stays warm", driver.isConnected());
        driver.reallyClose();
        assertFalse(driver.isConnected());
    }

    @Test
    public void databaseFileLivesInTheAppDatabaseDirectoryAndColumnsAreZeroBased() {
        assertEquals(db.name(), driver.getDatabaseFile().getName());
        assertTrue(driver.getDatabaseFile().exists());
        assertEquals(0, driver.getFirstColumnIndex());
    }

    // ---- selection arguments -------------------------------------------------------------------------------------

    @Test
    public void getSelectionArgs_convertsValuesToStrings() {
        assertNull(driver.getSelectionArgs(null));
        assertArrayEquals(new String[]{"7", "x", "1.5", "1", "0", "5000000000"},
                driver.getSelectionArgs(new Object[]{7, "x", 1.5, true, Boolean.FALSE, 5_000_000_000L}));
        assertEquals(0, driver.getSelectionArgs(new Object[0]).length);
    }

    @Test
    public void getSelectionArgs_nullArgumentDoesNotCrash() {
        driver.getSelectionArgs(new Object[]{"a", null});
    }

    // ---- insert / update / delete --------------------------------------------------------------------------------

    @Test
    public void insert_returnsTheGeneratedRowId() throws Exception {
        assertEquals(1, driver.insert("notes", values("title", "a")));
        assertEquals(2, driver.insert("notes", values("title", "b")));
        assertEquals(10, driver.insert("notes", values("id", 10, "title", "c")));
        assertEquals(11, driver.insert("notes", values("title", "d")));
    }

    @Test
    public void insert_storesNativeTypesAndNull() throws Exception {
        driver.insert("notes", values("title", "widget", "qty", 5, "price", 2.5, "tag", null));
        Map<String, Object> row = db.rows("SELECT title, qty, price, tag, typeof(qty) q, typeof(price) p FROM notes").get(0);
        assertEquals("widget", row.get("title"));
        assertEquals(5L, row.get("qty"));
        assertEquals(2.5, row.get("price"));
        assertNull(row.get("tag"));
        assertEquals("integer", row.get("q"));
        assertEquals("real", row.get("p"));
    }

    @Test
    public void insert_constraintViolationReturnsMinusOneInsteadOfThrowing() throws Exception {
        assertEquals(1, driver.insert("notes", values("title", "dup")));
        assertEquals("the Android API swallows the exception, callers must check the id", -1, driver.insert("notes", values("title", "dup")));
        assertEquals(1, db.count("notes"));
    }

    @Test
    public void update_changesOnlyMatchingRowsAndReturnsTheCount() throws Exception {
        seed();
        assertEquals(2, driver.update("notes", values("qty", 9), "tag = ?", new Object[]{"x"}));
        assertEquals(9L, db.scalar("SELECT qty FROM notes WHERE id = 1"));
        assertEquals(1L, db.scalar("SELECT qty FROM notes WHERE id = 2"));
        assertEquals(9L, db.scalar("SELECT qty FROM notes WHERE id = 3"));
    }

    @Test
    public void update_nullValueBecomesSqlNull_numericArgumentMatches() throws Exception {
        seed();
        assertEquals(1, driver.update("notes", values("tag", null), "id = ?", new Object[]{2}));
        assertNull(db.scalar("SELECT tag FROM notes WHERE id = 2"));
        assertEquals("x", db.scalar("SELECT tag FROM notes WHERE id = 1"));
    }

    @Test
    public void update_withNothingToChangeIsReportedAsSuccess() throws Exception {
        seed();
        assertEquals(1, driver.update("notes", new ContentValues(), "id = ?", new Object[]{1}));
        assertEquals(1, driver.update("notes", null, "id = ?", new Object[]{1}));
        assertEquals(3L, db.scalar("SELECT qty FROM notes WHERE id = 1"));
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
        assertEquals(1, db.count("notes"));
        assertEquals(0, driver.delete("notes", "tag = ?", new Object[]{"x"}));
    }

    @Test
    public void delete_withoutWhereClearsTheTableAndCountsTheRows() throws Exception {
        seed();
        assertEquals("a null where must still report the affected rows", 3, driver.delete("notes", null, null));
        assertEquals(0, db.count("notes"));
    }

    // ---- query ---------------------------------------------------------------------------------------------------

    @Test
    public void query_selectsRequestedColumnsFilteredOrderedAndLimited() throws Exception {
        seed();
        Cursor c = driver.query("notes", new String[]{"title", "qty"}, "qty >= ?", new Object[]{1}, null, null, "qty DESC", 2);
        try {
            assertEquals(2, driver.getResultColumnCount(c));
            assertEquals("title", driver.getResultColumnName(c, 0));
            assertEquals("qty", driver.getResultColumnName(c, 1));
            assertTrue(c.moveToFirst());
            assertEquals("alpha", c.getString(0));
            assertTrue(c.moveToNext());
            assertEquals("gamma", c.getString(0));
            assertFalse("limit 2", c.moveToNext());
        } finally {
            c.close();
        }
    }

    @Test
    public void query_stringAndBooleanArgumentsAreBound() throws Exception {
        seed();
        Cursor c = driver.query("notes", new String[]{"id"}, "title = ? AND qty = ?", new Object[]{"gamma", 2}, null, null, null, null);
        try {
            assertTrue(c.moveToFirst());
            assertEquals(3, c.getInt(0));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }
    }

    @Test
    public void query_bindsValuesSoQuotesAreSafe() throws Exception {
        db.execute("INSERT INTO notes (title) VALUES ('O''Brien')");
        Cursor c = driver.query("notes", new String[]{"id"}, "title = ?", new Object[]{"O'Brien"}, null, null, null, null);
        try {
            assertTrue(c.moveToFirst());
        } finally {
            c.close();
        }
        assertTrue(driver.exists("notes", "title = ?", new Object[]{"O'Brien"}));
        assertEquals(1, driver.getCount("notes", "title = ?", new Object[]{"O'Brien"}));
    }

    @Test
    public void query_groupByAggregates() throws Exception {
        seed();
        Cursor c = driver.query("notes", new String[]{"tag", "SUM(qty) AS total"}, null, null, "tag", null, "tag", null);
        try {
            assertTrue(c.moveToFirst());
            assertEquals("x", c.getString(0));
            assertEquals(5, c.getInt(1));
            assertTrue(c.moveToNext());
            assertEquals("y", c.getString(0));
            assertEquals(1, c.getInt(1));
        } finally {
            c.close();
        }
    }

    @Test
    public void rawQuery_bindsPositionalArguments() throws Exception {
        seed();
        Cursor c = driver.rawQuery("SELECT title FROM notes WHERE tag = ? AND qty > ?", new Object[]{"x", 2});
        try {
            assertTrue(c.moveToFirst());
            assertEquals("alpha", c.getString(0));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }
    }

    // ---- exists / count ------------------------------------------------------------------------------------------

    @Test
    public void exists_reflectsWhetherARowMatches_andAcceptsNoWhereClause() throws Exception {
        assertFalse("empty table", driver.exists("notes", null, null));
        seed();
        assertTrue(driver.exists("notes", null, null));
        assertTrue(driver.exists("notes", "title = ?", new Object[]{"beta"}));
        assertFalse(driver.exists("notes", "title = ?", new Object[]{"delta"}));
        assertTrue(driver.exists("notes", "qty > 2", null));
    }

    @Test
    public void getCount_countsMatchingRows() throws Exception {
        seed();
        assertEquals(2, driver.getCount("notes", "tag = ?", new Object[]{"x"}));
        assertEquals(0, driver.getCount("notes", "tag = ?", new Object[]{"z"}));
        assertEquals(3, driver.getCount("SELECT COUNT(*) FROM notes WHERE qty > ?", new Object[]{0}));
        assertEquals(2, driver.getCount("notes", "qty > 1", null));
    }

    @Test
    public void getCount_withoutWhereCountsAllRows() throws Exception {
        seed();
        assertEquals(3, driver.getCount("notes", null, null));
    }

    @Test
    public void isNumericType_followsQuery() {
        assertTrue(driver.isNumericType(Integer.class));
        assertFalse(driver.isNumericType(String.class));
    }
}

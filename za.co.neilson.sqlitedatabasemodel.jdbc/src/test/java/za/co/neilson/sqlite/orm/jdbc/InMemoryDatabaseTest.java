package za.co.neilson.sqlite.orm.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** The reusable in-memory database helper behaves as documented (other agents build repository tests on it). */
public class InMemoryDatabaseTest {

    @Test
    public void everyInstanceIsIsolated() throws Exception {
        try (InMemoryDatabase a = InMemoryDatabase.create(); InMemoryDatabase b = InMemoryDatabase.create()) {
            assertNotEquals(a.getName(), b.getName());
            a.execute("CREATE TABLE t (id INTEGER)");
            assertEquals(Collections.singletonList("t"), a.tables());
            assertTrue(b.tables().isEmpty());
        }
    }

    @Test
    public void dataSurvivesOtherConnectionsOpeningAndClosing() throws Exception {
        try (InMemoryDatabase db = InMemoryDatabase.create()) {
            db.execute("CREATE TABLE t (id INTEGER, name TEXT)");
            try (Connection other = DriverManager.getConnection("jdbc:sqlite:" + db.getName())) {
                other.createStatement().execute("INSERT INTO t VALUES (1, 'kept')");
            }
            assertEquals("kept", db.scalar("SELECT name FROM t WHERE id = ?", 1));
        }
    }

    @Test
    public void closingTheHelperDropsTheDatabase() throws Exception {
        String name;
        try (InMemoryDatabase db = InMemoryDatabase.create()) {
            name = db.getName();
            db.execute("CREATE TABLE t (id INTEGER)");
        }
        try (Connection reopened = DriverManager.getConnection("jdbc:sqlite:" + name)) {
            assertFalse(reopened.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE name = 't'").next());
        }
    }

    @Test
    public void queryHelpers_returnRowsScalarsCountsAndColumns() throws Exception {
        try (InMemoryDatabase db = InMemoryDatabase.create()) {
            db.execute("CREATE TABLE people (id INTEGER PRIMARY KEY, name TEXT, age INT)");
            db.execute("INSERT INTO people VALUES (1, 'Ann', 30), (2, 'Bob', NULL)");

            assertEquals(2, db.count("people"));
            assertEquals("Ann", db.scalar("SELECT name FROM people WHERE age = ?", 30));
            assertNull("no row", db.scalar("SELECT name FROM people WHERE id = ?", 99));
            assertNull("SQL NULL", db.rows("SELECT age FROM people WHERE id = 2").get(0).get("age"));
            assertEquals(Arrays.asList("id", "name", "age"), Arrays.asList(db.columns("people").keySet().toArray()));
            assertEquals("INT", db.columns("people").get("age"));
            assertEquals(Arrays.asList("Ann", "Bob"), Arrays.asList(
                    db.rows("SELECT name FROM people ORDER BY id").get(0).get("name"),
                    db.rows("SELECT name FROM people ORDER BY id").get(1).get("name")));
        }
    }
}

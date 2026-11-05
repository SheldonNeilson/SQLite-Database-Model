package za.co.neilson.sqlite.orm.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A private, uniquely named in-memory SQLite database for tests (reusable pattern for repository tests).
 * <p>
 * It is a shared-cache memory database ({@code file:ormN?mode=memory&cache=shared}), so it survives the code under test
 * opening and closing its own connections: the database lives as long as this object holds the "keeper" connection.
 * Use the keeper to arrange data and to assert what the code under test really wrote, independent of the ORM.
 * <pre>
 * try (InMemoryDatabase db = InMemoryDatabase.create()) {
 *     db.execute("CREATE TABLE t (id INTEGER PRIMARY KEY, name TEXT)");
 *     // pass db.getName() to the code under test as its database name
 *     assertEquals("a", db.scalar("SELECT name FROM t WHERE id = ?", 1));
 * }
 * </pre>
 */
public final class InMemoryDatabase implements AutoCloseable {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final String name;
    private final Connection keeper;

    private InMemoryDatabase(String name, Connection keeper) {
        this.name = name;
        this.keeper = keeper;
    }

    public static InMemoryDatabase create() throws SQLException {
        String name = "file:ormtest" + COUNTER.incrementAndGet() + "_" + System.nanoTime() + "?mode=memory&cache=shared";
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("sqlite-jdbc is not on the test classpath", e);
        }
        return new InMemoryDatabase(name, DriverManager.getConnection("jdbc:sqlite:" + name));
    }

    /** The SQLite URI file name (without the {@code jdbc:sqlite:} prefix). */
    public String getName() {
        return name;
    }

    /** The always-open connection that keeps the database alive. */
    public Connection keeper() {
        return keeper;
    }

    public void execute(String sql) throws SQLException {
        try (Statement s = keeper.createStatement()) {
            s.execute(sql);
        }
    }

    /** Runs a query with bound parameters and returns every row as column-name to value (insertion ordered). */
    public List<Map<String, Object>> rows(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = keeper.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++)
                ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int c = 1; c <= md.getColumnCount(); c++)
                        row.put(md.getColumnLabel(c), rs.getObject(c));
                    out.add(row);
                }
                return out;
            }
        }
    }

    /** First column of the first row, or null when there is no row. */
    public Object scalar(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> rows = rows(sql, params);
        return rows.isEmpty() ? null : rows.get(0).values().iterator().next();
    }

    public long count(String table) throws SQLException {
        return ((Number) scalar("SELECT COUNT(*) FROM \"" + table + "\"")).longValue();
    }

    /** User tables currently in the database. */
    public List<String> tables() throws SQLException {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> row : rows("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name"))
            out.add((String) row.get("name"));
        return out;
    }

    /** Column name to declared type, in table order (PRAGMA table_info). */
    public Map<String, String> columns(String table) throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows("PRAGMA table_info(\"" + table + "\")"))
            out.put((String) row.get("name"), (String) row.get("type"));
        return out;
    }

    @Override
    public void close() throws SQLException {
        keeper.close();
    }
}

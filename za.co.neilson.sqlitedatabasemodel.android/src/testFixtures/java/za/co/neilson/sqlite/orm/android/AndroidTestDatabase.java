package za.co.neilson.sqlite.orm.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import za.co.neilson.sqlite.orm.DatabaseDriverInterface;
import za.co.neilson.sqlite.orm.DatabaseInfo;
import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.OrmTestState;

/**
 * A real {@link DatabaseModel} on the Android driver over Robolectric's SQLite, created in one call. Reusable pattern
 * for repository tests:
 * <pre>
 * &#64;RunWith(AndroidJUnit4.class) &#64;Config(sdk = 34)
 * public class FooTest {
 *     &#64;Before public void setUp() throws Exception {
 *         OrmTestState.reset();
 *         db = AndroidTestDatabase.create(ApplicationProvider.getApplicationContext(), 1,
 *                 (d, models) -&gt; models.put(Foo.class, new FooModel(d)));
 *     }
 *     &#64;After public void tearDown() { db.close(); }
 *     // db.model(Foo.class).insert(foo); assertEquals(1, db.count("Foo"));
 * }
 * </pre>
 * The registration callback runs inside the DatabaseModel constructor (as {@code onRegisterObjectModels} does in the
 * app): register parent models before children. The schema is created on construction, in a unique database file
 * (a temp dir managed by Robolectric). Use {@link #open} to reopen the same file at another version (upgrade tests).
 * <p>
 * The module enables Robolectric native SQLite (src/test/resources/robolectric.properties): the legacy mode ships an
 * old SQLite without PRAGMA support, native mode behaves like a device.
 */
public final class AndroidTestDatabase extends DatabaseModel<Cursor, ContentValues> implements AutoCloseable {

    public interface Registrar {
        void register(AndroidTestDatabase db, HashMap<Type, ObjectModel<?, Cursor, ContentValues>> models) throws Exception;
    }

    /** Base class for test entities' models. */
    public abstract static class TestModel<T> extends AndroidObjectModel<T> {
        protected TestModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }

        public String table() {
            return getTableName();
        }
    }

    static final class DatabaseInfoModel extends TestModel<DatabaseInfo> {
        DatabaseInfoModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    private static final AtomicInteger COUNTER = new AtomicInteger();

    // Constructor configuration; the superclass constructor calls the hooks before subclass fields exist.
    private static String pendingName;
    private static int pendingVersion;
    private static Registrar pendingRegistrar;

    private final String name;

    /** Creates a new, uniquely named database and its schema. */
    public static AndroidTestDatabase create(Context context, int version, Registrar registrar) throws Exception {
        return open(context, "ormtest" + COUNTER.incrementAndGet() + ".db", version, registrar);
    }

    /**
     * Opens the named database, creating it (and the schema) if the file does not exist yet, upgrading it when the
     * stored version differs from {@code version}. Call {@link #close()} on the previous instance first.
     */
    public static AndroidTestDatabase open(Context context, String name, int version, Registrar registrar) throws Exception {
        pendingName = name;
        pendingVersion = version;
        pendingRegistrar = registrar;
        try {
            return new AndroidTestDatabase(context, name);
        } finally {
            pendingRegistrar = null;
        }
    }

    private AndroidTestDatabase(Context context, String name) throws SQLException, ClassNotFoundException, NoSuchFieldException {
        super(context);
        this.name = name;
    }

    public String name() {
        return name;
    }

    public AndroidSqliteDatabaseDriverInterface driver() {
        return (AndroidSqliteDatabaseDriverInterface) getDatabaseDriverInterface();
    }

    @SuppressWarnings("unchecked")
    public <T> TestModel<T> model(Class<T> type) {
        return (TestModel<T>) getObjectModel(type);
    }

    // ---- plain SQL helpers for arranging and asserting independent of the ORM ----------------------------------

    public void execute(String sql) throws SQLException {
        driver().execute(sql);
    }

    /** Rows as column to value (Long, Double, String, byte[] or null, as SQLite stores them), insertion ordered. */
    public List<Map<String, Object>> rows(String sql, Object... args) throws SQLException {
        Cursor c = driver().rawQuery(sql, args);
        try {
            List<Map<String, Object>> out = new ArrayList<>();
            while (c.moveToNext()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    Object v;
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            v = null;
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            v = c.getLong(i);
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            v = c.getDouble(i);
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            v = c.getBlob(i);
                            break;
                        default:
                            v = c.getString(i);
                    }
                    row.put(c.getColumnName(i), v);
                }
                out.add(row);
            }
            return out;
        } finally {
            c.close();
        }
    }

    /** First column of the first row, or null when there is no row. */
    public Object scalar(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> rows = rows(sql, args);
        return rows.isEmpty() ? null : rows.get(0).values().iterator().next();
    }

    public long count(String table) throws SQLException {
        return (Long) scalar("SELECT COUNT(*) FROM \"" + table + "\"");
    }

    public List<String> tables() throws SQLException {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> row : rows("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' ORDER BY name"))
            out.add((String) row.get("name"));
        return out;
    }

    /** Column name to declared type, in table order. */
    public Map<String, String> columns(String table) throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows("SELECT * FROM pragma_table_info('" + table + "')"))
            out.put((String) row.get("name"), (String) row.get("type"));
        return out;
    }

    // ---- DatabaseModel hooks -------------------------------------------------------------------------------------

    @Override
    public ObjectModel<DatabaseInfo, Cursor, ContentValues> onCreateDatabaseInfoModel() throws ClassNotFoundException, NoSuchFieldException {
        return new DatabaseInfoModel(this);
    }

    @Override
    protected DatabaseDriverInterface<Cursor, ContentValues> onInitializeDatabaseDriverInterface(Object... args) {
        return new AndroidSqliteDatabaseDriverInterface((Context) args[0], this);
    }

    @Override
    protected void onRegisterObjectModels(HashMap<Type, ObjectModel<?, Cursor, ContentValues>> objectModels) throws ClassNotFoundException, NoSuchFieldException {
        try {
            if (pendingRegistrar != null)
                pendingRegistrar.register(this, objectModels);
        } catch (ClassNotFoundException | NoSuchFieldException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDatabaseName() {
        return pendingName;
    }

    @Override
    public int getDatabaseVersion() {
        return pendingVersion;
    }

    @Override
    protected void onInsertDefaultValues() {
    }

    /** Closes the database for real (the driver's disconnect() is a no-op) and clears the ORM's static caches. */
    @Override
    public void close() {
        driver().reallyClose();
        driver().close();
        OrmTestState.reset();
    }
}

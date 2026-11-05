package za.co.neilson.sqlite.orm.jdbc;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import za.co.neilson.sqlite.orm.DatabaseDriverInterface;
import za.co.neilson.sqlite.orm.DatabaseInfo;
import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.OrmTestState;

/**
 * A real {@link DatabaseModel} on the JDBC driver over an {@link InMemoryDatabase}, created in one call:
 * <pre>
 * OrmTestState.reset();   // @Before
 * try (JdbcTestDatabase db = JdbcTestDatabase.create(1, (d, models) -&gt; models.put(Foo.class, new FooModel(d)))) {
 *     db.model(Foo.class).insert(foo);
 *     assertEquals(1, db.memory().count("Foo"));
 * }
 * </pre>
 * The registration callback runs inside the DatabaseModel constructor (as {@code onRegisterObjectModels} does in the
 * app), so register parent models before children. The schema is created on construction.
 */
public final class JdbcTestDatabase extends DatabaseModel<ResultSet, HashMap<String, Object>> implements AutoCloseable {

    public interface Registrar {
        void register(JdbcTestDatabase db, HashMap<Type, ObjectModel<?, ResultSet, HashMap<String, Object>>> models) throws Exception;
    }

    /** Base class for test entities' models: closes result sets like the Android model closes cursors. */
    public abstract static class TestModel<T> extends JdbcObjectModel<T> {
        protected TestModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }

        @Override
        protected void onQueryCompleted(ResultSet resultSet) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
        }

        public String table() {
            return getTableName();
        }
    }

    static final class DatabaseInfoModel extends TestModel<DatabaseInfo> {
        DatabaseInfoModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    // Constructor configuration; the superclass constructor calls the hooks before subclass fields exist.
    private static String pendingName;
    private static int pendingVersion;
    private static Registrar pendingRegistrar;

    private InMemoryDatabase memory;

    /**
     * Opens a database whose schema is valid and ready to use.
     * <p>
     * The constructor creates the schema through {@code JdbcObjectModel.onCreateTable()}, whose comma handling is only
     * correct when a table has exactly as many columns as there are mapped entity types (see
     * {@code JdbcSchemaTest#createTable_separatesColumnsWithCommas}). So the schema is re-created through
     * {@link DatabaseModel#rebuild()}, which uses the correct {@code ObjectModel.onCreateTable(true)} DDL.
     * Use {@link #createWithJdbcDdl} to observe the constructor's own result.
     */
    public static JdbcTestDatabase create(int version, Registrar registrar) throws Exception {
        JdbcTestDatabase db = createWithJdbcDdl(version, registrar);
        db.connect();
        try {
            db.rebuild();
        } finally {
            db.disconnect();
        }
        return db;
    }

    /** Opens a database exactly as the app would: the constructor's own schema creation, no repair. */
    public static JdbcTestDatabase createWithJdbcDdl(int version, Registrar registrar) throws Exception {
        InMemoryDatabase memory = InMemoryDatabase.create();
        pendingName = memory.getName();
        pendingVersion = version;
        pendingRegistrar = registrar;
        try {
            JdbcTestDatabase db = new JdbcTestDatabase();
            db.memory = memory;
            return db;
        } catch (Exception | Error e) {
            memory.close();
            throw e;
        } finally {
            pendingRegistrar = null;
        }
    }

    private JdbcTestDatabase() throws SQLException, ClassNotFoundException, NoSuchFieldException {
        super();
    }

    /** The memory database for arranging and asserting with plain SQL. */
    public InMemoryDatabase memory() {
        return memory;
    }

    @SuppressWarnings("unchecked")
    public <T> TestModel<T> model(Class<T> type) {
        return (TestModel<T>) getObjectModel(type);
    }

    @Override
    public ObjectModel<DatabaseInfo, ResultSet, HashMap<String, Object>> onCreateDatabaseInfoModel() throws ClassNotFoundException, NoSuchFieldException {
        return new DatabaseInfoModel(this);
    }

    @Override
    protected DatabaseDriverInterface<ResultSet, HashMap<String, Object>> onInitializeDatabaseDriverInterface(Object... args) {
        return new JdbcSqliteDatabaseDriverInterface(this);
    }

    @Override
    protected void onRegisterObjectModels(HashMap<Type, ObjectModel<?, ResultSet, HashMap<String, Object>>> objectModels) throws ClassNotFoundException, NoSuchFieldException {
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

    /** Re-run the schema upgrade path (drop and recreate every table). */
    public void upgrade() throws SQLException {
        connect();
        try {
            onUpgrade(0);
        } finally {
            disconnect();
        }
    }

    @Override
    public void close() throws SQLException {
        disconnect();
        OrmTestState.reset();
        memory.close();
    }
}

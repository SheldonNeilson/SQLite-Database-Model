package za.co.neilson.sqlite.orm;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link DatabaseModel} over {@link FakeDriver}. The superclass constructor calls the registration hooks before any
 * subclass field exists, so the configuration is passed through statics ({@link #create}).
 */
final class FakeDatabaseModel extends DatabaseModel<FakeRow, Map<String, Object>> {

    interface Registrar {
        void register(FakeDatabaseModel db, HashMap<Type, ObjectModel<?, FakeRow, Map<String, Object>>> models) throws Exception;
    }

    private static FakeDriver pendingDriver;
    private static Registrar pendingRegistrar;
    private static int pendingVersion;
    int defaultValuesInserted; // no initializer: the superclass constructor already calls onInsertDefaultValues

    static final class DatabaseInfoModel extends FakeObjectModel<DatabaseInfo> {
        DatabaseInfoModel(DatabaseModel<FakeRow, Map<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    static FakeDatabaseModel create(FakeDriver driver, int version, Registrar registrar) throws Exception {
        pendingDriver = driver;
        pendingVersion = version;
        pendingRegistrar = registrar;
        try {
            return new FakeDatabaseModel();
        } finally {
            pendingDriver = null;
            pendingRegistrar = null;
        }
    }

    /**
     * A database whose file already exists at the current version: construction only registers the models, creates
     * no tables. The recorded events/executed SQL are cleared so tests start from a clean log.
     */
    static FakeDatabaseModel createExisting(FakeDriver driver, int version, Registrar registrar) throws Exception {
        driver.fileExists = true;
        driver.queryResult = new FakeRow("version", version, "createdDate", 1000L, "accessedDate", 1000L);
        FakeDatabaseModel db = create(driver, version, registrar);
        driver.events.clear();
        driver.executed.clear();
        driver.queryResult = null;
        return db;
    }

    private FakeDatabaseModel() throws SQLException, ClassNotFoundException, NoSuchFieldException {
        super();
    }

    @Override
    public ObjectModel<DatabaseInfo, FakeRow, Map<String, Object>> onCreateDatabaseInfoModel() throws ClassNotFoundException, NoSuchFieldException {
        return new DatabaseInfoModel(this);
    }

    @Override
    protected DatabaseDriverInterface<FakeRow, Map<String, Object>> onInitializeDatabaseDriverInterface(Object... args) {
        return pendingDriver;
    }

    @Override
    protected void onRegisterObjectModels(HashMap<Type, ObjectModel<?, FakeRow, Map<String, Object>>> models) throws ClassNotFoundException, NoSuchFieldException {
        try {
            if (pendingRegistrar != null)
                pendingRegistrar.register(this, models);
        } catch (ClassNotFoundException | NoSuchFieldException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDatabaseName() {
        return "fake.db";
    }

    @Override
    public int getDatabaseVersion() {
        return pendingVersion;
    }

    @Override
    protected void onInsertDefaultValues() {
        defaultValuesInserted++;
    }

    void callUpgrade(int previousVersion) throws SQLException {
        onUpgrade(previousVersion);
    }

    void callRebuild() throws SQLException {
        rebuild();
    }
}

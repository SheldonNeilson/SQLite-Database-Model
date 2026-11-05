package za.co.neilson.sqlite.orm;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * The ORM keeps its reflection caches (columns, fields, primary keys, table names) and the per-class database driver
 * in static maps. Call {@link #reset()} in {@code @Before} of every test that defines entities or opens a database so
 * one test never sees the state of another (Gradle runs all test classes of a module in the same JVM).
 * <p>
 * Same helper as in the core module's tests (test sources are not shared between modules).
 */
public final class OrmTestState {

    private OrmTestState() {
    }

    public static void reset() {
        ObjectModel.fields.clear();
        ObjectModel.objectModelColumns.clear();
        ObjectModel.primaryKeys.clear();
        ObjectModel.columnNames.clear();
        clearStatic(ObjectModel.class, "tableName");
        clearStatic(DatabaseModel.class, "databaseDriverInterfaces");
    }

    private static void clearStatic(Class<?> owner, String fieldName) {
        try {
            Field f = owner.getDeclaredField(fieldName);
            f.setAccessible(true);
            ((Map<?, ?>) f.get(null)).clear();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ORM internals changed, update OrmTestState: " + fieldName, e);
        }
    }
}

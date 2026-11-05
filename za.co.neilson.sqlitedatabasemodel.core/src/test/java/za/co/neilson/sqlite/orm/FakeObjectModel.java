package za.co.neilson.sqlite.orm;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal concrete {@link ObjectModel} backed by {@link FakeDriver}. Subclass directly with a concrete T. */
abstract class FakeObjectModel<T> extends ObjectModel<T, FakeRow, Map<String, Object>> {

    FakeObjectModel(DatabaseModel<FakeRow, Map<String, Object>> databaseModel) throws ClassNotFoundException, NoSuchFieldException {
        super(databaseModel);
    }

    @Override
    protected Map<String, Object> setColumnValues(Object t, QueryType queryType) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ObjectModelColumn column : getObjectModelColumns()) {
            if (column.isAutoIncrement() && queryType == QueryType.INSERT)
                continue;
            try {
                values.put(column.getName(), setColumnValue(t, column));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return values;
    }

    @Override
    protected Object getColumnValue(FakeRow row, ObjectModelColumn column, int index) {
        Object value = row.values.get(index);
        if (value instanceof Long && column.getType() == java.util.Date.class)
            return new java.util.Date((Long) value);
        return value;
    }

    @Override
    public T getFirst(String whereClause, Object[] whereArgs, String orderBy) throws SQLException {
        FakeRow row = getResultSet(whereClause, whereArgs, null, null, orderBy, 1);
        return row == null ? null : getInstance(row);
    }

    @Override
    public T getFirstRaw(String sql, Object... selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> getAll(String whereClause, Object[] whereArgs, String orderBy, Integer limit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> getAllRaw(String sql, Object... selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long insert(T t) throws SQLException {
        return getDatabaseModel().getDatabaseDriverInterface().insert(getTableName(), setColumnValues(t, QueryType.INSERT));
    }

    @Override
    public long[] insertAll(Collection<T> collection) throws SQLException {
        long[] ids = new long[collection.size()];
        int i = 0;
        for (T t : collection)
            ids[i++] = insert(t);
        return ids;
    }

    @Override
    public int update(T t, String whereClause, Object... whereArgs) throws SQLException {
        return getDatabaseModel().getDatabaseDriverInterface().update(getTableName(), setColumnValues(t, QueryType.UPDATE), whereClause, whereArgs);
    }

    @Override
    protected void onQueryCompleted(FakeRow row) {
    }

    @Override
    protected List<String> getColumnValues(String tableName, String columnName, String whereClause, Object[] whereArgs) {
        return ((FakeDriver) getDatabaseModel().getDatabaseDriverInterface()).columnValuesResult;
    }

    /** Table name as the ORM sees it (protected in ObjectModel). */
    String table() {
        return getTableName();
    }
}

package za.co.neilson.sqlite.orm;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Recording {@link DatabaseDriverInterface}. Every call is appended to {@link #events} in order so tests can assert the
 * SQL and the call sequence without a database.
 */
class FakeDriver implements DatabaseDriverInterface<FakeRow, Map<String, Object>> {

    final List<String> events = new ArrayList<>();
    final List<String> executed = new ArrayList<>();
    /** What {@link #getDatabaseFile()} reports for exists(); false makes the DatabaseModel create the schema. */
    boolean fileExists = false;
    boolean connected = false;
    /** Returned by {@link #query}. */
    FakeRow queryResult;
    boolean existsResult = false;
    int deleteResult = 1;
    int updateResult = 1;
    long insertResult = 1;
    List<String> columnValuesResult = Collections.emptyList();
    /** Any executed SQL containing this text throws an SQLException. */
    String failOnSqlContaining;
    boolean failInsert;

    @Override
    public File getDatabaseFile() {
        return new File("fake.db") {
            @Override
            public boolean exists() {
                return fileExists;
            }
        };
    }

    @Override
    public void connect() {
        connected = true;
        events.add("connect");
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void disconnect() {
        connected = false;
        events.add("disconnect");
    }

    @Override
    public int getFirstColumnIndex() {
        return 0;
    }

    @Override
    public void beginTransaction() {
        events.add("beginTransaction");
    }

    @Override
    public void setTransactionSuccessful() {
        events.add("setTransactionSuccessful");
    }

    @Override
    public void endTransaction() {
        events.add("endTransaction");
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        events.add("execute " + sql);
        executed.add(sql);
        if (failOnSqlContaining != null && sql.contains(failOnSqlContaining))
            throw new SQLException("forced failure: " + sql);
        return false;
    }

    @Override
    public FakeRow query(String tableName, String[] columnNames, String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) {
        events.add("query " + tableName + " cols=" + Arrays.toString(columnNames) + " where=" + whereClause + " args=" + Arrays.toString(whereArgs) + " order=" + orderBy + " limit=" + limit);
        return queryResult;
    }

    @Override
    public FakeRow rawQuery(String sql, Object... selectionArgs) {
        events.add("rawQuery " + sql);
        return queryResult;
    }

    @Override
    public int update(String tableName, Map<String, Object> columnValues, String whereClause, Object[] whereArgs) {
        events.add("update " + tableName + " " + columnValues + " where=" + whereClause + " args=" + Arrays.toString(whereArgs));
        return updateResult;
    }

    @Override
    public int delete(String tableName, String whereClause, Object[] whereArgs) {
        events.add("delete " + tableName + " where=" + whereClause + " args=" + Arrays.toString(whereArgs));
        return deleteResult;
    }

    @Override
    public long insert(String tableName, Map<String, Object> columnValues) throws SQLException {
        events.add("insert " + tableName + " " + columnValues);
        if (failInsert)
            throw new SQLException("forced insert failure");
        return insertResult;
    }

    @Override
    public boolean isNumericType(Class<?> type) {
        return Query.isNumericType(type);
    }

    @Override
    public boolean exists(String tableName, String whereClause, Object[] whereArgs) {
        events.add("exists " + tableName + " where=" + whereClause + " args=" + Arrays.toString(whereArgs));
        return existsResult;
    }

    @Override
    public long getCount(String tableName, String whereClause, Object[] whereArgs) {
        events.add("count " + tableName + " where=" + whereClause);
        return 0;
    }

    @Override
    public long getCount(String sql, Object[] whereArgs) {
        events.add("count " + sql);
        return 0;
    }

    @Override
    public String getResultColumnName(FakeRow row, int index) {
        return row.names.get(index);
    }

    @Override
    public int getResultColumnCount(FakeRow row) {
        return row.names.size();
    }

    /** All recorded events starting with the given prefix. */
    List<String> eventsStartingWith(String prefix) {
        List<String> out = new ArrayList<>();
        for (String e : events)
            if (e.startsWith(prefix))
                out.add(e);
        return out;
    }
}

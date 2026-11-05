/* Copyright 2014 Sheldon Neilson www.neilson.co.za
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package za.co.neilson.sqlite.orm.jdbc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import za.co.neilson.sqlite.orm.DatabaseDriverInterface;
import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.Query;

/**
 * <p>
 * The interface between the sqlite JDBC driver and the DatabaseModel.
 * </p>
 * <p>
 * The DatabaseDriverInterface class should never be instantiated directly. This
 * is done via the DatabaseModel which holds an internal reference to the
 * DatabaseDriverInterface
 * </p>
 *
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 * @version 0.1
 * @since 0.1
 */
public class JdbcSqliteDatabaseDriverInterface implements DatabaseDriverInterface<ResultSet, HashMap<String, Object>> {

    private File dataBaseFile;
    private Connection connection;

    /**
     * <p>
     * The interface between the sqlite JDBC driver and the DatabaseModel.
     * </p>
     * <p>
     * The DatabaseDriverInterface class should never be instantiated directly.
     * This is done via the DatabaseModel which holds an internal reference to
     * the DatabaseDriverInterface
     * </p>
     *
     * @param databaseModel the model
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public JdbcSqliteDatabaseDriverInterface(DatabaseModel<ResultSet, HashMap<String, Object>> databaseModel) {

        try {
            // Load the sqlite-JDBC driver using the current class loader
            Class.forName("org.sqlite.JDBC");

            if (databaseModel.getDatabaseName() == null || databaseModel.getDatabaseName().length() == 0) {
                throw new IllegalArgumentException("Invalid path provided: The path provided is null or empty");
            }

            File dataBaseFile = new File(databaseModel.getDatabaseName());

            if (dataBaseFile.isDirectory()) {
                throw new IllegalArgumentException("Invalid path provided: The path provided is a directory.");
            } else {
                this.dataBaseFile = dataBaseFile;
            }
        } catch (IllegalArgumentException illegalArgumentException) {
            illegalArgumentException.printStackTrace();
            System.exit(1);
        } catch (ClassNotFoundException classNotFoundException) {
            classNotFoundException.printStackTrace();
            System.exit(1);
        }
    }

    /* (non-Javadoc)
     * @see za.co.neilson.sqlite.orm.DatabaseDriverInterface#getDatabaseFile()
     */
    @Override
    public File getDatabaseFile() {
        return dataBaseFile;
    }

    /**
     * Opens a connection to the database file using the jdbc sqlite driver. If
     * the file does not exist, calling this method creates the file.
     *
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public void connect() throws SQLException {
        if (!isConnected()) {
            if (dataBaseFile != null) {
                connection = DriverManager.getConnection("jdbc:sqlite:" + dataBaseFile.getName());
            } else {
                throw new SQLException("Database file not specified");
            }
        }
    }

    /**
     * Returns the database connection state
     *
     * @return <b>boolean</b> true if the database connection is open
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Releases the Connection object's database and JDBC resources immediately
     * instead of waiting for them to be automatically released.
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public void disconnect() {
        try {
            if (isConnected()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates / returns the database connection.
     * SQL statements are executed and results are returned within the context of a connection.
     *
     * @return the java.sql.Connection
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    private Connection getConnection() throws SQLException {
        if (!isConnected())
            connect();
        return connection;
    }

    /* (non-Javadoc)
     * @see za.co.neilson.sqlite.orm.DatabaseDriverInterface#getFirstColumnIndex()
     */
    @Override
    public int getFirstColumnIndex() {
        return 1;
    }

    @Override
    public void beginTransaction() throws SQLException {

    }

    @Override
    public void setTransactionSuccessful() {

    }

    @Override
    public void endTransaction() {

    }

    /**
     * Executes the given SQL statement and indicates the form of the first
     * result.
     *
     * @param sql the sql statement to execute
     * @return <b>boolean</b> true if the first result is a ResultSet object;
     * false if it is an update count or there are no results
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public boolean execute(String sql) throws SQLException {
        Statement statement;
        statement = getConnection().createStatement();
        return statement.execute(sql);
    }

    /**
     * Queries the given table using any non-null clauses, returning a single
     * ResultSet object containing the results of the query.
     *
     * @param tableName   the table to query
     * @param columnNames the specific columns to return in the result set. If
     *                    columNames is null, all columns will be returned.
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @param groupBy     a comma delimited list of the column names to group the
     *                    returned results by
     * @param having
     * @param orderBy     a comma delimited list of the column names to order the
     *                    returned results by
     * @param limit       an optional numeric limit on the number of results returned
     * @return <b>ResultSet</b> the results of the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public ResultSet query(String tableName, String[] columnNames, String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("select ");

        if (columnNames != null && columnNames.length > 0) {
            sqlBuilder.append(Query.join(columnNames));
        } else {
            sqlBuilder.append("*");
        }

        sqlBuilder.append(" from " + tableName);

        Object[] boundArgs = null;
        if (whereClause != null) {
            sqlBuilder.append(" where " + whereClause);
            boundArgs = whereArgs;
        }

        if (groupBy != null)
            sqlBuilder.append(" group by " + groupBy);

        if (having != null)
            sqlBuilder.append(" having " + having);

        if (orderBy != null)
            sqlBuilder.append(" order by " + orderBy);

        if (limit != null)
            sqlBuilder.append(" limit " + String.valueOf(limit));

        sqlBuilder.append(";");

        return rawQuery(sqlBuilder.toString(), boundArgs);
    }

    /**
     * @param sql       the sql query to execute
     * @param whereArgs any ?s in the sql query will be replaced by the String
     *                  representation of these selectionArgs in order
     * @return ResultSet
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public ResultSet rawQuery(String sql, Object[] whereArgs) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement(sql);
        bind(statement, 1, whereArgs);
        return statement.executeQuery();
    }

    /**
     * Binds the values to the ? placeholders of the statement, starting at the 1-based index given. Values are bound
     * rather than written into the SQL text, so quotes, dollar signs and backslashes in a value are harmless and text
     * that looks like a number stays text. Booleans are stored as 1/0 like the Android driver does.
     */
    private static int bind(PreparedStatement statement, int firstIndex, Object[] values) throws SQLException {
        int index = firstIndex;
        if (values != null) {
            for (Object value : values) {
                bind(statement, index++, value);
            }
        }
        return index;
    }

    private static void bind(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.NULL);
        } else if (value instanceof byte[]) {
            statement.setBytes(index, (byte[]) value);
        } else if (value instanceof Boolean) {
            statement.setInt(index, ((Boolean) value) ? 1 : 0);
        } else if (value instanceof Number) {
            statement.setObject(index, value);
        } else {
            statement.setString(index, String.valueOf(value));
        }
    }

    /**
     * @param tableName    the table to update
     * @param columnValues
     * @param whereClause  The optional WHERE clause to apply when selecting the records
     *                     to update. Passing null will select all rows
     * @param whereArgs    Question marks in the whereClause String will be replaced with
     *                     these whereArgs in order. (where id = ?)
     * @return <b>int</b> the number of rows affected
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public int update(String tableName, HashMap<String, Object> columnValues, String whereClause, Object[] whereArgs) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("update");

        sqlBuilder.append(" " + tableName);

        sqlBuilder.append(" set");

        List<Object> values = new ArrayList<Object>();
        int i = 0;
        for (Entry<String, Object> entry : columnValues.entrySet()) {
            sqlBuilder.append(" " + entry.getKey() + " = ?");
            values.add(entry.getValue());
            if (i++ < columnValues.size() - 1)
                sqlBuilder.append(",");
        }

        if (whereClause != null) {
            sqlBuilder.append(" where " + whereClause);
            if (whereArgs != null) {
                for (Object whereArg : whereArgs)
                    values.add(whereArg);
            }
        }

        sqlBuilder.append(";");

        try (PreparedStatement statement = getConnection().prepareStatement(sqlBuilder.toString())) {
            bind(statement, 1, values.toArray());
            return statement.executeUpdate();
        }
    }

    /**
     * @param tableName   the table from which to delete records
     * @param whereClause The optional WHERE clause to apply when deleting. Passing null
     *                    will delete all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>int</b> the number of rows affected by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public int delete(String tableName, String whereClause, Object[] whereArgs) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("delete from");

        sqlBuilder.append(" " + tableName);

        Object[] boundArgs = null;
        if (whereClause != null) {
            sqlBuilder.append(" where " + whereClause);
            boundArgs = whereArgs;
        }

        sqlBuilder.append(";");

        try (PreparedStatement statement = getConnection().prepareStatement(sqlBuilder.toString())) {
            bind(statement, 1, boundArgs);
            return statement.executeUpdate();
        }
    }

    /**
     * @param tableName    the name of the table into which the record should be inserted
     * @param columnValues a key value pair (columnName, columnValue) representation of
     *                     the record to insert.
     * @return <b>long</b> the rowid of the newly inserted row
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public long insert(String tableName, HashMap<String, Object> columnValues) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder();

        sqlBuilder.append("insert into");

        sqlBuilder.append(" " + tableName);

        List<Object> values = new ArrayList<Object>();
        if (columnValues.isEmpty()) {
            sqlBuilder.append(" default values");
        } else {
            sqlBuilder.append(" (");
            sqlBuilder.append(Query.join(columnValues.keySet().toArray(new String[]{})));
            sqlBuilder.append(") values (");

            int i = 0;
            for (Entry<String, Object> entry : columnValues.entrySet()) {
                sqlBuilder.append("?");
                values.add(entry.getValue());
                if (i++ < columnValues.size() - 1)
                    sqlBuilder.append(",");
            }

            sqlBuilder.append(")");
        }

        sqlBuilder.append(";");

        try (PreparedStatement statement = getConnection().prepareStatement(sqlBuilder.toString())) {
            bind(statement, 1, values.toArray());
            long result = statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    result = generatedKeys.getLong(1);
                }
            }
            return result;
        }
    }

    /* (non-Javadoc)
     * @see za.co.neilson.sqlite.orm.DatabaseDriverInterface#isNumericType(java.lang.Class)
     */
    @Override
    public boolean isNumericType(Class<?> type) {
        return Query.isNumericType(type);
    }

    /**
     * @param tableName   the name of the table to query
     * @param whereClause The WHERE clause used to identify which records exist in the table
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>boolean</b> true if at least one record matching the where clause exists in the table
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    @Override
    public boolean exists(String tableName, String whereClause, Object[] whereArgs) throws SQLException {
        ResultSet resultSet = null;
        try {

            StringBuilder sql = new StringBuilder("SELECT EXISTS(SELECT 1 FROM ");
            sql.append(tableName);
            if (whereClause != null) {
                sql.append(" WHERE ");
                sql.append(whereClause);
            }
            sql.append(" LIMIT 1);");

            resultSet = rawQuery(sql.toString(), whereArgs);

            if (resultSet.next()) {
                int result = resultSet.getInt(getFirstColumnIndex());
                return result == 1;
            } else {
                return false;
            }

        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
    }

    /**
     * @param tableName   the name of the table to query
     * @param whereClause The WHERE clause used to identify which records exist in the table
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>long</b> the number of records matching the where clause the given table
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    @Override
    public long getCount(String tableName, String whereClause, Object[] whereArgs) throws SQLException {

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(tableName);
        if (whereClause != null) {
            sql.append(" WHERE ");
            sql.append(whereClause);
        }
        sql.append(";");

        return getCount(sql.toString(), whereArgs);
    }

    /**
     * @param sql       the query to execute (SELECT COUNT(*) FROM ...)
     * @param whereArgs Question marks in the sql String will be replaced with
     *                  these whereArgs in order. (where id = ?)
     * @return <b>long</b> the number of records matching the where clause in the the given table
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 1
     */
    @Override
    public long getCount(String sql, Object[] whereArgs) throws SQLException {
        ResultSet resultSet = null;
        try {

            resultSet = rawQuery(sql, whereArgs);

            if (resultSet.next()) {
                long result = resultSet.getLong(getFirstColumnIndex());
                return result;
            } else {
                throw new SQLException("Empty result. A result with a count of 0 is expected. ");
            }

        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
    }

    @Override
    public String getResultColumnName(ResultSet resultSet, int objectModelColumnIndex) {
        try {
            return resultSet.getMetaData().getColumnLabel(objectModelColumnIndex);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getResultColumnCount(ResultSet resultSet) {
        try {
            return resultSet.getMetaData().getColumnCount();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

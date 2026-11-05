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
package za.co.neilson.sqlite.orm.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.io.File;
import java.sql.SQLException;

import za.co.neilson.sqlite.orm.DatabaseDriverInterface;
import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.Query;

/**
 * <p>
 * The interface between the sqlite JDBC driver and the AndroidDatabaseModel.
 * </p>
 * <p>
 * The DatabaseDriverInterface class should never be instantiated directly. This
 * is done via the AndroidDatabaseModel which holds an internal reference to the
 * DatabaseDriverInterface
 * </p>
 *
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 * @version 0.1
 * @since 0.1
 */
public class AndroidSqliteDatabaseDriverInterface extends SQLiteOpenHelper implements DatabaseDriverInterface<Cursor, ContentValues> {

    private File dataBaseFile;
    private SQLiteDatabase sqLiteDatabase;

    /**
     * <p>
     * The interface between the sqlite driver and the AndroidDatabaseModel.
     * </p>
     * <p>
     * The DatabaseDriverInterface class should never be instantiated directly.
     * This is done via the AndroidDatabaseModel which holds an internal
     * reference to the DatabaseDriverInterface
     * </p>
     *
     * @param databaseModel the model
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public AndroidSqliteDatabaseDriverInterface(Context context, DatabaseModel<Cursor, ContentValues> databaseModel) {
        super(context, databaseModel.getDatabaseName(), null, databaseModel.getDatabaseVersion());
        try {
            if (databaseModel.getDatabaseName() == null || databaseModel.getDatabaseName().length() == 0) {
                throw new IllegalArgumentException("Invalid path provided: The path provided is null or empty");
            }
            File dataBaseFile = context.getDatabasePath(databaseModel.getDatabaseName());
            if (dataBaseFile != null && dataBaseFile.isDirectory()) {
                throw new IllegalArgumentException("Invalid path provided: The path provided is a directory.");
            } else {
                this.dataBaseFile = dataBaseFile;
            }
        } catch (IllegalArgumentException illegalArgumentException) {
            illegalArgumentException.printStackTrace();
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
                sqLiteDatabase = getWritableDatabase();
            } else {
                throw new SQLException("Database file not specified");
            }
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.enableWriteAheadLogging();
        db.setMaxSqlCacheSize(SQLiteDatabase.MAX_SQL_CACHE_SIZE);
        //db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Returns the database connection state
     *
     * @return <b>boolean</b> true if the database connection is open
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public boolean isConnected() {
        return sqLiteDatabase != null && sqLiteDatabase.isOpen();
    }

    /**
     * Releases the Connection object's database and JDBC resources immediately
     * instead of waiting for them to be automatically released.
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public void disconnect() {
        if (isConnected()) {
            //sqLiteDatabase.close();
            //sqLiteDatabase = null;
        }
    }

    public void reallyClose(){
        if (isConnected()) {
            sqLiteDatabase.close();
            sqLiteDatabase = null;
        }
    }

    /**
     * Creates / returns the database connection.
     * SQL statements are executed and results are returned within the context of a connection.
     *
     * @return the android.database.sqlite.SQLiteDatabase
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    private SQLiteDatabase getConnection() throws SQLException {
        if (!isConnected()) {
            connect();
        }
        return sqLiteDatabase;
    }

    @Override
    public void beginTransaction() throws SQLException {
        //getConnection().beginTransactionNonExclusive();
    }

    @Override
    public void setTransactionSuccessful() {
        /*if (isConnected())
            sqLiteDatabase.setTransactionSuccessful();
        else
            throw new IllegalStateException("The database is not connected");*/
    }

    @Override
    public void endTransaction() {
        /*if (isConnected())
            sqLiteDatabase.endTransaction();
        else
            throw new IllegalStateException("The database is not connected");*/
    }

    /* (non-Javadoc)
     * @see za.co.neilson.sqlite.orm.DatabaseDriverInterface#getFirstColumnIndex()
     */
    @Override
    public int getFirstColumnIndex() {
        return 0;
    }

    protected String[] getSelectionArgs(Object[] whereArgs) {
        if (whereArgs == null)
            return null;

        String[] selectionArgs = new String[whereArgs.length];
        for (int i = 0; i < whereArgs.length; i++) {
            if (whereArgs[i] == null) {
                // cannot be turned into a String; left null for the caller instead of throwing here
                continue;
            }
            Class c = whereArgs[i].getClass();
            if (c == boolean.class || c == Boolean.class) {
                selectionArgs[i] = ((Boolean) whereArgs[i]) ? "1" : "0";
            } else {
                selectionArgs[i] = String.valueOf(whereArgs[i]);
            }
        }
        return selectionArgs;
    }

    /**
     * Executes the given SQL statement and indicates the form of the first
     * result.
     *
     * @param sql the sql statement to execute
     * @return <b>boolean</b> true if the first result is a ResultSet object;
     * false if it is an update count or there are no results. The
     * AndroidSqliteDatabaseDriverInterface will always return false.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public boolean execute(String sql) throws SQLException {
        SQLiteStatement statement = getConnection().compileStatement(sql);
        statement.execute();
        return false;
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
    public Cursor query(String tableName, String[] columnNames, String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
        String[] selectionArgs = getSelectionArgs(whereArgs);
        return getConnection().query("\"" + tableName + "\"", columnNames, whereClause, selectionArgs, groupBy, having, orderBy, limit == null ? null : String.valueOf(limit));
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
    public Cursor rawQuery(String sql, Object[] whereArgs) throws SQLException {
        //getWriteLock().lock();
        try {
            String[] selectionArgs = getSelectionArgs(whereArgs);
            return getConnection().rawQuery(sql, selectionArgs);
        } finally {
            //getWriteLock().unlock();
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
    public int update(String tableName, ContentValues columnValues, String whereClause, Object[] whereArgs) throws SQLException {
        String[] selectionArgs = getSelectionArgs(whereArgs);
        if (columnValues != null && columnValues.size() > 0) { //Must be updating something...
            try {
                return getConnection().update("\"" + tableName + "\"", columnValues, whereClause, selectionArgs);
            } finally {
            }
        } else {
            return 1;
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
        if (whereClause == null)
            whereClause = "1"; // else we won't get rows affected
        String[] selectionArgs = getSelectionArgs(whereArgs);
        //getWriteLock().lock();
        try {
            return getConnection().delete("\"" + tableName + "\"", whereClause, selectionArgs);
        } finally {
            //getWriteLock().unlock();
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
    public long insert(String tableName, ContentValues columnValues) throws SQLException {
        long rowId;
        rowId = getConnection().insert("\"" + tableName + "\"", null, columnValues);
        //if (rowId < 0)
        //    throw new SQLException("Insert exception hidden by Android API. Check the android log for details");
        // else
        return rowId;
    }

    /**
     * @param type the type to validate as numeric
     * @return <b>boolean</b> true if the supplied Type is used to contain a
     * numeric value
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
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
        Cursor cursor = null;
        try {

            StringBuilder sql = new StringBuilder("SELECT EXISTS(SELECT 1 FROM \"");
            sql.append(tableName);
            sql.append("\"");
            if (whereClause != null) {
                sql.append(" WHERE ");
                sql.append(whereClause);
            }
            sql.append(" LIMIT 1);");

            cursor = rawQuery(sql.toString(), whereArgs);

            if (cursor.moveToFirst()) {
                int result = cursor.getInt(getFirstColumnIndex());
                return result == 1;
            } else {
                return false;
            }

        } finally {
            if (cursor != null) {
                cursor.close();
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
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM \"");
        sql.append(tableName);
        sql.append("\"");
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
        Cursor cursor = null;
        try {

            cursor = rawQuery(sql, whereArgs);

            if (cursor.moveToFirst()) {
                long result = cursor.getLong(getFirstColumnIndex());
                return result;
            } else {
                throw new SQLException("Empty result. A result with a count of 0 is expected. ");
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public String getResultColumnName(Cursor cursor, int columnIndex) {
        return cursor.getColumnName(columnIndex);
    }

    @Override
    public int getResultColumnCount(Cursor cursor) {
        return cursor.getColumnCount();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //
    }
}

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
package za.co.neilson.sqlite.orm;

import java.io.File;
import java.sql.SQLException;

/**
 * <p>
 * The interface between the database driver and the DatabaseModel.
 * </p>
 * <p>
 * The DatabaseDriverInterface class should never be instantiated directly. This
 * is done via the DatabaseModel which holds an internal reference to the
 * DatabaseDriverInterface
 * </p>
 * 
 * @param <R>
 *            The type of the result collection returned by a query. Android
 *            Returns a Cursor Object whilst JDBC returns a ResultSet
  * @param <C>
 *            The type of the result collection returned by a query.
 *            The type of map used to map column names to column values. 
 *            Android requires the use of a ContentValues object whilst JDBC 
 *            simply uses a HashMap<String,Object>
 * @version 0.1
 * @since 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
public interface DatabaseDriverInterface<R,C> {

	/**
	 * @return The database File
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	File getDatabaseFile();

	/**
	 * Opens a connection to the database
	 *
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	void connect() throws SQLException;

	/**
	 * Returns the database connection state
	 * 
	 * @return <b>boolean</b> true if the database connection is open
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	boolean isConnected();

	/**
	 * Releases the Connection object's database and resources immediately
	 * instead of waiting for them to be automatically released.
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	void disconnect();

	/**
	 * Returns the index of the first column of a database result returned by
	 * this driver. Android's SQLiteCursor column indexes are 0 based, JDBC's
	 * ResultSet column indexes start at 1
	 * 
	 * @return <b>int</b> the index of the first column of a database result
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	int getFirstColumnIndex();

	/**
	 * Begins a transaction in EXCLUSIVE mode.
	 * <p>
	 * Transactions can be nested.
	 * When the outer transaction is ended all of
	 * the work done in that transaction and all of the nested transactions will be committed or
	 * rolled back. The changes will be rolled back if any transaction is ended without being
	 * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
	 * </p>
	 * <p>Here is the standard idiom for transactions:
	 *
	 * <pre>
	 *   db.beginTransaction();
	 *   try {
	 *     ...
	 *     db.setTransactionSuccessful();
	 *   } finally {
	 *     db.endTransaction();
	 *   }
	 * </pre>
	 *
	 * @return <b>void</b>
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.1
	 */
	void beginTransaction() throws SQLException;

	/**
	 * Marks the current transaction as successful. Do not do any more database work between
	 * calling this and calling endTransaction. Do as little non-database work as possible in that
	 * situation too. If any errors are encountered between this and endTransaction the transaction
	 * will still be committed.
	 *
	 * @return <b>void</b>
	 * @throws IllegalStateException if the current thread is not in a transaction or the
	 *                               transaction is already marked as successful.
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.1
	 */
	void setTransactionSuccessful();

	/**
	 * End a transaction. See beginTransaction for notes about how to use this and when transactions
	 * are committed and rolled back.
	 *
	 * @return <b>void</b>
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.1
	 */
	void endTransaction();

	/**
	 * Executes the given SQL statement and indicates the form of the first
	 * result.
	 * 
	 * @param sql
	 *            the sql statement to execute
	 * @return <b>boolean</b> true if the first result is a ResultSet object;
	 *         false if it is an update count or there are no results
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	boolean execute(String sql) throws SQLException;

	/**
	 * Queries the given table using any non-null clauses, returning a single
	 * ResultSet object containing the results of the query.
	 * 
	 * @param tableName
	 *            the table to query
	 * @param columnNames
	 *            the specific columns to return in the result set. If
	 *            columNames is null, all columns will be returned.
	 * @param whereClause
	 *            The optional WHERE clause to apply to the query. Passing null
	 *            will select all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @param groupBy
	 *            a comma delimited list of the column names to group the
	 *            returned results by
	 * @param having
	 * @param orderBy
	 *            a comma delimited list of the column names to order the
	 *            returned results by
	 * @param limit
	 *            an optional numeric limit on the number of results returned
	 * @return <b>ResultSet</b> the results of the query
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	R query(String tableName, String[] columnNames, String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException;


	/**
	 * @param sql
	 *            the sql query to execute
	 * @param selectionArgs
	 *            any ?s in the sql query will be replaced by the String
	 *            representation of these selectionArgs in order
	 * @return ResultSet
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	R rawQuery(String sql, Object... selectionArgs) throws SQLException;

	/**
	 * @param tableName
	 *            the table to update
	 * @param columnValues
	 * @param whereClause
	 *            The optional WHERE clause to apply when selecting the records
	 *            to update. Passing null will select all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @return <b>int</b> the number of rows affected
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	int update(String tableName, C columnValues, String whereClause, Object[] whereArgs) throws SQLException;

	/**
	 * @param tableName
	 *            the table from which to delete records
	 * @param whereClause
	 *            The optional WHERE clause to apply when deleting. Passing null
	 *            will delete all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @return <b>int</b> the number of rows affected by the query
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	int delete(String tableName, String whereClause, Object[] whereArgs) throws SQLException;

	/**
	 * @param tableName
	 *            the name of the table into which the record should be inserted
	 * @param columnValues
	 *            a key value pair (columnName, columnValue) representation of
	 *            the record to insert.
	 * @return <b>long</b> the id of the newly inserted row
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	long insert(String tableName, C columnValues) throws SQLException;

	/**
	 * Returns true if a value need not be wrapped in quotes in a SQL query
	 * 
	 * @param type
	 *            the type to validate as numeric
	 * @return <b>boolean</b> true if the supplied Type is used to contain a
	 *         numeric value
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	boolean isNumericType(Class<?> type);

	/**
	 * @param tableName    the name of the table to query
	 * @param whereClause  The WHERE clause used to identify which records exist in the table
	 * @param whereArgs    Question marks in the whereClause String will be replaced with
	 *                     these whereArgs in order. (where id = ?)
	 * @return <b>boolean</b> true if at least one record matching the where clause exists in the table
	 * @throws SQLException
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.2
	 */
	boolean exists(String tableName, String whereClause, Object[] whereArgs) throws SQLException;

	/**
	 * @param tableName    the name of the table to query
	 * @param whereClause  The WHERE clause used to identify which records exist in the table
	 * @param whereArgs    Question marks in the whereClause String will be replaced with
	 *                     these whereArgs in order. (where id = ?)
	 * @return <b>long</b> the number of records matching the where clause in the the given table
	 * @throws SQLException
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.2
	 */
	long getCount(String tableName, String whereClause, Object[] whereArgs) throws SQLException;

	/**
	 * @param sql    	   the query to execute (SELECT COUNT(*) FROM ...)
	 * @param whereArgs    Question marks in the sql String will be replaced with
	 *                     these whereArgs in order. (where id = ?)
	 * @return <b>long</b> the number of records matching the where clause in the the given table
	 * @throws SQLException
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 1
	 */
	long getCount(String sql, Object[] whereArgs) throws SQLException;

	String getResultColumnName(R r, int objectModelColumnIndex);

	int getResultColumnCount(R r);
}

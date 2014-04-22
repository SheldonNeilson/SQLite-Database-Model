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
	public File getDatabaseFile();

	/**
	 * Opens a connection to the database
	 * 
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract void connect() throws SQLException;

	/**
	 * Returns the database connection state
	 * 
	 * @return <b>boolean</b> true if the database connection is open
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract boolean isConnected();

	/**
	 * Releases the Connection object's database and resources immediately
	 * instead of waiting for them to be automatically released.
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract void disconnect();

	/**
	 * Returns the index of the first column of a database result returned by
	 * this driver. Android's SQLiteCursor column indexes are 0 based, JDBC's
	 * ResultSet column indexes start at 1
	 * 
	 * @return <b>int</b> the index of the first column of a database result
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract int getFirstColumnIndex();

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
	public abstract boolean execute(String sql) throws SQLException;

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
	public abstract R query(String tableName, String[] columnNames, String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException;

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
	public abstract R rawQuery(String sql, Object[] selectionArgs) throws SQLException;

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
	public abstract int update(String tableName, C columnValues, String whereClause, Object[] whereArgs) throws SQLException;

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
	public abstract int delete(String tableName, String whereClause, Object[] whereArgs) throws SQLException;

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
	public abstract long insert(String tableName, C columnValues) throws SQLException;

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
	public boolean isNumericType(Class<?> type);

}

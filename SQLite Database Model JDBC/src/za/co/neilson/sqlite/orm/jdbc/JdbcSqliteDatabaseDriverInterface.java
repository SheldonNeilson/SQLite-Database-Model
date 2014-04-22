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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
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
 * @version 0.1
 * @since 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
public class JdbcSqliteDatabaseDriverInterface implements DatabaseDriverInterface<ResultSet, HashMap<String,Object>> {

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
	 * @param databaseModel
	 *            the model
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public JdbcSqliteDatabaseDriverInterface(DatabaseModel<ResultSet, HashMap<String, Object>> databaseModel) {
		
		try {
			// Load the sqlite-JDBC driver using the current class loader
			Class.forName("org.sqlite.JDBC");

			if (databaseModel.getDatabaseName() == null || databaseModel.getDatabaseName().isEmpty()) {
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
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void connect() throws SQLException {
		if (!isConnected()) {
			if (dataBaseFile != null) {
				connection = DriverManager.getConnection("jdbc:sqlite:" + dataBaseFile.getName());

				// Ensure referential integrity is maintained
				execute("PRAGMA foreign_keys = 1;");
			}else{
				throw new SQLException("Database file not specified");
			}
		}
	}

	/**
	 * Returns the database connection state
	 * 
	 * @return <b>boolean</b> true if the database connection is open
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
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
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
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
	 * @return the java.sql.Connection
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws SQLException 
	 */
	private Connection getConnection() throws SQLException{
		if(!isConnected())
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
	public boolean execute(String sql) throws SQLException {
		Statement statement;
		statement = getConnection().createStatement();
		return statement.execute(sql);
	}

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
	public ResultSet query(String tableName, String[] columnNames, String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
		Statement statement = getConnection().createStatement();

		StringBuilder sqlBuilder = new StringBuilder();

		sqlBuilder.append("select ");

		if (columnNames != null && columnNames.length > 0) {
			sqlBuilder.append(Query.join(columnNames));
		} else {
			sqlBuilder.append("*");
		}

		sqlBuilder.append(" from " + tableName);

		if (whereClause != null) {
			if (whereArgs != null && whereArgs.length > 0) {
				for (Object whereArg : whereArgs) {
					whereArg = Query.isNumeric(whereArg) ? whereArg : "'" + whereArg + "'";
					whereClause = whereClause.replaceFirst("\\?", String.valueOf(whereArg));
				}
			}
			sqlBuilder.append(" where " + whereClause);
		}

		if (groupBy != null)
			sqlBuilder.append(" group by " + groupBy);

		if (orderBy != null)
			sqlBuilder.append(" order by " + orderBy);

		if (limit != null)
			sqlBuilder.append(" limit " + String.valueOf(limit));

		sqlBuilder.append(";");

		ResultSet resultSet = statement.executeQuery(sqlBuilder.toString());

		return resultSet;
	}

	/**
	 * @param sql
	 *            the sql query to execute
	 * @param whereArgs
	 *            any ?s in the sql query will be replaced by the String
	 *            representation of these selectionArgs in order
	 * @return ResultSet
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public ResultSet rawQuery(String sql, Object[] whereArgs) throws SQLException {

		for (Object selectionArg : whereArgs) {
			sql = sql.replaceFirst("\\?", String.valueOf(selectionArg));
		}

		Statement statement = getConnection().createStatement();
		ResultSet resultSet = statement.executeQuery(sql);
		return resultSet;
	}

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
	public int update(String tableName, HashMap<String, Object> columnValues, String whereClause, Object[] whereArgs) throws SQLException {
		int result = 0;

		Statement statement = getConnection().createStatement();

		StringBuilder sqlBuilder = new StringBuilder();

		sqlBuilder.append("update");

		sqlBuilder.append(" " + tableName);

		sqlBuilder.append(" set");

		int i = 0;
		for (Entry<String, Object> entry : columnValues.entrySet()) {
			sqlBuilder.append(" " + entry.getKey() + " = ");
			if (entry.getValue() == null) {
				sqlBuilder.append("NULL");
			} else {

				boolean numeric = Query.isNumeric(entry.getValue());
				if (!numeric)
					sqlBuilder.append("'");

				sqlBuilder.append(entry.getValue());

				if (!numeric)
					sqlBuilder.append("'");
			}
			if (i++ < columnValues.size() - 1)
				sqlBuilder.append(",");
		}

		if (whereClause != null) {
			if (whereArgs != null && whereArgs.length > 0) {
				for (Object whereArg : whereArgs) {
					whereArg = Query.isNumeric(whereArg) ? whereArg : "'" + whereArg + "'";
					whereClause = whereClause.replaceFirst("\\?", String.valueOf(whereArg));
				}
			}
			sqlBuilder.append(" where " + whereClause);
		}

		sqlBuilder.append(";");

		result = statement.executeUpdate(sqlBuilder.toString());

		return result;
	}

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
	public int delete(String tableName, String whereClause, Object[] whereArgs) throws SQLException {
		int result = 0;

		Statement statement = getConnection().createStatement();

		StringBuilder sqlBuilder = new StringBuilder();

		sqlBuilder.append("delete from");

		sqlBuilder.append(" " + tableName);

		if (whereClause != null) {
			if (whereArgs != null && whereArgs.length > 0) {
				for (Object whereArg : whereArgs) {
					whereArg = Query.isNumeric(whereArg) ? whereArg : "'" + whereArg + "'";
					whereClause = whereClause.replaceFirst("\\?", String.valueOf(whereArg));
				}
			}
			sqlBuilder.append(" where " + whereClause);
		}

		sqlBuilder.append(";");

		result = statement.executeUpdate(sqlBuilder.toString());

		return result;
	}

	/**
	 * @param tableName
	 *            the name of the table into which the record should be inserted
	 * @param columnValues
	 *            a key value pair (columnName, columnValue) representation of
	 *            the record to insert.
	 * @return <b>long</b> the rowid of the newly inserted row
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public long insert(String tableName, HashMap<String, Object> columnValues) throws SQLException {
		long result = 0;

		Statement statement = getConnection().createStatement();

		StringBuilder sqlBuilder = new StringBuilder();

		sqlBuilder.append("insert into");

		sqlBuilder.append(" " + tableName);

		sqlBuilder.append(" (");
		sqlBuilder.append(Query.join(columnValues.keySet().toArray(new String[] {})));
		sqlBuilder.append(") values (");

		int i = 0;
		for (Entry<String, Object> entry : columnValues.entrySet()) {
			if (entry.getValue() == null) {
				sqlBuilder.append("NULL");
			} else {
				boolean numeric = Query.isNumeric(entry.getValue());
				if (!numeric)
					sqlBuilder.append("'");

				sqlBuilder.append(entry.getValue());

				if (!numeric)
					sqlBuilder.append("'");
			}
			if (i++ < columnValues.size() - 1)
				sqlBuilder.append(",");
		}

		sqlBuilder.append(");");

		result = statement.executeUpdate(sqlBuilder.toString());

		long lastid = result;
		ResultSet generatedKeys = statement.getGeneratedKeys();
		if (generatedKeys.next()) {
			lastid = generatedKeys.getLong(1);
		}
		result = lastid;

		return result;
	}

	/* (non-Javadoc)
	 * @see za.co.neilson.sqlite.orm.DatabaseDriverInterface#isNumericType(java.lang.Class)
	 */
	@Override
	public boolean isNumericType(Class<?> type) {
		return Query.isNumericType(type);
	}		
}

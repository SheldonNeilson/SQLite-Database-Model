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

import java.io.File;
import java.sql.SQLException;
import za.co.neilson.sqlite.orm.DatabaseDriverInterface;
import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.Query;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

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
 * @version 0.1
 * @since 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
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
	 * @param databaseModel
	 *            the model
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public AndroidSqliteDatabaseDriverInterface(Context context, DatabaseModel<Cursor, ContentValues> databaseModel) {
		super(context, databaseModel.getDatabaseName(), null, databaseModel.getDatabaseVersion());
		try {
			
			if (databaseModel.getDatabaseName() == null || databaseModel.getDatabaseName().isEmpty()) {
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
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void connect() throws SQLException {
		if (!isConnected()) {
			if (dataBaseFile != null) {
				sqLiteDatabase = getWritableDatabase();

				// Ensure referential integrity is maintained
				execute("PRAGMA foreign_keys = 1;");
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
		return sqLiteDatabase != null && sqLiteDatabase.isOpen();
	}

	/**
	 * Releases the Connection object's database and JDBC resources immediately
	 * instead of waiting for them to be automatically released.
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void disconnect() {
		if (isConnected()) {
			sqLiteDatabase.close();
			sqLiteDatabase = null;
		}
	}
	
	/**
	 * Creates / returns the database connection.
	 * SQL statements are executed and results are returned within the context of a connection. 
	 * @return the android.database.sqlite.SQLiteDatabase
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws SQLException 
	 */
	private SQLiteDatabase getConnection() throws SQLException {
		if(!isConnected())
			connect();
		return  sqLiteDatabase;
	}
	
	/* (non-Javadoc)
	 * @see za.co.neilson.sqlite.orm.DatabaseDriverInterface#getFirstColumnIndex()
	 */
	@Override
	public int getFirstColumnIndex() {		
		return 0;
	}

	/**
	 * Executes the given SQL statement and indicates the form of the first
	 * result.
	 * 
	 * @param sql
	 *            the sql statement to execute
	 * @return <b>boolean</b> true if the first result is a ResultSet object;
	 *         false if it is an update count or there are no results. The
	 *         AndroidSqliteDatabaseDriverInterface will always return false.
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
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
	public Cursor query(String tableName, String[] columnNames, String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
		String[] selectionArgs = null;
		if (whereArgs != null) {
			selectionArgs = new String[whereArgs.length];
			for (int i = 0; i < whereArgs.length; i++)
				selectionArgs[i] = String.valueOf(whereArgs[i]);
		}
		return getConnection().query(tableName, columnNames, whereClause, selectionArgs, groupBy, having, orderBy, limit == null ? null : String.valueOf(limit));
	}

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
	public Cursor rawQuery(String sql, Object[] whereArgs) throws SQLException {

		String[] selectionArgs = whereArgs == null ? null : new String[whereArgs.length];
		for (int i = 0; i < whereArgs.length; i++)
			selectionArgs[i] = String.valueOf(whereArgs[i]);
		return getConnection().rawQuery(sql, selectionArgs);
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
	public int update(String tableName, ContentValues columnValues, String whereClause, Object[] whereArgs) throws SQLException {
		String[] selectionArgs = null;
		if (whereArgs != null) {
			selectionArgs = new String[whereArgs.length];
			for (int i = 0; i < whereArgs.length; i++)
				selectionArgs[i] = String.valueOf(whereArgs[i]);
		}
		return getConnection().update(tableName, columnValues, whereClause, selectionArgs);
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
		String[] selectionArgs = null;
		if (whereArgs != null) {
			selectionArgs = new String[whereArgs.length];
			for (int i = 0; i < whereArgs.length; i++)
				selectionArgs[i] = String.valueOf(whereArgs[i]);
		}
		return getConnection().delete(tableName, whereClause, selectionArgs);

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
	public long insert(String tableName, ContentValues columnValues) throws SQLException {
		return getConnection().insert(tableName, null, columnValues);
	}

	/**
	 * @param type
	 * 			the type to validate as numeric
	 * @return <b>boolean</b> true if the supplied Type is used to contain a
	 *         numeric value
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public boolean isNumericType(Class<?> type) {
		return Query.isNumericType(type);
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

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

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

/**
 * The base class for a DatabaseModel
 * 
 * @param <R>
 *            The type of the result collection returned by a query. Android
 *            Returns a Cursor Object whilst JDBC returns a ResultSet
 * @param <C>
 *            The type of the result collection returned by a query. The type of
 *            map used to map column names to column values. Android requires
 *            the use of a ContentValues object whilst JDBC simply uses a
 *            HashMap<String,Object>
 * @version 0.1
 * @since 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
public abstract class DatabaseModel<R, C> {

	private DatabaseDriverInterface<R, C> databaseDriverInterface;
	protected HashMap<Type, ObjectModel<?, R, C>> objectModels;
	
	/**
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public DatabaseModel(Object... args) throws SQLException, ClassNotFoundException, NoSuchFieldException {

		// Instantiate the database driver interface
		this.databaseDriverInterface = onInitializeDatabaseDriverInterface(args);

		// Instantiate the collection of ObjectModels managed by this
		// DatabaseModel
		this.objectModels = new HashMap<Type, ObjectModel<?, R, C>>();

		// Add DatabaseInfo Model (contains version info etc)
		ObjectModel<DatabaseInfo, R, C> databaseInfoModel = onCreateDatabaseInfoModel();
		this.objectModels.put(DatabaseInfo.class, databaseInfoModel);

		// Get Subclass Models
		onRegisterObjectModels(this.objectModels);

		if (!getDatabaseDriverInterface().getDatabaseFile().exists()) {
			onCreate();
		} else {
			// Ensure that the driver has an open connection to the database
			getDatabaseDriverInterface().connect();

			// Check if databaseVersion has changed
			DatabaseInfo databaseInfo = (DatabaseInfo) databaseInfoModel.getFirst(null);
			if (databaseInfo.getVersion() < getDatabaseVersion()) {
				onUpgrade(databaseInfo.getVersion());
			}
		}
	}

	/**
	 * @return A concrete subclass of ObjectModel for the DatabaseInfo class as
	 *         the type of driver in use is unknown, the ObjectModel
	 *         implementation must also be supplied by the concrete
	 *         DatabaseModel subclass
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract ObjectModel<DatabaseInfo, R, C> onCreateDatabaseInfoModel() throws ClassNotFoundException, NoSuchFieldException;

	/**
	 * @param args
	 *            An optional list of arguments to pass to your implementation
	 *            of onInitializeDatabaseDriverInterface that you might require
	 *            to instantiate your driver.
	 * @return the DatabaseDriverInterface connection used by this model. The
	 *         DatabaseDriverInterface connection is instantiated internally.
	 *         The databaseDriverInterface file connected to is determined by
	 *         the getDatabaseName() method.
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected abstract DatabaseDriverInterface<R, C> onInitializeDatabaseDriverInterface(Object... args);

	/**
	 * @return the DatabaseDriverInterface connection used by this model. The
	 *         DatabaseDriverInterface connection is instantiated internally.
	 *         The databaseDriverInterface file connected to is determined by
	 *         the getDatabaseName() method.
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public DatabaseDriverInterface<R, C> getDatabaseDriverInterface() {
		return this.databaseDriverInterface;
	}

	/**
	 * @return <b>HashMap&ltType,ObjectModel&lt?&gt&gt</b> the ObjectModels
	 *         registered to this DatabaseModel
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public HashMap<Type, ObjectModel<?, R, C>> getObjectModels() {
		return objectModels;
	}

	/**
	 * Returns the ObjectModel instance for the specified ObjectModel Type
	 * 
	 * @param type
	 *            the Type of the ObjectModel required
	 * @return <b>ObjectModel&ltT&gt</b> the ObjectModel for the given Type
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	@SuppressWarnings("unchecked")
	public <T> ObjectModel<T, R, C> getObjectModel(Class<T> type) {		
		return (ObjectModel<T, R, C>) getObjectModels().get(type);
	}

	/**
	 * @param objectModels
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 */
	protected abstract void onRegisterObjectModels(HashMap<Type, ObjectModel<?, R, C>> objectModels) throws ClassNotFoundException, NoSuchFieldException;

	/**
	 * The getDatabaseName() method must be overridden to supply a unique name
	 * for the database. This name returned by this method wil be used as the
	 * database file name
	 * 
	 * @param objectModels
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract String getDatabaseName();

	/**
	 * The getDatabaseVersion() method must be overridden to return the database
	 * version number. Future changes in this number will result in a call to
	 * onUpgrade() during DatabaseModel instantiation. database file name
	 * 
	 * @param objectModels
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract int getDatabaseVersion();

	/**
	 * Opens a connection to the database via the DatabaseDriverInterface. If
	 * the connection is already open calling this method will have no effect.
	 * 
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void connect() throws SQLException {
		getDatabaseDriverInterface().connect();
	}

	/**
	 * Returns the connected state of the DatabaseDriverInterface to the
	 * database.
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public boolean isConnected() {
		return getDatabaseDriverInterface().isConnected();
	}

	/**
	 * Close the DatabaseDriverInterface's connection to the database.
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void disconnect() {
		getDatabaseDriverInterface().disconnect();
	}

	/**
	 * Called if the specified database file does not exist during the
	 * instantiation of the DatabaseModel. This method creates the database
	 * file, tables for the objects mapped by the DatabaseModel as well as the
	 * DatabaseInfo table and calls onInsertDefaultValues()
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected void onCreate() throws SQLException {
		try {
			/*
			 * Create a database connection. This creates the database file if
			 * it does not exist.
			 */
			getDatabaseDriverInterface().connect();

			// Delegate table creation to the individual ObjectModels.
			for (ObjectModel<?, ?, ?> objectModel : getObjectModels().values()) {
				objectModel.onCreateTable();
			}

			// Create databaseDriverInterface info table
			DatabaseInfo databaseInfo = new DatabaseInfo();
			databaseInfo.setVersion(getDatabaseVersion());
			databaseInfo.setCreatedDate(new Date());
			databaseInfo.setAccessedDate(databaseInfo.getCreatedDate());
			getObjectModel(DatabaseInfo.class).insert(databaseInfo);

			onInsertDefaultValues();

		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			getDatabaseDriverInterface().disconnect();
		}
	}

	/**
	 * Called if an increase in the databaseVersion number is detected during
	 * the instantiation of the DatabaseModel. By default this method drops the
	 * databases tables and recreates them. Override this method to alter this
	 * behaviour and to insert any default records required.
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected void onUpgrade(int previousVersion) throws SQLException {
		for (ObjectModel<?, ?, ?> objectModel : getObjectModels().values()) {
			getDatabaseDriverInterface().execute("DROP TABLE IF EXISTS " + objectModel.getTableName() + ";");
			objectModel.onCreateTable();
		}

		onCreateDatabaseInfoTable();

		onInsertDefaultValues();
	}

	/**
	 * Creates the DatabaseInfo table and initializes it with database version,
	 * created date and last accessed date information
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected void onCreateDatabaseInfoTable() throws SQLException {
		DatabaseInfo databaseInfo = new DatabaseInfo();
		databaseInfo.setVersion(getDatabaseVersion());
		databaseInfo.setCreatedDate(new Date());
		databaseInfo.setAccessedDate(databaseInfo.getCreatedDate());
		try {
			getObjectModel(DatabaseInfo.class).insert(databaseInfo);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Called after DateBaseModel.onCreate() or DateBaseModel.onUpdate() have
	 * completed. Override this method to insert any default records required
	 * into the databaseDriverInterface.
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected abstract void onInsertDefaultValues();

}

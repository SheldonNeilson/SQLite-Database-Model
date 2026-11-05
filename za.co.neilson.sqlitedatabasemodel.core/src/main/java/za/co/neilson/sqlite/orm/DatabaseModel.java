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
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The base class for a DatabaseModel
 *
 * @param <R> The type of the result collection returned by a query. Android
 *            Returns a Cursor Object whilst JDBC returns a ResultSet
 * @param <C> The type of the result collection returned by a query. The type of
 *            map used to map column names to column values. Android requires
 *            the use of a ContentValues object whilst JDBC simply uses a
 *            HashMap<String,Object>
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 * @version 0.1
 * @since 0.1
 */
public abstract class DatabaseModel<R, C> {

    private static final Object LOCK = new Object();
    private boolean databaseVersionCheckCompleted = false;

    private static final ConcurrentHashMap<Class, DatabaseDriverInterface> databaseDriverInterfaces = new ConcurrentHashMap<>();
    //private DatabaseDriverInterface<R, C> databaseDriverInterface;
    private JournalMode journalMode = JournalMode.DEFAULT;
    protected final LinkedHashMap<Type, ObjectModel<?, R, C>> objectModels = new LinkedHashMap<>();
    ;

    /**
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public DatabaseModel(Object... args) throws SQLException, ClassNotFoundException, NoSuchFieldException {
        synchronized (LOCK) {
            // Instantiate the database driver interface
            if (!databaseDriverInterfaces.containsKey(getClass()))
                databaseDriverInterfaces.put(getClass(), onInitializeDatabaseDriverInterface(args));

            // Add DatabaseInfo Model (contains version info etc)
            ObjectModel<DatabaseInfo, R, C> databaseInfoModel = onCreateDatabaseInfoModel();
            this.objectModels.put(DatabaseInfo.class, databaseInfoModel);

            // Get Subclass Models
            onRegisterObjectModels(this.objectModels);

            if (!getDatabaseDriverInterface().getDatabaseFile().exists()) {
                onCreate();
            } else if (!databaseVersionCheckCompleted) {
                try {
                    // Ensure that the driver has an open connection to the database
                    connect();

                    // Check if databaseVersion has changed
                    DatabaseInfo databaseInfo = databaseInfoModel.getFirst(null);
                    if (databaseInfo.getVersion() != getDatabaseVersion()) {
                        onUpgrade(databaseInfo.getVersion());
                    }
                    databaseVersionCheckCompleted = true;
                } finally {
                    disconnect();
                }
            }
        }
    }

    public JournalMode getJournalMode() {
        return journalMode;
    }

    public synchronized void setJournalMode(JournalMode journalMode) {
        this.journalMode = journalMode;
    }

    /**
     * @return A concrete subclass of ObjectModel for the DatabaseInfo class as
     * the type of driver in use is unknown, the ObjectModel
     * implementation must also be supplied by the concrete
     * DatabaseModel subclass
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public abstract ObjectModel<DatabaseInfo, R, C> onCreateDatabaseInfoModel() throws ClassNotFoundException, NoSuchFieldException;

    /**
     * @return the DatabaseDriverInterface connection used by this model. The
     * DatabaseDriverInterface connection is instantiated internally.
     * The databaseDriverInterface file connected to is determined by
     * the getDatabaseName() method.
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized DatabaseDriverInterface<R, C> getDatabaseDriverInterface() {
        return databaseDriverInterfaces.get(getClass());
    }

    /**
     * @return <b>HashMap&ltType,ObjectModel&lt?&gt&gt</b> the ObjectModels
     * registered to this DatabaseModel
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized LinkedHashMap<Type, ObjectModel<?, R, C>> getObjectModels() {
        return objectModels;
    }

    /**
     * @param args An optional list of arguments to pass to your implementation
     *             of onInitializeDatabaseDriverInterface that you might require
     *             to instantiate your driver.
     * @return the DatabaseDriverInterface connection used by this model. The
     * DatabaseDriverInterface connection is instantiated internally.
     * The databaseDriverInterface file connected to is determined by
     * the getDatabaseName() method.
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected abstract DatabaseDriverInterface<R, C> onInitializeDatabaseDriverInterface(Object... args);

    /**
     * Returns the ObjectModel instance for the specified ObjectModel Type
     *
     * @param type the Type of the ObjectModel required
     * @return <b>ObjectModel&ltT&gt</b> the ObjectModel for the given Type
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized <T> ObjectModel<T, R, C> getObjectModel(Class<T> type) {
        return (ObjectModel<T, R, C>) getObjectModels().get(type);
    }

    /**
     * @param objectModels
     * @throws NoSuchFieldException
     * @throws ClassNotFoundException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected abstract void onRegisterObjectModels(HashMap<Type, ObjectModel<?, R, C>> objectModels) throws ClassNotFoundException, NoSuchFieldException;

    /**
     * The getDatabaseName() method must be overridden to supply a unique name
     * for the database. This name returned by this method wil be used as the
     * database file name
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public abstract String getDatabaseName();

    /**
     * The getDatabaseVersion() method must be overridden to return the database
     * version number. Future changes in this number will result in a call to
     * onUpgrade() during DatabaseModel instantiation. database file name
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public abstract int getDatabaseVersion();

    /**
     * Opens a connection to the database via the DatabaseDriverInterface. If
     * the connection is already open calling this method will have no effect.
     *
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized void connect() throws SQLException {

        getDatabaseDriverInterface().connect();

        // Ensure referential integrity is maintained
        //getDatabaseDriverInterface().execute("PRAGMA foreign_keys = 1;");
        /*if (getJournalMode() == JournalMode.WAL) {
            getDatabaseDriverInterface().execute("PRAGMA journal_mode = WAL;");
        }*/
    }

    /**
     * Returns the connected state of the DatabaseDriverInterface to the
     * database.
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized boolean isConnected() {
        return getDatabaseDriverInterface().isConnected();
    }

    /**
     * Close the DatabaseDriverInterface's connection to the database.
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized void disconnect() {
        getDatabaseDriverInterface().disconnect();
    }

    /**
     * Called if the specified database file does not exist during the
     * instantiation of the DatabaseModel. This method creates the database
     * file, tables for the objects mapped by the DatabaseModel as well as the
     * DatabaseInfo table and calls onInsertDefaultValues()
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected synchronized void onCreate() throws SQLException {
        try {
            /*
             * Create a database connection. This creates the database file if
             * it does not exist.
             */
            connect();

            // Delegate table creation to the individual ObjectModels.
            try {
                getDatabaseDriverInterface().beginTransaction();
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
                getDatabaseDriverInterface().setTransactionSuccessful();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                getDatabaseDriverInterface().endTransaction();
            }


        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            disconnect();
        }
    }

    /**
     * Called if an increase in the databaseVersion number is detected during
     * the instantiation of the DatabaseModel. By default this method drops the
     * databases tables and recreates them. Override this method to alter this
     * behaviour and to insert any default records required.
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected synchronized void onUpgrade(int previousVersion) throws SQLException {
        //Locking is managed by the caller
        getDatabaseDriverInterface().execute("begin;");
        try {
            for (ObjectModel<?, ?, ?> objectModel : getObjectModels().values()) {
                getDatabaseDriverInterface().execute("DROP TABLE IF EXISTS \"" + objectModel.getTableName() + "\";");
                objectModel.onCreateTable();
            }
            onCreateDatabaseInfoTable();
            onInsertDefaultValues();
            getDatabaseDriverInterface().execute("end;");
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly();
            throw e;
        }
    }

    public synchronized void rebuild() throws SQLException {
        //Locking is managed by the caller
        getDatabaseDriverInterface().execute("begin;");
        try {
            for (ObjectModel<?, ?, ?> objectModel : getObjectModels().values()) {
                try {
                    getDatabaseDriverInterface().execute("DROP TABLE IF EXISTS \"" + objectModel.getTableName() + "\";");
                    objectModel.onCreateTable(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            onCreateDatabaseInfoTable();
            onInsertDefaultValues();
            getDatabaseDriverInterface().execute("end;");
        } catch (SQLException | RuntimeException e) {
            rollbackQuietly();
            throw e;
        }
    }

    /**
     * Rolls back the transaction opened with execute("begin;") after a failure, without hiding the original exception.
     */
    private void rollbackQuietly() {
        try {
            getDatabaseDriverInterface().execute("rollback;");
        } catch (Exception ignored) {
            // the original failure is the one that matters to the caller
        }
    }

    /**
     * Creates the DatabaseInfo table and initializes it with database version,
     * created date and last accessed date information
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected synchronized void onCreateDatabaseInfoTable() throws SQLException {
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
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected abstract void onInsertDefaultValues() throws SQLException;

    public enum JournalMode {
        DEFAULT,
        WAL
    }

}

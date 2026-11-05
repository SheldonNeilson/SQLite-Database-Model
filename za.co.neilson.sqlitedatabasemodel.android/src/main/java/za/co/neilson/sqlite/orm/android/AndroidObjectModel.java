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
import android.database.Cursor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.InstanceStateTrackable;
import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.ObjectModelColumn;
import za.co.neilson.sqlite.orm.Relationship;

/**
 * @param <T> The object type managed by this model
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 * @version 0.1
 * @since 0.1
 */
public abstract class AndroidObjectModel<T> extends ObjectModel<T, Cursor, ContentValues> {

    /**
     * <p>
     * The ObjectModel constructor initializes the ObjectModelColumns included
     * in the Model.
     * </p>
     * <p>
     * By default this is done using reflection to inspect the class that the
     * model represents, and creating a column for each property.
     * </p>
     * <p>
     * Override the ObjectModel.onInitializeObjectModelColumns() for finer
     * control over which properties are persisted and how.
     * </p>
     *
     * @param databaseModel
     * @throws NoSuchFieldException
     * @throws ClassNotFoundException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public AndroidObjectModel(DatabaseModel<Cursor, ContentValues> databaseModel) throws ClassNotFoundException, NoSuchFieldException {
        super(databaseModel);
    }

    /**
     * Used internally by other query methods to obtain a <b>ResultSet</b> of a
     * query's results
     *
     * @param whereClause The optional WHERE clause to apply when deleting. Passing null
     *                    will delete all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @param groupBy     The optional group by clause to be appended to the sql
     *                    statement
     * @param having      The optional having clause to be appended to the sql statement
     * @param orderBy     The optional comma delimited list of the column names to order the
     *                    results by
     *                    statement
     * @param limit       The optional limit on the number of results returned
     * @return <b>ResultSet</b> the results returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected final Cursor getCursor(String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
        try {
            return getDatabaseModel().getDatabaseDriverInterface().query(getTableName(), getColumnNames(), whereClause, whereArgs, groupBy, having, orderBy, limit);
        } catch (SQLException e) {
            throw e;
        }
    }

    protected void putNullOrValue(ContentValues columnValues, String key, String value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Long value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Integer value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Short value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Byte value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, byte[] value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Double value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Float value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value);
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Date value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value.getTime());
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Calendar value) {
        if (value == null)
            columnValues.putNull(key);
        else
            columnValues.put(key, value.getTime().getTime());
    }

    protected void putNullOrValue(ContentValues columnValues, String key, Boolean value) {
        columnValues.put(key, value);
    }

    protected Long getValueOrDefault(String columnName, Cursor cursor, int i, Long def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getLong(i);
        } else {
            return def;
        }
    }

    protected Integer getValueOrDefault(String columnName, Cursor cursor, int i, Integer def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getInt(i);
        } else {
            return def;
        }
    }

    protected Short getValueOrDefault(String columnName, Cursor cursor, int i, Short def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getShort(i);
        } else {
            return def;
        }
    }

    protected Double getValueOrDefault(String columnName, Cursor cursor, int i, Double def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getDouble(i);
        } else {
            return def;
        }
    }

    protected Float getValueOrDefault(String columnName, Cursor cursor, int i, Float def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getFloat(i);
        } else {
            return def;
        }
    }

    protected String getValueOrDefault(String columnName, Cursor cursor, int i, String def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getString(i);
        } else {
            return def;
        }
    }

    protected Boolean getValueOrDefault(String columnName, Cursor cursor, int i, Boolean def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getInt(i) == 1;
        } else {
            return def;
        }
    }

    protected Date getValueOrDefault(String columnName, Cursor cursor, int i, Date def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return new Date(cursor.getLong(i));
        } else {
            return def;
        }
    }

    protected byte[] getValueOrDefault(String columnName, Cursor cursor, int i, byte[] def) {
        if (columnName != null && !cursor.isNull(i) && columnName.equals(getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(cursor, i))) {
            return cursor.getBlob(i);
        } else {
            return def;
        }
    }

    protected void onQueryCompleted(Cursor cursor) {
        cursor.close();
    }

    /**
     * <p>
     * Binds the state of the object's properties to the columns in which they
     * will be persisted.
     * </p>
     * <p>
     * If the ObjectModel's ObjectModelColumns were automatically generated,
     * i.e. onInitializeObjectModelColumns() was not overridden, the database
     * table's column names will exactly match. the names of the object's
     * properties.
     * </p>
     * <p>
     * Reflection is used to get the value of the property with the same name as
     * the database table column, and insert it into the Map with the column
     * name as the Map key for use in database operations.
     * </p>
     * <p>
     * Override the ObjectModel.getColumnValues() method for finer control over
     * the way that an object's properties are persisted in the database table.
     * </p>
     *
     * @param t         the Object whose properties must be transformed into the
     *                  HashMap&ltcolumnName, columnValue&gt form by this method
     * @param queryType the type of database query that is going to be performed using
     *                  these columns and values
     * @return <b>HashMap&ltString, Object&gt</b>
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected ContentValues setColumnValues(Object t, QueryType queryType) {
        ContentValues columnValues = new ContentValues();

        for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {

            if (queryType == QueryType.UPDATE && t instanceof InstanceStateTrackable) {
                InstanceStateTrackable stateTrackable = (InstanceStateTrackable) t;
                if (!stateTrackable.isDirty(objectModelColumn.getName()))
                    continue;
            }

            try {
                // Exclude AutoIncrement columns from insert operations
                if (!(objectModelColumn.isAutoIncrement() && queryType == QueryType.INSERT)) {
                    Object value = setColumnValue(t, objectModelColumn);
                    if (value == null) {
                        columnValues.putNull(objectModelColumn.getName());
                    } else if (value.getClass().equals(Byte.class)) {
                        columnValues.put(objectModelColumn.getName(), (Byte) value);
                    } else if (value.getClass().equals(byte[].class)) {
                        columnValues.put(objectModelColumn.getName(), (byte[]) value);
                    } else if (value.getClass().equals(Short.class)) {
                        columnValues.put(objectModelColumn.getName(), (Short) value);
                    } else if (value.getClass().equals(Integer.class)) {
                        columnValues.put(objectModelColumn.getName(), (Integer) value);
                    } else if (value.getClass().equals(Long.class)) {
                        columnValues.put(objectModelColumn.getName(), (Long) value);
                    } else if (value.getClass().equals(Float.class)) {
                        columnValues.put(objectModelColumn.getName(), (Float) value);
                    } else if (value.getClass().equals(Double.class)) {
                        columnValues.put(objectModelColumn.getName(), (Double) value);
                    } else if (value.getClass().equals(Short.class)) {
                        columnValues.put(objectModelColumn.getName(), (Short) value);
                    } else if (value.getClass().equals(String.class)) {
                        columnValues.put(objectModelColumn.getName(), (String) value);
                    } else if (value.getClass().equals(Boolean.class)) {
                        columnValues.put(objectModelColumn.getName(), (Boolean) value);
                    } else if (value.getClass().isEnum()) {
                        columnValues.put(objectModelColumn.getName(), value.toString());
                    }
                }
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }
        return columnValues;
    }

    /**
     * <p>
     * Gets the value of the field represented by the ObjectModelColumn supplied
     * from the ResultSet returned by a database query. The value is applied to
     * the corresponding field of an instance of the object managed by this
     * object model.
     * </p>
     * <p>
     * Reflection is used to set the value of the managed Object's field with
     * the value of the ResultSet field with the same. name.
     * </p>
     * <p>
     * Override the ObjectModel.setColumnValue() method for finer control over
     * the way that a managed object's fields are filled from the result of a
     * database query.
     * </p>
     *
     * @param cursor                          The ResultSet returned as the result of a database query and
     *                                        from which an instance of the object type that this
     *                                        ObjectModel represents must be instantiated
     * @param objectModelColumn               The ObjectModelColumn for which the corresponding Object Field
     *                                        value must be extracted from the ResultSet
     * @param resultSetObjectModelColumnIndex The index of this ObjectModelColumn's value in the ResultSet
     * @return <b>Object</b> the value to set on the Object's corresponding
     * Field from the database query ResultSet for the ObjectModelColumn
     * parameter provided.
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected Object getColumnValue(Cursor cursor, ObjectModelColumn objectModelColumn, int resultSetObjectModelColumnIndex) throws NoSuchFieldException, SecurityException, SQLException {

        if (cursor.getPosition() == -1)
            cursor.moveToFirst();

        Object value = null;
        if (cursor.isNull(resultSetObjectModelColumnIndex)) {
            return null;
        } else if (objectModelColumn.getType() == String.class) {
            value = cursor.getString(resultSetObjectModelColumnIndex);
        } else if (objectModelColumn.getType() == boolean.class || objectModelColumn.getType() == Boolean.class) {
            value = cursor.getInt(resultSetObjectModelColumnIndex) == 1;
        } else if (objectModelColumn.getType() == byte.class || objectModelColumn.getType() == Byte.class) {
            value = Byte.parseByte(cursor.getString(resultSetObjectModelColumnIndex));
        } else if (objectModelColumn.getType() == short.class || objectModelColumn.getType() == Short.class) {
            value = cursor.getShort(resultSetObjectModelColumnIndex);
        } else if (objectModelColumn.getType() == int.class || objectModelColumn.getType() == Integer.class) {
            value = cursor.getInt(resultSetObjectModelColumnIndex);
        } else if (objectModelColumn.getType() == long.class || objectModelColumn.getType() == Long.class) {
            value = cursor.getLong(resultSetObjectModelColumnIndex);
        } else if (objectModelColumn.getType() == double.class || objectModelColumn.getType() == Double.class) {
            value = cursor.getDouble(resultSetObjectModelColumnIndex);
        } else if (objectModelColumn.getType() == float.class || objectModelColumn.getType() == Float.class) {
            value = cursor.getFloat(resultSetObjectModelColumnIndex);
        } else if (objectModelColumn.getType() == Date.class) {
            value = new Date(cursor.getLong(resultSetObjectModelColumnIndex));
        } else if (objectModelColumn.getType() == Calendar.class) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(resultSetObjectModelColumnIndex));
            value = calendar;
        } else if (objectModelColumn.getType() == byte[].class) {
            value = cursor.getBlob(resultSetObjectModelColumnIndex);
        } else if (objectModelColumn.getType().isEnum()) {
            value = enumFromStoredText(objectModelColumn.getType(), cursor.getString(resultSetObjectModelColumnIndex));
        }
        return value;
    }

    /**
     * Creates the table for this object in the SQLite database according to the
     * ObjectModelColumns declared in the concrete subclass constructor.
     *
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */

    /**
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @param orderBy     a comma delimited list of the column names to order the
     *                    returned results by
     * @return <b>T</b> The first instance returned by the query
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public T getFirst(String whereClause, Object[] whereArgs, String orderBy) throws SQLException {
        T t = null;
        Cursor cursor = null;
        try {
            cursor = getCursor(whereClause, whereArgs, null, null, orderBy, 1);

            if (cursor.moveToFirst()) {
                t = getInstance(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return t;
    }

    /**
     * @param sql           The raw sql query to execute. the results returned by this query must be mappable to T
     * @param selectionArgs Question marks in the sql String will be replaced with
     *                      these selectionArgs in order. (where id = ?)
     * @return <b>T</b> the object returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    @Override
    public T getFirstRaw(String sql, Object... selectionArgs) throws SQLException {
        T result = null;
        Cursor cursor = null;
        try {
            cursor = getResultSetRaw(sql, selectionArgs);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result = getInstance(cursor);
                }
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * @param whereClause The optional WHERE clause to apply when deleting. Passing null
     *                    will delete all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @param orderBy     a comma delimited list of the column names to order the
     *                    returned results by
     * @param limit       an optional numeric limit on the number of results returned
     * @return <b>List&lt;T&gt;</b> a List of the objects returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final List<T> getAll(String whereClause, Object[] whereArgs, String orderBy, Integer limit) throws SQLException {
        List<T> list = new ArrayList<T>();
        Cursor cursor = null;
        try {
            cursor = getCursor(whereClause, whereArgs, null, null, orderBy, limit);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        T t = getInstance(cursor);
                        list.add(t);

                    } while (cursor.moveToNext());
                }

            }
        } catch (SQLException e) {
            throw e;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * @param sql           The raw sql query to execute. the results returned by this query must be mappable to T
     * @param selectionArgs Question marks in the sql String will be replaced with
     *                      these selectionArgs in order. (where id = ?)
     * @return <b>List&lt;T&gt;</b> a List of the objects returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    @Override
    public List<T> getAllRaw(String sql, Object... selectionArgs) throws SQLException {
        List<T> list = new ArrayList<T>();
        Cursor cursor = null;
        try {
            cursor = getResultSetRaw(sql, selectionArgs);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        T t = getInstance(cursor);
                        list.add(t);

                    } while (cursor.moveToNext());
                }

            }
        } catch (SQLException e) {
            throw e;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * @param t the Object to insert
     * @return <b>long</b> the row id of the inserted record
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final long insert(T t) throws SQLException {
        long rowId = -1;
        getDatabaseModel().getDatabaseDriverInterface().beginTransaction();
        try {
            rowId = getDatabaseModel().getDatabaseDriverInterface().insert(getTableName(), setColumnValues(t, QueryType.INSERT));

            if ((relationshipManagementMode & RELATIONSHIP_MANAGEMENT_INSERT_RELATED) == RELATIONSHIP_MANAGEMENT_INSERT_RELATED)
                insertOrUpdateRelatedChildObjects(t, rowId, QueryType.INSERT);

            if (t instanceof InstanceStateTrackable) {
                InstanceStateTrackable stateTrackable = (InstanceStateTrackable) t;
                stateTrackable.setClean();
            }
            getDatabaseModel().getDatabaseDriverInterface().setTransactionSuccessful();
        } finally {
            getDatabaseModel().getDatabaseDriverInterface().endTransaction();
        }
        return rowId;
    }

    /**
     * @param collection a list of the Objects to insert
     * @return <b>long[]</b> the row ids of the inserted records
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final long[] insertAll(Collection<T> collection) throws SQLException {
        long[] rowIds = new long[0];
        if (collection != null) {
            rowIds = new long[collection.size()];
            getDatabaseModel().getDatabaseDriverInterface().beginTransaction();
            try {
                int i = 0;
                for (T t : collection) {
                    rowIds[i++] = insert(t);
                }
                getDatabaseModel().getDatabaseDriverInterface().setTransactionSuccessful();
            } finally {
                getDatabaseModel().getDatabaseDriverInterface().endTransaction();
            }
        }
        return rowIds;
    }

    /**
     * Updates the database record identified by the <b>whereClause</b> with the
     * values of the Object <b>t</b>
     *
     * @param t           the Object to update
     * @param whereClause The optional WHERE clause to apply when deleting. Passing null
     *                    will delete all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int update(T t, String whereClause, Object... whereArgs) throws SQLException {
        int result = 0;
//        getDatabaseModel().getDatabaseDriverInterface().beginTransaction();
        try {
            result = getDatabaseModel().getDatabaseDriverInterface().update(getTableName(), setColumnValues(t, QueryType.UPDATE), whereClause, whereArgs);

            if ((relationshipManagementMode & RELATIONSHIP_MANAGEMENT_UPDATE_RELATED) == RELATIONSHIP_MANAGEMENT_UPDATE_RELATED)
                result += insertOrUpdateRelatedChildObjects(t, QueryType.UPDATE);

            if (t instanceof InstanceStateTrackable) {
                InstanceStateTrackable stateTrackable = (InstanceStateTrackable) t;
                stateTrackable.setClean();
            }
//            getDatabaseModel().getDatabaseDriverInterface().setTransactionSuccessful();
        } finally {
//            getDatabaseModel().getDatabaseDriverInterface().endTransaction();
        }
        return result;
    }

    @Override
    protected List<String> getColumnValues(String tableName, String columnName, String whereClause, Object[] whereArgs) throws SQLException {
        List<String> values = new ArrayList<>();
        Cursor cursor = getDatabaseModel().getDatabaseDriverInterface().query(tableName, new String[]{columnName}, whereClause, whereArgs, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int colIndex = getDatabaseModel().getDatabaseDriverInterface().getFirstColumnIndex();
                    do {
                        if (!cursor.isNull(colIndex)) {
                            values.add(cursor.getString(colIndex));
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                onQueryCompleted(cursor);
            }
        }
        return values;
    }
}

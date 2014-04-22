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

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.ObjectModelColumn;
import za.co.neilson.sqlite.orm.Relationship;

/**
 * @param <T>
 *            The object type managed by this model
 * @since 0.1
 * @version 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
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
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @param databaseModel2
	 * @throws NoSuchFieldException
	 * @throws ClassNotFoundException
	 */
	public AndroidObjectModel(DatabaseModel<Cursor, ContentValues> databaseModel) throws ClassNotFoundException, NoSuchFieldException {
		super(databaseModel);
	}

	/**
	 * Used internally by other query methods to obtain a <b>ResultSet</b> of a
	 * query's results
	 * 
	 * @param whereClause
	 *            The optional WHERE clause to apply when deleting. Passing null
	 *            will delete all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @param groupBy
	 *            The optional group by clause to be appended to the sql
	 *            statement
	 * @param having
	 *            The optional having clause to be appended to the sql statement
	 * @param orderBy
	 *            The optional comma delimited list of the column names to order the
	 *            results by
	 *            statement
	 * @param limit
	 *            The optional limit on the number of results returned
	 * @return <b>ResultSet</b> the results returned by the query
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws SQLException
	 */
	protected final Cursor getCursor(String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
		try {
			return (Cursor) getDatabaseModel().getDatabaseDriverInterface().query(getTableName(), getColumnNames(), whereClause, whereArgs, groupBy, having, orderBy, limit);
		} catch (SQLException e) {
			throw e;
		}
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
	 * @param t
	 *            the Object whose properties must be transformed into the
	 *            HashMap&ltcolumnName, columnValue&gt form by this method
	 * @param queryType
	 *            the type of database query that is going to be performed using
	 *            these columns and values
	 * @return <b>HashMap&ltString, Object&gt</b>
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected ContentValues setColumnValues(Object t, QueryType queryType) {
		ContentValues columnValues = new ContentValues();

		int objectModelColumnIndex = 0;
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {

			try {
				// Exclude AutoIncrement columns from insert operations
				if (!(objectModelColumn.isAutoIncrement() && queryType == QueryType.INSERT)) {
					Object value = setColumnValue(t, objectModelColumn);
					if (value == null) {
						columnValues.putNull(getObjectModelColumns()[objectModelColumnIndex].getName());
					} else if (value.getClass().equals(Byte.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Byte) value);
					} else if (value.getClass().equals(byte[].class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (byte[]) value);
					} else if (value.getClass().equals(Short.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Short) value);
					} else if (value.getClass().equals(Integer.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Integer) value);
					} else if (value.getClass().equals(Long.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Long) value);
					} else if (value.getClass().equals(Float.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Float) value);
					} else if (value.getClass().equals(Double.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Double) value);
					} else if (value.getClass().equals(Short.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Short) value);
					} else if (value.getClass().equals(String.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (String) value);
					} else if (value.getClass().equals(Boolean.class)) {
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), (Boolean) value);
					} else if (value.getClass().isEnum()){
						columnValues.put(getObjectModelColumns()[objectModelColumnIndex].getName(), value.toString());
					}
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
			objectModelColumnIndex++;
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
	 * @param cursor
	 *            The ResultSet returned as the result of a database query and
	 *            from which an instance of the object type that this
	 *            ObjectModel represents must be instantiated
	 * @param objectModelColumn
	 *            The ObjectModelColumn for which the corresponding Object Field
	 *            value must be extracted from the ResultSet
	 * @param resultSetObjectModelColumnIndex
	 *            The index of this ObjectModelColumn's value in the ResultSet
	 * @return <b>Object</b> the value to set on the Object's corresponding
	 *         Field from the database query ResultSet for the ObjectModelColumn
	 *         parameter provided.
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected Object getColumnValue(Cursor cursor, ObjectModelColumn objectModelColumn, int resultSetObjectModelColumnIndex) throws NoSuchFieldException, SecurityException, SQLException {

		Field field = getObjetType().getDeclaredField(objectModelColumn.getName());
		boolean accessible = field.isAccessible();
		field.setAccessible(true);

		Object value = null;
		if (cursor.isNull(resultSetObjectModelColumnIndex)) {
			return null;
		} else if (objectModelColumn.getType() == String.class) {
			value = cursor.getString(resultSetObjectModelColumnIndex);
		} else if (objectModelColumn.getType() == boolean.class || objectModelColumn.getType() == Boolean.class) {
			value = cursor.getInt(resultSetObjectModelColumnIndex) == 1;
		} else if (objectModelColumn.getType() == byte.class || objectModelColumn.getType() == Byte.class) {
			value = Byte.parseByte(cursor.getString(resultSetObjectModelColumnIndex));
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
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Object enumValue = Enum.valueOf((Class) objectModelColumn.getType(), cursor.getString(resultSetObjectModelColumnIndex));
			value = enumValue;
		}

		field.setAccessible(accessible);
		return value;
	}

	/**
	 * Creates the table for this object in the SQLite database according to the
	 * ObjectModelColumns declared in the concrete subclass constructor.
	 * 
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void onCreateTable() throws SQLException {
		StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + getTableName());
		sql.append(" ( ");

		boolean hasPrimaryKey = false;
		int primaryKeys = 0;
		for (ObjectModelColumn objectModelColumn : this.getObjectModelColumns()) {
			if (objectModelColumn.isPrimaryKey()) {
				if (!hasPrimaryKey) {
					hasPrimaryKey = true;
				}
				primaryKeys++;
			}
		}

		int columnIndex = 0;
		for (ObjectModelColumn objectModelColumn : this.getObjectModelColumns()) {
			sql.append(objectModelColumn.getName());

			if (objectModelColumn.isPrimaryKey() && objectModelColumn.isAutoIncrement()) {
				sql.append(" INTEGER");
			} else if (objectModelColumn.getType() == char.class) {
				sql.append(" TEXT");
			} else if (objectModelColumn.getType() == char[].class) {
				sql.append(" TEXT");
			} else if (objectModelColumn.getType() == String.class) {
				sql.append(" TEXT");
			} else if (objectModelColumn.getType() == boolean.class || objectModelColumn.getType() == Boolean.class) {
				sql.append(" TINYINT");
			} else if (objectModelColumn.getType() == byte.class || objectModelColumn.getType() == Byte.class) {
				sql.append(" TINYINT");
			} else if (objectModelColumn.getType() == short.class || objectModelColumn.getType() == Short.class) {
				sql.append("SMALLINT");
			} else if (objectModelColumn.getType() == int.class || objectModelColumn.getType() == Integer.class) {
				sql.append(" INT");
			} else if (objectModelColumn.getType() == long.class || objectModelColumn.getType() == Long.class) {
				sql.append(" BIGINT");
			} else if (objectModelColumn.getType() == float.class || objectModelColumn.getType() == Float.class) {
				sql.append(" FLOAT");
			} else if (objectModelColumn.getType() == double.class || objectModelColumn.getType() == Double.class) {
				sql.append(" DOUBLE");
			} else if (objectModelColumn.getType() == Date.class) {
				sql.append(" DATETIME");
			} else if (objectModelColumn.getType() == Calendar.class) {
				sql.append(" DATETIME");
			} else if (objectModelColumn.getType() == byte[].class) {
				sql.append(" BLOB");
			} else {
				sql.append(" BLOB");
			}

			if (objectModelColumn.isPrimaryKey() && primaryKeys == 1) {
				sql.append(" PRIMARY KEY");
				if (objectModelColumn.isAutoIncrement())
					sql.append(" AUTOINCREMENT");
			}

			if (objectModelColumn.isUnique() && !objectModelColumn.isPrimaryKey()) {
				sql.append(" UNIQUE");
			}

			if (!objectModelColumn.isNullable())
				sql.append(" NOT NULL");
			if (columnIndex++ < objectModelColumns.length - 1)
				sql.append(", ");
		}

		if (primaryKeys > 1) {
			int primaryKeyIndex = 0;
			sql.append(", ");
			sql.append(" PRIMARY KEY(");

			for (ObjectModelColumn databaseEntityColumn : this.getObjectModelColumns()) {
				if (databaseEntityColumn.isPrimaryKey()) {
					if (primaryKeyIndex++ != 0)
						sql.append(", ");
					sql.append(databaseEntityColumn.getName());
				}
			}
			sql.append(")");
		}

		columnIndex = 0;
		for (ObjectModelColumn databaseEntityColumn : this.getObjectModelColumns()) {
			if (databaseEntityColumn.getForeignKeyParentTable() != null && databaseEntityColumn.getForeignKeyParentColumn() != null) {
				sql.append(", ");
				sql.append(" FOREIGN KEY(");
				sql.append(databaseEntityColumn.getName());
				sql.append(")");
				sql.append(" REFERENCES ");
				sql.append(databaseEntityColumn.getForeignKeyParentTable());
				sql.append("(");
				sql.append(databaseEntityColumn.getForeignKeyParentColumn());
				sql.append(")");
			}
			columnIndex++;
		}
		sql.append(")");

		getDatabaseModel().getDatabaseDriverInterface().execute(sql.toString());
	}

	/**
	 * @param whereClause
	 *            The optional WHERE clause to apply to the query. Passing null
	 *            will select all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @param orderBy
	 *            a comma delimited list of the column names to order the
	 *            returned results by
	 * @return <b>T</b> The first instance returned by the query
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.1
	 */
	public T getFirst(String whereClause, Object[] whereArgs, String orderBy) {
		T t = null;
		Cursor cursor = null;
		try {
			cursor = getCursor(whereClause, whereArgs, null, null, orderBy, 1);

			if (cursor.moveToFirst()) {
				t = getInstance(cursor);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return t;
	}

	/**
	 * @param whereClause
	 *            The optional WHERE clause to apply when deleting. Passing null
	 *            will delete all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @param orderBy
	 *            a comma delimited list of the column names to order the
	 *            returned results by
	 * @param limit
	 *            an optional numeric limit on the number of results returned
	 * @return <b>List&ltT&gt</b> a List of the objects returned by the query
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
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
	 * @param t
	 *            the Object to insert
	 * @return <b>long</b> the row id of the inserted record
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final long insert(T t) throws SQLException {
		long result = ((AndroidSqliteDatabaseDriverInterface) getDatabaseModel().getDatabaseDriverInterface()).insert(getTableName(), setColumnValues(t, QueryType.INSERT));

		result += insertOrUpdateRelatedChildObjects(t);

		return result;
	}

	/**
	 * @param collection
	 *            a list of the Objects to insert
	 * @return <b>long[]</b> the row ids of the inserted records
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final long[] insertAll(Collection<T> collection) throws SQLException {
		long[] rowIds = new long[collection.size()];
		int i = 0;
		for (Object t : collection) {
			rowIds[i++] = ((AndroidSqliteDatabaseDriverInterface) getDatabaseModel().getDatabaseDriverInterface()).insert(getTableName(), setColumnValues(t, QueryType.INSERT));
		}
		return rowIds;
	}

	/**
	 * Updates the database record identified by the <b>whereClause</b> with the
	 * values of the Object <b>t</b>
	 * 
	 * @param t
	 *            the Object to update
	 * @param whereClause
	 *            The optional WHERE clause to apply when deleting. Passing null
	 *            will delete all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int update(T t, String whereClause, Object... whereArgs) throws SQLException {
		int result = ((AndroidSqliteDatabaseDriverInterface) getDatabaseModel().getDatabaseDriverInterface()).update(getTableName(), setColumnValues(t, QueryType.UPDATE), whereClause, whereArgs);

		result += insertOrUpdateRelatedChildObjects(t);

		return result;
	}

	@Override
	public int deleteAll(String whereClause, Object... whereArgs) throws SQLException {
		int result = 0;

		/*
		 * Delete dependent objects mapped by any relationships to the objects
		 * to be deleted
		 */
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			for (Relationship relationship : objectModelColumn.getRelationships()) {
				if (relationship.parentType.equals(getObjetType())) {

					StringBuilder whereClauseBuilder = new StringBuilder(relationship.childKeyField.getName() + " IN (");

					// Get referenced objects
					Cursor cursor = ((AndroidSqliteDatabaseDriverInterface) getDatabaseModel().getDatabaseDriverInterface()).query(getTableName(), new String[] { relationship.parentKeyField.getName() }, whereClause, whereArgs, null, null, null, null);
					int resultIndex = 0;
					if (cursor.moveToFirst()) {
						do {
							if (resultIndex++ > 0)
								whereClauseBuilder.append(", ");
							if (getDatabaseModel().getDatabaseDriverInterface().isNumericType(relationship.childKeyField.getType())) {
								whereClauseBuilder.append(cursor.getString(1));
							} else {
								whereClauseBuilder.append("'" + cursor.getString(getDatabaseModel().getDatabaseDriverInterface().getFirstColumnIndex()) + "'");
							}
						} while (cursor.moveToNext());
						whereClauseBuilder.append(")");

						ObjectModel<?, ?, ?> objectModel = getDatabaseModel().getObjectModel(relationship.childType);
						result += objectModel.deleteAll(whereClauseBuilder.toString());
					}
				}
			}
		}
		result += getDatabaseModel().getDatabaseDriverInterface().delete(getTableName(), whereClause, whereArgs);
		return result;
	}
}

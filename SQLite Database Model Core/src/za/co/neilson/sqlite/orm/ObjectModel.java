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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import za.co.neilson.sqlite.orm.Relationship.Type;
import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.Nullable;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;
import za.co.neilson.sqlite.orm.annotations.Transient;
import za.co.neilson.sqlite.orm.annotations.Unique;

/**
 * @param <T>
 *            The object type managed by this model
 * @param <R>
 *            The type of the result collection returned by a query. Android
 *            Returns a Cursor Object whilst JDBC returns a ResultSet
  * @param <C>
 *            The type of the result collection returned by a query.
 *            The type of map used to map column names to column values. 
 *            Android requires the use of a ContentValues object whilst JDBC 
 *            simply uses a HashMap<String,Object>
 * @since 0.1
 * @version 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
public abstract class ObjectModel<T, R, C> {

	/**
	 * The type of query being performed for reference in concrete subclasses
	 * when determining the columns and their values to be used in database
	 * operations.
	 * 
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public enum QueryType {
		SELECT, INSERT, UPDATE, DELETE
	}

	protected DatabaseModel<R,C> databaseModel;
	protected ObjectModelColumn[] objectModelColumns;

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
	public ObjectModel(DatabaseModel<R,C> databaseModel) throws ClassNotFoundException, NoSuchFieldException {
		setDatabaseModel(databaseModel);
		this.objectModelColumns = onInitializeObjectModelColumns();
	}

	/**
	 * Get the actual type of the generic type argument used to extend the
	 * ObjectModel class.
	 * 
	 * @return Class<T> the Type of the object managed by this ObjectModel
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getObjetType() {
		ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
		return (Class<T>) superclass.getActualTypeArguments()[0];
	}

	public static <T> T castToObjectType(Object o, Class<T> type) {
		try {
			return type.cast(o);
		} catch (ClassCastException e) {
			return null;
		}
	}

	/**
	 * @return <b>DatabaseModel</b> the DatabaseModel to which this ObjectModel
	 *         is connected
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected final DatabaseModel<R,C> getDatabaseModel() {
		if (this.databaseModel == null)
			throw new NullPointerException("DatabaseModel has not been initialized.");

		return this.databaseModel;
	}

	/**
	 * @param databaseModel
	 *            the DatabaseModel to which this ObjectModel is connected
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected final void setDatabaseModel(DatabaseModel<R,C> databaseModel) {
		if (this.databaseModel == null)
			this.databaseModel = databaseModel;
	}

	/**
	 * @return <b>String</b> the ObjectModel's database table name
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected String getTableName() {
		return getObjetType().getSimpleName();
	}

	/**
	 * <p>
	 * By default the fields for which database tables are created is determined
	 * using reflection to inspect the class that the model represents, and
	 * creating a column for each property.
	 * </p>
	 * <p>
	 * Override the ObjectModel.onInitializeObjectModelColumns() for finer
	 * control over which properties are persisted and how.
	 * </p>
	 * 
	 * @return <b>ObjectModelColumn[]</b> the ObjectModel's database table
	 *         columns
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws ClassNotFoundException
	 * @throws NoSuchFieldException
	 */
	protected ObjectModelColumn[] onInitializeObjectModelColumns() throws ClassNotFoundException, NoSuchFieldException {
		Class<?> type = getObjetType();
		Field[] fields = type.getDeclaredFields();

		/*
		 * Count the number of fields to model
		 */
		int supportedFields = 0;
		for (int i = 0; i < fields.length; i++)
			// Do not map field types that are not supported for ORM
			if (ObjectModelColumn.isSupportedType(fields[i].getType())
			// Do not map fields marked with the Transient annotation
			&& !fields[i].isAnnotationPresent(Transient.class))
				supportedFields++;

		/*
		 * Initialize the objectModelColumns array with the number of
		 * supportedFields
		 */
		this.objectModelColumns = new ObjectModelColumn[supportedFields];

		/*
		 * Create the objectModelColumns and add them to the
		 * objectModelColumns[] array
		 */
		int supportedFieldIndex = 0;
		for (int i = 0; i < fields.length; i++) {
			// Do not map field types that are not supported for ORM
			if (ObjectModelColumn.isSupportedType(fields[i].getType())
			// Do not map fields marked with the Transient annotation
			&& !fields[i].isAnnotationPresent(Transient.class)) {
				boolean accessible = fields[i].isAccessible();
				fields[i].setAccessible(true);
				try {
					// Construct new ObjectModelColumn
					objectModelColumns[supportedFieldIndex] = new ObjectModelColumn(fields[i].getName(), fields[i].getType(), true);

					// Create primary key if applicable
					if (fields[i].isAnnotationPresent(PrimaryKey.class)) {
						PrimaryKey primaryKey = fields[i].getAnnotation(PrimaryKey.class);
						objectModelColumns[supportedFieldIndex].setPrimaryKey(true);
						objectModelColumns[supportedFieldIndex].setAutoIncrement(primaryKey.autoIncrement());
					} else {
						// PrimaryKey columns cannot be nullable
						if (fields[i].isAnnotationPresent(Nullable.class)) {
							Nullable nullable = fields[i].getAnnotation(Nullable.class);
							objectModelColumns[supportedFieldIndex].setNullable(nullable.value());
						}
					}
					// Create foreign key if applicable
					if (fields[i].isAnnotationPresent(ForeignKey.class)) {
						ForeignKey foreignKey = fields[i].getAnnotation(ForeignKey.class);
						objectModelColumns[supportedFieldIndex].setForeignKeyParentTable(foreignKey.table());
						objectModelColumns[supportedFieldIndex].setForeignKeyColumn(foreignKey.column());

						za.co.neilson.sqlite.orm.Relationship relationship = new za.co.neilson.sqlite.orm.Relationship();
						try {
							/*
							 * Create a relationship with the object referenced
							 * by the foreign key. This creates a requirement
							 * for the parent ObjectModel to be created first.
							 */

							// Find the Parent Object Type
							ObjectModel<?,?,?> parentTypeObjectModel = null;
							Class<?> parentType = null;
							for (ObjectModel<?,?,? > objectModel : getDatabaseModel().getObjectModels().values()) {
								if (objectModel.getObjetType().getSimpleName().equalsIgnoreCase(foreignKey.table())) {
									parentTypeObjectModel = objectModel;
									parentType = objectModel.getObjetType();
									break;
								}
							}
							if (parentType == null)
								throw new ClassNotFoundException("The ObjectModel, " + foreignKey.table() + ", referenced by the foreign key, " + fields[i].getName() + ", does not exist. Ensure that it was added to the DatabaseModel first.");
							relationship.setParentType(parentType);

							// Find The Parent Object Field referenced by the
							// key
							Field parentKeyField = parentType.getDeclaredField(foreignKey.column());
							relationship.setParentKeyField(parentKeyField);

							ObjectModelColumn parentKeyFieldObjectModelColumn = null;
							for (ObjectModelColumn objectModelColumn : parentTypeObjectModel.getObjectModelColumns()) {
								if (objectModelColumn.getName().equals(foreignKey.column())) {
									parentKeyFieldObjectModelColumn = objectModelColumn;
									break;
								}
							}

							// The Child Object Type (This ObjectModel's type)
							Class<?> childType = getObjetType();
							relationship.setChildType(childType);

							// The Child Object Filed that references the parent
							// object's key Field
							Field foreignKeyField = fields[i];
							relationship.setChildKeyField(foreignKeyField);

							/*
							 * The Child Object's reference to the Parent Object
							 * This is an optional attribute that can be omitted
							 * to prevent the system from automatically
							 * retrieving the related object(s) and filling this
							 * reference.
							 */
							if (!foreignKey.childReference().isEmpty()) {
								Field childReferenceField = getObjetType().getDeclaredField(foreignKey.childReference());
								relationship.setChildReferenceField(childReferenceField);
							}

							/*
							 * The Parent Object's reference to the Child Object
							 * This is an optional attribute that can be omitted
							 * to prevent the system from automatically
							 * retrieving the related object(s) and filling this
							 * reference.
							 */
							if (!foreignKey.parentReference().isEmpty()) {
								Field parentReferenceField = parentType.getDeclaredField(foreignKey.parentReference());
								relationship.setParentReferenceField(parentReferenceField);
							}

							// What type of relationship is this?
							// If the field referenced in the parent is the
							// parent's primary key, there can only be one
							// matching parent
							if (parentKeyField.isAnnotationPresent(PrimaryKey.class)) {
								// If the foreign key field is also the table's
								// primary key, there can only be one matching
								// child
								if (foreignKeyField.isAnnotationPresent(PrimaryKey.class)) {
									relationship.setRelationshipType(Type.ONE_TO_ONE);
								} else {
									relationship.setRelationshipType(Type.ONE_TO_MANY);
								}
							} else {
								relationship.setRelationshipType(Type.MANY_TO_MANY);
							}

							objectModelColumns[supportedFieldIndex].getRelationships().add(relationship);
							parentKeyFieldObjectModelColumn.getRelationships().add(relationship);

						} catch (ClassNotFoundException e) {
							throw e;
						} catch (NoSuchFieldException e) {
							throw e;
						} catch (SecurityException e) {
							throw e;
						}
					}
					if (fields[i].isAnnotationPresent(Unique.class)) {
						// Unique unique =
						// fields[i].getAnnotation(Unique.class);
						objectModelColumns[supportedFieldIndex].setUnique(true);
					}

				} catch (UnsupportedTypeException e) {
					e.printStackTrace();
				}
				// Reset accessibility to default
				fields[i].setAccessible(accessible);
				supportedFieldIndex++;
			}
		}
		return this.objectModelColumns;
	}

	/**
	 * @return <b>ObjectModelColumn[]</b> the ObjectModel's database table
	 *         columns
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final ObjectModelColumn[] getObjectModelColumns() {
		if (objectModelColumns == null)
			throw new NullPointerException("ObjectModelColumns have not been initialized");
		return objectModelColumns;
	}

	/**
	 * @return <b>String[]</b> an array of the ObjectModel table's column names
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected String[] getColumnNames() {
		String[] columnNames = new String[getObjectModelColumns().length];

		for (int i = 0; i < columnNames.length; i++)
			columnNames[i] = getObjectModelColumns()[i].getName();

		return columnNames;
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
	 *            The optional order by clause to be appended to the sql
	 *            statement
	 * @param limit
	 *            The optional limit on the number of results returned
	 * @return <b>ResultSet</b> the results returned by the query
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws SQLException
	 */	
	protected final R getResultSet(String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
		try {
			return (R) getDatabaseModel().getDatabaseDriverInterface().query(getTableName(), getColumnNames(), whereClause, whereArgs, groupBy, having, orderBy, limit);
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
	protected abstract C setColumnValues(Object t, QueryType queryType);
	
	/**
	 * <p>
	 * Gets the value of the field belonging to Object <b>t</b> that should be
	 * stored in the database column corresponding to the ObjectModelColumn
	 * supplied.
	 * </p>
	 * <p>
	 * If the ObjectModel's ObjectModelColumns were automatically generated,
	 * i.e. onInitializeObjectModelColumns() was not overridden, the database
	 * table's column names will exactly match. the names of the object's
	 * properties.
	 * </p>
	 * <p>
	 * Reflection is used to get the value of the field with the same name as
	 * the database table column, and assign it to the ObjectModelColumn's value
	 * for use in database operations.
	 * </p>
	 * <p>
	 * Override the ObjectModel.setColumnValue() method for finer control over
	 * the way that an object's properties are persisted in the database table.
	 * </p>
	 * 
	 * @param t
	 *            the Object with which to set the column value
	 * @param objectModelColumn
	 *            the database column to set to the corresponding object field
	 *            value
	 * @return <b>Object</b> the appropriate table column value for the
	 *         ObjectModelColumn supplied
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected Object setColumnValue(Object t, ObjectModelColumn objectModelColumn) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		Object value = null;
		Class<?> c = getObjetType();
		Field field = c.getDeclaredField(objectModelColumn.getName());
		field.setAccessible(true);
		if (objectModelColumn.getType() == boolean.class) {
			value = (Boolean) field.get(t) ? 1 : 0;
		} else if (objectModelColumn.getType() == Date.class) {
			value = field.get(t) == null ? null : ((Date) field.get(t)).getTime();
		} else if (objectModelColumn.getType() == Calendar.class) {
			value = field.get(t) == null ? null : ((Calendar) field.get(t)).getTimeInMillis();
		} else {
			value = field.get(t);
		}

		return value;
	}

	/**
	 * <p>
	 * Gets the value of the field represented by the ObjectModelColumn supplied
	 * from the ResultSet returned by a database query. The value is applied to
	 * the corresponding field of an instance of the object managed by this
	 * object model.
	 * </p>
	 * <p>
	 * Reflection is used to set the value of the managed Object's field
	 * with the value of the ResultSet field with the same.
	 * name.
	 * </p>
	 * <p>
	 * Override the ObjectModel.setColumnValue() method for finer control over
	 * the way that a managed object's fields are filled from the result of a
	 * database query.
	 * </p>
	 * 
	 * @param r
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
	protected abstract Object getColumnValue(R r, ObjectModelColumn objectModelColumn, int resultSetObjectModelColumnIndex) throws NoSuchFieldException, SecurityException, SQLException;

	/**
	 * Create an instance of the Object represented by this model using the
	 * ResultSet returned by a database query
	 * 
	 * @param r
	 * @return the ResultSet containing the properties of the Object to
	 *         instantiate
	 * 
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected T getInstance(R r) {
		try {
			Class<?> c = getObjetType();

			@SuppressWarnings("unchecked")
			T t = (T) c.newInstance();

			int objectModelColumnIndex = getDatabaseModel().getDatabaseDriverInterface().getFirstColumnIndex();
			for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {

				Field field = c.getDeclaredField(objectModelColumn.getName());
				field.setAccessible(true);

				field.set(t, getColumnValue(r, objectModelColumn, objectModelColumnIndex));

				for (Relationship relationship : objectModelColumn.getRelationships()) {

					if (relationship.parentKeyField.equals(field)) {

						relationship.parent = t;

						/*
						 * I am the relationship parent, fill my reference to
						 * the child object
						 */

						ObjectModel<?,?,?> childObjectModel = getDatabaseModel().getObjectModel(relationship.childType);

						String whereClause = relationship.childKeyField.getName() + " = ? ";
						Object[] whereArgs = new String[] { field.get(t).toString() };

						switch (relationship.type) {
						case MANY_TO_MANY:
						case ONE_TO_MANY:
							Collection<?> collection = childObjectModel.getAll(whereClause, (Object[])whereArgs);
							relationship.child = collection;
							break;
						case ONE_TO_ONE:
							relationship.child = childObjectModel.getFirst(whereClause, whereArgs);
							break;
						}
						if (relationship.parentReferenceField != null) {
							boolean accessible = relationship.parentReferenceField.isAccessible();
							relationship.parentReferenceField.setAccessible(true);
							relationship.parentReferenceField.set(t, relationship.child);
							relationship.parentReferenceField.setAccessible(accessible);
						}
					} else if (relationship.childKeyField.equals(field)) {

						/*
						 * I am the relationship child, fill my reference to the
						 * parent object
						 */
						if (relationship.parent == null) {

							ObjectModel<?,?,?> parentObjectModel = getDatabaseModel().getObjectModel(relationship.parentType);

							String whereClause = relationship.parentKeyField.getName() + " = ? ";
							Object[] whereArgs = new String[] { field.get(t).toString() };

							switch (relationship.type) {
							case MANY_TO_MANY:
								break;
							case ONE_TO_MANY:
							case ONE_TO_ONE:
								relationship.parent = parentObjectModel.getFirst(whereClause, whereArgs);
								break;
							}

						}
						if (relationship.childReferenceField != null) {
							boolean accessible = relationship.childReferenceField.isAccessible();
							relationship.childReferenceField.setAccessible(true);
							relationship.childReferenceField.set(t, relationship.parent);
							relationship.childReferenceField.setAccessible(accessible);
						}
					}

				}

				objectModelColumnIndex++;

			}
			return t;
		} catch (NoSuchFieldException | SecurityException | SQLException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates the table for this object in the SQLite database according to the
	 * ObjectModelColumns declared in the concrete subclass constructor.
	 * 
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	protected void onCreateTable() throws SQLException {
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

			if (objectModelColumn.primaryKey && objectModelColumn.autoIncrement) {
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
	 * @return <b>T</b> The first instance returned by the query
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws SQLException 
	 * @since 0.1
	 */
	public T getFirst(String whereClause) throws SQLException {
		return getFirst(whereClause, (Object[])null);
	}

	/**
	 * @param whereClause
	 *            The optional WHERE clause to apply to the query. Passing null
	 *            will select all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @return <b>T</b> The first instance returned by the query
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @throws SQLException 
	 * @since 0.1
	 */
	public  T getFirst(String whereClause, Object... whereArgs) throws SQLException{
		return getFirst(whereClause, whereArgs, null);
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
	 * @throws SQLException 
	 * @since 0.1
	 */
	public abstract T getFirst(String whereClause, Object[] whereArgs, String orderBy) throws SQLException;
	
	/**
	 * @return <b>List&ltT&gt</b> a List of the objects returned by the query
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public List<T> getAll() throws SQLException {
		return getAll(null);
	}

	/**
	 * @param whereClause
	 *            The optional WHERE clause to apply to the query. Passing null
	 *            will select all rows
	 * @return <b>List&ltT&gt</b> a List of the objects returned by the query
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public List<T> getAll(String whereClause) throws SQLException {
		return getAll(whereClause, (Object[])null);
	}

	/**
	 * @param whereClause
	 *            The optional WHERE clause to apply to the query. Passing null
	 *            will select all rows
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @return <b>List&ltT&gt</b> a List of the objects returned by the query
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public List<T> getAll(String whereClause, Object... whereArgs) throws SQLException {
		return getAll(whereClause, whereArgs, null);
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
	 * @return <b>List&ltT&gt</b> a List of the objects returned by the query
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public List<T> getAll(String whereClause, Object[] whereArgs, String orderBy) throws SQLException {
		return getAll(whereClause, whereArgs, orderBy, null);
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
	public abstract List<T> getAll(String whereClause, Object[] whereArgs, String orderBy, Integer limit) throws SQLException;

	/**
	 * @param t
	 *            the Object to insert
	 * @return <b>long</b> the row id of the inserted record
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract long insert(T t) throws SQLException;

	/**
	 * @param collection
	 *            a list of the Objects to insert
	 * @return <b>long[]</b> the row ids of the inserted records
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public abstract long[] insertAll(Collection<T> collection) throws SQLException;

	/**
	 * @param t
	 *            The object to insert
	 * @return the inserted object as it is in the database after any triggers
	 *         etc
	 * @throws SQLException
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.1
	 */
	public final T insertAndReturnUpdated(T t) throws SQLException {
		long rowId = insert(t);
		return getFirst("ROWID = ?" , rowId);
	}

	/**
	 * @param ts
	 *            a list of the objects to insert
	 * @return a list of the inserted objects as they are in the database after
	 *         any triggers etc
	 * @throws SQLException
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 * @since 0.1
	 */
	public final List<T> insertAllAndReturnUpdated(Collection<T> ts) throws SQLException {
		List<T> results = new ArrayList<T>(ts.size());
		for (T t : ts) {
			long rowId = insert(t);
			results.add(getFirst("ROWID=" + rowId));
		}
		return results;
	}

	/**
	 * If an Object matching this objects primary key exists it is updated. If a
	 * matching record does not exist, the Object is inserted into the database.
	 * 
	 * @param t
	 *            the Object to insert or update
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 *             This method cannot be used on an ObjectModel that does not
	 *             have a primary key
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int insertOrUpdate(T t) throws SQLException {
		StringBuilder whereClauseBuilder = new StringBuilder();
		int primaryKeyCount = 0;
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			if (objectModelColumn.isPrimaryKey()) {
				if (primaryKeyCount++ > 0)
					whereClauseBuilder.append(" AND ");
				whereClauseBuilder.append(objectModelColumn.getName());
				whereClauseBuilder.append(" = ?");
			}
		}
		if (primaryKeyCount == 0) {
			throw new IllegalArgumentException("The insertOrUpdate method cannot be used on an ObjectModel that does not have a primary key");
		}
		String[] whereArgs = new String[primaryKeyCount];
		int whereArgsIndex = 0;
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			if (objectModelColumn.isPrimaryKey()) {
				try {
					whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(t, objectModelColumn));
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

		String whereClause = whereClauseBuilder.toString();

		return insertOrUpdate(t, whereClause, (Object[])whereArgs);
	}

	/**
	 * If an Object matching the whereClause exists it is updated. If a matching
	 * record does not exist, the Object is inserted into the database.
	 * 
	 * @param t
	 *            the Object to insert or update
	 * @param whereClause
	 *            The optional WHERE clause to use to find the record to update.
	 *            Passing null will update the first record returned by the
	 *            databases
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int insertOrUpdate(T t, String whereClause) throws SQLException {
		return insertOrUpdate(t, whereClause, (Object[])null);
	}

	/**
	 * If an Object matching the whereClause exists it is updated. If a matching
	 * record does not exist, the Object is inserted into the database.
	 * 
	 * @param t
	 *            the Object to insert or update
	 * @param whereClause
	 *            The optional WHERE clause to use to find the record to update.
	 *            Passing null will update the first record returned by the
	 *            database
	 * @param whereArgs
	 *            Question marks in the whereClause String will be replaced with
	 *            these whereArgs in order. (where id = ?)
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int insertOrUpdate(T t, String whereClause, Object... whereArgs) throws SQLException {
		T databaseT = getFirst(whereClause, whereArgs);
		if (databaseT == null) {
			return insert((T) t) > 0 ? 1 : 0;
		} else {
			return update((T) t, whereClause, whereArgs);
		}
	}

	/**
	 * If an Object matching the whereClause exists it is updated. If a matching
	 * record does not exist, the Object is inserted into the database.
	 * 
	 * @param collection
	 *            the Objects to insert or update
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 *             This method cannot be used on an ObjectModel that does not
	 *             have a primary key
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int insertOrUpdateAll(Collection<T> collection) throws SQLException, IllegalArgumentException {
		int result = 0;

		for (T t : collection) {
			result += insertOrUpdate(t);
		}

		return result;
	}

	/**
	 * If an Object matching this objects primary key exists it is updated. If a
	 * matching record does not exist, the Object is inserted into the database.
	 * 
	 * @param object
	 *            the Objects to insert or update
	 * @param type
	 *            the Type of the Object to update
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 *             This method cannot be used on an ObjectModel that does not
	 *             have a primary key
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	@SuppressWarnings("unchecked")
	public
	final int insertOrUpdateGeneric(Object object, Class<?> type) throws SQLException {
		return insertOrUpdate((T) ObjectModel.castToObjectType(object, type));
	}
	
	/**
	 * If an Object matching the whereClause exists it is updated. If a matching
	 * record does not exist, the Object is inserted into the database.
	 * 
	 * @param objects
	 *            the Objects to insert or update
	 * @param type
	 *            the Type of the Object to update
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 *             This method cannot be used on an ObjectModel that does not
	 *             have a primary key
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int insertOrUpdateAllGeneric(Collection<?> objects, Class<?> type) throws SQLException, IllegalArgumentException {
		int result = 0;
		for (Object object : objects) {
			result += insertOrUpdateGeneric(object, type);
		}
		return result;
	}

	/**
	 * Updates the database record identified by the object's primary key with
	 * the values of the Object <b>t</b>
	 * 
	 * @param t
	 *            the Object to update
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 *             This method cannot be used on an ObjectModel that does not
	 *             have a primary key
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int update(T t) throws SQLException {
		StringBuilder whereClauseBuilder = new StringBuilder();
		int primaryKeyCount = 0;
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			if (objectModelColumn.isPrimaryKey()) {
				if (primaryKeyCount++ > 0)
					whereClauseBuilder.append(" AND ");
				whereClauseBuilder.append(objectModelColumn.getName());
				whereClauseBuilder.append(" = ?");
			}
		}
		if (primaryKeyCount == 0) {
			throw new IllegalArgumentException("The update method cannot be used on an ObjectModel that does not have a primary key");
		}
		String[] whereArgs = new String[primaryKeyCount];
		int whereArgsIndex = 0;
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			if (objectModelColumn.isPrimaryKey()) {
				try {
					whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(t, objectModelColumn));
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return update(t, whereClauseBuilder.toString(), (Object[])whereArgs);
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
	public abstract int update(T t, String whereClause, Object... whereArgs) throws SQLException;

	protected int insertOrUpdateRelatedChildObjects(T t) throws SQLException {
		int result = 0;

		/*
		 * Insert objects mapped by any relationships to this object
		 */
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			for (Relationship relationship : objectModelColumn.getRelationships()) {
				if (relationship.parentType.equals(t.getClass()) && relationship.getParentReferenceField() != null) {

					boolean[] accessiblity = new boolean[] { relationship.getParentReferenceField().isAccessible(), relationship.getParentKeyField().isAccessible(), relationship.getChildKeyField().isAccessible() };
					relationship.getParentReferenceField().setAccessible(true);
					relationship.getParentKeyField().setAccessible(true);
					relationship.getChildKeyField().setAccessible(true);

					try {
						Object parentReference = relationship.getParentReferenceField().get(t);
						if (parentReference != null) {
							ObjectModel<?,?,?> objectModel = getDatabaseModel().getObjectModel(relationship.childType);
							if (parentReference instanceof Collection<?>) {

								// Ensure the child object's foreign key fields
								// are set to the primary key of the parent
								// object
								Object parentKeyValue = relationship.parentKeyField.get(t);
								for (Object object : (Collection<?>) parentReference) {
									relationship.childKeyField.set(object, parentKeyValue);
								}

								// Insert or update the child objects
								objectModel.insertOrUpdateAllGeneric((Collection<?>) parentReference, objectModel.getObjetType());

							} else {

								// Ensure the child object's foreign key field
								// is set to the primary key of the parent
								// object
								relationship.childKeyField.set(parentReference, relationship.parentKeyField.get(t));

								// Insert or update the child object
								objectModel.insertOrUpdateGeneric(parentReference, objectModel.getObjetType());

							}
						}
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}

					// Restore the parent reference field's accessibility
					relationship.getParentReferenceField().setAccessible(accessiblity[0]);
					relationship.getParentKeyField().setAccessible(accessiblity[1]);
					relationship.getChildKeyField().setAccessible(accessiblity[2]);
				}
			}
		}
		return result;
	}

	/**
	 * Deletes the object record from the ObjectModel's table
	 * 
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 *             This method cannot beilson.co.za">Sheldon Neilson</a>
	 */
	public final int delete(Object object) throws SQLException {

		StringBuilder whereClauseBuilder = new StringBuilder();
		int primaryKeyCount = 0;
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			if (objectModelColumn.isPrimaryKey()) {
				if (primaryKeyCount++ > 0)
					whereClauseBuilder.append(" AND ");
				whereClauseBuilder.append(objectModelColumn.getName());
				whereClauseBuilder.append(" = ?");
			}
		}
		if (primaryKeyCount == 0) {
			throw new IllegalArgumentException("The delete method cannot be used on an ObjectModel that does not have a primary key");
		}
		String[] whereArgs = new String[primaryKeyCount];
		int whereArgsIndex = 0;
		for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
			if (objectModelColumn.isPrimaryKey()) {
				try {
					whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(object, objectModelColumn));
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

		return deleteAll(whereClauseBuilder.toString(), (Object[])whereArgs);
	}

	/**
	 * @param collection
	 *            a list of the Objects to delete
	 * @return <b>int</b> the number of affected records
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int deleteAll(Collection<?> collection) throws SQLException {
		int result = 0;
		for (Object t : collection) {
			result += delete(t);
		}
		return result;
	}
	
	/**
	 * Deletes all records from the ObjectModel's table
	 * 
	 * @return <b>int</b> the number of rows affected.
	 * @throws SQLException
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public final int deleteAll() throws SQLException {
		return deleteAll("1", (Object[])null);
	}

	/**
	 * Deletes the database record(s) matching the <b>whereClause</b> from the
	 * ObjectModel's table
	 * 
	 * @param database
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
	public final int deleteAll(String whereClause) throws SQLException {
		return deleteAll(whereClause, (Object[])null);
	}

	/**
	 * Deletes the database record(s) matching the <b>whereClause</b> from the
	 * ObjectModel's table
	 * 
	 * @param database
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
	public abstract int deleteAll(String whereClause, Object... whereArgs) throws SQLException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		boolean equal = o instanceof ObjectModel;
		if (equal) {
			for (int i = 0; i < getObjectModelColumns().length; i++) {
				equal = getObjectModelColumns()[i].getName() == ((ObjectModel<?,?,?>) o).getObjectModelColumns()[i].getName() && getObjectModelColumns()[i].getType() == ((ObjectModel<?,?,?>) o).getObjectModelColumns()[i].getType();
				if (!equal)
					break;
			}
		}
		return equal;
	}

}

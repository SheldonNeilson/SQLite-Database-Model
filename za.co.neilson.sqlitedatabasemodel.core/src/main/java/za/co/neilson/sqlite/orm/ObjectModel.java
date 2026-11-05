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
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import za.co.neilson.sqlite.orm.Relationship.Type;
import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.Nullable;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;
import za.co.neilson.sqlite.orm.annotations.Transient;
import za.co.neilson.sqlite.orm.annotations.Unique;

/**
 * @param <T> The object type managed by this model
 * @param <R> The type of the result collection returned by a query. Android
 *            Returns a Cursor Object whilst JDBC returns a ResultSet
 * @param <C> The type of the result collection returned by a query.
 *            The type of map used to map column names to column values.
 *            Android requires the use of a ContentValues object whilst JDBC
 *            simply uses a HashMap<String,Object>
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 * @version 0.1
 * @since 0.1
 */
public abstract class ObjectModel<T, R, C> {


    public static final int RELATIONSHIP_MANAGEMENT_MANUAL = 0;
    public static final int RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED = 1;
    public static final int RELATIONSHIP_MANAGEMENT_INSERT_RELATED = 2;
    public static final int RELATIONSHIP_MANAGEMENT_UPDATE_RELATED = 4;
    public static final int RELATIONSHIP_MANAGEMENT_CASCADE_DELETE = 8;
    public static final int RELATIONSHIP_MANAGEMENT_FULL = RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED | RELATIONSHIP_MANAGEMENT_INSERT_RELATED | RELATIONSHIP_MANAGEMENT_UPDATE_RELATED | RELATIONSHIP_MANAGEMENT_CASCADE_DELETE;
    protected DatabaseModel<R, C> databaseModel;
    protected static HashMap<java.lang.reflect.Type, LinkedHashMap<String, Field>> fields = new HashMap<>();
    protected static final HashMap<java.lang.reflect.Type, LinkedHashMap<String, ObjectModelColumn>> objectModelColumns = new HashMap<>();
    protected static HashMap<java.lang.reflect.Type, ArrayList<ObjectModelColumn>> primaryKeys = new HashMap<>();
    protected int relationshipManagementMode = RELATIONSHIP_MANAGEMENT_FULL;
    protected static HashMap<java.lang.reflect.Type, String[]> columnNames = new HashMap<>();
    private static final HashMap<java.lang.reflect.Type, String> tableName = new HashMap<>();

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
    public ObjectModel(DatabaseModel<R, C> databaseModel) throws ClassNotFoundException, NoSuchFieldException {
        setDatabaseModel(databaseModel);

        //TODO: Lazy loading ObjectModelColumns means that relationships are not created unless the child is a initialized
        synchronized (objectModelColumns) {
            onInitializeObjectModelColumns();
        }
    }

    /**
     * Reads an enum back from its stored text. Enum values are stored with toString(), which differs from name() when an
     * enum overrides it, so the text is matched against toString() first and then name().
     *
     * @return the matching constant, or null when the text is null
     * @throws IllegalArgumentException when no constant matches
     */
    public static Object enumFromStoredText(Class<?> enumType, String text) {
        if (text == null)
            return null;
        Object[] constants = enumType.getEnumConstants();
        for (Object constant : constants) {
            if (constant.toString().equals(text))
                return constant;
        }
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equals(text))
                return constant;
        }
        throw new IllegalArgumentException("No enum constant " + enumType.getCanonicalName() + "." + text);
    }

    public static <T> T castToObjectType(Object o, Class<T> type) {
        try {
            return type.cast(o);
        } catch (ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configure the way that the {@link ObjectModel} handles related objects
     * using a bitmask of {@link ObjectModel#RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED} | {@link ObjectModel#RELATIONSHIP_MANAGEMENT_INSERT_RELATED} | {@link ObjectModel#RELATIONSHIP_MANAGEMENT_UPDATE_RELATED} | {@link ObjectModel#RELATIONSHIP_MANAGEMENT_CASCADE_DELETE}.
     * Set to {@link ObjectModel#RELATIONSHIP_MANAGEMENT_MANUAL} to disable automatic relationship management.
     * Set to {@link ObjectModel#RELATIONSHIP_MANAGEMENT_FULL} to enable all relationship management features (default).
     *
     * @param relationshipManagementMode
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    public void setRelationshipManagementMode(int relationshipManagementMode) {
        this.relationshipManagementMode = relationshipManagementMode;
    }

    public int getRelationshipManagementMode(){
        return relationshipManagementMode;
    }

    /**
     * Get the actual type of the generic type argument used to extend the
     * ObjectModel class.
     *
     * @return Class<T> the Type of the object managed by this ObjectModel
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    @SuppressWarnings("unchecked")
    public Class<T> getObjectType() {
        ParameterizedType superclass = (ParameterizedType) getClass().getGenericSuperclass();
        return (Class<T>) superclass.getActualTypeArguments()[0];
    }

    /**
     * @return <b>DatabaseModel</b> the DatabaseModel to which this ObjectModel
     * is connected
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final DatabaseModel<R, C> getDatabaseModel() {
        if (this.databaseModel == null)
            throw new NullPointerException("DatabaseModel has not been initialized.");

        return this.databaseModel;
    }

    /**
     * @param databaseModel the DatabaseModel to which this ObjectModel is connected
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected final void setDatabaseModel(DatabaseModel<R, C> databaseModel) {
        if (this.databaseModel == null)
            this.databaseModel = databaseModel;
    }

    /**
     * @return <b>String</b> the ObjectModel's database table name
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected synchronized String getTableName() {
        if (!tableName.containsKey(getObjectType()))
            tableName.put(getObjectType(), getObjectType().getSimpleName());
        return tableName.get(getObjectType());
    }

    /**
     * <p>
     * By default the fields for which database tables are created is determined
     * using reflection to inspect the class that the model represents, and
     * creating a column for each property.
     * </p>
     * <p>
     * Override the ObjectModel.onInitializeObjectModelFields() for finer
     * control over which properties are persisted.
     * </p>
     *
     * @return <b>Field[]</b> the Fields mapped by this ObjectModel to database table columns
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 1.1
     */
    protected synchronized LinkedHashMap<String, Field> onInitializeObjectModelFields() {
        Class<?> type = getObjectType();
        Field[] declaredFields = type.getDeclaredFields();
        LinkedHashMap<String, Field> fieldMap = new LinkedHashMap<>(declaredFields.length);
        for (Field field : declaredFields) {
            fieldMap.put(field.getName(), field);
        }
        return fieldMap;
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
     * columns
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected synchronized LinkedHashMap<String, ObjectModelColumn> onInitializeObjectModelColumns() throws ClassNotFoundException, NoSuchFieldException {

        if (objectModelColumns.containsKey(getObjectType()))
            return objectModelColumns.get(getObjectType());

        Collection<Field> fields = getFields();

        /*
         * Count the number of fields to model
         */
        int supportedFields = 0;

        Iterator fieldIterator = fields.iterator();
        while (fieldIterator.hasNext()) {
            Field field = (Field) fieldIterator.next();
            // Do not map field types that are not supported for ORM
            int modifiers = field.getModifiers();
            if (ObjectModelColumn.isSupportedType(field.getType())
                    // Do not map fields marked with the Transient annotation
                    && !field.isAnnotationPresent(Transient.class)
                    && !Modifier.isStatic(modifiers)
                    && !Modifier.isTransient(modifiers)
                    && !Modifier.isFinal(modifiers))
                supportedFields++;
        }

        /*
         * Initialize the objectModelColumns array with the number of
         * supportedFields
         */
        LinkedHashMap<String, ObjectModelColumn> lObjectModelColumns = new LinkedHashMap<>(supportedFields);
        objectModelColumns.put(getObjectType(), lObjectModelColumns);
        ArrayList<ObjectModelColumn> lPrimaryKeys = new ArrayList<>();
        String[] lColumnNames = new String[supportedFields];


        /*
         * Create the objectModelColumns and add them to the
         * objectModelColumns[] array
         */
        int supportedFieldIndex = 0;
        boolean nonForeignKeysCompleted = false;
        fieldIterator = null;
        while (!nonForeignKeysCompleted) {
            if (fieldIterator != null && !fieldIterator.hasNext() && !nonForeignKeysCompleted) {
                nonForeignKeysCompleted = true;
            }
            fieldIterator = fields.iterator();
            while (fieldIterator.hasNext()) {
                Field field = (Field) fieldIterator.next();


                // Do not map field types that are not supported for ORM
                int modifiers = field.getModifiers();
                if (ObjectModelColumn.isSupportedType(field.getType())
                        // Do not map fields marked with the Transient annotation
                        && !field.isAnnotationPresent(Transient.class)
                        && !Modifier.isStatic(modifiers)
                        && !Modifier.isTransient(modifiers)
                        && !Modifier.isFinal(modifiers)) {
                    boolean accessible = field.isAccessible();
                    try {

                        ForeignKey foreignKey = null;
                        if (field.isAnnotationPresent(ForeignKey.class)) {
                            foreignKey = field.getAnnotation(ForeignKey.class);
                            if (!nonForeignKeysCompleted)
                                continue;
                        } else if (nonForeignKeysCompleted) {
                            continue;
                        }

                        field.setAccessible(true);

                        // Construct new ObjectModelColumn
                        ObjectModelColumn objectModelColumn = new ObjectModelColumn(field.getName(), field.getType(), true);

                        // Create primary key if applicable
                        if (field.isAnnotationPresent(PrimaryKey.class)) {
                            PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
                            objectModelColumn.setPrimaryKey(true);
                            objectModelColumn.setAutoIncrement(primaryKey.autoIncrement());
                            lPrimaryKeys.add(objectModelColumn);
                        } else {
                            // PrimaryKey columns cannot be nullable
                            if (field.isAnnotationPresent(Nullable.class)) {
                                Nullable nullable = field.getAnnotation(Nullable.class);
                                objectModelColumn.setNullable(nullable.value());
                            }
                        }
                        // Create foreign key if applicable
                        if (foreignKey != null) {
                            objectModelColumn.setForeignKeyParentTable(foreignKey.table());
                            objectModelColumn.setForeignKeyColumn(foreignKey.column());

                            za.co.neilson.sqlite.orm.Relationship relationship = new za.co.neilson.sqlite.orm.Relationship();
                            try {
                                /*
                                 * Create a relationship with the object referenced
                                 * by the foreign key. This creates a requirement
                                 * for the parent ObjectModel to be created first.
                                 */

                                // Find the Parent Object Type
                                ObjectModel<?, ?, ?> parentTypeObjectModel = null;
                                Class<?> parentType = null;
                                if (getTableName().equals(foreignKey.table())) { //Is this a foreign key to the same table?
                                    parentTypeObjectModel = this;
                                    parentType = getObjectType();
                                } else {
                                    for (ObjectModel<?, ?, ?> objectModel : getDatabaseModel().getObjectModels().values()) {
                                        if (objectModel.getObjectType().getSimpleName().equalsIgnoreCase(foreignKey.table())) {
                                            parentTypeObjectModel = objectModel;
                                            parentType = objectModel.getObjectType();
                                            break;
                                        }
                                    }
                                }
                                if (parentType == null)
                                    throw new ClassNotFoundException("The ObjectModel, " + foreignKey.table() + ", referenced by the foreign key, " + field.getName() + ", does not exist. Ensure that it was added to the DatabaseModel first.");
                                relationship.setParentType(parentType);

                                // Find The Parent Object Field referenced by the
                                // key
                                Field parentKeyField = parentType.getDeclaredField(foreignKey.column());
                                relationship.setParentKeyField(parentKeyField);

                                ObjectModelColumn parentKeyFieldObjectModelColumn = null;
                                for (ObjectModelColumn parentObjectModelColumn : parentTypeObjectModel.getObjectModelColumns()) {
                                    if (parentObjectModelColumn.getName().equals(foreignKey.column())) {
                                        parentKeyFieldObjectModelColumn = parentObjectModelColumn;
                                        break;
                                    }
                                }

                                // The Child Object Type (This ObjectModel's type)
                                Class<?> childType = getObjectType();
                                relationship.setChildType(childType);

                                // The Child Object Filed that references the parent
                                // object's key Field
                                relationship.setChildKeyField(field);

                                /*
                                 * The Child Object's reference to the Parent Object
                                 * This is an optional attribute that can be omitted
                                 * to prevent the system from automatically
                                 * retrieving the related object(s) and filling this
                                 * reference.
                                 */
                                if (!foreignKey.childReference().isEmpty()) {
                                    Field childReferenceField = getObjectType().getDeclaredField(foreignKey.childReference());
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
                                    if (field.isAnnotationPresent(PrimaryKey.class)) {
                                        relationship.setRelationshipType(Type.ONE_TO_ONE);
                                    } else {
                                        relationship.setRelationshipType(Type.ONE_TO_MANY);
                                    }
                                } else {
                                    relationship.setRelationshipType(Type.MANY_TO_MANY);
                                }

                                objectModelColumn.getRelationships().add(relationship);
                                parentKeyFieldObjectModelColumn.getRelationships().add(relationship);

                            } catch (ClassNotFoundException e) {
                                throw e;
                            } catch (NoSuchFieldException e) {
                                throw e;
                            } catch (SecurityException e) {
                                throw e;
                            }
                        }
                        if (field.isAnnotationPresent(Unique.class)) {
                            // Unique unique =
                            // fields[i].getAnnotation(Unique.class);
                            objectModelColumn.setUnique(true);
                        }

                        lObjectModelColumns.put(objectModelColumn.getName(), objectModelColumn);
                        lColumnNames[supportedFieldIndex] = objectModelColumn.getName();

                    } catch (UnsupportedTypeException e) {
                        throw new RuntimeException(e);
                    }
                    // Reset accessibility to default
                    field.setAccessible(accessible);
                    supportedFieldIndex++;
                }
            }
        }


        primaryKeys.put(getObjectType(), lPrimaryKeys);
        columnNames.put(getObjectType(), lColumnNames);

        return lObjectModelColumns;
    }

    /**
     * @return <b>Collection<Field></b> the Fields mapped by this ObjectModel to database table columns
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 1.1
     */
    public Collection<Field> getFields() {
        LinkedHashMap<String, Field> lFields = fields.get(getObjectType());
        if (lFields == null) {
            lFields = onInitializeObjectModelFields();
            fields.put(getObjectType(), lFields);
        }
        return lFields.values();
    }

    /**
     * @return <b>Field</b> the Field mapped by this ObjectModel with the given name
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 1.1
     */
    public Field getField(String name) {
        LinkedHashMap<String, Field> lFields = fields.get(getObjectType());
        if (lFields == null) {
            lFields = onInitializeObjectModelFields();
            fields.put(getObjectType(), lFields);
        }
        return lFields.get(name);
    }

    /**
     * @return <b>Collection<ObjectModelColumn></ObjectModelColumn></b> the ObjectModel's database table
     * columns
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized final Collection<ObjectModelColumn> getObjectModelColumns() {
        synchronized (objectModelColumns) {
            LinkedHashMap<String, ObjectModelColumn> lObjectModelColumns = objectModelColumns.get(getObjectType());
            if (lObjectModelColumns == null) {
                try {
                    lObjectModelColumns = onInitializeObjectModelColumns();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to initialize ObjectModelColumns", e);
                }
            }
            return lObjectModelColumns.values();
        }
    }

    /**
     * @param columnName
     * @return <b>ObjectModelColumn</b> the ObjectModelColumn for the specified column
     * columns
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public synchronized final ObjectModelColumn getObjectModelColumn(String columnName) {
        synchronized (objectModelColumns) {
            LinkedHashMap<String, ObjectModelColumn> lObjectModelColumns = objectModelColumns.get(getObjectType());
            if (lObjectModelColumns == null) {
                try {
                    lObjectModelColumns = onInitializeObjectModelColumns();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to initialize ObjectModelColumns", e);
                }
            }
            return lObjectModelColumns.get(columnName);
        }
    }

    /**
     * @return <b>String[]</b> an array of the ObjectModel table's column names
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected synchronized String[] getColumnNames() {
        synchronized (objectModelColumns) {
            String[] lColumnNames = columnNames.get(getObjectType());
            if (columnNames == null) {
                try {
                    onInitializeObjectModelColumns();
                    lColumnNames = columnNames.get(getObjectType());
                } catch (Exception e) {
                    throw new RuntimeException("Unable to initialize ObjectModelColumns", e);
                }
            }
            return lColumnNames;
        }
    }

    private final HashSet<String> createdColumns = new HashSet<>();

    /**
     * Used to obtain a <b>ResultSet</b> of a raw SQL query's results.
     *
     * @param sql           The sql query to execute
     * @param selectionArgs Question marks in the sql String will be replaced with
     *                      these selectionArgs in order. (where id = ?)
     * @return <b>ResultSet</b> the results returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    protected final R getResultSetRaw(String sql, Object[] selectionArgs) throws SQLException {
        return getDatabaseModel().getDatabaseDriverInterface().rawQuery(sql, selectionArgs);
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
     * @param t                 the Object with which to set the column value
     * @param objectModelColumn the database column to set to the corresponding object field
     *                          value
     * @return <b>Object</b> the appropriate table column value for the
     * ObjectModelColumn supplied
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected Object setColumnValue(Object t, ObjectModelColumn objectModelColumn) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        Object value = null;
        Class<?> c = getObjectType();
        Field field = getField(objectModelColumn.getName());
        field.setAccessible(true);
        if (objectModelColumn.getType() == Boolean.class) {
            Boolean booleanField = (Boolean) field.get(t);
            value = booleanField != null ? booleanField.booleanValue() : null;
        } else if (objectModelColumn.getType() == Date.class) {
            Date dateField = (Date) field.get(t);
            value = dateField == null ? null : dateField.getTime();
        } else if (objectModelColumn.getType() == Calendar.class) {
            Calendar calendarField = (Calendar) field.get(t);
            value = calendarField == null ? null : calendarField.getTimeInMillis();
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
     * @param r                               The ResultSet returned as the result of a database query and
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
    protected abstract Object getColumnValue(R r, ObjectModelColumn objectModelColumn, int resultSetObjectModelColumnIndex) throws NoSuchFieldException, SecurityException, SQLException;

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
     * @param orderBy     The optional order by clause to be appended to the sql
     *                    statement
     * @param limit       The optional limit on the number of results returned
     * @return <b>ResultSet</b> the results returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected final R getResultSet(String whereClause, Object[] whereArgs, String groupBy, String having, String orderBy, Integer limit) throws SQLException {
        try {
            return getDatabaseModel().getDatabaseDriverInterface().query(getTableName(), getColumnNames(), whereClause, whereArgs, groupBy, having, orderBy, limit);
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Create an instance of the Object represented by this model using the
     * ResultSet returned by a database query
     *
     * @param r
     * @return the ResultSet containing the properties of the Object to
     * instantiate
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected T getInstance(R r) {
        try {
            Class<?> c = getObjectType();

            @SuppressWarnings("unchecked")
            T t = (T) c.newInstance();

            DatabaseDriverInterface driver = getDatabaseModel().getDatabaseDriverInterface();

            int columnCount = driver.getResultColumnCount(r);
            // Column indexes start at getFirstColumnIndex() (0 for Android cursors, 1 for JDBC result sets)
            for (int i = driver.getFirstColumnIndex(); i < columnCount + driver.getFirstColumnIndex(); i++) {
                String columnName = getDatabaseModel().getDatabaseDriverInterface().getResultColumnName(r, i);
                ObjectModelColumn objectModelColumn = getObjectModelColumn(columnName);

                Field field = null;
                try {
                    field = getField(objectModelColumn.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                boolean accessible = field.isAccessible();
                field.setAccessible(true);

                Object columnValue = getColumnValue(r, objectModelColumn, i);
                field.set(t, columnValue);

                if ((relationshipManagementMode & RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED) == RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED) {

                    for (Relationship relationship : objectModelColumn.getRelationships()) {

                        if (relationship.parentReferenceField != null && relationship.parentKeyField.equals(field)) {

                            // Only visible while this parent loads its children (they point back to it); restored afterwards so it
                            // is not served to unrelated rows read later through the shared Relationship
                            Object previousRelationshipParent = relationship.parent;
                            relationship.parent = t;

                            /*
                             * I am the relationship parent, fill my reference to
                             * the child object
                             */

                            ObjectModel<?, ?, ?> childObjectModel = getDatabaseModel().getObjectModel(relationship.childType);

                            String whereClause = relationship.childKeyField.getName() + " = ? ";
                            Object[] whereArgs = new String[]{field.get(t).toString()};

                            switch (relationship.type) {
                                case MANY_TO_MANY:
                                case ONE_TO_MANY:
                                    Collection<?> collection = childObjectModel.getAll(whereClause, whereArgs);
                                    relationship.child = collection;
                                    break;
                                case ONE_TO_ONE:
                                    relationship.child = childObjectModel.getFirst(whereClause, whereArgs);
                                    break;
                            }
                            relationship.parent = previousRelationshipParent;
                            if (relationship.parentReferenceField != null) {
                                boolean parentReferenceFieldIsAccessible = relationship.parentReferenceField.isAccessible();
                                relationship.parentReferenceField.setAccessible(true);
                                relationship.parentReferenceField.set(t, relationship.child);
                                relationship.parentReferenceField.setAccessible(parentReferenceFieldIsAccessible);
                            }
                        } else if (relationship.childReferenceField != null && relationship.childKeyField.equals(field)) {

                            /*
                             * I am the relationship child, fill my reference to the
                             * parent object
                             */
                            Object relationshipParent = relationship.parent;
                            if (relationshipParent == null) {

                                ObjectModel<?, ?, ?> parentObjectModel = getDatabaseModel().getObjectModel(relationship.parentType);

                                String whereClause = relationship.parentKeyField.getName() + " = ? ";
                                Object[] whereArgs = new String[]{field.get(t).toString()};

                                switch (relationship.type) {
                                    case MANY_TO_MANY:
                                        break;
                                    case ONE_TO_MANY:
                                    case ONE_TO_ONE:
                                        relationshipParent = parentObjectModel.getFirst(whereClause, whereArgs);
                                        break;
                                }

                            }
                            if (relationship.childReferenceField != null) {
                                boolean childReferenceFieldIsAccessible = relationship.childReferenceField.isAccessible();
                                relationship.childReferenceField.setAccessible(true);
                                relationship.childReferenceField.set(t, relationshipParent);
                                relationship.childReferenceField.setAccessible(childReferenceFieldIsAccessible);
                            }
                        }
                    }
                }

                field.setAccessible(accessible);
            }
            return t;
        } catch (NoSuchFieldException | SecurityException | SQLException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    protected void onCreateTable() throws SQLException {
        onCreateTable(false);
    }

    /**
     * Creates the table for this object in the SQLite database according to the
     * ObjectModelColumns declared in the concrete subclass constructor.
     *
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    protected void onCreateTable(boolean rebuild) throws SQLException {

        // The set only tracks the columns of one CREATE TABLE pass, so a repeated call regenerates the statement (IF NOT EXISTS keeps it a no-op)
        createdColumns.clear();

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS \"" + getTableName());
        sql.append("\" ( ");

        ArrayList<ObjectModelColumn> lPrimaryKeys = primaryKeys.get(getObjectType());
        int primaryKeyCount = lPrimaryKeys == null ? 0 : lPrimaryKeys.size();

        int columnIndex = 0;
        int columnsIncluded = 0;
        for (ObjectModelColumn objectModelColumn : this.getObjectModelColumns()) {

            if (createdColumns.contains(objectModelColumn.getName()))
                continue;

            //Cannot create foreign keys to same table
            if (!getTableName().equals(objectModelColumn.getForeignKeyParentTable())) {

                if (columnsIncluded++ > 0)
                    sql.append(", ");

                sql.append("\"");
                sql.append(objectModelColumn.getName());
                sql.append("\"");

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
                    sql.append(" SMALLINT");
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

                if (objectModelColumn.isPrimaryKey() && primaryKeyCount == 1) {
                    sql.append(" PRIMARY KEY");
                    if (objectModelColumn.isAutoIncrement())
                        sql.append(" AUTOINCREMENT");
                }

                if (objectModelColumn.isUnique() && !objectModelColumn.isPrimaryKey()) {
                    sql.append(" UNIQUE");
                }

                if (!objectModelColumn.isNullable())
                    sql.append(" NOT NULL");

                createdColumns.add(objectModelColumn.getName());
            }
        }

        if (primaryKeyCount > 1) {
            int primaryKeyIndex = 0;
            sql.append(", ");
            sql.append(" PRIMARY KEY(\"");

            for (ObjectModelColumn databaseEntityColumn : primaryKeys.get(getObjectType()) /*this.getObjectModelColumns()*/) {
                //if (databaseEntityColumn.isPrimaryKey()) {
                if (primaryKeyIndex++ != 0)
                    sql.append(", \"");
                sql.append(databaseEntityColumn.getName());
                sql.append("\"");
                //}
            }
            sql.append(")");
        }

        columnIndex = 0;
        for (ObjectModelColumn databaseEntityColumn : this.getObjectModelColumns()) {
            if (databaseEntityColumn.getForeignKeyParentTable() != null && databaseEntityColumn.getForeignKeyParentColumn() != null) {
                //Cannot create foreign keys to same table
                if (!getTableName().equals(databaseEntityColumn.getForeignKeyParentTable())) {
                    sql.append(", ");
                    sql.append(" FOREIGN KEY(\"");
                    sql.append(databaseEntityColumn.getName());
                    sql.append("\")");
                    sql.append(" REFERENCES \"");
                    sql.append(databaseEntityColumn.getForeignKeyParentTable());
                    sql.append("\"(\"");
                    sql.append(databaseEntityColumn.getForeignKeyParentColumn());
                    sql.append("\")");
                }
            }
            columnIndex++;
        }
        sql.append(")");

        if (columnsIncluded > 0) {
            getDatabaseModel().getDatabaseDriverInterface().execute(sql.toString());
        } else {
            throw new SQLException("At least one column is required: " + sql.toString());
        }

        //Create foreign keys to same table
        for (ObjectModelColumn databaseEntityColumn : this.getObjectModelColumns()) {
            if (databaseEntityColumn.getForeignKeyParentTable() != null &&
                    databaseEntityColumn.getForeignKeyParentColumn() != null &&
                    getTableName().equals(databaseEntityColumn.getForeignKeyParentTable())) {

                if (createdColumns.contains(databaseEntityColumn.getName()))
                    continue;

                sql = new StringBuilder();
                sql.append("ALTER TABLE \"");
                sql.append(getTableName());
                sql.append("\" ADD COLUMN \"");

                sql.append(databaseEntityColumn.getName());
                sql.append("\"");

                if (databaseEntityColumn.primaryKey && databaseEntityColumn.autoIncrement) {
                    sql.append(" INTEGER");
                } else if (databaseEntityColumn.getType() == char.class) {
                    sql.append(" TEXT");
                } else if (databaseEntityColumn.getType() == char[].class) {
                    sql.append(" TEXT");
                } else if (databaseEntityColumn.getType() == String.class) {
                    sql.append(" TEXT");
                } else if (databaseEntityColumn.getType() == boolean.class || databaseEntityColumn.getType() == Boolean.class) {
                    sql.append(" TINYINT");
                } else if (databaseEntityColumn.getType() == byte.class || databaseEntityColumn.getType() == Byte.class) {
                    sql.append(" TINYINT");
                } else if (databaseEntityColumn.getType() == short.class || databaseEntityColumn.getType() == Short.class) {
                    sql.append(" SMALLINT");
                } else if (databaseEntityColumn.getType() == int.class || databaseEntityColumn.getType() == Integer.class) {
                    sql.append(" INT");
                } else if (databaseEntityColumn.getType() == long.class || databaseEntityColumn.getType() == Long.class) {
                    sql.append(" BIGINT");
                } else if (databaseEntityColumn.getType() == float.class || databaseEntityColumn.getType() == Float.class) {
                    sql.append(" FLOAT");
                } else if (databaseEntityColumn.getType() == double.class || databaseEntityColumn.getType() == Double.class) {
                    sql.append(" DOUBLE");
                } else if (databaseEntityColumn.getType() == Date.class) {
                    sql.append(" DATETIME");
                } else if (databaseEntityColumn.getType() == Calendar.class) {
                    sql.append(" DATETIME");
                } else if (databaseEntityColumn.getType() == byte[].class) {
                    sql.append(" BLOB");
                } else {
                    sql.append(" BLOB");
                }

                if (databaseEntityColumn.isUnique() && !databaseEntityColumn.isPrimaryKey()) {
                    sql.append(" UNIQUE");
                }

                if (!databaseEntityColumn.isNullable())
                    sql.append(" NOT NULL");

                sql.append(" REFERENCES \"");
                sql.append(databaseEntityColumn.getForeignKeyParentTable());
                sql.append("\" (\"");
                sql.append(databaseEntityColumn.getForeignKeyParentColumn());
                sql.append("\");");

                createdColumns.add(databaseEntityColumn.getName());

                getDatabaseModel().getDatabaseDriverInterface().execute(sql.toString());
            }
        }

        //Create indexes on foreign keys
        for (ObjectModelColumn databaseEntityColumn : this.getObjectModelColumns()) {
            if (databaseEntityColumn.getForeignKeyParentTable() != null && databaseEntityColumn.getForeignKeyParentColumn() != null) {
                sql = new StringBuilder();
                sql.append(" CREATE INDEX IF NOT EXISTS IX_");
                sql.append(getTableName());
                sql.append("_");
                sql.append(databaseEntityColumn.getName());
                sql.append(" ON \"");
                sql.append(getTableName());
                sql.append("\" (\"");
                sql.append(databaseEntityColumn.getName());
                sql.append("\");");
                getDatabaseModel().getDatabaseDriverInterface().execute(sql.toString());
            }
        }
    }

    /**
     * @param whereClause The WHERE clause used to identify which records exist in the table
     * @return <b>boolean</b> true if at least one record matching the where clause exists in the table
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    public boolean exists(String whereClause) throws SQLException {
        return exists(whereClause, (Object[]) null);
    }

    /**
     * @param whereClause The WHERE clause used to identify which records exist in the table
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>boolean</b> true if at least one record matching the where clause exists in the table
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    public boolean exists(String whereClause, Object... whereArgs) throws SQLException {
        return getDatabaseModel().getDatabaseDriverInterface().exists(getTableName(), whereClause, whereArgs);
    }

    /**
     * @param whereClause The WHERE clause used to identify which records exist in the table
     * @return <b>long</b> the number of records matching the where clause in the the table
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    public long getCount(String whereClause) throws SQLException {
        return getCount(whereClause, (Object[]) null);
    }

    /**
     * @param whereClause The WHERE clause used to identify which records exist in the table
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>long</b> the number of records matching the where clause in the the table
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    public long getCount(String whereClause, Object... whereArgs) throws SQLException {
        return getDatabaseModel().getDatabaseDriverInterface().getCount(getTableName(), whereClause, whereArgs);
    }

    /**
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @return <b>T</b> The first instance returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public T getFirst(String whereClause) throws SQLException {
        return getFirst(whereClause, (Object[]) null);
    }

    /**
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>T</b> The first instance returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public T getFirst(String whereClause, Object... whereArgs) throws SQLException {
        return getFirst(whereClause, whereArgs, null);
    }

    /**
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @param orderBy     a comma delimited list of the column names to order the
     *                    returned results by
     * @return <b>T</b> The first instance returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public abstract T getFirst(String whereClause, Object[] whereArgs, String orderBy) throws SQLException;

    /**
     * @param sql           The raw sql query to execute. the results returned by this query must be mappable to T
     * @param selectionArgs Question marks in the sql String will be replaced with
     *                      these selectionArgs in order. (where id = ?)
     * @return <b>T</b> the object returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    public abstract T getFirstRaw(String sql, Object... selectionArgs) throws SQLException;

    /**
     * @return <b>List&lt;T&gt;</b> a List of the objects returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public List<T> getAll() throws SQLException {
        return getAll(null);
    }

    /**
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @return <b>List&lt;T&gt;</b> a List of the objects returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public List<T> getAll(String whereClause) throws SQLException {
        return getAll(whereClause, (Object[]) null);
    }

    /**
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>List&lt;T&gt;</b> a List of the objects returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public List<T> getAll(String whereClause, Object... whereArgs) throws SQLException {
        return getAll(whereClause, whereArgs, null);
    }

    /**
     * @param whereClause The optional WHERE clause to apply to the query. Passing null
     *                    will select all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @param orderBy     a comma delimited list of the column names to order the
     *                    returned results by
     * @return <b>List&lt;T&gt;</b> a List of the objects returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public List<T> getAll(String whereClause, Object[] whereArgs, String orderBy) throws SQLException {
        return getAll(whereClause, whereArgs, orderBy, null);
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
    public abstract List<T> getAll(String whereClause, Object[] whereArgs, String orderBy, Integer limit) throws SQLException;

    /**
     * @param sql           The raw sql query to execute. the results returned by this query must be mappable to T
     * @param selectionArgs Question marks in the sql String will be replaced with
     *                      these selectionArgs in order. (where id = ?)
     * @return <b>List&lt;T&gt;</b> a List of the objects returned by the query
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.2
     */
    public abstract List<T> getAllRaw(String sql, Object... selectionArgs) throws SQLException;

    /**
     * @param t the Object to insert
     * @return <b>long</b> the row id of the inserted record
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public abstract long insert(T t) throws SQLException;

    /**
     * @param collection a list of the Objects to insert
     * @return <b>long[]</b> the row ids of the inserted records
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public abstract long[] insertAll(Collection<T> collection) throws SQLException;

    /**
     * @param t The object to insert
     * @return the inserted object as it is in the database after any triggers
     * etc
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final T insertAndReturnUpdated(T t) throws SQLException {
        long rowId = insert(t);
        return getFirst("ROWID = ?", rowId);
    }

    /**
     * @param ts a list of the objects to insert
     * @return a list of the inserted objects as they are in the database after
     * any triggers etc
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final List<T> insertAllAndReturnUpdated(Collection<T> ts) throws SQLException {
        List<T> results = new ArrayList<T>(ts.size());
        for (T t : ts) {
            long rowId = insert(t);
            results.add(getFirst("ROWID = " + rowId));
        }
        return results;
    }

    /**
     * If an Object matching this objects primary key exists it is updated. If a
     * matching record does not exist, the Object is inserted into the database.
     *
     * @param t the Object to insert or update
     * @return <b>long</b> the ROW_ID of the inserted row or the number of rows affected if updated.
     * @throws SQLException
     * @throws IllegalAccessException This method cannot be used on an ObjectModel that does not
     *                                have a primary key
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final long insertOrUpdate(T t) throws SQLException, NoSuchFieldException, IllegalAccessException {
        StringBuilder whereClauseBuilder = new StringBuilder();

        ArrayList<ObjectModelColumn> lPrimaryKeys = primaryKeys.get(getObjectType());

        if (lPrimaryKeys == null || lPrimaryKeys.size() == 0) {
            throw new IllegalArgumentException("The insertOrUpdate method cannot be used on an ObjectModel that does not have a primary key");
        }

        int primaryKeyCount = 0;
        for (ObjectModelColumn objectModelColumn : lPrimaryKeys) {
            if (primaryKeyCount++ > 0)
                whereClauseBuilder.append(" AND ");
            whereClauseBuilder.append("\"");
            whereClauseBuilder.append(objectModelColumn.getName());
            whereClauseBuilder.append("\" = ?");
        }

        String[] whereArgs = new String[primaryKeyCount];
        int whereArgsIndex = 0;
        /*for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
            if (objectModelColumn.isPrimaryKey()) {
                try {
                    whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(t, objectModelColumn));
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }*/
        for (ObjectModelColumn objectModelColumn : lPrimaryKeys) {
            try {
                whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(t, objectModelColumn));
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }


        String whereClause = whereClauseBuilder.toString();

        return insertOrUpdate(t, whereClause, (Object[]) whereArgs);
    }

    /**
     * If an Object matching the whereClause exists it is updated. If a matching
     * record does not exist, the Object is inserted into the database.
     * ObjectModel.setPrimaryKey() is called on insert to retrieve the primary keys of newly inserted entities
     *
     * @param t           the Object to insert or update
     * @param whereClause The optional WHERE clause to use to find the record to update.
     *                    Passing null will update the first record returned by the
     *                    databases
     * @return <b>long</b>the ROW_ID of the inserted row or the number of rows affected if updated.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final long insertOrUpdate(T t, String whereClause) throws SQLException, NoSuchFieldException, IllegalAccessException {
        return insertOrUpdate(t, whereClause, (Object[]) null);
    }

    /**
     * If an Object matching the whereClause exists it is updated. If a matching
     * record does not exist, the Object is inserted into the database.
     * ObjectModel.setPrimaryKey() is called on insert to retrieve the primary keys of newly inserted entities
     *
     * @param t           the Object to insert or update
     * @param whereClause The optional WHERE clause to use to find the record to update.
     *                    Passing null will update the first record returned by the
     *                    database
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>long</b> the ROW_ID of the inserted row or the number of rows affected if updated.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final long insertOrUpdate(T t, String whereClause, Object... whereArgs) throws SQLException, NoSuchFieldException, IllegalAccessException {
        boolean exists = exists(whereClause, whereArgs);
        if (!exists) {
            long rowId = insert(t);
            if (rowId > 0)
                setPrimaryKey(t, rowId);
            return rowId;
        } else {

            if (t instanceof InstanceStateTrackable) {
                InstanceStateTrackable stateTrackable = (InstanceStateTrackable) t;
                if (!stateTrackable.isDirty())
                    return 1;
            }

            return update(t, whereClause, whereArgs);
        }
    }

    /**
     * Sets the Objects primary keys after an insert operation using the SQlite row id
     *
     * @param t
     * @param rowId
     * @throws SQLException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 1
     */
    public void setPrimaryKey(T t, long rowId) throws SQLException, NoSuchFieldException, IllegalAccessException {

        ArrayList<ObjectModelColumn> lPrimaryKeys = primaryKeys.get(getObjectType());

        String[] primaryKeyColumns = new String[lPrimaryKeys.size()];
        for (int i = 0; i < lPrimaryKeys.size(); i++) {
            ObjectModelColumn primaryKey = lPrimaryKeys.get(i);
            primaryKeyColumns[i] = primaryKey.getName();
        }

        R r = getDatabaseModel().getDatabaseDriverInterface().query(getTableName(), primaryKeyColumns, " ROWID = ?", new Object[]{rowId}, null, null, null, 1);

        int objectModelColumnIndex = getDatabaseModel().getDatabaseDriverInterface().getFirstColumnIndex();
        for (int i = 0; i < lPrimaryKeys.size(); i++) {

            Object primaryKeyValue = getColumnValue(r, lPrimaryKeys.get(i), objectModelColumnIndex++);

            ObjectModelColumn objectModelColumn = lPrimaryKeys.get(i);

            Field field = getField(objectModelColumn.getName());
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            field.set(t, primaryKeyValue);
            field.setAccessible(accessible);

        }

        onQueryCompleted(r);

    }

    protected abstract void onQueryCompleted(R r);

    /**
     * If an Object matching the whereClause exists it is updated. If a matching
     * record does not exist, the Object is inserted into the database.
     *
     * @param collection the Objects to insert or update
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @throws IllegalArgumentException This method cannot be used on an ObjectModel that does not
     *                                  have a primary key
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int insertOrUpdateAll(Collection<T> collection) throws SQLException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        int result = 0;
        getDatabaseModel().getDatabaseDriverInterface().execute("begin");
        try {
            for (T t : collection) {
                if (insertOrUpdate(t) > 0)
                    result++;
            }
            getDatabaseModel().getDatabaseDriverInterface().execute("end");
        } catch (Exception e) {
            rollbackQuietly();
            throw e;
        }
        return result;
    }

    /**
     * If an Object matching this objects primary key exists it is updated. If a
     * matching record does not exist, the Object is inserted into the database.
     *
     * @param object the Objects to insert or update
     * @param type   the Type of the Object to update
     * @return <b>long</b>the ROW_ID of the inserted row or the number of rows affected if updated.
     * @throws SQLException
     * @throws IllegalArgumentException This method cannot be used on an ObjectModel that does not
     *                                  have a primary key
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    @SuppressWarnings("unchecked")
    public
    final long insertOrUpdateGeneric(Object object, Class<?> type) throws SQLException, NoSuchFieldException, IllegalAccessException {
        return insertOrUpdate((T) ObjectModel.castToObjectType(object, type));
    }

    /**
     * If an Object matching the whereClause exists it is updated. If a matching
     * record does not exist, the Object is inserted into the database.
     *
     * @param objects the Objects to insert or update
     * @param type    the Type of the Object to update
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @throws IllegalArgumentException This method cannot be used on an ObjectModel that does not
     *                                  have a primary key
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int insertOrUpdateAllGeneric(Collection<?> objects, Class<?> type) throws SQLException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        int result = 0;
        getDatabaseModel().getDatabaseDriverInterface().execute("begin");
        try {
            for (Object object : objects.toArray()) {
                if (insertOrUpdateGeneric(object, type) > 0)
                    result++;
            }
            getDatabaseModel().getDatabaseDriverInterface().execute("end");
        } catch (Exception e) {
            rollbackQuietly();
            throw e;
        }
        return result;
    }

    /**
     * Updates the database record identified by the object's primary key with
     * the values of the Object <b>t</b>
     *
     * @param t the Object to update
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int update(T t) throws SQLException {

        if (t instanceof InstanceStateTrackable) {
            InstanceStateTrackable stateTrackable = (InstanceStateTrackable) t;
            if (!stateTrackable.isDirty())
                return -1;
        }

        ArrayList<ObjectModelColumn> lPrimaryKeys = primaryKeys.get(getObjectType());

        StringBuilder whereClauseBuilder = new StringBuilder();

        if (lPrimaryKeys == null || lPrimaryKeys.size() == 0) {
            throw new IllegalArgumentException("The update method cannot be used on an ObjectModel that does not have a primary key");
        }

        int primaryKeyCount = 0;
        for (ObjectModelColumn objectModelColumn : lPrimaryKeys) {
            if (primaryKeyCount++ > 0)
                whereClauseBuilder.append(" AND ");
            whereClauseBuilder.append("\"");
            whereClauseBuilder.append(objectModelColumn.getName());
            whereClauseBuilder.append("\" = ?");
        }

        String[] whereArgs = new String[primaryKeyCount];
        int whereArgsIndex = 0;
        /*for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
            if (objectModelColumn.isPrimaryKey()) {
                try {
                    whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(t, objectModelColumn));
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }*/
        for (ObjectModelColumn objectModelColumn : lPrimaryKeys) {
            try {
                whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(t, objectModelColumn));
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return update(t, whereClauseBuilder.toString(), (Object[]) whereArgs);
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
    public abstract int update(T t, String whereClause, Object... whereArgs) throws SQLException;

    /**
     * @param t the parent Object with the children to insert/update
     * @param queryType
     * @return <T>int</T> Always 0 at this stage
     * @throws SQLException
     */
    protected int insertOrUpdateRelatedChildObjects(T t, QueryType queryType) throws SQLException {
        return insertOrUpdateRelatedChildObjects(t, null, queryType);
    }

    /**
     * @param t           the parent Object with the children to insert/update
     * @param parentRowId If the parent object was newly inserted and has an auto-increment primary key, the rowId of the newly inserted parent object is required to get the auto-increment value
     * @return <T>int</T> Always 0 at this stage
     * @throws SQLException
     */
    protected int insertOrUpdateRelatedChildObjects(T t, Long parentRowId, QueryType queryType) throws SQLException {
        int result = 0;

        /*
         * Insert objects mapped by any relationships to this object
         */
        ArrayList<ObjectModelColumn> lPrimaryKeys = primaryKeys.get(getObjectType());

        for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
            for (Relationship relationship : objectModelColumn.getRelationships()) {
                if (relationship.parentType.isAssignableFrom(t.getClass()) && relationship.getParentReferenceField() != null) {

                    boolean[] accessiblity = new boolean[]{relationship.getParentReferenceField().isAccessible(), relationship.getParentKeyField().isAccessible(), relationship.getChildKeyField().isAccessible()};
                    relationship.getParentReferenceField().setAccessible(true);
                    relationship.getParentKeyField().setAccessible(true);
                    relationship.getChildKeyField().setAccessible(true);

                    try {
                        Object parentReference = relationship.getParentReferenceField().get(t);
                        if (parentReference != null) {

                            if (queryType == QueryType.UPDATE && t instanceof InstanceStateTrackable && !((InstanceStateTrackable) t).isDirty(relationship.getParentReferenceField().getName()))
                                continue;

                            // Ensure the child object's foreign key fields
                            // are set to the primary key of the parent
                            // object
                            Object parentKeyValue = relationship.parentKeyField.get(t);

                            if(parentKeyValue != null && parentKeyValue instanceof Number && ((Number)parentKeyValue).doubleValue() == 0)
                                parentKeyValue = null;

                            if (parentKeyValue == null && parentRowId != null && lPrimaryKeys != null) {

                                relationship.parentKeyField.getName();

                                String[] primaryKeyColumns = new String[1];
                                for (int i = 0; i < lPrimaryKeys.size(); i++) {
                                    ObjectModelColumn primaryKey = lPrimaryKeys.get(i);
                                    if (primaryKey.getName().equals(relationship.parentKeyField.getName())) {
                                        primaryKeyColumns[0] = primaryKey.getName();
                                    }
                                }

                                R r = getDatabaseModel().getDatabaseDriverInterface().query(getTableName(), primaryKeyColumns, " ROWID = ?", new Object[]{parentRowId}, null, null, null, 1);

                                if (parentReference != null /*&& ((Collection<?>)parentReference).size() > 0*/)
                                    parentKeyValue = getColumnValue(r, lPrimaryKeys.get(0), getDatabaseModel().getDatabaseDriverInterface().getFirstColumnIndex());

                                onQueryCompleted(r);
                            }

                            ObjectModel<?, ?, ?> objectModel = getDatabaseModel().getObjectModel(relationship.childType);
                            if (parentReference instanceof Collection<?>) {

                                // Prevent ConcurrentModificationException
                                Object[] parentReferenceSnapshot = ((Collection<?>) parentReference).toArray();

                                for (Object object : parentReferenceSnapshot) {
                                    relationship.childKeyField.set(object, parentKeyValue);
                                }

                                // Insert or update the child objects
                                objectModel.insertOrUpdateAllGeneric(Arrays.asList(parentReferenceSnapshot), objectModel.getObjectType());

                            } else {

                                // Ensure the child object's foreign key field
                                // is set to the primary key of the parent
                                // object
                                relationship.childKeyField.set(parentReference, parentKeyValue);

                                // Insert or update the child object
                                long rowIdOrAffectedCount = objectModel.insertOrUpdateGeneric(parentReference, objectModel.getObjectType());
                            }
                        }
                    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
                        throw new RuntimeException(e);
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
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int delete(Object object) throws SQLException {

        StringBuilder whereClauseBuilder = new StringBuilder();

        ArrayList<ObjectModelColumn> lPrimaryKeys = primaryKeys.get(getObjectType());

        if (lPrimaryKeys == null || lPrimaryKeys.size() == 0) {
            throw new IllegalArgumentException("The delete method cannot be used on an ObjectModel that does not have a primary key");
        }
        int primaryKeyCount = 0;
        for (ObjectModelColumn objectModelColumn : lPrimaryKeys) {
            if (primaryKeyCount++ > 0)
                whereClauseBuilder.append(" AND ");
            whereClauseBuilder.append("\"");
            whereClauseBuilder.append(objectModelColumn.getName());
            whereClauseBuilder.append("\" = ?");
        }

        String[] whereArgs = new String[lPrimaryKeys.size()];
        int whereArgsIndex = 0;

        for (ObjectModelColumn objectModelColumn : lPrimaryKeys) {
            try {
                whereArgs[whereArgsIndex++] = String.valueOf(setColumnValue(object, objectModelColumn));
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return deleteAll(whereClauseBuilder.toString(), (Object[]) whereArgs);
    }

    /**
     * @param collection a list of the Objects to delete
     * @return <b>int</b> the number of affected records
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int deleteAll(Collection<?> collection) throws SQLException {
        int result = 0;
        getDatabaseModel().getDatabaseDriverInterface().execute("begin");
        try {
            for (Object t : collection) {
                result += delete(t);
            }
            getDatabaseModel().getDatabaseDriverInterface().execute("end");
        } catch (Exception e) {
            rollbackQuietly();
            throw e;
        }
        return result;
    }

    /**
     * Rolls back the transaction opened with execute("begin") after a failure, without hiding the original exception.
     */
    private void rollbackQuietly() {
        try {
            getDatabaseModel().getDatabaseDriverInterface().execute("rollback");
        } catch (Exception ignored) {
            // the original failure is the one that matters to the caller
        }
    }

    /**
     * Deletes all records from the ObjectModel's table
     *
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int deleteAll() throws SQLException {
        return deleteAll("1", (Object[]) null);
    }

    /**
     * Deletes the database record(s) matching the <b>whereClause</b> from the
     * ObjectModel's table
     *
     * @param whereClause The optional WHERE clause to apply when deleting. Passing null
     *                    will delete all rows
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public final int deleteAll(String whereClause) throws SQLException {
        return deleteAll(whereClause, (Object[]) null);
    }

    /**
     * Deletes the database record(s) matching the <b>whereClause</b> from the
     * ObjectModel's table
     *
     * @param whereClause The optional WHERE clause to apply when deleting. Passing null
     *                    will delete all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    private static final ThreadLocal<Map<String, Set<String>>> activeDeletions = new ThreadLocal<Map<String, Set<String>>>() {
        @Override
        protected Map<String, Set<String>> initialValue() {
            return new HashMap<>();
        }
    };

    /**
     * Retrieves column string values for the given table and column matching the where clause.
     * Subclasses (Android and JDBC) implement this to extract values using their respective result sets (Cursor/ResultSet).
     *
     * @param tableName   the table name to query
     * @param columnName  the column name to extract
     * @param whereClause the WHERE clause
     * @param whereArgs   arguments for the WHERE clause
     * @return List of non-null string representations of column values
     * @throws SQLException
     */
    protected abstract List<String> getColumnValues(String tableName, String columnName, String whereClause, Object[] whereArgs) throws SQLException;

    /**
     * Deletes the database record(s) matching the <b>whereClause</b> from the
     * ObjectModel's table, automatically cascade deleting related dependent objects
     * if {@link #RELATIONSHIP_MANAGEMENT_CASCADE_DELETE} is enabled, while guarding
     * against self-referential / cyclic infinite recursion.
     *
     * @param whereClause The optional WHERE clause to apply when deleting. Passing null
     *                    will delete all rows
     * @param whereArgs   Question marks in the whereClause String will be replaced with
     *                    these whereArgs in order. (where id = ?)
     * @return <b>int</b> the number of rows affected.
     * @throws SQLException
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @since 0.1
     */
    public int deleteAll(String whereClause, Object... whereArgs) throws SQLException {
        int result = 0;
        getDatabaseModel().getDatabaseDriverInterface().beginTransaction();
        Map<String, Set<String>> deletionsMap = activeDeletions.get();
        Set<String> activeForTable = deletionsMap.get(getTableName());
        if (activeForTable == null) {
            activeForTable = new HashSet<>();
            deletionsMap.put(getTableName(), activeForTable);
        }
        Set<String> newlyTrackedKeys = new HashSet<>();

        try {
            /*
             * Delete dependent objects mapped by any relationships to the objects
             * to be deleted
             */
            if ((relationshipManagementMode & RELATIONSHIP_MANAGEMENT_CASCADE_DELETE) == RELATIONSHIP_MANAGEMENT_CASCADE_DELETE) {
                Set<Relationship> processedRelationships = new HashSet<>();
                for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
                    for (Relationship relationship : objectModelColumn.getRelationships()) {
                        if (relationship.parentType.equals(getObjectType()) && processedRelationships.add(relationship)) {

                            // Get referenced parent keys matching whereClause
                            List<String> parentKeyValues = getColumnValues(getTableName(), relationship.parentKeyField.getName(), whereClause, whereArgs);
                            if (parentKeyValues == null || parentKeyValues.isEmpty()) {
                                continue;
                            }

                            // Filter out parent keys that belong to an ancestor caller to break cycles
                            List<String> newParentKeysForCascade = new ArrayList<>();
                            for (String parentKeyVal : parentKeyValues) {
                                if (activeForTable.add(parentKeyVal)) {
                                    newlyTrackedKeys.add(parentKeyVal);
                                    newParentKeysForCascade.add(parentKeyVal);
                                } else if (newlyTrackedKeys.contains(parentKeyVal)) {
                                    newParentKeysForCascade.add(parentKeyVal);
                                }
                            }

                            if (newParentKeysForCascade.isEmpty()) {
                                continue;
                            }

                            boolean isSameTable = relationship.childType.equals(getObjectType());
                            boolean isNumericChild = getDatabaseModel().getDatabaseDriverInterface().isNumericType(relationship.childKeyField.getType());

                            // Build IN clause for matching children: childKeyField IN (...)
                            StringBuilder childWhereBuilder = new StringBuilder(relationship.childKeyField.getName()).append(" IN (");
                            for (int i = 0; i < newParentKeysForCascade.size(); i++) {
                                if (i > 0) {
                                    childWhereBuilder.append(", ");
                                }
                                if (isNumericChild) {
                                    childWhereBuilder.append(newParentKeysForCascade.get(i));
                                } else {
                                    childWhereBuilder.append("'").append(newParentKeysForCascade.get(i).replace("'", "''")).append("'");
                                }
                            }
                            childWhereBuilder.append(")");

                            // When relationship points to the SAME table (hierarchical structure):
                            // 1. A node referencing itself (parentKey == childKey) must NOT be treated as its own child.
                            // 2. Nodes already in the active deletion chain (activeForTable) must NOT be deleted again.
                            if (isSameTable) {
                                boolean isNumericParent = getDatabaseModel().getDatabaseDriverInterface().isNumericType(relationship.parentKeyField.getType());
                                childWhereBuilder.append(" AND ").append(relationship.parentKeyField.getName()).append(" NOT IN (");
                                int count = 0;
                                for (String activeKey : activeForTable) {
                                    if (count++ > 0) {
                                        childWhereBuilder.append(", ");
                                    }
                                    if (isNumericParent) {
                                        childWhereBuilder.append(activeKey);
                                    } else {
                                        childWhereBuilder.append("'").append(activeKey.replace("'", "''")).append("'");
                                    }
                                }
                                childWhereBuilder.append(")");
                            }

                            ObjectModel<?, ?, ?> childModel = getDatabaseModel().getObjectModel(relationship.childType);
                            result += childModel.deleteAll(childWhereBuilder.toString());
                        }
                    }
                }
            }
            result += getDatabaseModel().getDatabaseDriverInterface().delete(getTableName(), whereClause, whereArgs);
            getDatabaseModel().getDatabaseDriverInterface().setTransactionSuccessful();
        } finally {
            activeForTable.removeAll(newlyTrackedKeys);
            if (activeForTable.isEmpty()) {
                deletionsMap.remove(getTableName());
                if (deletionsMap.isEmpty()) {
                    activeDeletions.remove();
                }
            }
            getDatabaseModel().getDatabaseDriverInterface().endTransaction();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        boolean equal = o instanceof ObjectModel;
        if (equal) {
            ObjectModel other = (ObjectModel) o;
            equal = getTableName().equals(((ObjectModel) o).getTableName());

            if (!equal)
                return equal;

            for (ObjectModelColumn objectModelColumn : getObjectModelColumns()) {
                ObjectModelColumn otherObjectModelColumn = other.getObjectModelColumn(objectModelColumn.getName());
                equal = otherObjectModelColumn != null && objectModelColumn.equals(otherObjectModelColumn);

                if (!equal)
                    return equal;
            }
        }
        return equal;
    }

    /**
     * The type of query being performed for reference in concrete subclasses
     * when determining the columns and their values to be used in database
     * operations.
     *
     * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
     * @version 0.1
     * @since 0.1
     */
    public enum QueryType {
        SELECT, INSERT, UPDATE, DELETE
    }

}

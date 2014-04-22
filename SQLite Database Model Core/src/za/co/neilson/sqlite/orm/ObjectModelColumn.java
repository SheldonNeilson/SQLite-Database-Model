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

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * ObjectModelColumn represents an object field / database table column of an object represented by the ObjectModel
 * @since 0.1
 * @version 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
public class ObjectModelColumn{
        
	protected String name;
	protected Class<?> type;
	protected boolean nullable = false;
	protected boolean primaryKey = false;
	protected boolean autoIncrement = false; 
	protected String foreignKeyParentTable = null;
	protected String foreignKeyParentColumn = null;
	protected boolean unique = false;
	
	protected Set<Relationship> relationships;
	
	public Set<Relationship> getRelationships() {
		return relationships;
	}
	
	protected void setRelationships(HashSet<Relationship> relationships) {
		this.relationships = relationships;
	}

	/**
	 * ObjectModelColumn Constructor
	 * 
	 * @param name the name of the Obect field & table column represented by this ObjectModelColumn
	 * @param type the Object type persisted 
	 * @param nullable whether or not an an Object can be persisted when this value as a null reference
	 * @throws UnsupportedTypeException
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public ObjectModelColumn(String name, Class<?> type, boolean nullable) throws UnsupportedTypeException{		
	    this(name,type,nullable,false,false);
	}
	
	/**
	 * ObjectModelColumn Constructor
	 * 
	 * @param name the name of the Obect field & table column represented by this ObjectModelColumn
	 * @param type the Object type persisted 
	 * @param nullable whether or not an an Object can be persisted when this value as a null reference
	 * @param primaryKey whether this table column should be the table's primary key
	 * @param autoIncrement whether this numeric primary key should be automatically incremented when new records are added to the database
	 * @throws UnsupportedTypeException
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public ObjectModelColumn(String name, Class<?> type, boolean nullable,boolean primaryKey, boolean autoIncrement) throws UnsupportedTypeException{
	    this(name,type,nullable,primaryKey,autoIncrement,null,null);
	}
	
	/**
	 * ObjectModelColumn Constructor
	 * 
	 * @param name the name of the Obect field & table column represented by this ObjectModelColumn
	 * @param type the Object type persisted 
	 * @param nullable whether or not an an Object can be persisted when this value as a null reference
	 * @param primaryKey whether this table column should be the table's primary key
	 * @param autoIncrement whether this numeric primary key should be automatically incremented when new records are added to the database
	 * @param foreignKeyParentTable
	 * @param foreignKeyParentColumn
	 * @throws UnsupportedTypeException
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public ObjectModelColumn(String name, Class<?> type, boolean nullable,boolean primaryKey, boolean autoIncrement, String foreignKeyParentTable, String foreignKeyParentColumn) throws UnsupportedTypeException{
		if(!isSupportedType(type)){
			throw new UnsupportedTypeException();
		}
	    this.name = name;
	    this.type = type;
	    this.nullable = nullable && !primaryKey;
	    this.primaryKey = primaryKey;
	    this.autoIncrement = autoIncrement;
	    this.foreignKeyParentTable = foreignKeyParentTable;
	    this.foreignKeyParentColumn = foreignKeyParentColumn;
	    setRelationships(new HashSet<Relationship>());
	}

	/**
	 * @return the name
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setType(Class<?> type) {
		this.type = type;
	}

	/**
	 * @return the nullable
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public boolean isNullable() {
		return nullable;
	}

	/**
	 * @param nullable the nullable to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	/**
	 * @return the primaryKey
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public boolean isPrimaryKey() {
		return primaryKey;
	}

	/**
	 * @param primaryKey the primaryKey to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
		if(primaryKey)
			setNullable(false);
	}

	/**
	 * @return the autoIncrement
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	/**
	 * @param autoIncrement the autoIncrement to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

	/**
	 * @return the foreignKeyParentTable
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public String getForeignKeyParentTable() {
		return foreignKeyParentTable;
	}

	/**
	 * @param foreignKeyParentTable the foreignKeyParentTable to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setForeignKeyParentTable(String foreignKeyParentTable) {
		this.foreignKeyParentTable = foreignKeyParentTable;
	}
	
	/**
	 * @return the foreignKeyParentColumn
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public String getForeignKeyParentColumn() {
		return foreignKeyParentColumn;
	}

	/**
	 * @param foreignKeyParentColumn the foreignKeyParentColumn to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setForeignKeyColumn(String foreignKeyParentColumn) {
		this.foreignKeyParentColumn = foreignKeyParentColumn;
	}
		
	/**
	 * @return the unique
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public boolean isUnique() {
		return unique;
	}

	/**
	 * @param unique the unique to set
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	/**
	 * @param type the Object type to check
	 * @return <b>boolean</b> whether or not the specified Type is supported for ORM
	 * @since 0.1
	 * @version 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public static boolean isSupportedType(Class<?> type){
		return type == String.class ||
				type == boolean.class ||
				type == Boolean.class ||
				type == byte.class ||
				type == Byte.class ||
				type == short.class ||
				type == Short.class ||
				type == int.class ||
				type == Integer.class ||
				type == long.class ||
				type == Long.class ||
				type == float.class ||
				type == Float.class ||
				type == double.class ||
				type == Double.class ||
				type == Date.class ||
				type == Calendar.class||
				type == byte[].class ||
				type.isEnum();
	}
	
}
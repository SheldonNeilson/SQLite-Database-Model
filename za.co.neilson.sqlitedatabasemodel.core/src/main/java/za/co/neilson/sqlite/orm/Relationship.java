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

public class Relationship{
	
	public enum Type{
		ONE_TO_ONE,
		ONE_TO_MANY,
		MANY_TO_MANY
	}
	
	public Object parent;
	public Class<?> parentType;
	public Field parentKeyField;
	public Field parentReferenceField;
	
	public Object child;
	public Class<?> childType;
	public Field childKeyField;
	public Field childReferenceField;
	
	public Type type;

	public Class<?> getParentType() {
		return parentType;
	}

	public void setParentType(Class<?> parentType) {
		this.parentType = parentType;
	}

	public Field getParentKeyField() {
		return parentKeyField;
	}

	public void setParentKeyField(Field parentKeyField) {
		this.parentKeyField = parentKeyField;
	}

	public Field getParentReferenceField() {
		return parentReferenceField;
	}

	public void setParentReferenceField(Field parentReferenceField) {
		this.parentReferenceField = parentReferenceField;
	}

	public Class<?> getChildType() {
		return childType;
	}

	public void setChildType(Class<?> childType) {
		this.childType = childType;
	}

	public Field getChildKeyField() {
		return childKeyField;
	}

	public void setChildKeyField(Field childKeyField) {
		this.childKeyField = childKeyField;
	}

	public Field getChildReferenceField() {
		return childReferenceField;
	}

	public void setChildReferenceField(Field childReferenceField) {
		this.childReferenceField = childReferenceField;
	}

	

	/**
	 * @return the parent
	 */
	public Object getParent() {
		return parent;
	}

	/**
	 * @param parent the parent to set
	 */
	public void setParent(Object parent) {
		this.parent = parent;
	}

	/**
	 * @return the child
	 */
	public Object getChild() {
		return child;
	}

	/**
	 * @param child the child to set
	 */
	public void setChild(Object child) {
		this.child = child;
	}

	/**
	 * @return the type
	 */
	public Type getRelationshipType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setRelationshipType(Type type) {
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Relationship [parentType=" + parentType + ", parentKeyField=" + parentKeyField + ", parentReferenceField=" + parentReferenceField + ", childType=" + childType + ", childKeyField=" + childKeyField + ", childReferenceField=" + childReferenceField + "]";
	}
	
	
}

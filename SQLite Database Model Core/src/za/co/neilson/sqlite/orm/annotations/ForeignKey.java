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
package za.co.neilson.sqlite.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Designates an ObjectModel property as a foreign key reference to another
 * ObjectModel. The foreign key must reference the primary key of the foreign
 * table using the <b>table</b> and <b>column</b> parameters.
 * </p>
 * <p>
 * By convention, the table name will be the same as the referenced Type's name and the column name will
 * be the same as the name of the Field referenced. 
 * </p>
 * <p>
 * Where a normal Field references the the primary key of the foreign
 * table a one-to-one relationship is formed.
 * </p>
 * <p>
 * If the field decorated with the foreign key attribute is also the primary key of
 * the table, a one-to-one relationship is formed.
 * </p>
 * 
 * @param table
 *            the object name / table name to which the foreign key reference
 *            relates
 * @param column
 *            the object property / table column to which the foreign key
 *            reference relates
 * @param childReference
 *            <b>Optional</b> - this object's reference to the parent object
 * @param parentReference
 *            <b>Optional</b> - the parent object's reference to this object
 * @version 0.1
 * @since 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForeignKey {
	String table();

	String column();

	String childReference() default "";

	String parentReference() default "";
}

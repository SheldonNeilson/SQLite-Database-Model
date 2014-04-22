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
package za.co.neilson.sqlite.orm.android.demo;

import java.sql.SQLException;

import android.content.ContentValues;
import android.database.Cursor;

import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.ObjectModelColumn;
import za.co.neilson.sqlite.orm.Relationship;
import za.co.neilson.sqlite.orm.UnsupportedTypeException;
import za.co.neilson.sqlite.orm.android.AndroidObjectModel;
import za.co.neilson.sqlite.orm.android.demo.WheelNut.ThingaMaJigger.Status;

public class WheelNutModel extends AndroidObjectModel<WheelNut> {

	public WheelNutModel(DatabaseModel<Cursor,ContentValues> databaseModel) throws ClassNotFoundException, NoSuchFieldException {
		super(databaseModel);
	}

	/**
	 * <p>
	 * Here we're overriding onInitializeObjectModelColumns() to demonstrate how
	 * we can manually create a Model's ObjectModelColumns to have finer control
	 * over the columns that are created / or mapped to in the database.
	 * </p>
	 * <p>
	 * Overriding onInitializeObjectModelColumns() is appropriate if
	 * </p>
	 * <ul>
	 * <li>Your model's field names do not match your databases column names</li>
	 * <li>You need to manually map a complex type to a logical value in a
	 * custom column</li>
	 * <li>You would like to create custom relationships</li>
	 * </ul>
	 * <p>
	 * Generally, where onInitializeObjectModelColumns() has been overridden,
	 * setColumnValue() and getColumnValue() will need to be overridden as well
	 * to ensure that your object's fields are correctly mapped to your custom
	 * columns.
	 * </p>
	 * 
	 * @see za.co.neilson.sqlite.orm.ObjectModel#onInitializeObjectModelColumns()
	 */
	@Override
	protected ObjectModelColumn[] onInitializeObjectModelColumns() throws ClassNotFoundException, NoSuchFieldException {
		try {
			objectModelColumns = new ObjectModelColumn[] { new ObjectModelColumn("wheelNutId", int.class, false, true, true), new ObjectModelColumn("wheelId", int.class, false, false, false, "Wheel", "wheelId"),

					// Refers to a complex type in WheelNut.class to demonstrate
					// how custom objects can be
					// mapped to database table columns by overriding
					// onInitializeObjectModelColumns() in the ObjectModel
			new ObjectModelColumn("thingaMaJigger", int.class, true) };

			/*
			 * This relationship would normally be created automatically by
			 * onInitializeObjectModelColumns() provided that the
			 * 
			 * @ForeignKey(table = "Wheel", column = "wheelId") attribute is
			 * present on the wheelId field.
			 * 
			 * Another option would be to call
			 * super.onInitializeObjectModelColumns() before adding the
			 * thingaMaJigger ObjectModelColumn to the
			 * ObjectModel.objectModelColumns[] array.
			 */
			Relationship relationship = new Relationship();
			relationship.setChildType(WheelNut.class);
			relationship.setChildKeyField(WheelNut.class.getDeclaredField("wheelId"));

			relationship.setParentType(Wheel.class);
			relationship.setParentKeyField(Wheel.class.getDeclaredField("wheelId"));

			relationship.setRelationshipType(Relationship.Type.ONE_TO_MANY);

			objectModelColumns[1].getRelationships().add(relationship);

		} catch (UnsupportedTypeException e) {
			e.printStackTrace();
		}
		return objectModelColumns;
	}

	/**
	 * <p>
	 * Here we're overriding ObjectModel.setColumnValue() to provide the logic
	 * for mapping our WheelNut.thingaMaJigger complex type to an integer value
	 * in the database table.
	 * </p>
	 * <p>
	 * If our WheelNut's thingaMaJigger is charged we'll put a 2 in the
	 * thingaMaJigger column in the WheelNut table, if the thingaMaJigger is
	 * depleted we'll assign the column a value of 1. We're not using 0 as when
	 * a nullable integer column is queried, 0 is returned for a null value.
	 * </p>
	 * <p>
	 * We'll delegate the column value mapping for the other fields to the
	 * superclass
	 * </p>
	 * 
	 * @see za.co.neilson.sqlite.orm.ObjectModel#setColumnValue(java.lang.Object,
	 *      za.co.neilson.sqlite.orm.android.ObjectModelColumn)
	 */
	@Override
	protected Object setColumnValue(Object t, ObjectModelColumn objectModelColumn) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		// if the ObjectModelColumn is the thingaMaJigger column, we'll need
		// some custom logic to insert the value of the WheelNut's thingaMaJigger
		// field into the thingaMaJigger int column that we created in our override
		// of ObjectModel.onInitializeObjectModelColumns().
		if (objectModelColumn.getName().equals("thingaMaJigger")) {
			WheelNut.ThingaMaJigger thingaMaJigger = ((WheelNut) t).getThingaMaJigger();
			if (thingaMaJigger == null) {
				return null;
			} else if (thingaMaJigger.getStatus() == Status.CHARGED) {
				return 2;
			} else {
				return 1;
			}

		} else { // Other ObjectModelColumn values can be mapped using the
					// default logic
			return super.setColumnValue(t, objectModelColumn);
		}
	}

	/**
	 * <p>
	 * Here we're overriding ObjectModel.getColumnValue() to provide the logic
	 * for mapping an integer value in the database table the appropriate value
	 * for the WheelNut.thingaMaJigger complex type field
	 * </p>
	 * <p>
	 * If the WheelNut table record's thingaMaJigger column value is one, the
	 * WheelNut should be instantiated with a thingaMaJigger with a charged
	 * status otherwise the WheelNut's thingaMaJigger's status should be
	 * depleted.
	 * </p>
	 * <p>
	 * We'll delegate the column value mapping for the other fields to the
	 * superclass
	 * </p>
	 * 
	 * @see za.co.neilson.sqlite.orm.ObjectModel#getColumnValue(java.sql.ResultSet,
	 *      za.co.neilson.sqlite.orm.android.ObjectModelColumn, int)
	 */
	@Override
	protected Object getColumnValue(Cursor cursor, ObjectModelColumn objectModelColumn, int resultSetObjectModelColumnIndex) throws NoSuchFieldException, SecurityException, SQLException {

		// if the ObjectModelColumn is the thingaMaJigger column, we'll need
		// some custom logic to initialize the WheelNut's thingaMaJigger field.
		if (objectModelColumn.getName().equals("thingaMaJigger")) {

			// retrieve the value from the ResultSet for this ObjectModelColumn
			int value = cursor.getInt(resultSetObjectModelColumnIndex);

			switch (value) {
			case 1: // If the value is 1, we know the WheelNut has a DEPLETED
					// ThingaMaJigger
				return new WheelNut.ThingaMaJigger(Status.DEPLETED);
			case 2: // If the value is 2, we know the WheelNut has a CHARGED
					// ThingaMaJigger
				return new WheelNut.ThingaMaJigger(Status.CHARGED);
			default: // The only other value we should get here is 0, indicating
						// a null value in the table.
				return null;
			}

		} else { // Other ObjectModelColumn values can be mapped using the
					// default logic
			return super.getColumnValue(cursor, objectModelColumn, resultSetObjectModelColumnIndex);
		}
	}

}

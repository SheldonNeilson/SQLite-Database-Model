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

import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import za.co.neilson.sqlite.orm.DatabaseDriverInterface;
import za.co.neilson.sqlite.orm.DatabaseInfo;
import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.android.AndroidObjectModel;
import za.co.neilson.sqlite.orm.android.AndroidSqliteDatabaseDriverInterface;
import za.co.neilson.sqlite.orm.android.demo.Engine.Fuel;
import za.co.neilson.sqlite.orm.android.demo.WheelNut.ThingaMaJigger;
import za.co.neilson.sqlite.orm.android.demo.WheelNut.ThingaMaJigger.Status;

public class CarDatabaseModel extends DatabaseModel<Cursor, ContentValues> {

	public CarDatabaseModel(Context context) throws SQLException,
			ClassNotFoundException, NoSuchFieldException {
		super(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see za.co.neilson.sqlite.orm.DatabaseModel#onCreateDatabaseInfoModel()
	 */
	@Override
	public ObjectModel<DatabaseInfo, Cursor, ContentValues> onCreateDatabaseInfoModel()
			throws ClassNotFoundException, NoSuchFieldException {
		return new AndroidObjectModel<DatabaseInfo>(this) {
		};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * za.co.neilson.sqlite.orm.DatabaseModel#onInitializeDatabaseDriverInterface
	 * (java.lang.Object[])
	 */
	@Override
	protected DatabaseDriverInterface<Cursor, ContentValues> onInitializeDatabaseDriverInterface(
			Object... args) {
		return new AndroidSqliteDatabaseDriverInterface((Context) args[0], this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * za.co.neilson.sqlite.orm.DatabaseModel#onRegisterObjectModels(java.util
	 * .HashMap)
	 */
	@Override
	protected void onRegisterObjectModels(
			HashMap<Type, ObjectModel<?, Cursor, ContentValues>> objectModels)
			throws ClassNotFoundException, NoSuchFieldException {
		/*
		 * Tables Managed By This Model
		 */

		// Register the ObjectModel for the Car class with the DatabaseModel
		objectModels.put(Car.class, new AndroidObjectModel<Car>(this) {
		});

		// Engine has a foreign key reference to Car and must therefore be added
		// after Car.
		objectModels.put(Engine.class, new AndroidObjectModel<Engine>(this) {
		});

		// Wheel has a foreign key reference to Car and must therefore be added
		// after Car.
		objectModels.put(Wheel.class, new AndroidObjectModel<Wheel>(this) {
		});

		/* 
		 * WheelNut has a foreign key reference to Wheel and must therefore be
		 * added after Wheel.
		 * The WheelNutModel class contains custom code for mapping the WheelNut's
		 * thingaMaJigger complex type property to a column in the WheelNut
		 * table
		 */
		objectModels.put(WheelNut.class, new WheelNutModel(this));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see za.co.neilson.sqlite.orm.DatabaseModel#getDatabaseName()
	 */
	@Override
	public String getDatabaseName() {
		return "Cars.db";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see za.co.neilson.sqlite.orm.DatabaseModel#getDatabaseVersion()
	 */
	@Override
	public int getDatabaseVersion() {
		return 9;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see za.co.neilson.sqlite.orm.DatabaseModel#onCreate()
	 */
	@Override
	public void onCreate() throws SQLException {
		super.onCreate();
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see za.co.neilson.sqlite.orm.DatabaseModel#onUpgrade(int)
	 */
	@Override
	public void onUpgrade(int previousVersion) throws SQLException {
		super.onUpgrade(previousVersion);
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see za.co.neilson.sqlite.orm.DatabaseModel#onInsertDefaultValues()
	 */
	@Override
	protected void onInsertDefaultValues() {

		ObjectModel<Car, ?, ?> carModel = getObjectModel(Car.class);

		Car polo = new Car();
		polo.setMake("Volkswagen");
		polo.setModel("Polo");
		polo.setMileage(60149);
		polo.setManufacturedDate(Date.valueOf("2010-06-05"));
		polo.setRegistration("BM52ZVGP");
		polo.setRegistered(false);

		polo.setWheels(new ArrayList<Wheel>(4));
		for (int i = 0; i < 4; i++) {
			Wheel wheel = new Wheel();
			wheel.setSize(18);
			polo.getWheels().add(wheel);
		}

		try {
			carModel.insert(polo);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Car mazda = new Car();
		mazda.setMake("Mazda");
		mazda.setModel("3");
		mazda.setMileage(48526);
		mazda.setManufacturedDate(Date.valueOf("2012-03-17"));
		mazda.setRegistration("FS13TWGP");
		mazda.setRegistered(true);

		Engine mazdaEngine = new Engine();
		mazdaEngine.setFuel(Fuel.PETROL);
		mazdaEngine.setPower(77);
		mazdaEngine.setTorque(145);
		mazdaEngine.setEngineCapacity(1.6F);
		mazda.setEngine(mazdaEngine);

		mazda.setWheels(new ArrayList<Wheel>(4));
		for (int i = 0; i < 4; i++) {
			Wheel wheel = new Wheel();
			wheel.setSize(18);
			/*
			 * Since we have specified Car's "wheels" property as the
			 * parentReference in Wheel's foreign key relationship to car, all
			 * we need to do to add the wheels to the database is to add them to
			 * the car's wheels collection and insert the car. We don't even
			 * need to set the wheel's carRegistration property. This is done
			 * for us internally.
			 */
			mazda.getWheels().add(wheel);
		}

		try {
			carModel.insert(mazda);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Car ford = new Car();
		ford.setMake("Ford");
		ford.setModel("Ranger");
		ford.setMileage(162276);
		ford.setManufacturedDate(Date.valueOf("2011-07-26"));
		ford.setRegistration("PZ56XTGP");
		ford.setRegistered(true);

		Engine fordEngine = new Engine();
		fordEngine.setFuel(Fuel.PETROL);
		fordEngine.setPower(147);
		fordEngine.setTorque(470);
		fordEngine.setEngineCapacity(3.2F);
		ford.setEngine(fordEngine);

		ford.setWheels(new ArrayList<Wheel>(4));
		for (int i = 0; i < 4; i++) {
			Wheel wheel = new Wheel();
			wheel.setSize(18);
			ford.getWheels().add(wheel);
		}

		try {
			carModel.insert(ford);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			ObjectModel<Wheel, ?, ?> wheelModel = getObjectModel(Wheel.class);
			ObjectModel<WheelNut, ?, ?> wheelNutModel = getObjectModel(WheelNut.class);

			for (Wheel wheel : wheelModel
					.getAll("carRegistration = 'PZ56XTGP'")) {
				for (int i = 0; i < 5; i++) {
					WheelNut wheelNut = new WheelNut();
					wheelNut.setWheelId(wheel.getWheelId());
					wheelNut.setThingaMaJigger(new ThingaMaJigger(
							i % 2 == 0 ? Status.CHARGED : Status.DEPLETED));
					wheelNutModel.insert(wheelNut);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}

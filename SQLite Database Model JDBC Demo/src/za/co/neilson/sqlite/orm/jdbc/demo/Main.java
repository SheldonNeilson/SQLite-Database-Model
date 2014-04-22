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
package za.co.neilson.sqlite.orm.jdbc.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import za.co.neilson.collections.queryable.Queryable;
import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.jdbc.demo.Engine.Fuel;
import za.co.neilson.sqlite.orm.jdbc.demo.WheelNut.ThingaMaJigger;
import za.co.neilson.sqlite.orm.jdbc.demo.WheelNut.ThingaMaJigger.Status;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		try {
			boolean run = true;
			while (run) {
				try {
					System.out.println("");
					System.out.println("1: Insert Demo");
					System.out.println("2: Select Demo");
					System.out.println("3: Update Demo");
					System.out.println("4: Delete Demo");
					System.out
							.println("5: Charge the Fords wheel nut's ThingaMaJiggers");
					System.out
							.println("6: Deplete the Fords wheel nut's ThingaMaJiggers");
					System.out
							.print("Select a demo to execute (1, 2, 3, 4 or 5) or type anything else to exit: ");
					String input = reader.readLine();
					System.out.println("");
					int selection = Integer.parseInt(input);
					switch (selection) {
					case 1:
						insertDemo();
						break;
					case 2:
						selectDemo();
						break;
					case 3:
						updateDemo();
						break;
					case 4:
						deleteDemo();
						break;
					case 5:
						setTheFordsWheelNutThingaMaJiggersStatuses(Status.CHARGED);
						break;
					case 6:
						setTheFordsWheelNutThingaMaJiggersStatuses(Status.DEPLETED);
						break;
					default:
						throw new IllegalArgumentException("Invalid option");
					}

				} catch (IllegalArgumentException ex) {
					run = false;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void selectDemo() {
		CarDatabaseModel carDatabaseModel = null;
		try {

			carDatabaseModel = new CarDatabaseModel();

			ObjectModel<WheelNut, ?, ?> wheelNutModel = carDatabaseModel
					.getObjectModel(WheelNut.class);

			// Use the DatabaseModel to get the ObjectModel for the Car class
			List<Car> cars = carDatabaseModel.getObjectModel(Car.class)
			// Call get all cars with a milege > 50000
					.getAll("mileage > ?", 50000);


			// Display the results
			for (Car car : cars) {
				System.out.println(car);

				// The Volkswagen's Engine was not created in the
				// CarDataBaseModel.onInsertDefaultValues() method.
				System.out.println(car.getEngine());

				for (Wheel wheel : car.getWheels()) {
					System.out.println(wheel);

					/*
					 * The WheelNut Class was not created with a reference to
					 * it's wheel and the wheel has no reference to it's wheel
					 * nuts. Therefore we need to use the WheelNutModel to
					 * insert the wheel nuts.
					 * 
					 * If you would like the wheel nuts to be inserted, updated
					 * & deleted along with their wheels. Add a
					 * Collection<WheelNut> property to the Wheel class and a
					 * Wheel property to the WheelNut class. Then include the
					 * reference variables in WheelNut.wheelId's ForeignKey
					 * attribute:
					 * 
					 * @ForeignKey(table="Wheel", column="wheelId",
					 * parentReference="wheelNuts", childReference="wheel")
					 * private int wheelId;
					 * 
					 * Only the Ford Ranger's Wheels were seeded with WheelNuts
					 */
					for (WheelNut wheelNut : wheelNutModel.getAll(
							"wheelId = ?", wheel.getWheelId()))
						System.out.println(wheelNut);
				}
				System.out.println();
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			e.printStackTrace();
		} finally {
			if (carDatabaseModel != null) {
				// Disconnect the DatabaseModel's connection to the SQLite
				// database
				carDatabaseModel.disconnect();
				// Make the DatabaseModel eligible for garbage collection
				carDatabaseModel = null;
			}
		}
	}

	public static void insertDemo() {
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = new CarDatabaseModel();

			Car honda = new Car();
			honda.setMake("Honda");
			honda.setModel("Jazz");
			honda.setRegistered(true);
			honda.setRegistration("RG23BTGP");
			honda.setMileage(55673);
			honda.setManufacturedDate(Date.valueOf("2013-12-03"));

			honda.setWheels(new ArrayList<Wheel>(4));
			for (int w = 0; w < 4; w++) {
				Wheel wheel = new Wheel();
				wheel.setSize(16);
				honda.getWheels().add(wheel);
			}

			/*
			 * Inserting the honda will automatically insert it's wheels!
			 * 
			 * Using insertAndReturnUpdated() means that after the honda and
			 * it's wheels are inserted, the honda will be updated by a fresh
			 * select on the database. As the Wheel's foreign key specifies the
			 * Car classes Collection<Wheel> wheels property as the parent
			 * reference, the honda's Wheels will also be updated including the
			 * wheelIds that were assigned by SQLite when they were inserted
			 * into the database
			 */
			honda = carDatabaseModel.getObjectModel(Car.class)
					.insertAndReturnUpdated(honda);

			/*
			 * When we inserted the Honda into the database, the Wheels were
			 * inserted as well and SQLite automatically assigned each wheel a
			 * new wheelId as the wheelId was created as an auto-incrementing
			 * primary key.
			 * 
			 * Since we used the insertAndReturnUpdated() method to insert the
			 * Honda, it and its wheels were inserted and then updated from the
			 * database including the newly assigned wheelIds. had we just used
			 * the insert() method, all the wheelIds would still be 0.
			 */
			ObjectModel<WheelNut, ?, ?> wheelNutModel = carDatabaseModel
					.getObjectModel(WheelNut.class);

			for (Wheel wheel : honda.getWheels()) {
				for (int i = 0; i < 5; i++) {
					WheelNut wheelNut = new WheelNut();
					wheelNut.setWheelId(wheel.getWheelId());
					wheelNut.setThingaMaJigger(new ThingaMaJigger(
							Status.CHARGED));
					wheelNutModel.insert(wheelNut);
				}
			}

			/*
			 * For the sake of this demonstration let's close our database
			 * connection and dispose our models to prove that the records were
			 * persisted.
			 */
			carDatabaseModel.disconnect();
			carDatabaseModel = null;
			wheelNutModel = null;
			System.gc();

			/*
			 * Now lets get the Honda from the database and display the results.
			 * Getting the Honda will automatically get it's wheels due to the
			 * relationship we created between the two but we'll need to get
			 * each wheel's wheel nuts manually
			 */
			carDatabaseModel = new CarDatabaseModel();
			honda = carDatabaseModel.getObjectModel(Car.class).getFirst(
					"registration = ?", "RG23BTGP");
			System.out.println(honda);

			wheelNutModel = carDatabaseModel.getObjectModel(WheelNut.class);
			for (Wheel wheel : honda.getWheels()) {
				System.out.println(wheel);
				for (WheelNut wheelNut : wheelNutModel.getAll("wheelId = ?",
						wheel.getWheelId())) {
					System.out.println(wheelNut);
				}
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			e.printStackTrace();
		} finally {
			if (carDatabaseModel != null) {
				carDatabaseModel.disconnect();
				carDatabaseModel = null;
			}
		}
	}

	public static void updateDemo() {
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = new CarDatabaseModel();

			ObjectModel<Car, ?, ?> carModel = carDatabaseModel
					.getObjectModel(Car.class);

			Car polo = carModel.getFirst("make = ? AND model = ? ",
					"Volkswagen", "Polo");

			/*
			 * Let's update the Polo's mileage
			 */
			int mileage = polo.getMileage() + 10000;
			polo.setMileage(mileage);

			/*
			 * The Polo was initially created without a corresponding record in
			 * the Engine table. Let's give it one!
			 */
			Engine engine = new Engine();
			engine.setEngineCapacity(1.2F);
			engine.setFuel(Fuel.DIESEL);
			engine.setPower(55F);
			engine.setTorque(180F);
			polo.setEngine(engine);

			/*
			 * All that we have to do to update the Polo is pass it to the
			 * ObjectModel's update() method
			 */
			carModel.update(polo);

			/*
			 * For the sake of this demonstration let's close our database
			 * connection and dispose our models to prove that the records were
			 * persisted.
			 */
			carDatabaseModel.disconnect();
			carDatabaseModel = null;
			carModel = null;
			System.gc();

			/*
			 * Now lets get the Polo from the database and display the results.
			 * Getting the Polo will automatically get it's Engine due to the
			 * relationship we created between the Car and Engine Models
			 */
			carDatabaseModel = new CarDatabaseModel();
			carModel = carDatabaseModel.getObjectModel(Car.class);

			polo = carModel.getFirst("make = ? AND model = ?", "Volkswagen",
					"Polo");

			System.out.println(polo);
			System.out.println(polo.getEngine());

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			e.printStackTrace();
		} finally {
			if (carDatabaseModel != null) {
				carDatabaseModel.disconnect();
				carDatabaseModel = null;
			}
		}
	}

	public static void deleteDemo() {
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = new CarDatabaseModel();

			ObjectModel<Car, ?, ?> carModel = carDatabaseModel
					.getObjectModel(Car.class);

			Car honda = carModel.getFirst("make = ? ", "Honda");
			if (honda != null) {

				/*
				 * Deleting the Honda will automatically delete it's wheels,
				 * however since we opted not to create a reference to a wheel's
				 * wheel nuts in the wheel class we need to delete each wheel's
				 * wheel nuts manually before we can delete them
				 */
				for (Wheel wheel : honda.getWheels()) {
					carDatabaseModel.getObjectModel(WheelNut.class).deleteAll(
							"wheelId = ?", wheel.getWheelId());
				}

				carModel.delete(honda);
			}

			/*
			 * Now that we have deleted the Honda, let's output all the
			 * remaining cars in the Car table
			 */
			for (Car car : carModel.getAll()) {
				System.out.println(car);
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			e.printStackTrace();
		} finally {
			if (carDatabaseModel != null) {
				carDatabaseModel.disconnect();
				carDatabaseModel = null;
			}
		}
	}

	public static void queryableDemo() {
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = new CarDatabaseModel();

			ObjectModel<Car, ?, ?> carModel = carDatabaseModel
					.getObjectModel(Car.class);

			// Transform the list of cars returned by carModel.getAll() into a
			// queryable collection
			Queryable<Car> queryableCars = Queryable.asQueryable(carModel
					.getAll());

			// Select a random Manufactured Date to filter the results on
			final Date aSignificantDate = Date.valueOf("2012-01-01");

			for (Car car : queryableCars.where(new Queryable.Matcher<Car>() {
				@Override
				public boolean isMatch(Car t) {
					return t.getManufacturedDate().before(aSignificantDate);
				}
			}).orderByDescending(new Queryable.Field<Car>() {
				@Override
				public Comparable<?> getFieldValue(Car t) {
					return ((Car) t).getManufacturedDate();
				}

			})) {
				System.out.println(car);
			}

		} catch (SQLException | ClassNotFoundException | NoSuchFieldException e) {
			e.printStackTrace();
		} finally {
			if (carDatabaseModel != null) {
				carDatabaseModel.disconnect();
				carDatabaseModel = null;
			}
		}
	}

	public static void setTheFordsWheelNutThingaMaJiggersStatuses(Status status) {
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = new CarDatabaseModel();

			ObjectModel<Car, ?, ?> carModel = carDatabaseModel
					.getObjectModel(Car.class);
			ObjectModel<WheelNut, ?, ?> wheelNutModel = carDatabaseModel
					.getObjectModel(WheelNut.class);

			List<Car> cars = carModel.getAll("make = ? AND model = ?", "Ford",
					"Ranger");

			for (Car car : cars) {
				for (Wheel wheel : car.getWheels()) {

					/*
					 * The WheelNut Class was not created with a reference to
					 * it's wheel and the wheel has no reference to it's wheel
					 * nuts. Therefore we need to use the WheelNutModel to
					 * update the wheel nuts.
					 * 
					 * If you would like the wheel nuts to be inserted, updated
					 * & deleted along with their wheels. Add a
					 * Collection<WheelNut> property to the Wheel class and a
					 * Wheel property to the WheelNut class. Then include the
					 * reference variables in WheelNut.wheelId's ForeignKey
					 * attribute:
					 * 
					 * @ForeignKey(table="Wheel", column="wheelId",
					 * parentReference="wheelNuts", childReference="wheel")
					 * private int wheelId;
					 */
					for (WheelNut wheelNut : wheelNutModel.getAll(
							"wheelId = ?", wheel.getWheelId())) {
						wheelNut.getThingaMaJigger().setStatus(status);
						wheelNutModel.update(wheelNut);
					}
				}
			}

			/*
			 * For the sake of this demonstration let's close our database
			 * connection and dispose our models to prove that the records were
			 * persisted.
			 */
			carDatabaseModel.disconnect();
			carDatabaseModel = null;
			carModel = null;
			wheelNutModel = null;

			System.gc();

			/*
			 * Let's make sure the Ford's wheel's nuts statuses were changed!
			 */
			carDatabaseModel = new CarDatabaseModel();
			carModel = carDatabaseModel.getObjectModel(Car.class);
			wheelNutModel = carDatabaseModel.getObjectModel(WheelNut.class);

			cars = carModel.getAll("make = ? AND model = ?", "Ford", "Ranger");

			for (Car car : cars) {
				System.out.println(car);
				System.out.println(car.getEngine());
				for (Wheel wheel : car.getWheels()) {
					// Output the Wheel
					System.out.println(wheel);
					// Output the wheel's nuts
					for (WheelNut wheelNut : wheelNutModel.getAll(
							"wheelId = ?", new Object[] { wheel.getWheelId() }))
						System.out.println(wheelNut);
				}
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			e.printStackTrace();
		} finally {
			if (carDatabaseModel != null) {
				carDatabaseModel.disconnect();
				carDatabaseModel = null;
			}
		}
	}

}

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

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.android.demo.Engine.Fuel;
import za.co.neilson.sqlite.orm.android.demo.WheelNut.ThingaMaJigger;
import za.co.neilson.sqlite.orm.android.demo.WheelNut.ThingaMaJigger.Status;
import za.co.neilson.sqlite.orm.app.R;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	private static CarDatabaseModel carDatabaseModel;

	public static CarDatabaseModel getCarDatabaseModel(Context context)
			throws ClassNotFoundException, NoSuchFieldException, SQLException {
		if (carDatabaseModel == null) {
			carDatabaseModel = new CarDatabaseModel(context);
		}
		return carDatabaseModel;
	}

	public static void setCarDatabaseModel(CarDatabaseModel carDatabaseModel) {
		MainActivity.carDatabaseModel = carDatabaseModel;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		if (carDatabaseModel != null)
			carDatabaseModel.disconnect();
		super.onPause();
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);

			final TextView textView = (TextView) rootView
					.findViewById(R.id.textView);
			Button buttonSelectDemo = (Button) rootView
					.findViewById(R.id.buttonSelectDemo);
			buttonSelectDemo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					selectDemo(getActivity(), textView);
				}
			});
			Button buttonInsertDemo = (Button) rootView
					.findViewById(R.id.buttonInsertDemo);
			buttonInsertDemo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					insertDemo(getActivity(), textView);
				}
			});
			Button buttonUpdateDemo = (Button) rootView
					.findViewById(R.id.buttonUpdateDemo);
			buttonUpdateDemo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					updateDemo(getActivity(), textView);
				}
			});
			Button buttonDeleteDemo = (Button) rootView
					.findViewById(R.id.buttonDeleteDemo);
			buttonDeleteDemo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					deleteDemo(getActivity(), textView);
				}
			});
			Button buttonChargeTheFordsWheelNutThingaMaJiggers = (Button) rootView
					.findViewById(R.id.buttonChargeTheFordsWheelNutThingaMaJiggers);
			buttonChargeTheFordsWheelNutThingaMaJiggers
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							setTheFordsWheelNutThingaMaJiggersStatuses(
									Status.CHARGED, getActivity(), textView);
						}
					});

			try {

				CarDatabaseModel carDatabaseModel = new CarDatabaseModel(
						getActivity());
				ObjectModel<Car, ?, ?> carModel = carDatabaseModel
						.getObjectModel(Car.class);

				Car car = carModel.getFirst(null);
				textView.setText(car.toString());

			} catch (ClassNotFoundException | NoSuchFieldException
					| SQLException e) {
				Log.e(MainActivity.class.getName(), "Exception", e);
			}

			return rootView;
		}
	}

	public static void selectDemo(Context context, TextView textView) {
		CarDatabaseModel carDatabaseModel = null;
		StringBuilder stringBuilder = new StringBuilder();
		try {
			carDatabaseModel = getCarDatabaseModel(context);

			ObjectModel<WheelNut, ?, ?> wheelNutModel = getCarDatabaseModel(
					context).getObjectModel(WheelNut.class);

			// Use the DatabaseModel to get the ObjectModel for the Car class
			List<Car> cars = carDatabaseModel.getObjectModel(Car.class)
			// Call get all cars with a milege > 50000
					.getAll("mileage > ?", 50000);

			// Append the results to the String for display
			for (Car car : cars) {
				stringBuilder.append(String.valueOf(car) + "\n");

				// The Volkswagen's Engine was not created in the
				// CarDataBaseModel.onInsertDefaultValues() method.
				stringBuilder.append(String.valueOf(car.getEngine()) + "\n");

				for (Wheel wheel : car.getWheels()) {
					stringBuilder.append(String.valueOf(wheel) + "\n");

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
						stringBuilder.append(String.valueOf(wheelNut) + "\n");
				}
				stringBuilder.append("\n");
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			Log.e(MainActivity.class.getName(), e.getMessage(), e);
		} finally {
			if (carDatabaseModel != null) {
				// Disconnect the DatabaseModel's connection to the SQLite
				// database
				carDatabaseModel.disconnect();
				// Make the DatabaseModel eligible for garbage collection
				carDatabaseModel = null;
			}
		}
		textView.setText(stringBuilder.toString());
	}

	public static void insertDemo(Context context, TextView textView) {
		StringBuilder stringBuilder = new StringBuilder();
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = getCarDatabaseModel(context);

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
			carDatabaseModel = getCarDatabaseModel(context);
			honda = carDatabaseModel.getObjectModel(Car.class).getFirst(
					"registration = ?", "RG23BTGP");
			stringBuilder.append(String.valueOf(honda) + "\n");

			wheelNutModel = carDatabaseModel.getObjectModel(WheelNut.class);
			for (Wheel wheel : honda.getWheels()) {
				System.out.println(wheel);
				for (WheelNut wheelNut : wheelNutModel.getAll("wheelId = ?",
						wheel.getWheelId())) {
					System.out.println(wheelNut);
				}
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			Log.e(MainActivity.class.getName(), e.getMessage(), e);
		} finally {
			if (carDatabaseModel != null) {
				// Disconnect the DatabaseModel's connection to the SQLite
				// database
				carDatabaseModel.disconnect();
				// Make the DatabaseModel eligible for garbage collection
				carDatabaseModel = null;
			}
		}
		textView.setText(stringBuilder.toString());
	}

	public static void updateDemo(Context context, TextView textView) {
		StringBuilder stringBuilder = new StringBuilder();
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = getCarDatabaseModel(context);

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
			 * The polo was initially created without a corresponding record in
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

			carDatabaseModel = getCarDatabaseModel(context);
			carModel = carDatabaseModel.getObjectModel(Car.class);

			polo = carModel.getFirst("make = ? AND model = ?", "Volkswagen",
					"Polo");

			stringBuilder.append(String.valueOf(polo) + "\n");
			stringBuilder.append(String.valueOf(polo.getEngine()) + "\n");

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			Log.e(MainActivity.class.getName(), e.getMessage(), e);
		} finally {
			if (carDatabaseModel != null) {
				// Disconnect the DatabaseModel's connection to the SQLite
				// database
				carDatabaseModel.disconnect();
				// Make the DatabaseModel eligible for garbage collection
				carDatabaseModel = null;
			}
		}
		textView.setText(stringBuilder.toString());
	}

	public static void deleteDemo(Context context, TextView textView) {
		StringBuilder stringBuilder = new StringBuilder();
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = getCarDatabaseModel(context);

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
				stringBuilder.append(String.valueOf(car) + "\n");
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			Log.e(MainActivity.class.getName(), e.getMessage(), e);
		} finally {
			if (carDatabaseModel != null) {
				// Disconnect the DatabaseModel's connection to the SQLite
				// database
				carDatabaseModel.disconnect();
				// Make the DatabaseModel eligible for garbage collection
				carDatabaseModel = null;
			}
		}
		textView.setText(stringBuilder.toString());
	}

	public static void setTheFordsWheelNutThingaMaJiggersStatuses(
			Status status, Context context, TextView textView) {
		StringBuilder stringBuilder = new StringBuilder();
		CarDatabaseModel carDatabaseModel = null;
		try {
			carDatabaseModel = getCarDatabaseModel(context);

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
					 * update the wheel nuts manually.
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
			carDatabaseModel = getCarDatabaseModel(context);
			carModel = carDatabaseModel.getObjectModel(Car.class);
			wheelNutModel = carDatabaseModel.getObjectModel(WheelNut.class);

			cars = carModel.getAll("make = ? AND model = ?", "Ford", "Ranger");

			for (Car car : cars) {
				stringBuilder.append(String.valueOf(car) + "\n");
				stringBuilder.append(String.valueOf(car.getEngine()) + "\n");
				for (Wheel wheel : car.getWheels()) {
					// Output the Wheel
					stringBuilder.append(String.valueOf(wheel) + "\n");
					// Output the wheel's nuts
					for (WheelNut wheelNut : wheelNutModel.getAll(
							"wheelId = ?", new Object[] { wheel.getWheelId() }))
						stringBuilder.append(String.valueOf(wheelNut) + "\n");
				}
			}

		} catch (ClassNotFoundException | NoSuchFieldException | SQLException e) {
			Log.e(MainActivity.class.getName(), e.getMessage(), e);
		} finally {
			if (carDatabaseModel != null) {
				// Disconnect the DatabaseModel's connection to the SQLite
				// database
				carDatabaseModel.disconnect();
				// Make the DatabaseModel eligible for garbage collection
				carDatabaseModel = null;
			}
		}
		textView.setText(stringBuilder.toString());
	}

}

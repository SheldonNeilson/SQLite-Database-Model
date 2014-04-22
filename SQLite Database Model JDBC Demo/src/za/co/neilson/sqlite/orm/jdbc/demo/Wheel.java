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

import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.Nullable;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;

public class Wheel {

	/*
	 * Setting the @PrimaryKey annotation's autoIncrement option to true means
	 * that SQLite will automatically assign a sequential numeric value for the
	 * primary key when the object is first inserted into the database.
	 */
	@PrimaryKey(autoIncrement = true)
	private int wheelId;

	/*
	 * By decorating the size property with the @Nullable annotation with the
	 * value set to false, we are instructing SQLite Database Model to create
	 * the Wheel table with a non-null value constraint on the size column
	 */
	@Nullable(value = false)
	private int size;

	/*
	 * Specifying the childReference as the car Field means that this object's
	 * car property will automatically be initialized when this wheel is
	 * retrieved from the database. Specifying the parentReference as "wheels"
	 * means that when the parent (Car) object is retrieved from the database,
	 * SQLite Database Model will search for a property in the Car object called
	 * wheels that is of the type Collection<Wheel> and fill it with all the
	 * wheels in the Wheel table with the same registration as the car.
	 */
	@ForeignKey(table = "Car", column = "registration", childReference = "car", parentReference = "wheels")
	private String carRegistration;

	private Car car;

	public int getWheelId() {
		return wheelId;
	}

	public void setWheelId(int wheelId) {
		this.wheelId = wheelId;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getCarRegistration() {
		return carRegistration;
	}

	public void setCarRegistration(String carRegistration) {
		this.carRegistration = carRegistration;
	}

	public Car getCar() {
		return car;
	}

	public void setCar(Car car) {
		this.car = car;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Wheel [wheelId=" + wheelId + ", size=" + size + ", car="
				+ car.getMake() + " " + car.getModel() + " ("
				+ car.getRegistration() + ")" + "]";
	}

}

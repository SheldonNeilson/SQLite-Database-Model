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

import java.util.Collection;
import java.util.Date;

import za.co.neilson.sqlite.orm.annotations.PrimaryKey;

public class Car {

	@PrimaryKey
	private String registration;
	private String make;
	private String model;
	private int mileage;
	private Date manufacturedDate;
	private boolean registered;

	/*
	 * The wheels collection will automatically be filled by SQLite Database
	 * Model when the car is fetched from the database as it has been defined as
	 * the parent reference in the foreign key relationship in the Wheel class
	 */
	private Collection<Wheel> wheels;

	/*
	 * The Engine object will automatically be initialized by SQLite Database
	 * Model when the car is fetched from the database as it has been defined as
	 * the parent reference in the foreign key relationship in the Engine class
	 */
	private Engine engine;

	public Car() {

	}

	public String getRegistration() {
		return registration;
	}

	public void setRegistration(String registration) {
		this.registration = registration;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public int getMileage() {
		return mileage;
	}

	public void setMileage(int mileage) {
		this.mileage = mileage;
	}

	public Date getManufacturedDate() {
		return manufacturedDate;
	}

	public void setManufacturedDate(Date manufacturedDate) {
		this.manufacturedDate = manufacturedDate;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

	public Collection<Wheel> getWheels() {
		return wheels;
	}

	public void setWheels(Collection<Wheel> wheels) {
		this.wheels = wheels;
	}
	

	public Engine getEngine() {
		return engine;
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Car [registration=" + registration + ", make=" + make
				+ ", model=" + model + ", mileage="
				+ mileage + ", manufacturedDate=" + manufacturedDate
				+ ", registered=" + registered + "]";
	}

}

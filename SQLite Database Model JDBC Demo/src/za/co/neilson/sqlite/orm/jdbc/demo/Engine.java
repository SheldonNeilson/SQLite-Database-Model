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
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;

public class Engine {

	// SQLite Database Model can map any enum to a column in a database table by
	// default
	enum Fuel {
		PETROL, DIESEL
	}

	@PrimaryKey
	@ForeignKey(table = "car", column = "registration", parentReference = "engine")
	private String carRegistration;
	private float engineCapacity;
	private float torque;
	private float power;
	private Fuel fuel;

	/**
	 * @return the carRegistration
	 */
	public String getCarRegistration() {
		return carRegistration;
	}

	/**
	 * @param carRegistration
	 *            the carRegistration to set
	 */
	public void setCarRegistration(String carRegistration) {
		this.carRegistration = carRegistration;
	}

	/**
	 * @return the engineCapacity
	 */
	public float getEngineCapacity() {
		return engineCapacity;
	}

	/**
	 * @param engineCapacity
	 *            the engineCapacity to set
	 */
	public void setEngineCapacity(float engineCapacity) {
		this.engineCapacity = engineCapacity;
	}

	/**
	 * @return the torque
	 */
	public float getTorque() {
		return torque;
	}

	/**
	 * @param torque
	 *            the torque to set
	 */
	public void setTorque(float torque) {
		this.torque = torque;
	}

	/**
	 * @return the power
	 */
	public float getPower() {
		return power;
	}

	/**
	 * @param power
	 *            the power to set
	 */
	public void setPower(float power) {
		this.power = power;
	}

	/**
	 * @return the fuel
	 */
	public Fuel getFuel() {
		return fuel;
	}

	/**
	 * @param fuel
	 *            the fuel to set
	 */
	public void setFuel(Fuel fuel) {
		this.fuel = fuel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Engine [carRegistration=" + carRegistration
				+ ", engineCapacity=" + engineCapacity + ", torque=" + torque
				+ ", power=" + power + ", fuel=" + fuel + "]";
	}

}

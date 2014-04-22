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

import android.annotation.SuppressLint;
import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;

public class WheelNut {

	@PrimaryKey(autoIncrement = true)
	private int wheelNutId;

	@ForeignKey(table = "Wheel", column = "wheelId")
	private int wheelId;

	/**
	 * A complex type to demonstrate how custom objects can be mapped to database table columns by an ObjectModel
	 * @version 0.1
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */	
	public static class ThingaMaJigger {

		public enum Status{
			CHARGED,
			DEPLETED;
			
			@SuppressLint("DefaultLocale")
			public String toString() {
				return this.name().toLowerCase();
			};
		}
		private Status status;
		
		public ThingaMaJigger(Status status){
			this.status = status;
		}
		
		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		@Override
		public String toString() {
			return " ThingaMaJigger [The WheelNut's ThingaMaJigger is " + status.toString() + "]";
		}

	}

	private ThingaMaJigger thingaMaJigger;

	/**
	 * @return the wheelNutId
	 */
	public int getWheelNutId() {
		return wheelNutId;
	}

	/**
	 * @param wheelNutId
	 *            the wheelNutId to set
	 */
	public void setWheelNutId(int wheelNutId) {
		this.wheelNutId = wheelNutId;
	}

	/**
	 * @return the wheelId
	 */
	public int getWheelId() {
		return wheelId;
	}

	/**
	 * @param wheelId
	 *            the wheelId to set
	 */
	public void setWheelId(int wheelId) {
		this.wheelId = wheelId;
	}
	
	public ThingaMaJigger getThingaMaJigger() {
		return thingaMaJigger;
	}

	public void setThingaMaJigger(ThingaMaJigger thingaMaJigger) {
		this.thingaMaJigger = thingaMaJigger;
	}

	@Override
	public String toString() {
		return "WheelNut [wheelNutId=" + wheelNutId + ", wheelId=" + wheelId + ", thingaMaJigger="+ getThingaMaJigger() + "]";
	}
}

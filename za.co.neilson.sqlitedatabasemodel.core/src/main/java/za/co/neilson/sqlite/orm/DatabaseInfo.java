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

import java.util.Date;

/**
 * Represents an entry in the the DatabaseInfo Table that stores database version
 * and audit information
 * 
 * @version 0.1
 * @since 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
public class DatabaseInfo {

	protected int version;
	protected Date createdDate;
	protected Date accessedDate;

	/**
	 * @return the version
	 * @version 0.1
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * @param version
	 *            the version to set
	 * @version 0.1
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setVersion(int version) {
		this.version = version;
	}

	/**
	 * @return the createdDate
	 * @version 0.1
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public Date getCreatedDate() {
		return createdDate;
	}

	/**
	 * @param createdDate
	 *            the createdDate to set
	 * @version 0.1
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	/**
	 * @return the accessedDate
	 * @version 0.1
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public Date getAccessedDate() {
		return accessedDate;
	}

	/**
	 * @param accessedDate
	 *            the accessedDate to set
	 * @version 0.1
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public void setAccessedDate(Date accessedDate) {
		this.accessedDate = accessedDate;
	}

}

package za.co.neilson.sqlite.orm;

import java.util.Arrays;
import java.util.regex.Pattern;

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

/**
 * Utility methods for creating database queries
 * 
 * @version 0.1
 * @since 0.1
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 */
public class Query {

	/**
	 * Join an array of strings or individual arguments into into a single comma
	 * delimited String
	 * 
	 * @param args
	 *            the Strings to join into a single comma delimited String
	 * @return joined String
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public static String join(String... args) {
		if (args.length < 1)
			throw new IllegalArgumentException();

		String joined = Arrays.toString(args);
		String result = joined.substring(1, joined.length() - 1);
		return result;
	}

	/**
	 * @param object
	 *            the variable to validate as numeric
	 * @return <b>boolean</b> true if the supplied Object can be parsed as a
	 *         numeric value
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public static boolean isNumeric(Object object) {
		// Double.parseDouble also accepts "NaN", "Infinity", "1d" and "2f", which are not valid SQL numbers
		return NUMERIC_PATTERN.matcher(String.valueOf(object).trim()).matches();
	}

	private static final Pattern NUMERIC_PATTERN = Pattern.compile("[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?");

	/**
	 * @param type
	 *            the Type to validate as a numeric type
	 * @return <b>boolean</b> true if the Type is used to contain numeric values
	 * @since 0.1
	 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
	 */
	public static boolean isNumericType(Class<?> type) {
		return type.equals(Double.class) || type.equals(Float.class)
				|| type.equals(Integer.class) || type.equals(Short.class)
				|| type.equals(Long.class) || type.equals(Byte.class)
				|| type.equals(double.class) || type.equals(float.class)
				|| type.equals(int.class) || type.equals(short.class)
				|| type.equals(long.class) || type.equals(byte.class);
	}

}

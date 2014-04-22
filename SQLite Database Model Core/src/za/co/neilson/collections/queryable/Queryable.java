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
package za.co.neilson.collections.queryable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Queryable<T> extends ArrayList<T> {

	public static abstract class Matcher<T> {
		public abstract boolean isMatch(T t);
	}

	public static abstract class Field<T> {
		public abstract Comparable<?> getFieldValue(T o);
	}

	public static <X> Queryable<X> asQueryable(List<X> list) {
		Queryable<X> queryable = new Queryable<X>();
		queryable.addAll(list);
		return queryable;
	}

	private static final long serialVersionUID = 1L;

	public Queryable<T> where(Matcher<T> matcher) {
		Queryable<T> result = new Queryable<>();
		for (T t : this) {
			if (matcher.isMatch(t)) {
				result.add(t);
			}
		}
		return result;
	}

	public T first(Matcher<T> matcher) {
		for (T t : this) {
			if (matcher.isMatch(t)) {
				return t;
			}
		}
		return null;
	}

	public Queryable<T> orderBy(final Field<T> field) {

		Collections.sort(this, new Comparator<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Object o1, Object o2) {
				return ((Comparable<Object>) field.getFieldValue((T) o1))
						.compareTo((Comparable<Object>) field
								.getFieldValue((T) o2));
			}
		});

		return this;
	}

	public Queryable<T> orderByDescending(final Field<T> field) {
		Collections.sort(this, new Comparator<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Object o1, Object o2) {
				return ((Comparable<Object>) field.getFieldValue((T) o2))
						.compareTo((Comparable<Object>) field
								.getFieldValue((T) o1));
			}
		});
		return this;
	}
}

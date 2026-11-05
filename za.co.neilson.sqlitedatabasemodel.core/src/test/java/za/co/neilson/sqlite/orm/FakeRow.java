package za.co.neilson.sqlite.orm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One result row for {@link FakeDriver}: ordered column names and values (index 0 based). */
final class FakeRow {
    final List<String> names = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    FakeRow(Object... nameValuePairs) {
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            names.add((String) nameValuePairs[i]);
            values.add(nameValuePairs[i + 1]);
        }
    }

    @Override
    public String toString() {
        return names + "=" + Arrays.toString(values.toArray());
    }
}

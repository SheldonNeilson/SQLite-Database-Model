package za.co.neilson.sqlite.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QueryTest {

    @Test
    public void join_singleValueIsReturnedUnchanged() {
        assertEquals("id", Query.join("id"));
    }

    @Test
    public void join_multipleValuesAreCommaSeparated() {
        assertEquals("id, name, created", Query.join("id", "name", "created"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void join_requiresAtLeastOneValue() {
        Query.join();
    }

    @Test
    public void isNumeric_acceptsPlainNumbers() {
        String[] numeric = {"0", "42", "-7", "+3", "3.14", ".5", "1e3", "007", " 5 "};
        for (String s : numeric)
            assertTrue(s, Query.isNumeric(s));
        assertTrue(Query.isNumeric(12));
        assertTrue(Query.isNumeric(12L));
        assertTrue(Query.isNumeric(1.5d));
    }

    @Test
    public void isNumeric_rejectsTextAndNull() {
        String[] text = {"abc", "", "12abc", "1,5", "1 2", "--1"};
        for (String s : text)
            assertFalse(s, Query.isNumeric(s));
        assertFalse(Query.isNumeric(null));
        assertFalse(Query.isNumeric(new Object()));
    }

    @Test
    public void isNumeric_rejectsDoubleParseSpecials() {
        String[] notSqlNumbers = {"NaN", "Infinity", "-Infinity", "1d", "2f", "0x1p3"};
        for (String s : notSqlNumbers)
            assertFalse(s, Query.isNumeric(s));
    }

    @Test
    public void isNumericType_classifiesBoxedFloatingAndSmallIntegers() {
        assertTrue(Query.isNumericType(Double.class));
        assertTrue(Query.isNumericType(Float.class));
        assertTrue(Query.isNumericType(Integer.class));
        assertTrue(Query.isNumericType(Short.class));
        assertFalse(Query.isNumericType(String.class));
        assertFalse(Query.isNumericType(Boolean.class));
    }

    @Test
    public void isNumericType_coversLongByteAndPrimitives() {
        Class<?>[] numeric = {Long.class, Byte.class, long.class, int.class, short.class, byte.class, float.class, double.class};
        for (Class<?> c : numeric)
            assertTrue(c.getName(), Query.isNumericType(c));
    }
}

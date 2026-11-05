package za.co.neilson.sqlite.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class ObjectModelColumnTest {

    private enum Color {RED, GREEN}

    @Test
    public void isSupportedType_acceptsEveryMappedType() {
        Class<?>[] supported = {
                String.class, boolean.class, Boolean.class, byte.class, Byte.class, short.class, Short.class,
                int.class, Integer.class, long.class, Long.class, float.class, Float.class, double.class,
                Double.class, Date.class, Calendar.class, byte[].class, Color.class};
        for (Class<?> type : supported)
            assertTrue(type.getName(), ObjectModelColumn.isSupportedType(type));
    }

    @Test
    public void isSupportedType_rejectsUnmappableTypes() {
        Class<?>[] unsupported = {Object.class, BigDecimal.class, char.class, Character.class, int[].class, java.util.List.class, java.sql.Timestamp.class};
        for (Class<?> type : unsupported)
            assertFalse(type.getName(), ObjectModelColumn.isSupportedType(type));
    }

    @Test
    public void constructor_rejectsUnsupportedType() {
        try {
            new ObjectModelColumn("price", BigDecimal.class, true);
            fail("expected UnsupportedTypeException");
        } catch (UnsupportedTypeException e) {
            assertTrue(e.getMessage().contains("not supported"));
        }
    }

    @Test
    public void constructor_shortFormDefaults() throws Exception {
        ObjectModelColumn c = new ObjectModelColumn("name", String.class, true);
        assertEquals("name", c.getName());
        assertEquals(String.class, c.getType());
        assertTrue(c.isNullable());
        assertFalse(c.isPrimaryKey());
        assertFalse(c.isAutoIncrement());
        assertFalse(c.isUnique());
        assertNull(c.getForeignKeyParentTable());
        assertNull(c.getForeignKeyParentColumn());
        assertTrue("relationships start as an empty, usable set", c.getRelationships().isEmpty());
    }

    @Test
    public void constructor_primaryKeyIsNeverNullable() throws Exception {
        ObjectModelColumn c = new ObjectModelColumn("id", long.class, true, true, true);
        assertTrue(c.isPrimaryKey());
        assertTrue(c.isAutoIncrement());
        assertFalse("a primary key cannot be nullable even if asked", c.isNullable());
    }

    @Test
    public void constructor_keepsForeignKeyTarget() throws Exception {
        ObjectModelColumn c = new ObjectModelColumn("parentId", long.class, false, false, false, "Parent", "id");
        assertEquals("Parent", c.getForeignKeyParentTable());
        assertEquals("id", c.getForeignKeyParentColumn());
        assertFalse(c.isNullable());
    }

    @Test
    public void setPrimaryKey_forcesNotNullable() throws Exception {
        ObjectModelColumn c = new ObjectModelColumn("id", int.class, true);
        c.setPrimaryKey(true);
        assertTrue(c.isPrimaryKey());
        assertFalse(c.isNullable());
    }

    @Test
    public void setPrimaryKey_falseLeavesNullabilityAlone() throws Exception {
        ObjectModelColumn c = new ObjectModelColumn("id", int.class, false, true, false);
        c.setPrimaryKey(false);
        assertFalse(c.isPrimaryKey());
        assertFalse(c.isNullable());
    }

    @Test
    public void equals_comparesNameAndTypeOnly() throws Exception {
        ObjectModelColumn a = new ObjectModelColumn("x", int.class, true);
        ObjectModelColumn sameNameAndType = new ObjectModelColumn("x", int.class, false, true, true);
        assertEquals(a, sameNameAndType);
        assertNotEquals(a, new ObjectModelColumn("y", int.class, true));
        assertNotEquals(a, new ObjectModelColumn("x", long.class, true));
        assertNotEquals(a, "x");
        assertNotEquals(a, null);
    }

    @Test
    public void equalColumnsHaveEqualHashCodes() throws Exception {
        ObjectModelColumn a = new ObjectModelColumn("x", int.class, true);
        ObjectModelColumn b = new ObjectModelColumn("x", int.class, true);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}

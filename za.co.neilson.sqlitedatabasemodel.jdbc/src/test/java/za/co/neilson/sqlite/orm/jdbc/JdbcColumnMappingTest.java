package za.co.neilson.sqlite.orm.jdbc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import za.co.neilson.sqlite.orm.ObjectModel.QueryType;
import za.co.neilson.sqlite.orm.OrmTestState;
import za.co.neilson.sqlite.orm.jdbc.Entities.AllTypes;
import za.co.neilson.sqlite.orm.jdbc.Entities.Mode;
import za.co.neilson.sqlite.orm.jdbc.Entities.Tracked;

/** Entity to column values ({@code setColumnValues}) and result set cell to field value ({@code getColumnValue}). */
public class JdbcColumnMappingTest {

    private JdbcTestDatabase db;
    private JdbcTestDatabase.TestModel<AllTypes> model;

    @Before
    public void setUp() throws Exception {
        OrmTestState.reset();
        db = JdbcTestDatabase.create(1, Entities.ALL);
        model = db.model(AllTypes.class);
    }

    @After
    public void tearDown() throws Exception {
        db.close();
    }

    private static AllTypes sample() {
        AllTypes a = new AllTypes();
        a.id = 4;
        a.text = "hello";
        a.flag = true;
        a.boxedFlag = Boolean.FALSE;
        a.tiny = 5;
        a.small = 300;
        a.number = 70000;
        a.big = 5_000_000_000L;
        a.ratio = 1.5f;
        a.precise = 2.25;
        a.date = new Date(1_700_000_000_000L);
        a.calendar = Calendar.getInstance();
        a.calendar.setTimeInMillis(1_600_000_000_000L);
        a.blob = new byte[]{1, 2, 3};
        a.mode = Mode.B;
        a.optionalNumber = 42;
        return a;
    }

    // ---- setColumnValues -----------------------------------------------------------------------------------------

    @Test
    public void setColumnValues_insertLeavesOutTheAutoIncrementKey() {
        HashMap<String, Object> values = model.setColumnValues(sample(), QueryType.INSERT);
        assertFalse(values.containsKey("id"));
        assertEquals(14, values.size());
    }

    @Test
    public void setColumnValues_updateIncludesTheKey() {
        assertEquals(4L, model.setColumnValues(sample(), QueryType.UPDATE).get("id"));
        assertEquals(15, model.setColumnValues(sample(), QueryType.UPDATE).size());
    }

    @Test
    public void setColumnValues_convertsDatesToEpochMillisAndKeepsOtherTypes() {
        HashMap<String, Object> v = model.setColumnValues(sample(), QueryType.INSERT);
        assertEquals("hello", v.get("text"));
        assertEquals(1_700_000_000_000L, v.get("date"));
        assertEquals(1_600_000_000_000L, v.get("calendar"));
        assertEquals((byte) 5, v.get("tiny"));
        assertEquals((short) 300, v.get("small"));
        assertEquals(70000, v.get("number"));
        assertEquals(5_000_000_000L, v.get("big"));
        assertEquals(1.5f, v.get("ratio"));
        assertEquals(2.25, v.get("precise"));
        assertEquals(Mode.B, v.get("mode"));
        assertEquals(Boolean.TRUE, v.get("flag"));
        assertEquals(Boolean.FALSE, v.get("boxedFlag"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) v.get("blob"));
    }

    @Test
    public void setColumnValues_nullsStayNull() {
        AllTypes a = new AllTypes();
        HashMap<String, Object> v = model.setColumnValues(a, QueryType.INSERT);
        assertTrue(v.containsKey("text"));
        assertNull(v.get("text"));
        assertNull(v.get("date"));
        assertNull(v.get("calendar"));
        assertNull(v.get("boxedFlag"));
        assertNull(v.get("optionalNumber"));
        assertNull(v.get("mode"));
        assertNull(v.get("blob"));
    }

    @Test
    public void setColumnValues_updateOfTrackedInstanceContainsOnlyDirtyColumns() throws Exception {
        JdbcTestDatabase.TestModel<Tracked> tracked = db.model(Tracked.class);
        Tracked t = new Tracked();
        t.id = 1;
        t.a = "changed";
        t.b = "untouched";
        t.setDirty("a", true);

        HashMap<String, Object> update = tracked.setColumnValues(t, QueryType.UPDATE);
        assertEquals(1, update.size());
        assertEquals("changed", update.get("a"));

        assertEquals("inserts always write every column", 3, tracked.setColumnValues(t, QueryType.INSERT).size());
    }

    // ---- getColumnValue ------------------------------------------------------------------------------------------

    /** Inserts one row with SQLite-native storage, reads it back through the driver and maps column {@code name}. */
    private Object read(String insertValues, String column) throws Exception {
        db.memory().execute("DELETE FROM AllTypes");
        db.memory().execute("INSERT INTO AllTypes (text, flag, boxedFlag, tiny, small, number, big, ratio, precise, date, calendar, blob, mode, optionalNumber) VALUES (" + insertValues + ")");
        ResultSet rs = db.getDatabaseDriverInterface().query("AllTypes", new String[]{column}, null, null, null, null, null, null);
        try {
            assertTrue(rs.next());
            return model.getColumnValue(rs, model.getObjectModelColumn(column), 1);
        } finally {
            rs.close();
        }
    }

    private static final String FILLED = "'hi', 1, 0, 5, 300, 70000, 5000000000, 1.5, 2.25, 1700000000000, 1600000000000, x'010203', 'B', 42";
    private static final String NULLS = "NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL";

    @Test
    public void getColumnValue_mapsEveryTypeFromItsSqliteStorage() throws Exception {
        assertEquals("hi", read(FILLED, "text"));
        assertEquals(Boolean.TRUE, read(FILLED, "flag"));
        assertEquals(Boolean.FALSE, read(FILLED, "boxedFlag"));
        assertEquals(Byte.valueOf((byte) 5), read(FILLED, "tiny"));
        assertEquals(Short.valueOf((short) 300), read(FILLED, "small"));
        assertEquals(Integer.valueOf(70000), read(FILLED, "number"));
        assertEquals(Long.valueOf(5_000_000_000L), read(FILLED, "big"));
        assertEquals(Float.valueOf(1.5f), read(FILLED, "ratio"));
        assertEquals(Double.valueOf(2.25), read(FILLED, "precise"));
        assertEquals(new Date(1_700_000_000_000L), read(FILLED, "date"));
        assertEquals(1_600_000_000_000L, ((Calendar) read(FILLED, "calendar")).getTimeInMillis());
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) read(FILLED, "blob"));
        assertEquals(Mode.B, read(FILLED, "mode"));
        assertEquals(Integer.valueOf(42), read(FILLED, "optionalNumber"));
    }

    @Test
    public void getColumnValue_nullTextAndBlobAreNull() throws Exception {
        assertNull(read(NULLS, "text"));
        assertNull(read(NULLS, "blob"));
    }

    @Test
    public void getColumnValue_unknownEnumConstantIsRejected() throws Exception {
        try {
            read(FILLED.replace("'B'", "'ZZZ'"), "mode");
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void getColumnValue_nullBecomesNullForBoxedTypes() throws Exception {
        assertNull("Integer", read(NULLS, "optionalNumber"));
        assertNull("Boolean", read(NULLS, "boxedFlag"));
        assertNull("Date", read(NULLS, "date"));
        assertNull("Calendar", read(NULLS, "calendar"));
        assertNull("enum", read(NULLS, "mode"));
    }
}

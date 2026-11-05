package za.co.neilson.sqlite.orm.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import za.co.neilson.sqlite.orm.OrmTestState;
import za.co.neilson.sqlite.orm.jdbc.Entities.AllTypes;
import za.co.neilson.sqlite.orm.jdbc.Entities.Child;
import za.co.neilson.sqlite.orm.jdbc.Entities.Parent;
import za.co.neilson.sqlite.orm.jdbc.Entities.Person;

/** Tables created by the ORM on a real (in-memory) SQLite database. */
public class JdbcSchemaTest {

    private JdbcTestDatabase db;

    @Before
    public void setUp() {
        OrmTestState.reset();
    }

    @After
    public void tearDown() throws Exception {
        if (db != null)
            db.close();
    }

    @Test
    public void everyModelGetsATable_plusDatabaseInfo() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        assertEquals(Arrays.asList("AllTypes", "Child", "Country", "DatabaseInfo", "Membership", "Parent", "Person", "Tracked"), db.memory().tables());
    }

    @Test
    public void columnsUseTheDeclaredSqliteTypes() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        Map<String, String> columns = db.memory().columns("AllTypes");
        assertEquals(Arrays.asList("id", "text", "flag", "boxedFlag", "tiny", "small", "number", "big", "ratio", "precise", "date", "calendar", "blob", "mode", "optionalNumber"),
                Arrays.asList(columns.keySet().toArray()));
        assertEquals("INTEGER", columns.get("id"));
        assertEquals("TEXT", columns.get("text"));
        assertEquals("TINYINT", columns.get("flag"));
        assertEquals("TINYINT", columns.get("tiny"));
        assertEquals("SMALLINT", columns.get("small"));
        assertEquals("INT", columns.get("number"));
        assertEquals("BIGINT", columns.get("big"));
        assertEquals("FLOAT", columns.get("ratio"));
        assertEquals("DOUBLE", columns.get("precise"));
        assertEquals("DATETIME", columns.get("date"));
        assertEquals("DATETIME", columns.get("calendar"));
        assertEquals("BLOB", columns.get("blob"));
    }

    @Test
    public void primaryKeyAndNotNullFlagsAreApplied() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        Map<String, Map<String, Object>> byName = tableInfo("Person");
        assertEquals(1, ((Number) byName.get("id").get("pk")).intValue());
        assertEquals("autoincrement key is never null", 1, ((Number) byName.get("id").get("notnull")).intValue());
        assertEquals(0, ((Number) byName.get("name").get("notnull")).intValue());
        assertEquals("@Nullable(false)", 1, ((Number) byName.get("country").get("notnull")).intValue());
        assertTrue(db.memory().scalar("SELECT sql FROM sqlite_master WHERE name = 'Person'").toString().contains("AUTOINCREMENT"));
    }

    @Test
    public void uniqueAnnotationCreatesAUniqueIndex() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        boolean uniqueOnEmail = false;
        for (Map<String, Object> index : db.memory().rows("PRAGMA index_list(\"Person\")")) {
            if (((Number) index.get("unique")).intValue() == 1) {
                for (Map<String, Object> col : db.memory().rows("PRAGMA index_info(\"" + index.get("name") + "\")"))
                    uniqueOnEmail |= "email".equals(col.get("name"));
            }
        }
        assertTrue(uniqueOnEmail);
    }

    @Test
    public void compositePrimaryKeyCoversBothColumnsInDeclarationOrder() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        Map<String, Map<String, Object>> byName = tableInfo("Membership");
        assertEquals(1, ((Number) byName.get("groupId").get("pk")).intValue());
        assertEquals(2, ((Number) byName.get("userId").get("pk")).intValue());
        assertEquals(0, ((Number) byName.get("role").get("pk")).intValue());
    }

    @Test
    public void foreignKeyIsDeclaredAndIndexed() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        List<Map<String, Object>> fks = db.memory().rows("PRAGMA foreign_key_list(\"Child\")");
        assertEquals(1, fks.size());
        assertEquals("Parent", fks.get(0).get("table"));
        assertEquals("parentId", fks.get(0).get("from"));
        assertEquals("id", fks.get(0).get("to"));
        assertEquals("index on the foreign key column", "parentId",
                db.memory().scalar("SELECT name FROM pragma_index_info('IX_Child_parentId')"));
    }

    @Test
    public void databaseInfoRowRecordsTheVersion() throws Exception {
        db = JdbcTestDatabase.create(7, Entities.ALL);
        List<Map<String, Object>> rows = db.memory().rows("SELECT version, createdDate, accessedDate FROM DatabaseInfo");
        assertEquals(1, rows.size());
        assertEquals(7, ((Number) rows.get(0).get("version")).intValue());
        assertNotNull(rows.get(0).get("createdDate"));
        assertEquals(rows.get(0).get("createdDate"), rows.get(0).get("accessedDate"));
    }

    @Test
    public void rebuild_dropsAllRowsAndLeavesAWorkingSchema() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        db.memory().execute("INSERT INTO Person (email, name, country) VALUES ('a@x.com', 'A', 'ZA')");
        assertEquals(1, db.memory().count("Person"));

        db.connect();
        db.rebuild();
        db.disconnect();

        assertEquals(0, db.memory().count("Person"));
        db.memory().execute("INSERT INTO Person (email, name, country) VALUES ('a@x.com', 'A', 'ZA')");
        assertEquals(1, db.memory().count("Person"));
        assertEquals("one fresh DatabaseInfo row", 1, db.memory().count("DatabaseInfo"));
    }

    @Test
    public void tablesUseTheEntitySimpleNameAndRegisteredModelsAreLookedUpByType() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        assertEquals("AllTypes", db.model(AllTypes.class).table());
        assertEquals(Person.class, db.model(Person.class).getObjectType());
        assertEquals(Parent.class, db.model(Child.class).getObjectModelColumn("parentId").getRelationships().iterator().next().getParentType());
    }

    @Test
    public void createTable_separatesColumnsWithCommas() throws Exception {
        db = JdbcTestDatabase.createWithJdbcDdl(1, (d, models) -> models.put(Person.class, new Entities.PersonModel(d)));
        Map<String, String> columns = db.memory().columns("Person");
        assertEquals(Arrays.asList("id", "email", "name", "country", "age"), Arrays.asList(columns.keySet().toArray()));
    }

    @Test
    public void upgrade_recreatesEveryTable() throws Exception {
        db = JdbcTestDatabase.create(1, Entities.ALL);
        db.upgrade();
        assertEquals(8, db.memory().tables().size());
        assertEquals(5, db.memory().columns("Person").size());
    }

    private Map<String, Map<String, Object>> tableInfo(String table) throws Exception {
        Map<String, Map<String, Object>> byName = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : db.memory().rows("PRAGMA table_info(\"" + table + "\")"))
            byName.put((String) row.get("name"), row);
        assertFalse(table + " has columns", byName.isEmpty());
        return byName;
    }
}

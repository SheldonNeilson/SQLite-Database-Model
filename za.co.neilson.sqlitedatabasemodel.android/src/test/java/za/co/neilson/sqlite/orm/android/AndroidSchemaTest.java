package za.co.neilson.sqlite.orm.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import za.co.neilson.sqlite.orm.OrmTestState;
import za.co.neilson.sqlite.orm.android.Entities.AllTypes;
import za.co.neilson.sqlite.orm.android.Entities.Child;
import za.co.neilson.sqlite.orm.android.Entities.Parent;
import za.co.neilson.sqlite.orm.android.Entities.Person;
import za.co.neilson.sqlite.orm.android.Entities.TreeNode;

/** Schema creation and upgrade by the ORM on a real SQLite database (Robolectric). */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AndroidSchemaTest {

    private Context context;
    private AndroidTestDatabase db;

    @Before
    public void setUp() {
        OrmTestState.reset();
        context = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        if (db != null)
            db.close();
    }

    private Map<String, Map<String, Object>> tableInfo(String table) throws Exception {
        Map<String, Map<String, Object>> byName = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : db.rows("SELECT * FROM pragma_table_info('" + table + "')"))
            byName.put((String) row.get("name"), row);
        assertFalse(table + " has columns", byName.isEmpty());
        return byName;
    }

    @Test
    public void everyModelGetsATable_plusDatabaseInfo() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        assertEquals(Arrays.asList("AllTypes", "Child", "Country", "DatabaseInfo", "Membership", "Parent", "Person", "Tracked", "TreeNode"), db.tables());
    }

    @Test
    public void columnsUseTheDeclaredSqliteTypes() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        Map<String, String> columns = db.columns("AllTypes");
        assertEquals(Arrays.asList("id", "text", "flag", "boxedFlag", "tiny", "small", "number", "big", "ratio", "precise", "date", "calendar", "blob", "mode",
                "optionalNumber", "optionalBig", "optionalDouble"), Arrays.asList(columns.keySet().toArray()));
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
        assertEquals("BLOB", columns.get("blob"));
    }

    @Test
    public void primaryKeyNotNullAndAutoIncrementAreApplied() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        Map<String, Map<String, Object>> person = tableInfo("Person");
        assertEquals(1L, person.get("id").get("pk"));
        assertEquals(1L, person.get("id").get("notnull"));
        assertEquals(0L, person.get("name").get("notnull"));
        assertEquals("@Nullable(false)", 1L, person.get("country").get("notnull"));
        assertTrue(db.scalar("SELECT sql FROM sqlite_master WHERE name = 'Person'").toString().contains("AUTOINCREMENT"));
    }

    @Test
    public void uniqueAnnotationCreatesAUniqueIndex() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        boolean uniqueOnEmail = false;
        for (Map<String, Object> index : db.rows("SELECT * FROM pragma_index_list('Person')")) {
            if (((Long) index.get("unique")) == 1L) {
                for (Map<String, Object> col : db.rows("SELECT * FROM pragma_index_info('" + index.get("name") + "')"))
                    uniqueOnEmail |= "email".equals(col.get("name"));
            }
        }
        assertTrue(uniqueOnEmail);
    }

    @Test
    public void compositePrimaryKeyCoversBothColumnsInDeclarationOrder() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        Map<String, Map<String, Object>> byName = tableInfo("Membership");
        assertEquals(1L, byName.get("groupId").get("pk"));
        assertEquals(2L, byName.get("userId").get("pk"));
        assertEquals(0L, byName.get("role").get("pk"));
    }

    @Test
    public void foreignKeyIsDeclaredAndIndexed() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        List<Map<String, Object>> fks = db.rows("SELECT * FROM pragma_foreign_key_list('Child')");
        assertEquals(1, fks.size());
        assertEquals("Parent", fks.get(0).get("table"));
        assertEquals("parentId", fks.get(0).get("from"));
        assertEquals("id", fks.get(0).get("to"));
        assertEquals("parentId", db.scalar("SELECT name FROM pragma_index_info('IX_Child_parentId')"));
    }

    @Test
    public void selfReferencingForeignKeyIsAddedAfterTheTable() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        assertEquals(Arrays.asList("id", "label", "parentId"), Arrays.asList(db.columns("TreeNode").keySet().toArray()));
        List<Map<String, Object>> fks = db.rows("SELECT * FROM pragma_foreign_key_list('TreeNode')");
        assertEquals(1, fks.size());
        assertEquals("TreeNode", fks.get(0).get("table"));
        assertEquals("id", fks.get(0).get("to"));
        assertNotNull(db.scalar("SELECT name FROM sqlite_master WHERE name = 'IX_TreeNode_parentId'"));
    }

    @Test
    public void databaseInfoRowRecordsTheVersion() throws Exception {
        db = AndroidTestDatabase.create(context, 7, Entities.ALL);
        List<Map<String, Object>> rows = db.rows("SELECT version, createdDate, accessedDate FROM DatabaseInfo");
        assertEquals(1, rows.size());
        assertEquals(7L, rows.get(0).get("version"));
        assertNotNull(rows.get(0).get("createdDate"));
        assertEquals(rows.get(0).get("createdDate"), rows.get(0).get("accessedDate"));
    }

    @Test
    public void reopeningAtTheSameVersionKeepsTheData() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        String name = db.name();
        db.execute("INSERT INTO Person (email, name, country) VALUES ('a@x.com', 'A', 'ZA')");
        db.close();

        OrmTestState.reset();
        db = AndroidTestDatabase.open(context, name, 1, Entities.ALL);

        assertEquals(1, db.count("Person"));
        assertEquals("no second DatabaseInfo row", 1, db.count("DatabaseInfo"));
    }

    @Test
    public void reopeningAtANewVersionRecreatesTheSchemaAndDropsTheData() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        String name = db.name();
        db.execute("INSERT INTO Person (email, name, country) VALUES ('a@x.com', 'A', 'ZA')");
        db.close();

        OrmTestState.reset();
        db = AndroidTestDatabase.open(context, name, 2, Entities.ALL);

        assertEquals("tables are recreated", 0, db.count("Person"));
        assertEquals(Arrays.asList("id", "email", "name", "country", "age"), Arrays.asList(db.columns("Person").keySet().toArray()));
        assertEquals(2L, db.scalar("SELECT version FROM DatabaseInfo"));
    }

    @Test
    public void rebuild_dropsRowsAndLeavesAWorkingSchema() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        db.execute("INSERT INTO Person (email, name, country) VALUES ('a@x.com', 'A', 'ZA')");

        db.rebuild();

        assertEquals(0, db.count("Person"));
        Person p = new Person();
        p.email = "b@x.com";
        p.country = "ZA";
        assertEquals(1, db.model(Person.class).insert(p));
    }

    @Test
    public void modelsAreLookedUpByEntityTypeAndKnowTheirTable() throws Exception {
        db = AndroidTestDatabase.create(context, 1, Entities.ALL);
        assertEquals("AllTypes", db.model(AllTypes.class).table());
        assertEquals(Parent.class, db.model(Child.class).getObjectModelColumn("parentId").getRelationships().iterator().next().getParentType());
        assertEquals(TreeNode.class, db.model(TreeNode.class).getObjectType());
    }
}

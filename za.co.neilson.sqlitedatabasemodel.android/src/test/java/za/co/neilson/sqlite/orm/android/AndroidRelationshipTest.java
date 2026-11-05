package za.co.neilson.sqlite.orm.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import za.co.neilson.sqlite.orm.ObjectModel;
import za.co.neilson.sqlite.orm.OrmTestState;
import za.co.neilson.sqlite.orm.android.Entities.Child;
import za.co.neilson.sqlite.orm.android.Entities.Parent;
import za.co.neilson.sqlite.orm.android.Entities.TreeNode;

/** Foreign key relationships: related-object insert/update/retrieve and cascade delete on a real SQLite database. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AndroidRelationshipTest {

    private AndroidTestDatabase db;
    private AndroidTestDatabase.TestModel<Parent> parents;
    private AndroidTestDatabase.TestModel<Child> children;
    private AndroidTestDatabase.TestModel<TreeNode> tree;

    @Before
    public void setUp() throws Exception {
        OrmTestState.reset();
        db = AndroidTestDatabase.create(ApplicationProvider.getApplicationContext(), 1, Entities.ALL);
        parents = db.model(Parent.class);
        children = db.model(Child.class);
        tree = db.model(TreeNode.class);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static Child child(String label) {
        Child c = new Child();
        c.label = label;
        return c;
    }

    private static Parent parent(String name, String... labels) {
        Parent p = new Parent();
        p.name = name;
        p.children = new ArrayList<>();
        for (String label : labels)
            p.children.add(child(label));
        return p;
    }

    private void seedTwoFamilies() throws Exception {
        db.execute("INSERT INTO Parent (id, name) VALUES (1, 'p1'), (2, 'p2')");
        db.execute("INSERT INTO Child (parentId, label) VALUES (1, 'a'), (1, 'b'), (2, 'c')");
    }

    // ---- insert / update related ---------------------------------------------------------------------------------

    @Test
    public void insert_savesChildrenWithTheGeneratedParentKey() throws Exception {
        Parent p = parent("p", "a", "b");

        long id = parents.insert(p);

        assertEquals(1, id);
        assertEquals(Arrays.asList(1L, 1L), column("SELECT parentId FROM Child ORDER BY id"));
        assertEquals(Arrays.asList("a", "b"), column("SELECT label FROM Child ORDER BY id"));
    }

    @Test
    public void insert_childrenReceiveTheirOwnGeneratedKeys() throws Exception {
        Parent p = parent("p", "a", "b");
        parents.insert(p);
        assertEquals(1, p.children.get(0).id);
        assertEquals(2, p.children.get(1).id);
        assertEquals(1, p.children.get(0).parentId);
    }

    @Test
    public void insert_withoutInsertRelatedModeLeavesTheChildrenAlone() throws Exception {
        parents.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);
        parents.insert(parent("p", "a"));
        assertEquals(1, db.count("Parent"));
        assertEquals(0, db.count("Child"));
    }

    @Test
    public void insert_parentWithoutChildrenReferenceIsFine() throws Exception {
        Parent p = new Parent();
        p.name = "lonely";
        assertEquals(1, parents.insert(p));
        assertEquals(0, db.count("Child"));
    }

    @Test
    public void update_savesNewAndChangedChildren() throws Exception {
        Parent p = parent("p", "a");
        parents.insert(p);
        p.id = 1; // insert() returns the key, it does not write it back (insertOrUpdate does)
        p.children.get(0).label = "a2";
        p.children.add(child("b"));

        parents.update(p);

        assertEquals(Arrays.asList("a2", "b"), column("SELECT label FROM Child ORDER BY id"));
        assertEquals(Arrays.asList(1L, 1L), column("SELECT parentId FROM Child ORDER BY id"));
    }

    @Test
    public void update_withoutUpdateRelatedModeIgnoresTheChildren() throws Exception {
        Parent p = parent("p", "a");
        parents.insert(p);
        p.children.get(0).label = "changed";
        parents.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);

        parents.update(p);

        assertEquals("a", db.scalar("SELECT label FROM Child"));
    }

    // ---- retrieve related ----------------------------------------------------------------------------------------

    @Test
    public void getFirst_parentLoadsItsChildren() throws Exception {
        seedTwoFamilies();
        Parent p = parents.getFirst("id = ?", 1);
        assertNotNull(p.children);
        assertEquals(Arrays.asList("a", "b"), labels(p.children));
    }

    @Test
    public void getFirst_parentWithoutChildrenGetsAnEmptyList() throws Exception {
        db.execute("INSERT INTO Parent (id, name) VALUES (5, 'empty')");
        Parent p = parents.getFirst("id = ?", 5);
        assertNotNull(p.children);
        assertTrue(p.children.isEmpty());
    }

    @Test
    public void getFirst_childLoadsItsParent() throws Exception {
        seedTwoFamilies();
        Child c = children.getFirst("label = ?", "c");
        assertNotNull(c.parent);
        assertEquals(2, c.parent.id);
        assertEquals("p2", c.parent.name);
    }

    @Test
    public void retrieval_canBeSwitchedOff() throws Exception {
        seedTwoFamilies();
        parents.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);
        children.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);
        assertNull(parents.getFirst("id = ?", 1).children);
        assertNull(children.getFirst("label = ?", "a").parent);
    }

    @Test
    public void getAll_eachChildPointsToItsOwnParent() throws Exception {
        seedTwoFamilies();
        List<Child> all = children.getAll(null, null, "id ASC", null);
        assertEquals(3, all.size());
        for (Child c : all) {
            assertNotNull(c.parent);
            assertEquals("child " + c.label + " must reference parent " + c.parentId, c.parentId, c.parent.id);
        }
    }

    // ---- cascade delete ------------------------------------------------------------------------------------------

    @Test
    public void cascadeDelete_removesDependentRowsOfTheDeletedParentOnly() throws Exception {
        seedTwoFamilies();

        int deleted = parents.deleteAll("id = ?", 1);

        assertEquals("1 parent + 2 children", 3, deleted);
        assertEquals(Collections.singletonList("c"), column("SELECT label FROM Child"));
        assertEquals(Collections.singletonList(2L), column("SELECT id FROM Parent"));
    }

    @Test
    public void cascadeDelete_viaDeleteOfAnObject() throws Exception {
        seedTwoFamilies();
        Parent p = new Parent();
        p.id = 2;
        assertEquals(2, parents.delete(p));
        assertEquals(2, db.count("Child"));
    }

    @Test
    public void cascadeDelete_deletingAChildLeavesTheParent() throws Exception {
        seedTwoFamilies();
        assertEquals(1, children.deleteAll("label = ?", "a"));
        assertEquals(2, db.count("Parent"));
        assertEquals(2, db.count("Child"));
    }

    @Test
    public void cascadeDelete_canBeDisabled() throws Exception {
        seedTwoFamilies();
        parents.setRelationshipManagementMode(ObjectModel.RELATIONSHIP_MANAGEMENT_MANUAL);
        assertEquals(1, parents.deleteAll("id = ?", 1));
        assertEquals("orphans stay when cascade is off", 3, db.count("Child"));
    }

    @Test
    public void cascadeDelete_noMatchingParentDeletesNothing() throws Exception {
        seedTwoFamilies();
        assertEquals(0, parents.deleteAll("id = ?", 99));
        assertEquals(3, db.count("Child"));
    }

    @Test
    public void cascadeDelete_deleteAllRemovesEveryChild() throws Exception {
        seedTwoFamilies();
        assertEquals(5, parents.deleteAll());
        assertEquals(0, db.count("Child"));
    }

    // ---- self referencing hierarchy ------------------------------------------------------------------------------

    private void seedTree() throws Exception {
        //        1
        //      /   \
        //     2     3        7 (separate root)
        //    / \
        //   4   5   6 -> child of 3
        db.execute("INSERT INTO TreeNode (id, label, parentId) VALUES (1, 'root', NULL)");
        db.execute("INSERT INTO TreeNode (id, label, parentId) VALUES (2, 'a', 1), (3, 'b', 1), (4, 'a1', 2), (5, 'a2', 2), (6, 'b1', 3)");
        db.execute("INSERT INTO TreeNode (id, label, parentId) VALUES (7, 'other root', NULL), (8, 'other child', 7)");
    }

    @Test
    public void tree_selfReferenceIsStoredAsNullForRootsAndReadBack() throws Exception {
        seedTree();
        assertNull(tree.getFirst("id = ?", 1).parentId);
        assertEquals(Long.valueOf(2), tree.getFirst("id = ?", 4).parentId);
    }

    @Test(timeout = 20000)
    public void tree_deletingARootDeletesTheWholeSubtreeOnly() throws Exception {
        seedTree();

        int deleted = tree.deleteAll("id = ?", 1);

        assertEquals("root + 2 children + 3 grandchildren", 6, deleted);
        assertEquals(Arrays.asList(7L, 8L), column("SELECT id FROM TreeNode ORDER BY id"));
    }

    @Test(timeout = 20000)
    public void tree_deletingAnInnerNodeKeepsItsAncestorsAndSiblings() throws Exception {
        seedTree();
        assertEquals(3, tree.deleteAll("id = ?", 2));
        assertEquals(Arrays.asList(1L, 3L, 6L, 7L, 8L), column("SELECT id FROM TreeNode ORDER BY id"));
    }

    @Test(timeout = 20000)
    public void tree_deletingALeafDeletesOnlyThatRow() throws Exception {
        seedTree();
        assertEquals(1, tree.deleteAll("id = ?", 4));
        assertEquals(7, db.count("TreeNode"));
    }

    @Test(timeout = 20000)
    public void tree_aNodeThatIsItsOwnParentDoesNotLoopForever() throws Exception {
        db.execute("INSERT INTO TreeNode (id, label, parentId) VALUES (9, 'self', 9), (10, 'kid', 9)");
        assertEquals(2, tree.deleteAll("id = ?", 9));
        assertEquals(0, db.count("TreeNode"));
    }

    @Test(timeout = 20000)
    public void tree_aTwoNodeCycleTerminates() throws Exception {
        db.execute("INSERT INTO TreeNode (id, label, parentId) VALUES (11, 'x', 12), (12, 'y', 11)");
        assertEquals(2, tree.deleteAll("id = ?", 11));
        assertEquals(0, db.count("TreeNode"));
    }

    @Test(timeout = 20000)
    public void tree_deleteAllRemovesEveryNode() throws Exception {
        seedTree();
        tree.deleteAll();
        assertEquals(0, db.count("TreeNode"));
    }

    @Test
    public void cascadeDelete_textKey() throws Exception {
        reopenWith(Entities.TEXT_KEYED);
        AndroidTestDatabase textDb = db;
        textDb.execute("INSERT INTO Folder (path) VALUES ('/a'), ('/b')");
        textDb.execute("INSERT INTO Doc (folderPath) VALUES ('/a'), ('/a'), ('/b')");
        assertEquals(3, textDb.model(Entities.Folder.class).deleteAll("path = ?", "/a"));
        assertEquals(1L, textDb.count("Doc"));
    }

    @Test
    public void cascadeDelete_textKeyWithApostrophe() throws Exception {
        reopenWith(Entities.TEXT_KEYED);
        AndroidTestDatabase textDb = db;
        textDb.execute("INSERT INTO Folder (path) VALUES ('O''Brien')");
        textDb.execute("INSERT INTO Doc (folderPath) VALUES ('O''Brien')");
        assertEquals(2, textDb.model(Entities.Folder.class).deleteAll("path = ?", "O'Brien"));
        assertEquals(0L, textDb.count("Doc"));
    }

    /** Replaces the database created in setUp with a fresh one holding a different set of entities. */
    private void reopenWith(AndroidTestDatabase.Registrar registrar) throws Exception {
        db.close();
        OrmTestState.reset();
        db = AndroidTestDatabase.create(ApplicationProvider.getApplicationContext(), 1, registrar);
    }

    private List<String> labels(List<Child> list) {
        List<String> out = new ArrayList<>();
        for (Child c : list)
            out.add(c.label);
        return out;
    }

    private List<Object> column(String sql) throws Exception {
        List<Object> out = new ArrayList<>();
        for (java.util.Map<String, Object> row : db.rows(sql))
            out.add(row.values().iterator().next());
        return out;
    }
}

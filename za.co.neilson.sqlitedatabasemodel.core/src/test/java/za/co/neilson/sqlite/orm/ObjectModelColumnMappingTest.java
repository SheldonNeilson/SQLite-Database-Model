package za.co.neilson.sqlite.orm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.Nullable;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;
import za.co.neilson.sqlite.orm.annotations.Transient;
import za.co.neilson.sqlite.orm.annotations.Unique;

/** Reflection mapping of an entity class to {@link ObjectModelColumn}s, including relationships. */
public class ObjectModelColumnMappingTest {

    enum Color {RED, GREEN}

    static class Everything {
        @PrimaryKey(autoIncrement = true)
        long id;
        String name;
        @Nullable(false)
        String code;
        @Unique
        String sku;
        @Transient
        String scratch;
        static String STATIC_FIELD;
        final String finalField = "x";
        transient String javaTransient;
        Object unsupported;
        List<String> unsupportedList;
        int count;
        Integer boxedCount;
        Color color;
        Date when;
        Calendar calendar;
        byte[] blob;
    }

    static class KeyIgnoresNullable {
        @PrimaryKey
        @Nullable(true)
        String key;
    }

    static class CompositeKey {
        @PrimaryKey
        int a;
        @PrimaryKey
        String b;
        String payload;
    }

    static class NothingMappable {
        static int counter;
        transient int skipped;
        @Transient
        int skippedToo;
    }

    static class MapParent {
        @PrimaryKey(autoIncrement = true)
        long id;
        String name;
        List<MapChild> children;
    }

    static class MapChild {
        @ForeignKey(table = "MapParent", column = "id", childReference = "parent", parentReference = "children")
        long parentId;
        @PrimaryKey(autoIncrement = true)
        long id;
        String note;
        MapParent parent;
    }

    static class OneToOneChild {
        @PrimaryKey
        @ForeignKey(table = "MapParent", column = "id")
        long id;
        String detail;
    }

    static class SecondaryKeyParent {
        @PrimaryKey
        long id;
        String externalCode;
    }

    static class SecondaryKeyChild {
        @PrimaryKey(autoIncrement = true)
        long id;
        @ForeignKey(table = "SecondaryKeyParent", column = "externalCode")
        String parentCode;
    }

    static class TreeNode {
        @PrimaryKey(autoIncrement = true)
        long id;
        @ForeignKey(table = "TreeNode", column = "id")
        Long parentId;
    }

    static class Orphan {
        @PrimaryKey
        long id;
        @ForeignKey(table = "DoesNotExist", column = "id")
        long missingId;
    }

    static class BadParentColumn {
        @PrimaryKey
        long id;
        @ForeignKey(table = "MapParent", column = "nope")
        long parentId;
    }

    static class BadChildReference {
        @PrimaryKey
        long id;
        @ForeignKey(table = "MapParent", column = "id", childReference = "nope")
        long parentId;
    }

    private FakeDriver driver;

    @Before
    public void setUp() {
        OrmTestState.reset();
        driver = new FakeDriver();
    }

    private <T> FakeObjectModel<T> modelOf(final Class<T> type) throws Exception {
        final Object[] holder = new Object[1];
        FakeDatabaseModel.createExisting(driver, 1, (db, models) -> {
            FakeObjectModel<?> m;
            if (type == Everything.class) m = new FakeObjectModel<Everything>(db) {};
            else if (type == KeyIgnoresNullable.class) m = new FakeObjectModel<KeyIgnoresNullable>(db) {};
            else if (type == CompositeKey.class) m = new FakeObjectModel<CompositeKey>(db) {};
            else if (type == NothingMappable.class) m = new FakeObjectModel<NothingMappable>(db) {};
            else throw new IllegalArgumentException(type.getName());
            models.put(type, m);
            holder[0] = m;
        });
        @SuppressWarnings("unchecked") FakeObjectModel<T> model = (FakeObjectModel<T>) holder[0];
        return model;
    }

    private static List<String> names(Collection<ObjectModelColumn> columns) {
        List<String> names = new ArrayList<>();
        for (ObjectModelColumn c : columns)
            names.add(c.getName());
        return names;
    }

    @Test
    public void onlyPersistableFieldsBecomeColumns_inDeclarationOrder() throws Exception {
        FakeObjectModel<Everything> model = modelOf(Everything.class);
        assertEquals(
                Arrays.asList("id", "name", "code", "sku", "count", "boxedCount", "color", "when", "calendar", "blob"),
                names(model.getObjectModelColumns()));
        assertEquals("columnNames array mirrors the column order",
                Arrays.asList("id", "name", "code", "sku", "count", "boxedCount", "color", "when", "calendar", "blob"),
                Arrays.asList(ObjectModel.columnNames.get(Everything.class)));
    }

    @Test
    public void tableNameIsTheSimpleClassName() throws Exception {
        assertEquals("Everything", modelOf(Everything.class).table());
    }

    @Test
    public void columnTypesAreTheFieldTypes() throws Exception {
        FakeObjectModel<Everything> model = modelOf(Everything.class);
        assertEquals(long.class, model.getObjectModelColumn("id").getType());
        assertEquals(Integer.class, model.getObjectModelColumn("boxedCount").getType());
        assertEquals(Color.class, model.getObjectModelColumn("color").getType());
        assertEquals(byte[].class, model.getObjectModelColumn("blob").getType());
    }

    @Test
    public void annotationsDriveColumnFlags() throws Exception {
        FakeObjectModel<Everything> model = modelOf(Everything.class);

        ObjectModelColumn id = model.getObjectModelColumn("id");
        assertTrue(id.isPrimaryKey());
        assertTrue(id.isAutoIncrement());
        assertFalse(id.isNullable());

        ObjectModelColumn name = model.getObjectModelColumn("name");
        assertTrue("columns are nullable unless declared otherwise", name.isNullable());
        assertFalse(name.isPrimaryKey());
        assertFalse(name.isUnique());

        assertFalse(model.getObjectModelColumn("code").isNullable());
        assertTrue(model.getObjectModelColumn("sku").isUnique());
    }

    @Test
    public void primaryKeyWithoutAutoIncrementIsNotAutoIncrement_andNullableAnnotationIsIgnored() throws Exception {
        FakeObjectModel<KeyIgnoresNullable> model = modelOf(KeyIgnoresNullable.class);
        ObjectModelColumn key = model.getObjectModelColumn("key");
        assertTrue(key.isPrimaryKey());
        assertFalse(key.isAutoIncrement());
        assertFalse("@Nullable must not make a primary key nullable", key.isNullable());
    }

    @Test
    public void compositePrimaryKeysAreCollectedInDeclarationOrder() throws Exception {
        modelOf(CompositeKey.class);
        List<ObjectModelColumn> keys = ObjectModel.primaryKeys.get(CompositeKey.class);
        assertEquals(Arrays.asList("a", "b"), names(keys));
    }

    @Test
    public void entityWithoutPrimaryKeyHasEmptyKeyList() throws Exception {
        modelOf(Everything.class);
        modelOf(NothingMappable.class);
        assertTrue(ObjectModel.primaryKeys.get(NothingMappable.class).isEmpty());
        assertTrue(modelOf(NothingMappable.class).getObjectModelColumns().isEmpty());
    }

    @Test
    public void getObjectModelColumn_unknownNameIsNull() throws Exception {
        assertNull(modelOf(Everything.class).getObjectModelColumn("scratch"));
        assertNull(modelOf(Everything.class).getObjectModelColumn("nope"));
    }

    @Test
    public void getField_exposesEveryDeclaredFieldEvenUnmapped() throws Exception {
        FakeObjectModel<Everything> model = modelOf(Everything.class);
        assertNotNull(model.getField("scratch"));
        assertEquals(String.class, model.getField("name").getType());
        assertNull(model.getField("nope"));
    }

    @Test
    public void getObjectType_isTheEntityClass() throws Exception {
        assertEquals(Everything.class, modelOf(Everything.class).getObjectType());
    }

    // ---- relationships -------------------------------------------------------------------------------------------

    private FakeDatabaseModel parentAndChild() throws Exception {
        return FakeDatabaseModel.createExisting(driver, 1, (db, models) -> {
            models.put(MapParent.class, new FakeObjectModel<MapParent>(db) {});
            models.put(MapChild.class, new FakeObjectModel<MapChild>(db) {});
        });
    }

    @Test
    public void foreignKeyColumnsAreOrderedAfterOtherColumns() throws Exception {
        FakeDatabaseModel db = parentAndChild();
        assertEquals(Arrays.asList("id", "note", "parentId"), names(db.getObjectModel(MapChild.class).getObjectModelColumns()));
    }

    @Test
    public void foreignKeyColumnRecordsTheParentTableAndColumn() throws Exception {
        ObjectModelColumn fk = parentAndChild().getObjectModel(MapChild.class).getObjectModelColumn("parentId");
        assertEquals("MapParent", fk.getForeignKeyParentTable());
        assertEquals("id", fk.getForeignKeyParentColumn());
    }

    @Test
    public void foreignKeyToParentPrimaryKeyIsOneToMany_andLinksBothColumns() throws Exception {
        FakeDatabaseModel db = parentAndChild();
        ObjectModelColumn fk = db.getObjectModel(MapChild.class).getObjectModelColumn("parentId");
        ObjectModelColumn parentKey = db.getObjectModel(MapParent.class).getObjectModelColumn("id");

        assertEquals(1, fk.getRelationships().size());
        Relationship r = fk.getRelationships().iterator().next();
        assertSame("same relationship instance is reachable from the parent key column", r, parentKey.getRelationships().iterator().next());

        assertEquals(Relationship.Type.ONE_TO_MANY, r.getRelationshipType());
        assertEquals(MapParent.class, r.getParentType());
        assertEquals(MapChild.class, r.getChildType());
        assertEquals("id", r.getParentKeyField().getName());
        assertEquals("parentId", r.getChildKeyField().getName());
        assertEquals("parent", r.getChildReferenceField().getName());
        assertEquals("children", r.getParentReferenceField().getName());
    }

    @Test
    public void foreignKeyThatIsAlsoPrimaryKeyIsOneToOne() throws Exception {
        FakeDatabaseModel db = FakeDatabaseModel.createExisting(driver, 1, (d, models) -> {
            models.put(MapParent.class, new FakeObjectModel<MapParent>(d) {});
            models.put(OneToOneChild.class, new FakeObjectModel<OneToOneChild>(d) {});
        });
        Relationship r = db.getObjectModel(OneToOneChild.class).getObjectModelColumn("id").getRelationships().iterator().next();
        assertEquals(Relationship.Type.ONE_TO_ONE, r.getRelationshipType());
        assertNull("reference fields are optional", r.getChildReferenceField());
        assertNull(r.getParentReferenceField());
    }

    @Test
    public void foreignKeyToNonKeyColumnIsManyToMany() throws Exception {
        FakeDatabaseModel db = FakeDatabaseModel.createExisting(driver, 1, (d, models) -> {
            models.put(SecondaryKeyParent.class, new FakeObjectModel<SecondaryKeyParent>(d) {});
            models.put(SecondaryKeyChild.class, new FakeObjectModel<SecondaryKeyChild>(d) {});
        });
        Relationship r = db.getObjectModel(SecondaryKeyChild.class).getObjectModelColumn("parentCode").getRelationships().iterator().next();
        assertEquals(Relationship.Type.MANY_TO_MANY, r.getRelationshipType());
    }

    @Test
    public void selfReferencingForeignKeyRelatesTheTypeToItself() throws Exception {
        FakeDatabaseModel db = FakeDatabaseModel.createExisting(driver, 1, (d, models) ->
                models.put(TreeNode.class, new FakeObjectModel<TreeNode>(d) {}));
        ObjectModel<TreeNode, FakeRow, java.util.Map<String, Object>> model = db.getObjectModel(TreeNode.class);
        Relationship r = model.getObjectModelColumn("parentId").getRelationships().iterator().next();
        assertEquals(TreeNode.class, r.getParentType());
        assertEquals(TreeNode.class, r.getChildType());
        assertEquals(Relationship.Type.ONE_TO_MANY, r.getRelationshipType());
        assertEquals(Arrays.asList("id", "parentId"), names(model.getObjectModelColumns()));
    }

    @Test
    public void foreignKeyToModelRegisteredLaterIsRejectedWithGuidance() throws Exception {
        try {
            FakeDatabaseModel.createExisting(driver, 1, (d, models) -> {
                models.put(MapChild.class, new FakeObjectModel<MapChild>(d) {});
                models.put(MapParent.class, new FakeObjectModel<MapParent>(d) {});
            });
            fail("expected ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("MapParent"));
            assertTrue(e.getMessage(), e.getMessage().contains("added to the DatabaseModel first"));
        }
    }

    @Test(expected = ClassNotFoundException.class)
    public void foreignKeyToUnknownTableIsRejected() throws Exception {
        FakeDatabaseModel.createExisting(driver, 1, (d, models) -> models.put(Orphan.class, new FakeObjectModel<Orphan>(d) {}));
    }

    @Test(expected = NoSuchFieldException.class)
    public void foreignKeyToUnknownParentColumnIsRejected() throws Exception {
        FakeDatabaseModel.createExisting(driver, 1, (d, models) -> {
            models.put(MapParent.class, new FakeObjectModel<MapParent>(d) {});
            models.put(BadParentColumn.class, new FakeObjectModel<BadParentColumn>(d) {});
        });
    }

    @Test(expected = NoSuchFieldException.class)
    public void foreignKeyWithUnknownReferenceFieldIsRejected() throws Exception {
        FakeDatabaseModel.createExisting(driver, 1, (d, models) -> {
            models.put(MapParent.class, new FakeObjectModel<MapParent>(d) {});
            models.put(BadChildReference.class, new FakeObjectModel<BadChildReference>(d) {});
        });
    }
}

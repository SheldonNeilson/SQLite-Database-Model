package za.co.neilson.sqlite.orm.annotations;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

/**
 * The ORM discovers everything by reflection at run time, so every annotation must be retained at RUNTIME and be
 * applicable to fields, and the documented defaults must hold.
 */
public class AnnotationsTest {

    @SuppressWarnings("unused")
    static class Sample {
        @PrimaryKey
        long plainKey;
        @PrimaryKey(autoIncrement = true)
        long autoKey;
        @Nullable(false)
        String required;
        @Unique
        String unique;
        @Transient
        String ignored;
        @ReadOnly
        String readOnly;
        @ForeignKey(table = "T", column = "c")
        long fkDefaults;
        @ForeignKey(table = "T", column = "c", childReference = "child", parentReference = "parent", createIndex = false)
        long fkFull;
        String undecorated;
    }

    private static java.lang.reflect.Field field(String name) throws Exception {
        return Sample.class.getDeclaredField(name);
    }

    @Test
    public void everyAnnotationIsRuntimeRetainedAndFieldOnly() {
        Class<?>[] all = {PrimaryKey.class, Nullable.class, Unique.class, Transient.class, ReadOnly.class, ForeignKey.class};
        for (Class<?> a : all) {
            Retention retention = a.getAnnotation(Retention.class);
            assertNotNull(a.getSimpleName(), retention);
            assertEquals(a.getSimpleName(), RetentionPolicy.RUNTIME, retention.value());
            Target target = a.getAnnotation(Target.class);
            assertNotNull(a.getSimpleName(), target);
            assertArrayEquals(a.getSimpleName(), new ElementType[]{ElementType.FIELD}, target.value());
        }
    }

    @Test
    public void primaryKey_autoIncrementDefaultsToFalse() throws Exception {
        assertFalse(field("plainKey").getAnnotation(PrimaryKey.class).autoIncrement());
        assertTrue(field("autoKey").getAnnotation(PrimaryKey.class).autoIncrement());
    }

    @Test
    public void nullable_carriesItsValue() throws Exception {
        assertFalse(field("required").getAnnotation(Nullable.class).value());
    }

    @Test
    public void foreignKey_defaults() throws Exception {
        ForeignKey fk = field("fkDefaults").getAnnotation(ForeignKey.class);
        assertEquals("T", fk.table());
        assertEquals("c", fk.column());
        assertEquals("", fk.childReference());
        assertEquals("", fk.parentReference());
        assertTrue("indexes are created by default", fk.createIndex());
    }

    @Test
    public void foreignKey_explicitValues() throws Exception {
        ForeignKey fk = field("fkFull").getAnnotation(ForeignKey.class);
        assertEquals("child", fk.childReference());
        assertEquals("parent", fk.parentReference());
        assertFalse(fk.createIndex());
    }

    @Test
    public void markerAnnotationsAreDetectedOnlyWhereDeclared() throws Exception {
        assertNotNull(field("unique").getAnnotation(Unique.class));
        assertNotNull(field("ignored").getAnnotation(Transient.class));
        assertNotNull(field("readOnly").getAnnotation(ReadOnly.class));
        assertNull(field("undecorated").getAnnotation(Unique.class));
        assertNull(field("undecorated").getAnnotation(Transient.class));
        assertNull(field("undecorated").getAnnotation(PrimaryKey.class));
    }
}

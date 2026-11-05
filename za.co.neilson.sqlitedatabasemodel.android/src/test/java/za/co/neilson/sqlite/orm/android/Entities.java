package za.co.neilson.sqlite.orm.android;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.InstanceStateTrackable;
import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.Nullable;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;
import za.co.neilson.sqlite.orm.annotations.Unique;

/**
 * Test entities and their Android models. Entities and constructors are public because the ORM instantiates them with
 * {@code Class.newInstance()}.
 */
public final class Entities {

    private Entities() {
    }

    /** Registers every entity below (parents before children). */
    public static final AndroidTestDatabase.Registrar ALL = (d, m) -> {
        m.put(AllTypes.class, new AllTypesModel(d));
        m.put(Person.class, new PersonModel(d));
        m.put(Country.class, new CountryModel(d));
        m.put(Membership.class, new MembershipModel(d));
        m.put(Parent.class, new ParentModel(d));
        m.put(Child.class, new ChildModel(d));
        m.put(TreeNode.class, new TreeNodeModel(d));
        m.put(Tracked.class, new TrackedModel(d));
    };

    public enum Mode {A, B}

    /** An enum whose toString() differs from its name (typical for display labels). */
    public enum Level {
        LOW("Low priority"), HIGH("High priority");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static class Ticket {
        @PrimaryKey(autoIncrement = true)
        public long id;
        public Level level;
    }

    /** Parent with a text primary key, to exercise string keys in cascade deletes. */
    public static class Folder {
        @PrimaryKey
        public String path;
    }

    public static class Doc {
        @PrimaryKey(autoIncrement = true)
        public long id;
        @ForeignKey(table = "Folder", column = "path")
        public String folderPath;
    }

    public static final AndroidTestDatabase.Registrar ENUMS = (d, m) -> m.put(Ticket.class, new TicketModel(d));

    public static final AndroidTestDatabase.Registrar TEXT_KEYED = (d, m) -> {
        m.put(Folder.class, new FolderModel(d));
        m.put(Doc.class, new DocModel(d));
    };

    /** One column of every supported type. */
    public static class AllTypes {
        @PrimaryKey(autoIncrement = true)
        public long id;
        public String text;
        public boolean flag;
        public Boolean boxedFlag;
        public byte tiny;
        public short small;
        public int number;
        public long big;
        public float ratio;
        public double precise;
        public Date date;
        public Calendar calendar;
        public byte[] blob;
        public Mode mode;
        public Integer optionalNumber;
        public Long optionalBig;
        public Double optionalDouble;
    }

    public static class Person {
        @PrimaryKey(autoIncrement = true)
        public long id;
        @Unique
        public String email;
        public String name;
        @Nullable(false)
        public String country;
        public Integer age;
    }

    /** Natural (non auto-increment) text primary key. */
    public static class Country {
        @PrimaryKey
        public String code;
        public String name;
    }

    public static class Membership {
        @PrimaryKey
        public int groupId;
        @PrimaryKey
        public int userId;
        public String role;
    }

    public static class Parent {
        @PrimaryKey(autoIncrement = true)
        public long id;
        public String name;
        public List<Child> children;
    }

    public static class Child {
        @PrimaryKey(autoIncrement = true)
        public long id;
        @ForeignKey(table = "Parent", column = "id", childReference = "parent", parentReference = "children")
        public long parentId;
        public String label;
        public Parent parent;
    }

    /** Self referencing foreign key (hierarchy). */
    public static class TreeNode {
        @PrimaryKey(autoIncrement = true)
        public long id;
        public String label;
        @ForeignKey(table = "TreeNode", column = "id")
        public Long parentId;
    }

    /** Change tracking: only dirty columns are written by update. */
    public static class Tracked implements InstanceStateTrackable {
        @PrimaryKey
        public int id;
        public String a;
        public String b;
        // not persisted
        public transient Set<String> dirty = new HashSet<>();

        @Override
        public boolean isDirty() {
            return !dirty.isEmpty();
        }

        @Override
        public boolean isDirty(String fieldName) {
            return dirty.contains(fieldName);
        }

        @Override
        public void setDirty(String fieldName, boolean isDirty) {
            if (isDirty) dirty.add(fieldName);
            else dirty.remove(fieldName);
        }

        @Override
        public void setClean() {
            dirty.clear();
        }
    }

    public static class TicketModel extends AndroidTestDatabase.TestModel<Ticket> {
        public TicketModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class FolderModel extends AndroidTestDatabase.TestModel<Folder> {
        public FolderModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class DocModel extends AndroidTestDatabase.TestModel<Doc> {
        public DocModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class AllTypesModel extends AndroidTestDatabase.TestModel<AllTypes> {
        public AllTypesModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class PersonModel extends AndroidTestDatabase.TestModel<Person> {
        public PersonModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class CountryModel extends AndroidTestDatabase.TestModel<Country> {
        public CountryModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class MembershipModel extends AndroidTestDatabase.TestModel<Membership> {
        public MembershipModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class ParentModel extends AndroidTestDatabase.TestModel<Parent> {
        public ParentModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class ChildModel extends AndroidTestDatabase.TestModel<Child> {
        public ChildModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class TreeNodeModel extends AndroidTestDatabase.TestModel<TreeNode> {
        public TreeNodeModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class TrackedModel extends AndroidTestDatabase.TestModel<Tracked> {
        public TrackedModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }
}

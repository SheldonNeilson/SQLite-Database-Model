package za.co.neilson.sqlite.orm.jdbc;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import za.co.neilson.sqlite.orm.DatabaseModel;
import za.co.neilson.sqlite.orm.InstanceStateTrackable;
import za.co.neilson.sqlite.orm.annotations.ForeignKey;
import za.co.neilson.sqlite.orm.annotations.Nullable;
import za.co.neilson.sqlite.orm.annotations.PrimaryKey;
import za.co.neilson.sqlite.orm.annotations.Unique;

/**
 * Test entities and their JDBC models. Entities and constructors are public because the ORM instantiates them with
 * {@code Class.newInstance()}.
 */
public final class Entities {

    private Entities() {
    }

    /** Registers every entity below (parents before children). */
    public static final JdbcTestDatabase.Registrar ALL = (d, m) -> {
        m.put(AllTypes.class, new AllTypesModel(d));
        m.put(Person.class, new PersonModel(d));
        m.put(Country.class, new CountryModel(d));
        m.put(Membership.class, new MembershipModel(d));
        m.put(Parent.class, new ParentModel(d));
        m.put(Child.class, new ChildModel(d));
        m.put(Tracked.class, new TrackedModel(d));
    };

    public enum Mode {A, B}

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

    /** Change tracking: only dirty columns are written by update. */
    public static class Tracked implements InstanceStateTrackable {
        @PrimaryKey
        public int id;
        public String a;
        public String b;
        // not persisted
        public transient java.util.Set<String> dirty = new java.util.HashSet<>();

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
            if (isDirty) dirty.add(fieldName); else dirty.remove(fieldName);
        }

        @Override
        public void setClean() {
            dirty.clear();
        }
    }

    public static class TrackedModel extends JdbcTestDatabase.TestModel<Tracked> {
        public TrackedModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class AllTypesModel extends JdbcTestDatabase.TestModel<AllTypes> {
        public AllTypesModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class PersonModel extends JdbcTestDatabase.TestModel<Person> {
        public PersonModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class CountryModel extends JdbcTestDatabase.TestModel<Country> {
        public CountryModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class MembershipModel extends JdbcTestDatabase.TestModel<Membership> {
        public MembershipModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class ParentModel extends JdbcTestDatabase.TestModel<Parent> {
        public ParentModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }

    public static class ChildModel extends JdbcTestDatabase.TestModel<Child> {
        public ChildModel(DatabaseModel<ResultSet, HashMap<String, Object>> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }
}

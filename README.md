# SQLite Database Model

A small, reflection-based Object Relational Mapping (ORM) library for Java and SQLite 3, for **Android** and for plain
**Java (JDBC)**.

Decorate plain Java classes with a few annotations, register them in a `DatabaseModel`, and the library creates the
tables, keys, foreign keys and indexes, upgrades the schema when you bump the version, and gives you typed insert, update,
delete and query methods. No SQL is needed for everyday work, and raw SQL stays available when you want it.

This document is the full documentation. Everything you need is on this page.

- [Modules](#modules)
- [Installation](#installation)
- [Quick start (Android)](#quick-start-android)
- [Entities and annotations](#entities-and-annotations)
- [Supported types](#supported-types)
- [Reading data](#reading-data)
- [Writing data](#writing-data)
- [Relationships](#relationships)
- [Database lifecycle: create, version and upgrade](#database-lifecycle-create-version-and-upgrade)
- [Dirty tracking (`InstanceStateTrackable`)](#dirty-tracking-instancestatetrackable)
- [Using it with plain Java (JDBC)](#using-it-with-plain-java-jdbc)
- [Threading and connections](#threading-and-connections)
- [Testing your code](#testing-your-code)
- [Building and running this repository's tests](#building-and-running-this-repositorys-tests)
- [Known limitations](#known-limitations)
- [Licence](#licence)

## Modules

| Gradle project | Purpose | Depends on |
|---|---|---|
| `:za.co.neilson.sqlitedatabasemodel.core` | The ORM itself: annotations, `DatabaseModel`, `ObjectModel`, `Query`, `Queryable`. Pure Java 8, no Android or JDBC dependency. | nothing |
| `:za.co.neilson.sqlitedatabasemodel.android` | Android driver (`AndroidSqliteDatabaseDriverInterface`, `AndroidObjectModel`) on top of `SQLiteOpenHelper`, plus test fixtures. `minSdk 26`. | core |
| `:za.co.neilson.sqlitedatabasemodel.jdbc` | JDBC driver (`JdbcSqliteDatabaseDriverInterface`, `JdbcObjectModel`) for desktop/server Java. You supply a SQLite JDBC driver such as [Xerial sqlite-jdbc](https://github.com/xerial/sqlite-jdbc). | core |

The core is generic over the driver's result type `R` and write-values type `C`: `DatabaseModel<R, C>` and
`ObjectModel<T, R, C>`. On Android these are `Cursor` and `ContentValues`; on JDBC they are `ResultSet` and
`HashMap<String, Object>`. You normally never touch these generics beyond the class declarations shown below.

## Installation

The library is not published to a Maven repository. Include the modules as Gradle subprojects, either by copying this
repository's three module folders into your project or by adding the repository as a Git submodule.

`settings.gradle`:

```groovy
include ':za.co.neilson.sqlitedatabasemodel.core'
include ':za.co.neilson.sqlitedatabasemodel.android'   // Android apps
include ':za.co.neilson.sqlitedatabasemodel.jdbc'       // desktop / server Java
```

If the module folders live in a subfolder (for example a submodule at `libs/sqlite-database-model`), point each project at
its folder:

```groovy
project(':za.co.neilson.sqlitedatabasemodel.core').projectDir    = file('libs/sqlite-database-model/za.co.neilson.sqlitedatabasemodel.core')
project(':za.co.neilson.sqlitedatabasemodel.android').projectDir = file('libs/sqlite-database-model/za.co.neilson.sqlitedatabasemodel.android')
```

`app/build.gradle` (Android):

```groovy
dependencies {
    implementation project(':za.co.neilson.sqlitedatabasemodel.core')
    implementation project(':za.co.neilson.sqlitedatabasemodel.android')
}
```

For JDBC also add a SQLite JDBC driver, for example `runtimeOnly 'org.xerial:sqlite-jdbc:3.49.1.0'`.

## Quick start (Android)

You need three small pieces: an **entity** (a plain class), an **object model** for it, and a **database model** that
registers the object models.

### 1. The entity

```java
public class Employee {
    @PrimaryKey(autoIncrement = true)
    public long id;

    @Unique
    public String email;

    public String name;
    public String department;
    public double salary;
    public Date hired;
}
```

Rules: the class needs a public no-argument constructor (the ORM creates instances with `newInstance()`), the table is
named after the class's simple name (`Employee`), and every non-static, non-final, non-transient field of a
[supported type](#supported-types) becomes a column. Fields may be public, or private with the ORM reaching them by
reflection.

### 2. The object model

One tiny class per entity. It holds all CRUD behaviour for that entity.

```java
public class EmployeeModel extends AndroidObjectModel<Employee> {
    public EmployeeModel(DatabaseModel<Cursor, ContentValues> databaseModel)
            throws ClassNotFoundException, NoSuchFieldException {
        super(databaseModel);
    }
}
```

### 3. The database model

```java
public class AppDatabase extends DatabaseModel<Cursor, ContentValues> {

    public AppDatabase(Context context) throws SQLException, ClassNotFoundException, NoSuchFieldException {
        super(context);                        // the arguments are passed to onInitializeDatabaseDriverInterface
    }

    @Override
    public String getDatabaseName() { return "app.db"; }       // file name in the app's databases folder

    @Override
    public int getDatabaseVersion() { return 1; }               // bump this when the schema changes

    @Override
    protected DatabaseDriverInterface<Cursor, ContentValues> onInitializeDatabaseDriverInterface(Object... args) {
        return new AndroidSqliteDatabaseDriverInterface((Context) args[0], this);
    }

    @Override
    public ObjectModel<DatabaseInfo, Cursor, ContentValues> onCreateDatabaseInfoModel()
            throws ClassNotFoundException, NoSuchFieldException {
        return new DatabaseInfoModel(this);
    }

    @Override
    protected void onRegisterObjectModels(HashMap<Type, ObjectModel<?, Cursor, ContentValues>> models)
            throws ClassNotFoundException, NoSuchFieldException {
        // Register parents before children (see Relationships).
        models.put(Employee.class, new EmployeeModel(this));
    }

    @Override
    protected void onInsertDefaultValues() throws SQLException {
        // Optional seed data. Called after the tables are created or upgraded.
    }

    /** Bookkeeping table: stores the schema version, created and last accessed dates. */
    static class DatabaseInfoModel extends AndroidObjectModel<DatabaseInfo> {
        DatabaseInfoModel(DatabaseModel<Cursor, ContentValues> db) throws ClassNotFoundException, NoSuchFieldException {
            super(db);
        }
    }
}
```

`getDatabaseName()` and `getDatabaseVersion()` are called from inside the `DatabaseModel` constructor, so they must
return constants and must not depend on fields of your subclass (those are not yet initialised).

Create the database once (for example in `Application.onCreate()`) and keep the instance. The first construction creates
the file and all tables; later constructions check the stored version and upgrade if needed.

### 4. Use it

```java
AppDatabase db = new AppDatabase(context);
ObjectModel<Employee, Cursor, ContentValues> employees = db.getObjectModel(Employee.class);

Employee e = new Employee();
e.email = "jo@example.com";
e.name = "Jo";
e.department = "HR";
e.salary = 32000;
e.hired = new Date();

employees.insert(e);                           // returns the new row id; the autoincrement key is NOT written back
                                               // into `e` (use insertAndReturnUpdated(e) for that)

// Everyone in HR earning at least 30000, highest paid first
List<Employee> hr = employees.getAll("department = ? AND salary >= ?",
        new Object[] { "HR", 30000 }, "salary DESC");

e.salary = e.salary * 1.15;
employees.update(e);                           // matched by primary key

employees.delete(e);
```

## Entities and annotations

All annotations live in `za.co.neilson.sqlite.orm.annotations` and apply to fields.

| Annotation | Parameters | Effect |
|---|---|---|
| `@PrimaryKey` | `autoIncrement` (default `false`) | Marks the primary-key column. Put it on several fields for a composite key. `autoIncrement = true` is for numeric keys and lets SQLite assign the value. A primary key is never nullable. Update and delete use the primary key to find the row. |
| `@Unique` | none | Adds a `UNIQUE` constraint to the column. |
| `@Nullable` | `value` (required) | `@Nullable(false)` makes the column `NOT NULL`. Nullable is the default for non-key columns. |
| `@ForeignKey` | `table`, `column` (both required); `childReference`, `parentReference` (default empty); `createIndex` (default `true`) | Declares a foreign key to `table(column)` and, with the reference fields, a managed [relationship](#relationships). Self-references are supported. |
| `@Transient` | none | The field is not persisted. Java's `transient` and `static` and `final` fields are ignored too. |
| `@ReadOnly` | none | Intended to mark fields that are not persisted but can be filled by custom queries. **The annotation exists but is not implemented yet** (the source marks it `TODO`); use `@Transient` today. |

Example with constraints and a composite key:

```java
public class Membership {
    @PrimaryKey public int groupId;
    @PrimaryKey public int userId;
    public String role;
}

public class Person {
    @PrimaryKey(autoIncrement = true) public long id;
    @Unique                           public String email;
    @Nullable(false)                  public String country;
    public Integer age;               // boxed types can store NULL
    @Transient                        public String cachedDisplayName;
}
```

## Supported types

| Java type | Stored as |
|---|---|
| `String` | `TEXT` |
| `boolean` / `Boolean` | `INTEGER` (0 or 1) |
| `byte`, `short`, `int`, `long` and their boxed types | `INTEGER` |
| `float`, `double` and their boxed types | `FLOAT` / `DOUBLE` columns (REAL affinity) |
| `java.util.Date`, `java.util.Calendar` | `DATETIME` column holding epoch milliseconds |
| `byte[]` | `BLOB` |
| any `enum` | `TEXT`, written with the constant's `toString()` and read back by matching `toString()` first and `name()` second, so enums with display-text `toString()` overrides round-trip correctly |

Boxed types (`Integer`, `Long`, `Double`, `Boolean`, ...) can hold `NULL`; primitives cannot. A field of any other type
is ignored unless it takes part in a [relationship](#relationships). To map a complex type, override
`ObjectModel.onInitializeObjectModelColumns()` and `getColumnValue(...)` / `setColumnValues(...)` in your model to convert
to and from one of the types above.

## Reading data

`T` is your entity type. Where clauses use SQLite syntax with `?` placeholders, so values are always bound as parameters
and never concatenated into SQL.

| Method | Returns |
|---|---|
| `getAll()` | every row |
| `getAll(String where)` | rows matching a where clause |
| `getAll(String where, Object... args)` | rows matching, with bound arguments |
| `getAll(String where, Object[] args, String orderBy)` | ... ordered, for example `"salary DESC"` |
| `getAll(String where, Object[] args, String orderBy, Integer limit)` | ... with a row limit |
| `getAllRaw(String sql, Object... args)` | rows from a full `SELECT` of your own |
| `getFirst(String where)`, `getFirst(String where, Object... args)`, `getFirst(String where, Object[] args, String orderBy)` | the first match, or `null` |
| `getFirstRaw(String sql, Object... args)` | the first row of your own `SELECT`, or `null` |
| `exists(String where)`, `exists(String where, Object... args)` | `boolean` |
| `getCount(String where)`, `getCount(String where, Object... args)` | `long` |

Pass `null` as the where clause to mean "no filter".

```java
Employee jo = employees.getFirst("email = ?", "jo@example.com");
long inHr = employees.getCount("department = ?", "HR");
boolean any = employees.exists("salary > ?", 100000);
List<Employee> top = employees.getAll(null, null, "salary DESC", 10);
```

When [relationship management](#relationships) retrieval is on, related objects are loaded automatically with the parent.

### In-memory filtering with `Queryable`

`za.co.neilson.collections.queryable.Queryable<T>` is an `ArrayList` with `where(...)`, `first(...)` and
`orderBy(...)` helpers for filtering and sorting results already in memory:

```java
Queryable<Employee> all = Queryable.asQueryable(employees.getAll());
Queryable<Employee> rich = all.where(new Queryable.Matcher<Employee>() {
    @Override public boolean isMatch(Employee e) { return e.salary > 50000; }
});
```

### Query helpers

`Query.join(String...)` joins strings into a comma-separated list; `Query.isNumeric(Object)` reports whether a value parses
as a SQL number.

## Writing data

| Method | Behaviour |
|---|---|
| `insert(T)` | inserts one row, returns the new row id |
| `insertAll(Collection<T>)` | inserts many, returns `long[]` of row ids |
| `insertAndReturnUpdated(T)` / `insertAllAndReturnUpdated(Collection<T>)` | inserts, then returns the row(s) read back from the database (generated key and any trigger or default values included) |
| `update(T)` | updates the row with the same primary key, returns the row count (`-1` and no write when a dirty-tracked object has no changes) |
| `update(T, String where, Object... args)` | updates the rows matching the where clause |
| `insertOrUpdate(T)` | updates when the row exists, otherwise inserts |
| `insertOrUpdate(T, String where, Object... args)` | the same, deciding existence with your where clause |
| `insertOrUpdateAll(Collection<T>)` | `insertOrUpdate` for every element, returns the count |
| `setPrimaryKey(T, long rowId)` | writes a generated row id into the entity's key field |
| `delete(Object)` | deletes the row for that object's primary key |
| `deleteAll(Collection<?>)` | deletes each object in the collection |
| `deleteAll()` | deletes every row in the table |
| `deleteAll(String where)` / `deleteAll(String where, Object... args)` | deletes the rows matching the where clause |

`Date` and `Calendar` values, enums and `byte[]` are converted for you. Several writes can be grouped into one SQLite
transaction through the driver (`db.getDatabaseDriverInterface()`): `beginTransaction()`, then
`setTransactionSuccessful()`, then `endTransaction()` in a `finally` block.

## Relationships

Mark the child's key field with `@ForeignKey` and name the two object references so the ORM can wire the objects together:

```java
public class Parent {
    @PrimaryKey(autoIncrement = true) public long id;
    public String name;
    public List<Child> children;                 // filled by the ORM
}

public class Child {
    @PrimaryKey(autoIncrement = true) public long id;

    @ForeignKey(table = "Parent", column = "id",
                childReference = "parent",        // field in Child that points to the parent
                parentReference = "children")     // field in Parent that holds the children
    public long parentId;

    public String label;
    public Parent parent;
}
```

- `table` and `column` name the referenced table and its primary-key column.
- `childReference` is the field on the **child** that references the parent object. `parentReference` is the field on the
  **parent** that references the child (a `List`/collection for one-to-many, or a single object for one-to-one).
- Register the **parent model before the child model** in `onRegisterObjectModels` so the child table can reference it.
- `createIndex = false` skips the index the ORM otherwise creates on the foreign-key column.
- A foreign key to the same table (for example `TreeNode.parentId` to `TreeNode.id`) is supported.

### Relationship management modes

Each object model has a bitmask, `setRelationshipManagementMode(int)`. The default is `RELATIONSHIP_MANAGEMENT_FULL`.

| Constant | Value | Meaning |
|---|---|---|
| `RELATIONSHIP_MANAGEMENT_MANUAL` | 0 | The ORM never touches related objects; you handle them. |
| `RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED` | 1 | Loading a parent also loads its children. |
| `RELATIONSHIP_MANAGEMENT_INSERT_RELATED` | 2 | Inserting a parent also inserts its children. |
| `RELATIONSHIP_MANAGEMENT_UPDATE_RELATED` | 4 | Updating a parent also inserts or updates its children. |
| `RELATIONSHIP_MANAGEMENT_CASCADE_DELETE` | 8 | Deleting a parent also deletes its children. |
| `RELATIONSHIP_MANAGEMENT_FULL` | 15 | All of the above (the default). |

Combine flags with `|`:

```java
parentModel.setRelationshipManagementMode(
        ObjectModel.RELATIONSHIP_MANAGEMENT_RETRIEVE_RELATED | ObjectModel.RELATIONSHIP_MANAGEMENT_CASCADE_DELETE);
```

## Database lifecycle: create, version and upgrade

When you construct a `DatabaseModel`:

1. The driver is created (`onInitializeDatabaseDriverInterface`).
2. Your object models are registered (`onRegisterObjectModels`).
3. If the database file does not exist, `onCreate()` runs: it creates every table, writes the `DatabaseInfo` row (version,
   created date, accessed date) and calls `onInsertDefaultValues()`.
4. If the file exists, the stored version is compared with `getDatabaseVersion()`. If they differ, `onUpgrade(previousVersion)`
   runs.

**The default `onUpgrade` drops and recreates every registered table, which deletes all data.** That is fine for caches
and data you can re-download. If you keep user data, override it and migrate yourself:

```java
@Override
protected synchronized void onUpgrade(int previousVersion) throws SQLException {
    if (previousVersion < 2) {
        getDatabaseDriverInterface().execute("ALTER TABLE Employee ADD COLUMN phone TEXT;");
    }
    // Do not call super.onUpgrade(...) unless you want the tables dropped.
}
```

`rebuild()` drops and recreates all tables on demand and then calls `onInsertDefaultValues()`.

`setJournalMode(DatabaseModel.JournalMode.WAL)` switches SQLite to write-ahead logging (`DEFAULT` is the normal rollback
journal).

## Dirty tracking (`InstanceStateTrackable`)

By default `update(T)` writes every column. Implement `InstanceStateTrackable` on an entity to have the ORM write only the
changed columns, and skip the write entirely when nothing changed (`isDirty()` is `false`). The ORM calls `setClean()`
after every insert and update:

```java
public class Employee implements InstanceStateTrackable {
    // ... fields ...
    private final Set<String> dirty = new HashSet<>();

    public void setSalary(double salary) { this.salary = salary; dirty.add("salary"); }

    @Override public boolean isDirty() { return !dirty.isEmpty(); }
    @Override public boolean isDirty(String fieldName) { return dirty.contains(fieldName); }
    @Override public void setDirty(String fieldName, boolean d) { if (d) dirty.add(fieldName); else dirty.remove(fieldName); }
    @Override public void setClean() { dirty.clear(); }       // called by the ORM after insert and update
}
```

## Using it with plain Java (JDBC)

The JDBC module mirrors the Android one with `JdbcObjectModel` and `JdbcSqliteDatabaseDriverInterface`. The database name
is the path to the SQLite file.

```java
public class AppDatabase extends DatabaseModel<ResultSet, HashMap<String, Object>> {

    public AppDatabase() throws SQLException, ClassNotFoundException, NoSuchFieldException { super(); }

    @Override public String getDatabaseName() { return "app.db"; }
    @Override public int getDatabaseVersion() { return 1; }

    @Override
    protected DatabaseDriverInterface<ResultSet, HashMap<String, Object>> onInitializeDatabaseDriverInterface(Object... args) {
        return new JdbcSqliteDatabaseDriverInterface(this);
    }

    @Override
    public ObjectModel<DatabaseInfo, ResultSet, HashMap<String, Object>> onCreateDatabaseInfoModel()
            throws ClassNotFoundException, NoSuchFieldException {
        return new DatabaseInfoModel(this);
    }

    @Override
    protected void onRegisterObjectModels(HashMap<Type, ObjectModel<?, ResultSet, HashMap<String, Object>>> models)
            throws ClassNotFoundException, NoSuchFieldException {
        models.put(Employee.class, new EmployeeModel(this));
    }

    @Override protected void onInsertDefaultValues() throws SQLException { }
}
```

`EmployeeModel` and `DatabaseInfoModel` extend `JdbcObjectModel<T>` instead of `AndroidObjectModel<T>`, and implement
`onQueryCompleted` as described below. `JdbcObjectModel` leaves one method abstract, so every JDBC object model must implement
`onQueryCompleted(ResultSet)`; close the result set there (`resultSet.close()`, ignoring `SQLException`). The Android model
does this for cursors itself. The test fixture in `za.co.neilson.sqlitedatabasemodel.jdbc/src/test/.../JdbcTestDatabase.java`
shows the pattern. The driver loads
`org.sqlite.JDBC`, so a SQLite JDBC driver must be on the runtime classpath.

## Threading and connections

- `DatabaseModel` methods are synchronised and the driver caches one connection per `DatabaseModel` class.
- Call `connect()` / `disconnect()` on the database model to open and close the connection explicitly; most operations
  connect on demand.
- On Android, do database work off the main thread.

## Testing your code

The Android module publishes JVM test fixtures (`src/testFixtures`) that give you a real SQLite database
under [Robolectric](https://robolectric.org/), so repository code can be tested without a device:

```groovy
dependencies {
    testImplementation testFixtures(project(':za.co.neilson.sqlitedatabasemodel.android'))
}
```

```java
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class EmployeeRepositoryTest {
    private AndroidTestDatabase db;

    @Before public void setUp() throws Exception {
        OrmTestState.reset();           // clears the ORM's static caches between tests
        db = AndroidTestDatabase.create(ApplicationProvider.getApplicationContext(), 1,
                (d, models) -> models.put(Employee.class, new EmployeeTestModel(d)));
    }

    @After public void tearDown() { db.close(); }

    @Test public void insertsARow() throws Exception {
        db.model(Employee.class).insert(new Employee());
        assertEquals(1, db.count("Employee"));
    }
}
```

`AndroidTestDatabase` also offers `execute`, `rows`, `scalar`, `tables` and `columns` helpers for arranging and asserting
directly against SQLite, and `open(...)` to reopen the same file at a different version (upgrade tests). Use
`OrmTestState.reset()` in `@Before` because the ORM caches column metadata in static fields. Enable Robolectric's native
SQLite mode (`sqliteMode=NATIVE` in `src/test/resources/robolectric.properties`) so it behaves like a device.

## Building and running this repository's tests

Requirements: JDK 17 (Gradle 8.7 cannot run on the newest JDKs) and the Android SDK (set `sdk.dir` in
`local.properties`, which is git-ignored).

```powershell
./gradlew unitTests
```

This runs the core, JDBC and Android (Robolectric) JVM tests.

## Known limitations

- The default `onUpgrade` drops all tables; override it to keep data.
- `@ReadOnly` is declared but not implemented.
- Column types are limited to the [supported types](#supported-types); anything else needs a custom model.
- The driver constructors call `System.exit(1)` on an invalid database name or a missing JDBC driver class. Validate your
  configuration before constructing the database model.
- Entity classes need a public no-argument constructor.

## Licence

Apache License 2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE). Copyright 2014 Sheldon Neilson.

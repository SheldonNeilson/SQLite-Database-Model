SQLite-Database-Model
=====================

SQLite Database Model is a super lightweight yet powerful Object Relational Mapping (ORM) Framework for Java and SQLite 3.

 - [Project Website](http://www.neilson.co.za/sqlite-database-model/ "SQLite Database Model Java/SQLite ORM Project")
 - [Getting Started](http://www.neilson.co.za/getting-started-with-sqlite-database-model "Getting Started With SQLite Database Model")
 - [Documentation](http://www.neilson.co.za/javadoc/sqlite-database-model/ "SQLite Database Model Java/SQLite ORM Project Documentation")
 - [Licence](http://www.neilson.co.za/sqlite-database-model/sqlite-database-model-licence/ "SQLite Database Model Licence")

_SQLite Database Model weighs in at under 50KB** yet offers a powerful feature set including…_

## Automatic database generation

Database tables can be created from or mapped to any Java class without the need for inheritance or interface implementation.

By simply decorating your classes properties with a few key annotations and adding them to the model, SQLite Database Model can generate your enitre SQLite 3 database including primary keys, foreign keys relationships, unique constraints and more all without writing a single line of SQL.

## Versatile, reflection powered CRUD methods

Creating, querying, updating and deleting records in your database can be done with just a few simple lines of code via the SQLite Database Model API.

For example, to query a database table containing Employee data for all employees that work in HR and earn a salary greater than or equal to 30000 is as simple as:
```java
    List<Employee> employees = db.getObjectModel(Employee.class)
        .getAll("department = ? AND salary >= ?", Department.HR, 30000);
```
To update an employee’s salary with a 15% increase, all that we’d need to do is update the object property and pass the employee as a parameter to the update(T t) method of the Employee model…
```java
    employee.setSalary(employee.getSalary() * 1.15);
    employeeModel.update(employee);
```

## Relationship management

Foreign key constraints and relationships between objects can be defined by simply marking the appropriate property with the @ForeignKey annotation.

With relationships defined, SQLite Database Model can cascade insert, update & delete operations and automatically retrieve any related objects when the parent object is retrieved from the database.

For example, SQLite Database Model could be configured to automatically fill an employee’s pencils list with the employees pencils from the pencil table when the employee object is inflated as a result of a database query.

Furthermore, adding a new pencil to the employee’s pencils list and then updating the employee would automatically insert the new pencil into the SQLite 3 database’s pencil table and deleting the employee could also automatically delete the employees pencils.

## Comprehensive type support

Not only does SQLite Database Model support a comprehensive list of primitive types out of the box, but with ease of extensibility as a primary design concern, you can easily extend your model to map any complex type/java object to a custom table column and SQLite Database Model will persist and re-construct the complex type for you.

- String
- boolean/Boolean
- byte/Byte/byte[]
- short/Short
- int/Integer
- long/Long
- float/Float
- double/Double
- Date/Calendar
- enum – any enum can be mapped out of the box
- Custom column types – map complex types to a logical primitive value

_**The size of the SQLite Database Model library is under 50KB. However, if you would like to use the library with JDBC you will need to include the [Xerial SQLite JDBC driver](https://bitbucket.org/xerial/sqlite-jdbc "Xerial SQLite JDBC driver") which is considerably larger(~3000KB).
The Android version does not require a driver as it makes use of Android’s internal SQLite APIs._

package za.co.neilson.sqlite.orm;

/**
 * When an object mapped by the ORM implements this interface, the ORM can have finer control over when properties need to be updated in the database, dramatically improving performance.
 *
 * @author <a href="http://www.neilson.co.za">Sheldon Neilson</a>
 * @version 0.1
 * @since 0.1
 */
public interface InstanceStateTrackable {

    /**
     * @return true when the object has at least one member that has been changed and must be updated in the database.
     */
    boolean isDirty();

    /**
     * @param fieldName the name of the field to check
     * @return true when the object's field with the supplied name has been changed and must be updated in the database.
     */
    boolean isDirty(String fieldName);

    /**
     * @param fieldName the name of the field to check
     * @return true when the object's field with the supplied name has been changed and must be updated in the database.
     */
    void setDirty(String fieldName, boolean dirty);

    /**
     * When called this must clear the object's dirty fields. This method is called by the ORM after a database insert / update has taken place.
     */
    void setClean();
}

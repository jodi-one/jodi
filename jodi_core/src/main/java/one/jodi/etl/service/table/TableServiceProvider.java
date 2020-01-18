package one.jodi.etl.service.table;

import java.util.List;
import java.util.Map;

public interface TableServiceProvider {

    /**
     * Set JKM options to a Model
     *
     * @param modelCode
     * @param jkmOptions
     */
    void setJKMOptions(String modelCode, Map<String, Object> jkmOptions);

    /**
     * Set Journalizing Knowledge Module
     *
     * @param modelCode
     * @param jkmName
     */
    void setJKM(String modelCode, String jkmName);

    /**
     * Remove Journalizing for all configured modules.
     */
    void resetJKMs();

    /**
     * Remove the data capture flags from all Data Stores
     */
    void resetCDCDescriptor();

    /**
     * Set the Change Data Capture flag to a DataStore
     *
     * @param dataStoreName TODO
     * @param dataModelCode TODO
     * @param order
     */
    void setCDCDescriptor(String dataStoreName, String dataModelCode, int order);

    void alterTables(List<TableDefaultBehaviors> tablesToChange);

    void alterSCDTables(List<TableDefaultBehaviors> tablesToChange);

    void checkTables();

    void setSubscriber(String dataStoreName, String dataModelCode,
                       List<String> subscribers);

    /**
     * Delete Primary Key / Foreign Key references.
     *
     * @param modelcode
     */
    void deleteReferencesByModel(String modelcode);

}
package one.jodi.core.service;

import one.jodi.base.model.types.DataStore;
import one.jodi.etl.service.table.TableDefaultBehaviors;

import java.util.List;

/**
 * This interface contains the business logic/rules for what needs to be changed.
 */
public interface TableService {

   /**
    * Compilation of a list of tables to be changed in the etl layer based upon
    * the table flags set.
    */
   List<TableDefaultBehaviors> assembleDefaultBehaviors();

   /**
    * Used to gather dataStores.
    *
    * @return List of datastores
    */
   List<DataStore> getDataStores();

}


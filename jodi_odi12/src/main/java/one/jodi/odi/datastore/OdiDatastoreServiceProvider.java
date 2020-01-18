package one.jodi.odi.datastore;

import com.google.inject.Inject;
import one.jodi.etl.service.datastore.DatastoreServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;

@Singleton
public class OdiDatastoreServiceProvider implements DatastoreServiceProvider {
    private final static Logger logger = LogManager.getLogger(OdiDatastoreServiceProvider.class);
    private final OdiInstance odiInstance;

    @Inject
    protected OdiDatastoreServiceProvider(final OdiInstance odiInstance) {
        this.odiInstance = odiInstance;
    }

    @Override
    public boolean deleteDatastore(String name, String modelCode) {
        logger.debug("deleteDatastore:" + name);
        boolean success = true;
        try {
            OdiDataStore odiDatastore =
                    ((IOdiDataStoreFinder) odiInstance
                            .getTransactionalEntityManager().getFinder(OdiDataStore.class))
                            .findByName(name, modelCode);
            if (odiDatastore == null) {
                success = false;
                logger.debug("Datastore: " + name + " not found and not deleted.");
            } else {
                logger.debug("deleting target datastore " + name);
                odiInstance.getTransactionalEntityManager().remove(odiDatastore);
            }
        } catch (Exception ex) {
            logger.debug("Exception encountered: Datastore " + name + " not removed.");
            success = false;
        }

        return success;
    }
}

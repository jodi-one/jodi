package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.service.DatastoreService;
import one.jodi.etl.service.datastore.DatastoreServiceProvider;


/**
 * Implementation of the {@link DatastoreService} interface.
 */
public class DatastoreServiceImpl implements DatastoreService {
    private final DatastoreServiceProvider datastoreServiceProvider;

    /**
     * Creates a new DatastoreServiceImpl instance.
     *
     * @param datastoreServiceProvider
     */
    @Inject
    protected DatastoreServiceImpl(final SchemaMetaDataProvider etlProvider,
                                   final DatastoreServiceProvider datastoreServiceProvider) {
        this.datastoreServiceProvider = datastoreServiceProvider;
    }

    /**
     * @see DatastoreService#deleteDatastore(java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteDatastore(final String odDatastoreName, final String modelCode) {
        datastoreServiceProvider.deleteDatastore(odDatastoreName, modelCode);
    }

}

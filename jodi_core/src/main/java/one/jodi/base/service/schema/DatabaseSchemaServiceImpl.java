package one.jodi.base.service.schema;

import com.google.inject.Inject;
import one.jodi.base.context.Context;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.*;
import one.jodi.base.model.types.impl.DataModelImpl;
import one.jodi.base.model.types.impl.DataStoreForeignReferenceImpl;
import one.jodi.base.model.types.impl.DataStoreImpl;
import one.jodi.base.service.metadata.DataModelDescriptor;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.ForeignReference;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class DatabaseSchemaServiceImpl
        implements DatabaseSchemaService, LazyCreation {

    private final static Logger logger =
            LogManager.getLogger(DatabaseSchemaServiceImpl.class);

    private final static String ERROR_MESSAGE_80900 =
            "Unable to find data store '%1$s' in  model '%2$s'. Check name of" +
                    " the specified data store.";

    private final SchemaMetaDataProvider etlProvider;
    private final Context context;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public DatabaseSchemaServiceImpl(final SchemaMetaDataProvider etlProvider,
                                     final Context context,
                                     final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.etlProvider = etlProvider;
        this.context = context;
        this.errorWarningMessages = errorWarningMessages;
    }

    protected DataStoreType getDataStoreType(final String dataStoreName) {
        return DataStoreType.UNKNOWN;
    }

    protected DataModel createDataModel(final DataModelDescriptor descriptor) {
        String layerName = null;
        boolean ignoredByHeuristics = false;
        return new DataModelImpl(descriptor.getModelCode(),
                descriptor.getDataServerName(),
                descriptor.getPhysicalDataServerName(),
                descriptor.getDataServerTechnology(),
                descriptor.getSchemaName(), descriptor.getModelFlexfields(),
                layerName, ignoredByHeuristics,
                descriptor.getDataBaseServiceName(),
                descriptor.getDataBaseServicePort());
    }

    protected DataModel findOrCreateDataModel(final DataModelDescriptor descriptor) {
        DataModel model = context.getDataModel(descriptor.getModelCode());
        if (model == null) {
            model = createDataModel(descriptor);
        }
        return model;
    }

    private DataStore createDataStore(final DataStoreDescriptor desc) {

        DataStoreType dsType = getDataStoreType(desc.getDataStoreName());
        //logger.info(String.format("createDataStore %s with type %s ", desc.getDataStoreName() , (dsType != null ?  dsType.name() : "null") ));
        DataModel dataModel = findOrCreateDataModel(desc.getDataModelDescriptor());
        return new DataStoreImpl(desc, dsType, dataModel, this);
    }

    protected DataStore findOrCreateDataStore(DataStoreDescriptor desc) {
        DataStore foundDataStore;
        DataStore cachedDataStore = context.getDataStore(desc.getDataStoreName(),
                desc.getDataModelDescriptor()
                        .getModelCode());
        if (cachedDataStore != null) {
            // data store was previously created and can be returned
            foundDataStore = cachedDataStore;
        } else {
            // was not previously cached and needs to be constructed
            foundDataStore = createDataStore(desc);
            context.addDataStore(foundDataStore);
        }
        return foundDataStore;
    }

    @Override
    public Map<String, DataStore> getAllDataStoresInModel(final String modelCode) {
        TreeMap<String, DataStore> allDataStores = new TreeMap<>();
        Map<String, DataStoreDescriptor> dataStoresInModel = etlProvider.getDataStoreDescriptorsInModel(modelCode);
        if (dataStoresInModel == null ||
                dataStoresInModel.values() == null) {
            String msg = "Couldn't retrieve datastores. Verify your connection details.";
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        for (DataStoreDescriptor desc : etlProvider
                .getDataStoreDescriptorsInModel(modelCode).values()) {
            assert (desc != null && desc.getDataStoreName() != null && !desc
                    .getDataStoreName().equals(""));
            // create a unique key <model_code>.<datastore_name>
            allDataStores.put(desc.getDataModelDescriptor().getModelCode()
                            + "." + desc.getDataStoreName(),
                    findOrCreateDataStore(desc));
        }

        return allDataStores;
    }

    //
    //

    public DataStore getDataStoreInModel(final String dataStoreName,
                                         final String modelCode) {
        assert (modelCode != null) : "Model code must be defined in this method";
        DataStore foundDataStore = null;

        Map<String, DataStoreDescriptor> d =
                etlProvider.getDataStoreDescriptorsInModel(modelCode);
        if (d.containsKey(dataStoreName)) {
            foundDataStore = findOrCreateDataStore(d.get(dataStoreName));
        } else {
            String msg = errorWarningMessages.formatMessage(80900,
                    ERROR_MESSAGE_80900, this.getClass(), dataStoreName, modelCode);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            logger.error(msg);
        }

        return foundDataStore;
    }

    public DataStore getSourceDataStoreInModel(final String dataStoreName,
                                               final String modelCode) {
        return getDataStoreInModel(dataStoreName, modelCode);
    }

    @Override
    public List<DataStoreForeignReference> createForeignReferences(
            final List<ForeignReference> foreignRefs,
            final DataStore foreignDataStore) {

        List<DataStoreForeignReference> references = new ArrayList<>();
        for (final ForeignReference fref : foreignRefs) {
            DataStore primaryDataStore =
                    getSourceDataStoreInModel(fref.getPrimaryKeyDataStoreName(),
                            fref.getPrimaryKeyDataStoreModelCode());
            if (primaryDataStore != null) {
                references.add(new DataStoreForeignReferenceImpl(fref, foreignDataStore,
                        primaryDataStore));
            }
        }
        return Collections.unmodifiableList(references);
    }

    //
    //
    //


    @Override
    public void initializeDbConnection(String jdbcUrl, String userName, String password) {
        etlProvider.initDBConnection(jdbcUrl, userName, password);
    }

    @Override
    public void closeDbConnection() {
        etlProvider.closeDBConnection();
    }

}

package one.jodi.odi12.service;

import com.google.inject.Inject;
import one.jodi.base.annotations.Password;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.annotations.MasterPassword;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.Targetcolumn;
import one.jodi.core.model.Transformation;
import one.jodi.etl.service.EtlDataStoreBuildService;
import one.jodi.odi.runtime.OdiConnection;
import one.jodi.odi.runtime.OdiConnectionFactory;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.transaction.ITransactionDefinition;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.model.OdiColumn;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.OdiModel;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import oracle.odi.domain.model.finder.IOdiModelFinder;
import oracle.odi.domain.topology.OdiDataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

//import oracle.odi.domain.model.OdiDataStore;

/**
 * OdiBuild service builds odi datastore from target datastores from
 * Transformations
 */
public class OdiDatastoreBuildService implements EtlDataStoreBuildService {
    private final static Logger logger = LogManager.getLogger(OdiDatastoreBuildService.class);
    public final String ODI_CREATE_MODEL = "odi.create.model";
    private final JodiProperties jodiProperties;
    private final OdiConnection odiConnection;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public OdiDatastoreBuildService(final JodiProperties jodiProperties,
                                    final ErrorWarningMessageJodi errorWarningMessages,
                                    final @MasterPassword String ODI_MASTER_REPO_PWD,
                                    final @Password String ODI_USER_PASSWORD

    ) {
        this.odiConnection = OdiConnectionFactory.getOdiConnection(jodiProperties.getProperty("odi.master.repo.url"),
                jodiProperties.getProperty("odi.master.repo.username"), ODI_MASTER_REPO_PWD,
                jodiProperties.getProperty("odi.login.username"), ODI_USER_PASSWORD,
                jodiProperties.getProperty("odi.repo.db.driver"), jodiProperties.getProperty("odi.work.repo"));
        this.jodiProperties = jodiProperties;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public void build(Transformation t) {
        logger.info("Starting ODI Build.");
        try {
            removeDataStore(t);
        } catch (Exception e) {
            logger.error(e);
        }
        OdiInstance odiInstance = this.odiConnection.getOdiInstance();
        ITransactionDefinition txnDef = new DefaultTransactionDefinition();
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        logger.info("Building transformation: " + t.getName());
        // this.odiInstance;
        OdiModel pModel = findModel();
        OdiDataStore odiDataStore = new OdiDataStore(pModel, t.getMappings().getTargetDataStore());
        odiDataStore.setName(t.getMappings().getTargetDataStore());
        odiDataStore.setDefaultAlias(t.getMappings().getTargetDataStore());
        for (int columPosition = 0; columPosition < t.getMappings().getTargetColumn().size(); columPosition++) {
            Targetcolumn targetColumn = t.getMappings().getTargetColumn().get(columPosition);
            logger.debug("Processing + " + t.getMappings().getTargetDataStore() + "." + targetColumn.getName());
            OdiColumn column = new OdiColumn(odiDataStore, targetColumn.getName());
            column.setDataType(mapFrom(targetColumn.getProperties().getDataType(), pModel));
            column.setScale(targetColumn.getProperties().getScale());
            column.setLength(targetColumn.getProperties().getLength());
            column.setPosition((columPosition + 1));
            column.setDescription(targetColumn.getProperties().getComments());
        }
        odiDataStore.setDescription(t.getMappings().getTargetDataStoreComment());
        // this was for checking FKs connected tot target.
        // for (Targetcolumn tc : t.getMappings().getTargetColumn()) {
        // String comments = tc.getProperties().getComments();
        // if (comments != null &&
        // comments.startsWith(PatternBasedAnnotationKeywords.FK_TO)) {
        // String fkTable =
        // comments.substring(PatternBasedAnnotationKeywords.FK_TO.length(),
        // comments.length());
        // IOdiDataStoreFinder dsFinder =
        // (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
        // .getFinder(OdiDataStore.class);
        // OdiDataStore fkDS =
        // dsFinder.findByName(fkTable,
        // jodiProperties.getProperty("odi.create.model"));
        // assert fkDS != null : "Can't find: " + fkTable;
        //
        // }
        // }
        odiInstance.getTransactionalEntityManager().persist(odiDataStore);
        tm.commit(txnStatus);
    }

    private void deleteOdiDataStores() {
        OdiInstance odiInstance = this.odiConnection.getOdiInstance();
        ITransactionDefinition txnDef = new DefaultTransactionDefinition();
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        IOdiDataStoreFinder finder = ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class));
        Collection<OdiDataStore> dataStores = finder.findAll();
        for (OdiDataStore ds : dataStores) {
            if (ds.getModel().getCode().equals(jodiProperties.getProperty("odi.create.model"))) {
                odiInstance.getTransactionalEntityManager().remove(ds);
            }
        }
        tm.commit(txnStatus);
    }

    private void deleteOldModelArtifacts() {
        deleteOdiDataStores();
    }

    protected OdiModel findModel() {
        IOdiModelFinder finder = (IOdiModelFinder) this.odiConnection.getOdiInstance().getTransactionalEntityManager()
                .getFinder(OdiModel.class);
        return finder.findByCode(jodiProperties.getProperty(ODI_CREATE_MODEL));
    }

    /**
     * @param dataType datatype
     * @param pModel   odiModel
     * @return OdiDataType
     * //TODO This is very basic mapping, should be extended.
     */
    protected OdiDataType mapFrom(String dataType, OdiModel pModel) {
        switch (dataType) {
            case "VARCHAR":
                dataType = "VARCHAR2";
                break;
            case "DOUBLE":
                dataType = "NUMBER";
                break;
            case "DATETIME":
                dataType = "DATE";
                break;
            case "DATE":
                dataType = "DATE";
                break;
            case "NUMBER":
                dataType = "NUMBER";
                break;
            default:
                throw new UnsupportedOperationException("Unsupported datatype: " + dataType);
        }
        return pModel.getLogicalSchema().getTechnology().getDataType(dataType);
    }

    private void removeDataStore(Transformation t) {
        OdiInstance odiInstance = this.odiConnection.getOdiInstance();
        IOdiDataStoreFinder mappingsFinder = (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class);
        OdiDataStore odiDs = mappingsFinder.findByName(t.getMappings().getTargetDataStore(),
                jodiProperties.getProperty(ODI_CREATE_MODEL));
        if (odiDs == null)
            return;
        ITransactionDefinition txnDef = new DefaultTransactionDefinition();
        ITransactionManager tm = odiInstance.getTransactionManager();
        ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        odiInstance.getTransactionalEntityManager().remove(odiDs);
        tm.commit(txnStatus);
    }

}

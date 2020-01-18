//Created by DI Studio

import java.util.Collection;

import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.model.OdiColumn;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;

DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tme = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);

// String procedureName = "DDL Oracle OHICAH CON OHICLA 026";
String modelCode = "ORACLE_OHICAH_CON_OHIPOL";
IOdiDataStoreFinder finder = (IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiDataStore.class);

Collection<OdiDataStore> dss = finder.findByModel(modelCode);
for(OdiDataStore ds : dss){
        for (OdiColumn col : ds.getColumns()) {
                if(col.getDefaultValue() != null &&
                                col.getDefaultValue().length() > 0
                                ){
                        col.setDefaultValue("");
                        tme.persist(col);
                }
        }
}
tm.commit(txnStatus);

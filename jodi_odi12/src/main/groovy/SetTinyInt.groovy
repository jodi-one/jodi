 import java.util.Collection;

import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.model.OdiColumn;
import oracle.odi.domain.model.finder.IOdiColumnFinder;
import oracle.odi.domain.topology.OdiDataType;
import oracle.odi.domain.topology.finder.IOdiDataTypeFinder;
	
DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);
IOdiColumnFinder finder = (IOdiColumnFinder) tem.getFinder(OdiColumn.class);

IOdiDataTypeFinder finderDataType = (IOdiDataTypeFinder) tem.getFinder(OdiDataType.class);

OdiDataType tinyInt = finderDataType.findByTechnology("MYSQL", "TINYINT");

Collection<OdiColumn> collection = finder.findAll();
for (OdiColumn f : collection) {
        if(f.getDataType() == null){
               println "Set tiny int for " +f.getName();
               // f.setDataType(tinyInt);
        }
}
tm.commit(txnStatus);
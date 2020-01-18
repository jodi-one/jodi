import java.util.Collection;

import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.OdiUserProcedure;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder;

DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);
IOdiPackageFinder finder = (IOdiPackageFinder) tem.getFinder(OdiPackage.class);
Collection<OdiPackage> collection = finder.findAll();
for (OdiPackage f : collection) {
        if(f.getName().endsWith("TEST")){
                tem.remove(f);
        }
}
tm.commit(txnStatus);
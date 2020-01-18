import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Collection;

import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.project.OdiPackage;
import oracle.odi.domain.project.OdiUserProcedure;
import oracle.odi.domain.project.OdiVariable;
import oracle.odi.domain.project.finder.IOdiPackageFinder;
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder;
import oracle.odi.domain.project.finder.IOdiVariableFinder;

DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);
IOdiVariableFinder finder = (IOdiVariableFinder) tem
                .getFinder(OdiVariable.class);
Collection<OdiVariable> collection = finder.findAll();
for (OdiVariable f : collection) {
        if (f.getName().endsWith("TEST") || f.getName().startsWith("TEST")) {
                tem.remove(f);
        }
}
tm.commit(txnStatus);
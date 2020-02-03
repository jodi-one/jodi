import oracle.odi.core.persistence.IOdiEntityManager
import oracle.odi.core.persistence.transaction.ITransactionManager
import oracle.odi.core.persistence.transaction.ITransactionStatus
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition
import oracle.odi.domain.project.OdiVariable
import oracle.odi.domain.project.finder.IOdiVariableFinder

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
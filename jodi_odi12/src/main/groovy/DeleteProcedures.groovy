import oracle.odi.core.persistence.IOdiEntityManager
import oracle.odi.core.persistence.transaction.ITransactionManager
import oracle.odi.core.persistence.transaction.ITransactionStatus
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition
import oracle.odi.domain.project.OdiUserProcedure
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder

DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);
IOdiUserProcedureFinder finder = (IOdiUserProcedureFinder) tem.getFinder(OdiUserProcedure.class);
Collection<OdiUserProcedure> collection = finder.findAll();
for (OdiUserProcedure f : collection) {
    if (f.getName().endsWith("TEST")) {
        tem.remove(f);
    }
}
tm.commit(txnStatus);
import oracle.odi.core.persistence.IOdiEntityManager
import oracle.odi.core.persistence.transaction.ITransactionManager
import oracle.odi.core.persistence.transaction.ITransactionStatus
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition
import oracle.odi.domain.runtime.scenario.OdiScenario
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder

DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);
IOdiScenarioFinder finder = (IOdiScenarioFinder) tem.getFinder(OdiScenario.class);
Collection<OdiScenario> scenarios = finder.findAll();
for (OdiScenario f : scenarios) {
    tem.remove(f);
}
tm.commit(txnStatus);	
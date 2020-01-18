import oracle.odi.core.persistence.transaction.ITransactionManager
import oracle.odi.core.persistence.transaction.ITransactionStatus
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition
import oracle.odi.domain.model.OdiDataStore
import oracle.odi.domain.model.finder.IOdiDataStoreFinder

import java.util.regex.Pattern

IOdiDataStoreFinder finder = (IOdiDataStoreFinder) odiInstance
        .getTransactionalEntityManager().getFinder(OdiDataStore.class);
String regex = "(S[0-9]){1,1}";
Pattern pattern = Pattern.compile(regex);
DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);
int counter;
for (OdiDataStore odiDataStore : finder.findAll()) {
   counter++
}
println counter
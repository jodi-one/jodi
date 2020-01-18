import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.mapping.Mapping;
import java.util.regex.Pattern

DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tem = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);
IMappingFinder finder = (IMappingFinder) tem.getFinder(Mapping.class);
for (Mapping f : finder.findAll()) {
    if(f.getName().startsWith("New")){
  		 tem.remove(f);
   }
}
tm.commit(txnStatus);
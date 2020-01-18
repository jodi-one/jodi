//Created by DI Studio

import java.util.Collection;

import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.model.OdiDataStore;
import oracle.odi.domain.model.finder.IOdiDataStoreFinder;
import oracle.odi.domain.project.OdiUserProcedure;
import oracle.odi.domain.project.OdiUserProcedureLine;
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder;

DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
ITransactionManager tm = odiInstance.getTransactionManager();
IOdiEntityManager tme = odiInstance.getTransactionalEntityManager();
ITransactionStatus txnStatus = tm.getTransaction(txnDef);


// String procedureName = "DDL Oracle OHICAH CON OHICLA 026";
String procedureName = "DDL Oracle OHICAH CON OHIPOL 001";
IOdiUserProcedureFinder finder = (IOdiUserProcedureFinder) odiInstance
	.getTransactionalEntityManager().getFinder(OdiUserProcedure.class);

Collection<OdiUserProcedure> procedures = finder.findByName(procedureName);
OdiUserProcedure proc =  procedures.iterator().next();
for (OdiUserProcedureLine line : proc.getLines()) {
      	if (line.getName().startsWith("Create Table")) {
              	line.setIgnoreError(true);
    	}
      tme.persist(proc);
}
tm.commit(txnStatus);

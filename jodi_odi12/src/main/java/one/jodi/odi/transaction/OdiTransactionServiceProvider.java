package one.jodi.odi.transaction;

import com.google.inject.Inject;
import one.jodi.etl.service.transaction.TransactionServiceProvider;
import one.jodi.etl.service.transaction.TransactionWrapper;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.transaction.ITransactionDefinition;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Singleton;

@Singleton
public class OdiTransactionServiceProvider implements TransactionServiceProvider {
    private final OdiInstance odiInstance;
    private final Logger logger = LogManager.getLogger(TransactionServiceProvider.class);
    @Inject
    protected OdiTransactionServiceProvider(OdiInstance odiInstance) {
        this.odiInstance = odiInstance;
    }

    @Override
    public TransactionWrapper beginTransaction() {
        return new OdiTransactionWrapper(odiInstance
                .getTransactionManager()
                .getTransaction(
                        new DefaultTransactionDefinition(
                                new DefaultTransactionDefinition(
                                        ITransactionDefinition.PROPAGATION_REQUIRES_NEW))));
    }

    @Override
    public void commitTransaction(TransactionWrapper tranStatus) {
        odiInstance.getTransactionManager().commit(((OdiTransactionWrapper) tranStatus).transactionStatus);
        //	StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        //	logger.info("commit: "+ stack[7].getClassName() +"."+stack[7].getMethodName());
    }

    @Override
    public TransactionWrapper joinTransaction() {
        return new OdiTransactionWrapper(odiInstance.getTransactionManager().getTransaction(
                new DefaultTransactionDefinition(
                        ITransactionDefinition.PROPAGATION_REQUIRED)));
    }

    @Override
    public void rollbackTransaction(TransactionWrapper tranStatus) {
        odiInstance.getTransactionManager().rollback(((OdiTransactionWrapper) tranStatus).transactionStatus);
    }

    class OdiTransactionWrapper implements TransactionWrapper {
        final ITransactionStatus transactionStatus;

        OdiTransactionWrapper(ITransactionStatus transactionStatus) {
            this.transactionStatus = transactionStatus;
        }

        @Override
        public boolean isCompleted() {
            return transactionStatus.isCompleted();
        }

        @Override
        public boolean isNewTransaction() {
            return transactionStatus.isNewTransaction();
        }

        @Override
        public void commit() {
            OdiTransactionServiceProvider.this.commitTransaction(this);
        }

        @Override
        public void rollback() {
            OdiTransactionServiceProvider.this.rollbackTransaction(this);
        }
    }
}

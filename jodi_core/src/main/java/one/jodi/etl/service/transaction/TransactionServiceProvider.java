package one.jodi.etl.service.transaction;

public interface TransactionServiceProvider {
    TransactionWrapper beginTransaction();

    TransactionWrapper joinTransaction();

    void commitTransaction(TransactionWrapper tranStatus);

    void rollbackTransaction(TransactionWrapper tranStatus);
}

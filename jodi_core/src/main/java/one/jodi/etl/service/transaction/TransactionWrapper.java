package one.jodi.etl.service.transaction;

public interface TransactionWrapper {
    boolean isCompleted();

    boolean isNewTransaction();

    void commit();

    void rollback();
}

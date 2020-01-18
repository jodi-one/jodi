package one.jodi.odi.runtime;

import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.transaction.ITransactionStatus;

public class OdiConnection {

    private final OdiInstance odiInstance;
    private final ITransactionStatus transactionStatus;

    public OdiConnection(final OdiInstance odiInstance, final ITransactionStatus trans) {
        this.odiInstance = odiInstance;
        this.transactionStatus = trans;
    }

    public OdiInstance getOdiInstance() {
        return odiInstance;
    }

    public ITransactionStatus getTransactionStatus() {
        return transactionStatus;
    }
}

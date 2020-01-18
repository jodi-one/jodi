package one.jodi.odi.runtime;

import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.publicapi.samples.SimpleOdiInstanceHandle;

public class OdiConnectionFactory {

    public static OdiConnection getOdiConnection(String odiMasterRepoUrl, String odiMasterUser, String odiMasterRepoPassword,
                                                 String odiLoginUsername, String odiLoginPassword, String odiRepoDbDriver,
                                                 String odiWorkRepo) {
        @SuppressWarnings("deprecation") final SimpleOdiInstanceHandle odiInstanceHandle = SimpleOdiInstanceHandle.create(odiMasterRepoUrl,
                odiRepoDbDriver, odiMasterUser, odiMasterRepoPassword, odiWorkRepo, odiLoginUsername, odiLoginPassword);
        // Allocate an odisinstance of the name
        OdiInstance odiInstance = odiInstanceHandle.getOdiInstance();
        ITransactionStatus trans = odiInstance.getTransactionManager()
                .getTransaction(new DefaultTransactionDefinition());
        return new OdiConnection(odiInstance, trans);
    }
}

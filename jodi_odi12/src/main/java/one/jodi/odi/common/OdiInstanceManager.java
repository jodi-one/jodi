package one.jodi.odi.common;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import one.jodi.base.annotations.Password;
import one.jodi.base.util.Version;
import one.jodi.core.annotations.MasterPassword;
import one.jodi.core.config.JodiProperties;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.config.MasterRepositoryDbInfo;
import oracle.odi.core.config.OdiInstanceConfig;
import oracle.odi.core.config.PoolingAttributes;
import oracle.odi.core.config.WorkRepositoryDbInfo;
import oracle.odi.core.security.Authentication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Creates ODIInstance instance. Is a singleton per Guice injector and offers
 * caching of a fixed number of OdiInsatnces to facilitate reuse. In case more
 * instances were created than being able to cache, the least recently used
 * OdiInstance is evicted from cache and closed.
 *
 */
@Singleton // reminder only - needs to be defined in Guice module since annotation does not work
public class OdiInstanceManager implements Provider<OdiInstance> {

    private final static Logger logger = LogManager.getLogger(OdiInstanceManager.class);
    private final static int MAX_SIZE = 3;
    private final static Map<Integer, OdiInstance> allOdiInstances
            = new DeferredCleanupCache(MAX_SIZE);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                allOdiInstances.values().stream().filter(oi -> !oi.isClosed())
                        .forEach(oi -> {
                            LogManager.getLogger(OdiInstanceManager.class)
                                    .info("Closing ODI instance " + oi);
                            try {
                                oi.close();
                            } catch (Exception e) {
                                LogManager.getLogger(OdiInstanceManager.class)
                                        .info("Exception generated closing ODI instance " +
                                                        oi,
                                                e);
                            }

                        });

            }
        });
    }

    private final JodiProperties properties;
    private final String masterPassword;
    private final String password;
    private OdiInstance odiInstance;


    @Inject
    public OdiInstanceManager(final JodiProperties properties,
                              @MasterPassword final String masterPassword,
                              @Password final String password) {
        this.properties = properties;
        this.masterPassword = masterPassword;
        this.password = password;
    }


    public synchronized OdiInstance get() {
        if (odiInstance == null) {
            odiInstance = init();
        }
        return odiInstance;
    }

    public synchronized void close() {
        if (odiInstance.isClosed()) {
            odiInstance.close();
        }
    }


    public OdiInstance init() {
        logger.info("request for ODI instance");
        Version.init();
        String odiRepoUrl = properties.getProperty(OdiConstants.ODI_MASTER_REPO_URL);
        String odiRepoDbDriver = properties.getProperty(OdiConstants.ODI_REPO_DB_DRIVER);
        String odiMasterUser = properties.getProperty(OdiConstants.ODI_MASTER_REPO_USERNAME);
        String odiMasterPassword = this.masterPassword;
        String odiWorkRepo = properties.getProperty(OdiConstants.ODI_WORK_REPO);
        String odiUser = properties.getProperty(OdiConstants.ODI_LOGIN_USERNAME);
        String odiPassword = this.password;
        int currentHash = getCurrentHash(odiRepoUrl, odiRepoDbDriver, odiMasterUser,
                odiWorkRepo, odiUser);
        OdiInstance odiInst = allOdiInstances.get(currentHash);
        if (odiInst == null) {
            logger.info(String.format("create new ODI Instance for ConnectionUrl: %1$s and OdiMasterUser: %2$s",
                    odiRepoUrl, odiMasterUser));
            MasterRepositoryDbInfo masterInfo = new MasterRepositoryDbInfo(
                    odiRepoUrl, odiRepoDbDriver, odiMasterUser,
                    odiMasterPassword.toCharArray(), new PoolingAttributes());
            WorkRepositoryDbInfo workInfo = new WorkRepositoryDbInfo(
                    odiWorkRepo, new PoolingAttributes());
            odiInst = OdiInstance
                    .createInstance(new OdiInstanceConfig(masterInfo, workInfo));
            logger.debug("created ODI instance " + odiInst);
            Authentication auth = odiInst.getSecurityManager()
                    .createAuthentication(odiUser, odiPassword.toCharArray());
            odiInst.getSecurityManager().setCurrentThreadAuthentication(
                    auth);
            logger.debug("passed ODI authentication.");

        }
        // note that existing entries are re-inserted. This is needed to ensure
        // that the underlying LinkedHashMap is maintaining a access-based
        // sequence, which is needed to supports eviction based on last accessed rule.
        allOdiInstances.put(currentHash, odiInst);
        return odiInst;
    }

    private int getCurrentHash(String odiRepoUrl, String odiRepoDbDriver,
                               String odiMasterUser, String odiWorkRepo, String odiUser) {

        int hash = (odiRepoUrl + odiRepoDbDriver + odiMasterUser + odiWorkRepo + odiUser).hashCode();
        return hash;
    }

}

class DeferredCleanupCache extends LinkedHashMap<Integer, OdiInstance> {
    private static final long serialVersionUID = 1L;
    private final transient Logger logger = LogManager.getLogger(DeferredCleanupCache.class);
    private int maxSize;

    public DeferredCleanupCache(final int maxSize) {
        //define access ordered Hash
        super(maxSize, 0.75f, true);
        this.maxSize = maxSize;
    }

    protected boolean removeEldestEntry(Map.Entry<Integer, OdiInstance> eldest) {
        boolean remove = size() > maxSize;

        if (remove) {
            //close OdiInstande before evicting from Hash, which makes them eligible for GC
            eldest.getValue().close();
            logger.debug(String.format("ODI Instance %1$s closed and ready for garbage collection",
                    eldest.getValue().toString()));
        }

        return remove;
    }
}

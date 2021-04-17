package one.jodi.base.factory;

import com.google.inject.Injector;


/**
 * DOCUMENT ME!
 */
public class ServiceFactory {
    private static ServiceFactory instance;
    private final Injector injector;

    private ServiceFactory(final Injector injector) {
        this.injector = injector;
    }

    public static ServiceFactory getInstance() {
        return instance;
    }

    /**
     * DOCUMENT ME!
     *
     * @param injector DOCUMENT ME!
     */
    public static synchronized void initInstance(final Injector injector) {

        if (instance == null) {
            instance = new ServiceFactory(injector);
        }
    }

    public <T> T getServiceInstance(final Class<T> type) {
        return (T) injector.getInstance(type);
    }
}

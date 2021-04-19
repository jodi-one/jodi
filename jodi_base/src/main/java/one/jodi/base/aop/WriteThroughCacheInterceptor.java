package one.jodi.base.aop;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import one.jodi.base.annotations.Registered;
import one.jodi.base.util.Register;
import one.jodi.base.util.Resource;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a simple write-through cache in an Guice interceptor.
 * <p>
 * The cache can be used on any method that returns an object type. Method
 * signature and its parameters are the key to the result to be cached.
 */
@Singleton
public class WriteThroughCacheInterceptor implements MethodInterceptor, Resource {

    private static final Logger logger = LogManager.getLogger(WriteThroughCacheInterceptor.class);

    @Inject
    @Registered
    private Register registerInstance;
    private boolean registered = false;

    private Map<String, Object> cache = new HashMap<>();
    private int inserts = 0;
    private int hits = 0;

    @Inject
    public WriteThroughCacheInterceptor() {
        super();
    }

    private void register() {
        //register with Register (a.k.a. JodiController) to allow access to flush
        //cache and get statistics hard-coded infrastructure because JodiController
        //is not injectable by Guice
        registerInstance.register(this);
        logger.debug("created write through cache " + this);
    }

    /**
     * intercepts calls to methods
     *
     * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // lazy registration - could not find an alternative way to inject and register in one step.
        if (!registered) {
            register();
            registered = true;
        }

        Object result;

        String method = invocation.getMethod()
                                  .toString();
        assert (!method.contains("void ")) : "Methods with void return type must not be cached : " + method;

        //creates key using method signature and
        StringBuilder sb = new StringBuilder();
        sb.append(method);
        Object[] x = invocation.getArguments();
        for (Object aX : x) {
            sb.append(":");
            sb.append(aX.toString());
        }
        String key = sb.toString();
        if (cache.containsKey(key)) {
            hits++;
            result = cache.get(key);
        } else {
            inserts++;
            result = invocation.proceed();
            cache.put(key, result);
            logger.debug("caching method call " + sb);
        }

        return result;
    }

    @Override
    public void logStatistics() {
        logger.debug(String.format("Cache Statistics --  inserts : %1$s  hits: %2$s  ratio: %3$.1f", inserts, hits,
                                   ((float) hits / inserts)));
    }

    @Override
    public void flush() {
        logger.debug(String.format("Flushing Cache %1$s.", this.toString()));
        inserts = 0;
        hits = 0;
    }

    @Override
    public void clear() {
        cache = new HashMap<>();
    }

    @Override
    protected void finalize() throws Throwable {
        logger.debug(String.format("Finalized Cache %1$s.", this));
        super.finalize();
    }
}

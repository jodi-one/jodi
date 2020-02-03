package one.jodi.base.factory;

import com.google.inject.Module;
import one.jodi.base.bootstrap.RunConfig;

import java.util.List;


/**
 * Interface that allows ETL specific application extensions to define
 * dependency injection {@link Module} instances at runtime.
 */
public interface ModuleProvider {

    /**
     * Returns a collection of {@link Module} instances that define
     * application specific dependency injection configuration. Provided
     * instances may also override core DI configuration.
     *
     * @param config run configuration of the module
     * @return a collection of {@link Module} instances.
     */
    List<? extends Module> getModules(RunConfig config);

    List<? extends Module> getOverrideModules(RunConfig config);
}

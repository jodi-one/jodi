package one.jodi.extensions;

import com.google.inject.Module;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.factory.ModuleProvider;
import one.jodi.odi.factory.OdiModuleProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Class GbuModuleProvider provide a ModuleProvider implementation that
 * adds a GBU specific Guice Module implementation that allows for overriding
 * of core bindings.
 *
 */
public class GbuModuleProvider implements ModuleProvider {

    /*
     * (non-Javadoc)
     *
     * @see
     * one.jodi.core.factory.ModuleProvider#getModules(one.jodi.
     * bootstrap.RunConfig)
     */
    @Override
    public List<? extends Module> getModules(RunConfig config) {
        OdiModuleProvider odiProvider = new OdiModuleProvider();
        List<Module> result = new ArrayList<>(odiProvider.getModules(config));
        result.add(new GbuExtensionModule());

        return Collections.unmodifiableList(result);
    }

    @Override
    public List<? extends Module> getOverrideModules(RunConfig config) {
        return Collections.emptyList();
    }

}

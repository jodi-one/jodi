package one.jodi.extensions;

import com.google.inject.Module;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.factory.ModuleProvider;
import one.jodi.odi.common.OdiVersion;
import one.jodi.odi.factory.OdiModuleProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Class CustomModuleProvider provide a ModuleProvider implementation that
 * adds an  Guice Module implementation that allows for overriding
 * of core bindings for the use with this experimental functionality
 *
 */
public class ToolsModuleProvider implements ModuleProvider {

    private static final Logger logger = LogManager.getLogger(ToolsModuleProvider.class);

    /*
     * (non-Javadoc)
     *
     * @see
     * one.jodi.core.factory.ModuleProvider#getModules(one.jodi.bootstrap.RunConfig)
     */
    @Override
    public List<? extends Module> getModules(RunConfig config) {
        // Tools are not supported for ODI 11g
        if (new OdiVersion().isVersion11()) {
            String msg = "Tools only supports ODI 12.1.3 and higher.";
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }

        OdiModuleProvider odiProvider = new OdiModuleProvider();
        List<Module> modules = new ArrayList<Module>(odiProvider.getModules(config));
        modules.add(new ToolsExtensionModule(config));
        return Collections.unmodifiableList(modules);
    }

    @Override
    public List<? extends Module> getOverrideModules(RunConfig config) {
        return Collections.emptyList();
    }
}


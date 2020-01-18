package one.jodi.odi12.factory;

import com.google.inject.Module;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.factory.ModuleProvider;
import one.jodi.odi.factory.OdiModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Odi12ModuleProvider implements ModuleProvider {

    @Override
    public List<? extends Module> getModules(final RunConfig config) {
        Module core = new OdiModule(config);
        Module od12module = new Odi12Module();
        List<Module> modules = new ArrayList<>();
        modules.add(od12module);
        modules.add(core);
        return Collections.unmodifiableList(modules);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends Module> getOverrideModules(RunConfig config) {
        return (List<? extends Module>) Collections.EMPTY_LIST;
    }

}

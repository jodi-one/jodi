package one.jodi.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import one.jodi.base.bootstrap.BaseCmdlineArgumentProcessor;
import one.jodi.base.bootstrap.JodiControllerBase;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.context.Context;
import one.jodi.base.factory.BaseModule;
import one.jodi.core.factory.CoreETLModule;

import java.util.Collection;

public class JodiController extends JodiControllerBase {

    public JodiController() {
        super();
    }

    public JodiController(boolean testBehavior) {
        super(testBehavior);
    }

    public static void main(String[] args) {
        JodiController controller = new JodiController();
        createAndRun(controller, args);
    }

    @Override
    protected BaseCmdlineArgumentProcessor getCmdlineProcessor() {
        return new EtlCmdlineArgumentProcessor();
    }

    @Override
    protected void customBinding(final Injector injector) {
        // register Context object instance with command line tool to be cleaned up after use
        register(injector.getInstance(Context.class));
    }

    @Override
    protected Injector createInjector(Collection<? extends Module> applicationModules,
                                      boolean isDevMode, RunConfig config) {
        EtlRunConfig etlConfig = (EtlRunConfig) config;
        return Guice.createInjector(
                (isDevMode ? Stage.DEVELOPMENT : Stage.PRODUCTION),
                Modules.override(new BootstrapModule(config)
                        , new BaseModule(config, this)
                        , new CoreETLModule(etlConfig))
                        .with(applicationModules));
    }

}

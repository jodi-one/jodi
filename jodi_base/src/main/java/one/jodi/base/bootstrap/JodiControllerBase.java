package one.jodi.base.bootstrap;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.factory.ModuleProvider;
import one.jodi.base.factory.ServiceFactory;
import one.jodi.base.util.Register;
import one.jodi.base.util.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The bootstrap class that is used to run the Jodi functionality
 * from the command line.
 */
public abstract class JodiControllerBase implements Register {
    private static final Logger LOGGER = LogManager.getLogger(JodiControllerBase.class);

    private static final boolean ENABLE_TEST_BEHAVIOR_DEFAULT = false;
    private static final String ERROR_MESSAGE_80000 =
            "Could not initialize, please check jodi.properties, " + "DB connection and Repository. %s";
    private static final String ERROR_MESSAGE_80010 = "Unable to instantiate ModuleProvider implementation %s";
    private static final String ERROR_MESSAGE_80020 = "Encoding %s is not supported.";
    private final boolean enableTestBehavior;
    private Set<Resource> registered = new HashSet<>();
    private ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();
    private String cachedErrorWarningMessages;

    /**
     * Creates a new JodiController instance.
     */
    public JodiControllerBase() {
        this.enableTestBehavior = ENABLE_TEST_BEHAVIOR_DEFAULT;
    }

    /**
     * Creates a new JodiController instance
     *
     * @param testBehavior throws exceptions up the stack if <code>true</code>; otherwise
     *                     exists with System.exit(code) with code != 0 indicating an
     *                     error or exception
     */
    public JodiControllerBase(final boolean testBehavior) {
        this.enableTestBehavior = testBehavior;
    }

    /**
     * Main entry point
     *
     * @param controller the super class of the controller
     * @param args       Command line arguments
     */
    protected static void createAndRun(final JodiControllerBase controller, final String[] args) {
        int returnCode = controller.run(args);
        System.exit(returnCode);
    }

    @Override
    public void register(final Resource resource) {
        this.registered.add(resource);
    }

    @Override
    public void register(ErrorWarningMessageJodi errorWarningMessage) {
        synchronized (this) {
            this.errorWarningMessages = errorWarningMessage;
        }
    }

    protected abstract void customBinding(final Injector injector);

    /**
     * Uses the RunConfig instance to create the DI injector
     *
     * @param config application configuration
     * @return Initialized DI Injector instance
     */
    public Injector init(final RunConfig config) {
        List<ModuleProvider> modelProviders = new ArrayList<>();
        for (String moduleClassName : config.getModuleClasses()) {
            ModuleProvider mp = createModuleProvider(moduleClassName);
            modelProviders.add(mp);
        }
        return init(modelProviders, config);
    }

    /**
     * Uses the provided ModuleProvider implementation to create the DI Injector
     *
     * @param mps    ModuleProvider instance that constructs the DI Module used to
     *               create the Injector
     * @param config Application configuration
     * @return Initialized DI Injector instance
     */
    private Injector init(final List<ModuleProvider> mps, final RunConfig config) {
        Collection<Module> modules = new ArrayList<>();
        for (ModuleProvider mp : mps) {
            if (mp.getOverrideModules(config) != null && !mp.getOverrideModules(config)
                                                            .isEmpty()) {
                modules.add(Modules.override(mp.getModules(config))
                                   .with(mp.getOverrideModules(config)));
            } else {
                modules.addAll(mp.getModules(config));
            }
        }
        Injector injector = createInjector(modules, config.isDevMode(), config);
        ServiceFactory.initInstance(injector);
        return injector;
    }

    private void cleanup() {
        // always flush and clear resources and make eligible for GC
        for (Resource resource : registered) {
            resource.logStatistics();
            // make conservative assumption about future use of
            // associated cache -> flush cache
            resource.flush();
            resource.clear();
        }
        // unregister resources in instance.
        registered = new HashSet<>();
    }

    private synchronized void cleanCacheAndPrintErrors() {
        cleanup();
        cachedErrorWarningMessages = errorWarningMessages.printMessages();
        errorWarningMessages.clear();
    }

    /**
     * Initialize and execute the Jodi app with the specified parameters
     *
     * @param args     command line arguments array
     * @param fileName optional parameter
     * @return exitCode the return code
     */
    public synchronized int run(String[] args, String... fileName) {
        // remove cached error reports from previous run
        int exitCode;
        String exceptionMessage = "";
        this.cachedErrorWarningMessages = null;

        String[] argsList = Arrays.copyOf(args, args.length + 1);
        if (fileName.length > 0) {
            argsList[argsList.length - 1] = fileName[0];
        } else {
            argsList[argsList.length - 1] = "";
        }
        args = Arrays.copyOf(argsList, argsList.length);
        BaseCmdlineArgumentProcessor config = createRunConfig(args);
        Injector injector;
        try {
            injector = init(config);
        } catch (Exception ex) {
            String message = (ex.getMessage() != null) ? ex.getMessage() : "";
            String msg;
            synchronized (this) {
                msg = errorWarningMessages.formatMessage(80000, ERROR_MESSAGE_80000, this.getClass(), message);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                MESSAGE_TYPE.WARNINGS);
                LOGGER.fatal(msg, ex);
            }
            if (enableTestBehavior()) {
                if (config.isDevMode()) {
                    ex.printStackTrace();
                }
                throw new UnRecoverableException(msg, ex);
            }
            return 2;
        } finally {
            cleanCacheAndPrintErrors();
        }

        ActionRunner handler = getActionRunner(config.getAction(args), injector);
        try {
            handler.validateRunConfig(config);
        } catch (UsageException e) {
            config.usage(e.getMessage(), -1);
            if (enableTestBehavior()) {
                if (config.isDevMode()) {
                    e.printStackTrace();
                }
                throw e;
            }
            return 3;
        } finally {
            cleanCacheAndPrintErrors();
        }

        try {
            // establish context singleton instance to be able to clean
            // context after operation
            customBinding(injector);
            handler.run(config);
            exitCode = errorWarningMessages.getErrorMessages()
                                           .isEmpty() ? 0 : 1;
        } catch (UnRecoverableException u) {
            if (enableTestBehavior()) {
                if (config.isDevMode()) {
                    u.printStackTrace();
                }
                throw u;
            }
            return 4;
        } catch (Exception e) {
            String msg = "[00001] Unexpected exception: " + e.getMessage();
            LOGGER.fatal(msg, e);
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            if (enableTestBehavior()) {
                if (config.isDevMode()) {
                    e.printStackTrace();
                }
                throw new UnRecoverableException(msg, e);
            }
            return 5;
        } finally {
            if (enableTestBehavior() && errorWarningMessages.getErrorMessages()
                                                            .size() > 0) {
                errorWarningMessages.setMetaDataDirectory(config.getMetadataDirectory());
                exceptionMessage = "Number of errors detected :" + errorWarningMessages.getErrorMessages()
                                                                                       .size();
                exceptionMessage += " First error is: " + errorWarningMessages.getErrorMessages()
                                                                              .values()
                                                                              .iterator()
                                                                              .next()
                                                                              .iterator()
                                                                              .next();
            }
            cleanCacheAndPrintErrors();
        }

        if (enableTestBehavior() && exitCode > 0) {
            throw new UnRecoverableException(exceptionMessage);
        }
        return exitCode;
    }

    /**
     * Create and initialize the DI Injector instance
     *
     * @param applicationModules A collection of Module instances that provide DI configuration
     * @param isDevMode          True if the DI system should be configured in Dev mode
     * @param config             Application configuration
     * @return An initialized DI Injector instance
     */
    protected abstract Injector createInjector(final Collection<? extends Module> applicationModules,
                                               final boolean isDevMode, final RunConfig config);

    /**
     * Instantiate the ModuleProvider given the class name
     *
     * @param className ModulePRovider class name
     * @return Instantiated ModuleProvider instance
     */
    @SuppressWarnings("unchecked")
    private ModuleProvider createModuleProvider(final String className) {
        ModuleProvider module;
        try {
            Class<? extends ModuleProvider> moduleClass = (Class<? extends ModuleProvider>) Class.forName(className);
            module = moduleClass.getDeclaredConstructor()
                                .newInstance();
        } catch (Exception e) {
            String msg;
            synchronized (this) {
                msg = errorWarningMessages.formatMessage(80010, ERROR_MESSAGE_80010, this.getClass(), className);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                LOGGER.fatal(msg, e);
            }
            throw new UnRecoverableException(msg, e);
        }

        return module;
    }

    /**
     * Create and initialize a RunConfig instance using the command line
     * arguments array
     *
     * @param args Command line arguments array
     * @return An initialized CmdlineArgumentProcessor instance
     */
    private BaseCmdlineArgumentProcessor createRunConfig(final String[] args) {
        BaseCmdlineArgumentProcessor config = getCmdlineProcessor();
        config.parseCommandLine(args);
        return config;
    }

    protected abstract BaseCmdlineArgumentProcessor getCmdlineProcessor();

    /**
     * Get the ActionRunner instance associated with the specified action
     *
     * @param action   Name of the desired action
     * @param injector DI Inject instance used to resolve the ActionRunner instance
     *                 associated with the desired ActionRunner
     * @return ActionRunner instance associated with the specified action
     */
    private ActionRunner getActionRunner(final String action, final Injector injector) {
        Annotation nameAnnotation = new Named() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Named.class;
            }

            @Override
            public String value() {
                return action;
            }
        };

        ActionRunner runner;
        try {
            Key<ActionRunner> actionRunner = Key.get(ActionRunner.class, nameAnnotation);
            runner = injector.getInstance(actionRunner);
        } catch (ConfigurationException e) {
            LOGGER.error("No ActionRunner registered for name " + action, e);
            throw e;
        }

        return runner;
    }

    public String getErrorReport() {
        return this.cachedErrorWarningMessages;
    }

    /**
     * Defines the condition under which a fatal exception that reaches the
     * JodiCommand object is allowed to be thrown to the calling application or
     * terminal.
     * <p>
     * <p>
     * In production, the returned value must be <code>false</code>. In
     * development exception may be used to assists in automated regression
     * testing.
     *
     * @return <code>true</code> indicates that exceptions are thrown to the
     * calling application.
     */
    private boolean enableTestBehavior() {
        return enableTestBehavior;
    }
}

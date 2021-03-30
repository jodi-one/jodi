package one.jodi.base.factory;

import com.google.inject.AbstractModule;
import com.google.inject.util.Providers;
import one.jodi.base.annotations.DevMode;
import one.jodi.base.annotations.Password;
import one.jodi.base.annotations.PropertyFileName;
import one.jodi.base.annotations.Registered;
import one.jodi.base.annotations.XmlFolderName;
import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.matcher.DamerauLevenshteinDistanceImpl;
import one.jodi.base.matcher.StringDistanceMeasure;
import one.jodi.base.util.Register;


/**
 * An implementation of a guice {@link com.google.inject.Module} that configures
 * dependency injection instances that are fundamental to the core framework.
 */
public class BaseModule extends AbstractModule {

    private final static String ERROR_MESSAGE_80200 = "Guice injection error. "
            + "Action: Pass in the password parameters, "
            + "-pw <password> -mpw <masterPassword>.";
    private final RunConfig config;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final Register register;

    /**
     * Creates a new CoreModule instance.
     *
     * @param config   runconfiguration of the module
     * @param register a link to register the module
     */
    public BaseModule(final RunConfig config, final Register register) {
        this.config = config;
        this.register = register;
        //create singleton instance of error message before Guice can inject it
        this.errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();
        register.register(errorWarningMessages);
    }

    /**
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        bind(Register.class).annotatedWith(Registered.class).toInstance(register);
        // bind error framework class to instance that was created previously in
        // constructor to maintain its Singleton nature
        bind(ErrorWarningMessageJodi.class).toInstance(this.errorWarningMessages);

        // Set globally relevant properties that are typically passed through
        // command line
        if (config.getPassword() == null) {
            String msg = errorWarningMessages.formatMessage(80200, ERROR_MESSAGE_80200, this.getClass());
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
        } else {
            bind(String.class).annotatedWith(Password.class).toInstance(config.getPassword());
        }
        bind(Boolean.class).annotatedWith(DevMode.class).toInstance(config.isDevMode());
        bind(String.class).annotatedWith(PropertyFileName.class).toInstance(config.getPropertyFile());
        bind(String.class).annotatedWith(XmlFolderName.class).toProvider(Providers.of(config.getMetadataDirectory()));
        bind(StringDistanceMeasure.class).to(DamerauLevenshteinDistanceImpl.class);
    }

}

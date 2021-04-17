package one.jodi.base.bootstrap;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import one.jodi.base.util.Version;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link RunConfig} implementation that is initialized from the command line.
 */
public abstract class BaseCmdlineArgumentProcessor implements RunConfig {

    protected static final String OPTION_ACTION = "action";
    protected static final String OPTION_MODULE = "module";

    private final static Logger LOGGER = LogManager.getLogger(BaseCmdlineArgumentProcessor.class);

    private final static String ERROR_MESSAGE_80050 = "Usage failure %s";
    private static final String OPTION_CONFIG = "config";
    private static final String OPTION_METADATA = "metadata";
    private static final String OPTION_HELP = "help";
    private static final String OPTION_PACKAGE = "package";
    private static final String OPTION_SCENARIO = "scenario";
    private static final String OPTION_DEV_MODE = "devmode";
    private static final String OPTION_SOURCE_MODEL = "sourcemodel";
    private static final String OPTION_TARGET_MODEL = "targetmodel";
    private static final String OPTION_PACKAGE_SEQUENCE = "packagesequence";
    private static final String OPTION_MODEL = "model";
    private static final String OPTION_PASSWORD = "password";
    private static final String OPTION_WORKREP_PASSWORD = "masterpassword";
    private static final String OPTION_IDS = "ids";
    private static final String OPTION_DEPLOYMENT_ARCHIVE_TYPE = "da_type";

    protected String prefix;
    protected String action;
    protected String[] modules;
    private final ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiImpl.getInstance();
    private String metadataDirectory;
    private String propertyFile;
    private final List<String> moduleProviderClass = new ArrayList<>();
    private boolean devMode;
    private String packageItem;
    private String scenario;
    private String targetModel;
    private String sourceModel;
    private String packageSequence;
    private String modelCode;
    private String password;
    private String masterPassword;
    private boolean generateIds;
    private String da_type;

    /**
     * Creates a new CmdlineArgumentProcessor instance.
     */
    public BaseCmdlineArgumentProcessor() {
    }

    /**
     * Creates and initializes an {@link Options} instance.
     *
     * @return an initialized {@link Options} instance.
     */
    protected Options createOptions() {
        Options opts = new Options();

        opts.addOption(OPTION_HELP, false, "prints this message");

        opts.addOption("m", OPTION_METADATA, true, "metadata directory path");

        opts.addOption("c", OPTION_CONFIG, true,
                "configuration property file path");

        opts.addOption("pkg", OPTION_PACKAGE, true, "no defaults.");

        opts.addOption("scn", OPTION_SCENARIO, true, "no defaults.");

        Option moduleOpt = new Option(OPTION_MODULE, true,
                "one.jodi.bootstrap.ModuleProvider implementation");
        // this is not a maximum but minimum, ant that shouldn't be set to 4.
       // moduleOpt.setArgs(4); // maximal number of values for this option
        opts.addOption(moduleOpt);

        opts.addOption(OPTION_DEV_MODE, false,
                "indicates development mode semantics to the DI framework");

        opts.addOption(OPTION_MODEL, true, "model code");

        opts.addOption("srcmdl", OPTION_SOURCE_MODEL, true, "source model");
        opts.addOption("tgtmdl", OPTION_TARGET_MODEL, true, "target model");
        opts.addOption("ps", OPTION_PACKAGE_SEQUENCE, true,
                "package sequence (integer)");

        opts.addOption("pw", OPTION_PASSWORD, true,
                "password for ODI work repository or database schema");
        opts.addOption("mpw", OPTION_WORKREP_PASSWORD, true,
                "password for ODI master repository");

        opts.addOption(OPTION_IDS, false,
                "Generate id and uid for rpd generation default value is true.");

        opts.addOption("da_type", OPTION_DEPLOYMENT_ARCHIVE_TYPE, true,
                "Deployment Archive type ( DA_INITIAL,\n" +
                        "        DA_PATCH_DEV_REPOS,\n" +
                        "        DA_PATCH_EXEC_REPOS)");

        opts = addOptions(opts);

        return opts;
    }

    protected abstract Options addOptions(final Options existing);

    @Override
    public String getMetadataDirectory() {
        return metadataDirectory;
    }

    @Override
    public List<String> getModuleClasses() {
        return moduleProviderClass;
    }

    @Override
    public String getPropertyFile() {
        return propertyFile;
    }

    public String getPackage() {
        return packageItem;
    }

    public String getScenario() {
        return scenario;
    }

    @Override
    public boolean isDevMode() {
        return devMode;
    }

    @Override
    public String getSourceModel() {
        return sourceModel;
    }

    @Override
    public String getTargetModel() {
        return targetModel;
    }

    public String getPackageSequence() {
        return packageSequence;
    }

    @Override
    public String getModelCode() {
        return modelCode;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getMasterPassword() {
        return masterPassword;
    }

    public boolean generateIds() {
        return generateIds;
    }

    @Override
    public String getDeploymentArchiveType() {
        return da_type;
    }

    /**
     * Returns <code>true</code> if the {@link #moduleProviderClass} property
     * has a value. Returns <code>false</code> otherwise
     *
     * @return true if {@link #moduleProviderClass} property has a value, false
     * otherwise
     */
    public boolean hasModule() {
        return !moduleProviderClass.isEmpty();
    }

    /**
     * Parses the command line arguments to extract application options.
     *
     * @param args arguments
     */
    public void parseCommandLine(final String[] args) {

        Options opts = createOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = null;
        try {
            cmdLine = parser.parse(opts, args);
        } catch (ParseException e) {
            usage("Unexpected exception:" + e.getMessage(), opts, -1);
        }
        if (cmdLine == null || cmdLine.hasOption(OPTION_HELP)) {
            usage(null, opts, 1);
        }
        if (cmdLine == null) {
            return;
        }
        metadataDirectory = cmdLine.getOptionValue(OPTION_METADATA, "");

        propertyFile = cmdLine.getOptionValue(OPTION_CONFIG);
        if (propertyFile == null) {
            usage("The '" + OPTION_CONFIG + "' is required", opts, 1);
        }

        this.password = cmdLine.getOptionValue(OPTION_PASSWORD);
        this.masterPassword = cmdLine.getOptionValue(OPTION_WORKREP_PASSWORD);

        //journalized = cmdLine.hasOption(OPTION_JOURNALIZED);
        devMode = cmdLine.hasOption(OPTION_DEV_MODE);
        packageItem = cmdLine.getOptionValue(OPTION_PACKAGE);
        scenario = cmdLine.getOptionValue(OPTION_SCENARIO);
        sourceModel = cmdLine.getOptionValue(OPTION_SOURCE_MODEL);
        targetModel = cmdLine.getOptionValue(OPTION_TARGET_MODEL);
        packageSequence = cmdLine.getOptionValue(OPTION_PACKAGE_SEQUENCE);
        modelCode = cmdLine.getOptionValue(OPTION_MODEL);
        generateIds = !cmdLine.hasOption(OPTION_IDS) || Boolean.parseBoolean(cmdLine.getOptionValue(OPTION_IDS));
        da_type = cmdLine.getOptionValue(OPTION_DEPLOYMENT_ARCHIVE_TYPE);
        modules = cmdLine.getOptionValues(OPTION_MODULE);
    }

    protected abstract String getAction(String[] args);

    protected abstract String[] getDefaultModule(String[] args);

    /**
     * Prints the usage message and exits the application.
     *
     * @param header   the header of the usage message
     * @param opts     options
     * @param exitCode the return / exit code
     */
    protected void usage(final String header, final Options opts, final int exitCode) {
        try {
            HelpFormatter formatter = new HelpFormatter();
            Version.init();
            System.out.println("Jodi.one Version " + Version.getProductVersion());
            formatter.printHelp(" ", header, opts, null);
        } catch (RuntimeException r) {
            String msg = errorWarningMessages.formatMessage(80050,
                    ERROR_MESSAGE_80050,
                    Class.class, r.getMessage());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            LOGGER.error(msg, r);
        }
    }

    protected void usage(final String header, final int exitCode) {
        usage(header, createOptions(), exitCode);
    }
}

package one.jodi.bootstrap;

import one.jodi.base.bootstrap.BaseCmdlineArgumentProcessor;
import one.jodi.base.bootstrap.RunConfig;
import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Derived class of BaseCmdlineArgumentProcessor class for an
 * {@link RunConfig} implementation that is initialized from the command line.
 *
 */
@SuppressWarnings("deprecation")
public class EtlCmdlineArgumentProcessor extends BaseCmdlineArgumentProcessor implements EtlRunConfig {
    private static final String OPTION_ACTION = "action";
    private static final String OPTION_PREFIX = "prefix";
    private static final String OPTION_JOURNALIZED = "journalized";
    private static final String OPTION_DEFAULTSCENARIONAMES = "defaultscenarionames";
    private static final String OPTION_FOLDER = "folder";
    private static final String OPTION_INCLUDE_VARIABLES = "includeVariables";
    private static final String OPTION_INCLUDE_CONSTRAINTS = "includeConstraints";
    private static final String OPTION_EXPORT_DB_CONSTRAINTS = "exportDBConstraints";
    private static final String OPTION_DEPLOYMENT_ARCHIVE_PWD = "deploymentArchivePassword";
    private final static ActionType ACTION_DEFAULT_VALUE = ActionType.CREATE_ETLS;
    private final static String MODULE_CLASS_DEFAULT_VALUE = "one.jodi.odi.factory.OdiModuleProvider";
    private List<String> moduleProviderClass = new ArrayList<>();
    private boolean journalized;
    private boolean useDefaultScenarionames;
    private String folder;
    private boolean includeVariables;
    private boolean includingConstraints;
    private boolean exportingDBConstraints;
    private String deploymentArchivePassword;
    public EtlCmdlineArgumentProcessor() {
        super();
    }

    @Override
    public boolean isIncludeVariables() {
        return includeVariables;
    }

    public boolean isJournalized() {
        return journalized;
    }

    @Override
    public List<String> getModuleClasses() {
        return moduleProviderClass;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public boolean hasModule() {
        return super.hasModule();
    }

    @Override
    protected Options addOptions(Options existingOptions) {
        existingOptions.addOption("a", OPTION_ACTION, true, "defaults to '" + ACTION_DEFAULT_VALUE.getCode() + "'");
        existingOptions.addOption("p", OPTION_PREFIX, true, "transformation prefix");
        existingOptions.addOption(OPTION_JOURNALIZED, false,
                "presence of this option indicates 'journalized=true'. "
                        + "Absence indicates 'journalized=false'");
        existingOptions.addOption("f", OPTION_FOLDER, true, "folder name");
        existingOptions.addOption("dsn", OPTION_DEFAULTSCENARIONAMES, true, "Applicable for loadplan import; while importing use the default names for scenarios.");
        existingOptions.addOption("includeVariables", OPTION_INCLUDE_VARIABLES, true, "includeVariables");
        existingOptions.addOption("includeConstraints", OPTION_INCLUDE_CONSTRAINTS, true, "generate constraints with etls");
        existingOptions.addOption(OPTION_EXPORT_DB_CONSTRAINTS, OPTION_EXPORT_DB_CONSTRAINTS, true, "export constraints defined in database.");
        existingOptions.addOption("dapwd", OPTION_DEPLOYMENT_ARCHIVE_PWD, true, "password used for deploymentArchive.");
        return existingOptions;
    }

    @Override
    public void parseCommandLine(String[] args) {
        super.parseCommandLine(args);

        Options opts = createOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = null;

        try {
            cmdLine = parser.parse(opts, args);
        } catch (ParseException e) {
            usage("Unexpected exception:" + e.getMessage(), opts, -1);
        }

        action = cmdLine.getOptionValue(OPTION_ACTION);
        if (action == null) {
            usage("Please specify an action with the '" + OPTION_ACTION +
                    "' command line option", opts, 1);
        }

        prefix = cmdLine.getOptionValue(OPTION_PREFIX);
        if (prefix == null) {
            prefix = "";
        }
        journalized = cmdLine.hasOption(OPTION_JOURNALIZED);

        folder = cmdLine.getOptionValue(OPTION_FOLDER);
        modules = cmdLine.getOptionValues(OPTION_MODULE);
        if (modules == null) {
            modules = getDefaultModule(args);
        }
        for (String moduleName : modules) {
            if (!moduleName.trim().isEmpty()) {
                moduleProviderClass.add(moduleName);
            }
        }
        if (cmdLine.hasOption(OPTION_INCLUDE_VARIABLES)) {
            includeVariables = cmdLine.getOptionValue(OPTION_INCLUDE_VARIABLES).equalsIgnoreCase("true") ? true : false;
        } else {
            includeVariables = true;
        }

        includingConstraints = cmdLine.hasOption(OPTION_INCLUDE_CONSTRAINTS) ? Boolean
                .parseBoolean(cmdLine.getOptionValue(OPTION_INCLUDE_CONSTRAINTS)) : true;

        exportingDBConstraints = cmdLine.hasOption(OPTION_EXPORT_DB_CONSTRAINTS) ? Boolean
                .parseBoolean(cmdLine.getOptionValue(OPTION_EXPORT_DB_CONSTRAINTS)) : false;


        String aDefaultScenarioNames = cmdLine.getOptionValue(OPTION_DEFAULTSCENARIONAMES);
        if (aDefaultScenarioNames != null && aDefaultScenarioNames.equals("false")) {
            useDefaultScenarionames = false;
        } else {
            useDefaultScenarionames = true;
        }
        deploymentArchivePassword = cmdLine.hasOption(OPTION_DEPLOYMENT_ARCHIVE_PWD) ? cmdLine.getOptionValue(OPTION_DEPLOYMENT_ARCHIVE_PWD) : "";
    }

    @Override
    protected String getAction(String[] args) {
        return action;
    }

    public ActionType getActionType() {
        return ActionType.forCode(OPTION_ACTION);
    }

    protected String getPrefix(String[] args) {
        return prefix;
    }

    protected String[] getDefaultModule(String[] args) {
        return new String[]{MODULE_CLASS_DEFAULT_VALUE};
    }

    @Override
    public boolean isUsingDefaultscenarioNames() {
        return useDefaultScenarionames;
    }

    @Override
    public String getFolder() {
        return folder;
    }

    @Override
    public boolean isExportingDBConstraints() {
        return exportingDBConstraints;
    }

    @Override
    public boolean isIncludingConstraints() {
        return this.includingConstraints;
    }

    @Override
    public String getDeployementArchivePassword() {
        return deploymentArchivePassword;
    }
}

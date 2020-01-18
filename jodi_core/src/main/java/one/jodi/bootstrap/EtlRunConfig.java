package one.jodi.bootstrap;

import one.jodi.base.bootstrap.RunConfig;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;

public interface EtlRunConfig extends RunConfig {

    String ERROR_MESSAGE_00370 = "No matching ActionType for code %s";

    /**
     * Gets the package.
     *
     * @return the package
     */
    String getPackage();

    /**
     * Gets the scenario.
     *
     * @return the scenario
     */
    String getScenario();

    /**
     * Checks if journalized is set to true.
     *
     * @return true, if journalized is set to true
     */
    boolean isJournalized();

    /**
     * Gets the package sequence as string
     *
     * @return package sequence
     */
    String getPackageSequence();

    /**
     * Gets the interface prefix.
     *
     * @return the interface prefix
     */
    String getPrefix();

    /**
     * Indicates deletion and creation of variables.
     *
     * @return boolean
     */
    boolean isIncludeVariables();


    /**
     * Applicable for loadplan import,
     * use the default scenario names,
     * the names that are used when generating all scenarios.
     */
    boolean isUsingDefaultscenarioNames();

    String getFolder();

    boolean isExportingDBConstraints();

    boolean isIncludingConstraints();

    String getDeployementArchivePassword();

    enum ActionType {

        /**
         * The create etls action.
         */
        CREATE_ETLS("etls"),
        /**
         * The create transformations action.
         */
        CREATE_TRANSFORMATIONS("ct"),
        /**
         * The create etls action.
         */
        CREATE_SCENARIOS("cs"),
        /**
         * The delete transformations.
         */
        DELETE_TRANSFORMATIONS("dt"),
        /**
         * The create package action
         */
        CREATE_PACKAGES("cp"),
        /**
         * The delete package action.
         */
        DELETE_PACKAGE("dp"),
        /**
         * The delete scenario action.
         */
        DELETE_SCENARIO("ds"),
        /**
         * Alter SCD Tables action
         */
        ALTER_SCD_TABLES("atbs"),
        /**
         * Alter Tables action
         */
        ALTER_TABLES("atb"),
        /**
         * Check Tables action
         */
        CHECK_TABLES("cktb"),
        /**
         * Extraction Tables action
         */
        EXTRACTION_TABLES("etb"),
        /**
         * Prefix for an action that must be used as a prefix for custom extensions
         */
        EXTENSION_POINT("ext:"),
        /**
         * Delete references
         */
        DELETE_REFERENCES("dr"),
        /**
         * Delete all packages
         */
        DELETE_ALL_PACKAGES("dap"),
        /**
         * Dimension Import
         */
        ODI_IMPORT("oim"),
        /**
         * Dimension Export
         */
        ODI_EXPORT("oex"),
        /**
         * LoadPlanImport
         */
        LOAD_PLAN_EXPORT("lpe"),
        /**
         * LoadPlanPrint
         */
        LOAD_PLAN_PRINT("lpp"),
        /**
         * LoadPlan
         */
        LOAD_PLAN("lp"),
        /**
         * VariablesImpl export
         */
        VARIABLES_EXPORT("expvar"),
        /**
         * VariablesImpl import
         */
        VARIABLES_CREATE("crtvar"),
        /**
         * VariablesImpl delete
         */
        VARIABLES_DELETE("delvar"),
        /**
         * SequencesImpl export
         */
        SEQUENCES_EXPORT("expseq"),
        /**
         * SequencesImpl import
         */
        SEQUENCES_CREATE("crtseq"),
        /**
         * SequencesImpl delete
         */
        SEQUENCES_DELETE("delseq"),
        CONSTRAINTS_CREATE("crtcon"),
        CONSTRAINTS_DELETE("delcon"),
        CONSTRAINTS_EXPORT("expcon"),
        /**
         * create procedures
         */
        PROCEDURE_CREATE("crtproc"),
        /**
         * delete procedures
         */
        PROCEDURE_DELETE("delproc"),
        PRINT("prnt"),
        VALIDATE("vldt"),
        CREATE_DATASTORES("crtds");


        /**
         * The code.
         */
        private String code;

        /**
         * Instantiates a new action type.
         *
         * @param code the code
         */
        ActionType(final String code) {
            this.code = code;
        }

        /**
         * Map the specified action code to an ActionType enum constant.
         *
         * @param code the code
         * @return the action type
         */
        public static ActionType forCode(final String code) {

            // handle special case for "ext:xxxxx"
            if (code.startsWith(ActionType.EXTENSION_POINT.getCode())) {
                return ActionType.EXTENSION_POINT;
            }

            String msg = errorWarningMessages.formatMessage(370,
                    ERROR_MESSAGE_00370, Class.class.getClass(), code);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.ERRORS);
            throw new IllegalArgumentException(msg);
        }

        /**
         * Gets the action code.
         *
         * @return the code
         */
        public String getCode() {
            return code;
        }


    }
}
package one.jodi.core.validation.etl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import one.jodi.base.annotations.DevMode;
import one.jodi.base.config.JodiPropertyNotFoundException;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.model.types.*;
import one.jodi.base.service.schema.DataStoreNotInModelException;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.extensions.contexts.JournalizingExecutionContext;
import one.jodi.core.extensions.strategies.*;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.metadata.ETLSubsystemService;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.etl.common.EtlSubSystemVersion;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.journalizng.JournalizingConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
public class ETLValidatorImpl implements ETLValidator {

    private final static Logger logger = LogManager.getLogger(ETLValidatorImpl.class);
    private final static String newLine = System.getProperty("line.separator");

    private final static String ERROR_MESSAGE_02090 = "Dataset index not found";
    private final static String ERROR_MESSAGE_02100 = "Source index not found";
    private final static String ERROR_MESSAGE_02110 = "%s has no column with name %s or column is a reusable mapping column.";
    private final static String ERROR_MESSAGE_02120 = "Cannot find datastore %s amongst dataset's stores %s";
    private final static String ERROR_MESSAGE_10000 = "A transformation with name %s has already been defined associated with package sequence %s.";
    private final static String ERROR_MESSAGE_10001 = "A transformation with sequence %s has already been defined in file %s.";
    private final static String ERROR_MESSAGE_10020 = "While processing transformation with sequence number %s an unpredicted error occurred in plug-in %s during the calculation of the transformation name.";
    private final static String ERROR_MESSAGE_10030 = "While processing transformation with sequence # %s an unpredicted error occurred in plug-in %s during the calculation of target folder.";
    private final static String ERROR_MESSAGE_10040 = "Folder Name Strategy %s returned null or empty file name.";
    private final static String ERROR_MESSAGE_10050 = "Transformation %s has data store %s of length %s which exceeds maximum length of 30 characters for regular mappings and 28 characters for reusable mappings, a possible solution could be to use an alias for reusable mappings, please note the first 28 characters of the datastores should be unique (only applies to ORACLE technology).";
    private final static String ERROR_MESSAGE_10060 = "Transformation does not specify any associated packages.";
    private final static String ERROR_MESSAGE_10061 = "Transformation specifies package list item %s which is not contained in package file (0.xml/1.xml).";
    private final static String ERROR_MESSAGE_10070 = "Transformation contains more than one Source or Lookup with journalized option set.";
    private final static String ERROR_MESSAGE_20002 = "Dataset[0] defines set operator %s but should not define one for the first dataset.";
    private final static String ERROR_MESSAGE_30000 = "Source %s in Dataset[%s] sets Subselect option to true. This option is only valid for temporary interfaces.";
    private final static String ERROR_MESSAGE_30010 = "Source data store %s in Dataset[%s] does not exist in any data model.";
    private final static String ERROR_MESSAGE_30011 = "Model %s was selected for source data store %s in Dataset[%s] but this data store does not exist in the selected model.";
    private final static String ERROR_MESSAGE_30012 = "Source data store %s in Dataset[%s] explicitly defines model code %s but it is not defined in properties file.";
    private final static String ERROR_MESSAGE_30013 = "Source data store %s in Dataset[%s] explicitly defines model code %s but the source data store does not exist in this model.";
    private final static String ERROR_MESSAGE_30014 = "Source data store %s can't have a dollar sign in alias, if a name has a dollar sign it must have alias without dollar sign.";
    private final static String ERROR_MESSAGE_30020 = "An unpredicted error occurred in plug-in %s while determining the model code for source data store %s in Dataset[%s].";
    private final static String ERROR_MESSAGE_30030 = "Dataset[%s] refers to data source %s more than once but with non-distinct alias %s.";
    private final static String ERROR_MESSAGE_30031 = "Dataset[%s] refers to two data source %s and %s with same alias %s.";
    private final static String ERROR_MESSAGE_30100 = "Filter condition %s of Source %s in Dataset[%s] refers to source %s by name even though it must be referenced by its alias. ";
    private final static String ERROR_MESSAGE_30101 = "Filter condition %s of Source %s in Dataset[%s] refers exclusively to other sources. A filter must refer to this source with one part of the expression.";
    private final static String ERROR_MESSAGE_30103 = "Filter condition %s of source %s in Dataset[%s] refers to alias %s that is not in scope for this operation.";
    private final static String ERROR_MESSAGE_30102 = "Filter condition %s of source %s in Dataset[%s] refers to package variable %s that does not exist.";
    private final static String ERROR_MESSAGE_30104 = "Filter condition %s of source %s in Dataset[%s] makes the left outer join an innner join, the filter may be moved in the join.";
    private final static String ERROR_MESSAGE_20120 = "Unpredicted error during calculation of filter execution location for join at Source %s in Dataset[%s] occurred in plug-in {%s}.";
    private final static String ERROR_MESSAGE_30121 = "Filter of source %s in Dataset[%s] explicitly defines invalid execution location %s. Only Values SOURCE and WORK are allowed.";
    private final static String ERROR_MESSAGE_30122 = "Execution location strategy %s attempted to set invalid filter (%s) for source %s in Dataset[%s].";
    private final static String ERROR_MESSAGE_30130 = "Filter for Source %s in Dataset[%s] refers to undefined source %s.";
    private final static String ERROR_MESSAGE_30131 = "Filter for Source %s in Dataset[%s] refers to undefined column %s of source %s.";
    private final static String ERROR_MESSAGE_30140 = "Name for Pivot/Unpivot %s in Source %s in Dataset[%s] uses name used by either source or another Flow item.";
    private final static String ERROR_MESSAGE_30141 = "Row locator for Pivot %s in Source %s in Dataset[%s] refers to unknown alias %s.";
    private final static String ERROR_MESSAGE_30142 = "Row locator for Pivot %s in Source %s in Dataset[%s] refers to unknown column %s.";
    private final static String ERROR_MESSAGE_30143 = "Row locator for Pivot %s in Source %s in Dataset[%s] refers to source/lookup by name %s; please use alias instead.";
    private final static String ERROR_MESSAGE_30144 = "Column Expression for Pivot/Unpivot %s in Source %s in Dataset[%s] refers to unknown component %s.";
    private final static String ERROR_MESSAGE_30145 = "Column Expression for Pivot/Unpivot %s in Source %s in Dataset[%s] refers to unknown column %s. ";
    private final static String ERROR_MESSAGE_30146 = "Column Expression for Pivot/Unpivot %s in Source %s in Dataset[%s] refers to unknown project variable %s.";
    private final static String ERROR_MESSAGE_30147 = "Column Expression for Pivot/Unpivot %s in Source %s in Dataset[%s] refers to source/lookup by name %s; please use alias instead.";
    private final static String ERROR_MESSAGE_30148 = "Column name %s for Pivot/Unpivot %s in Source %s in Dataset[%s] has previously been declared. ";
    private final static String ERROR_MESSAGE_30149 = "Source %s in Dataset[%s] uses Flows.  This is only supported for ODI 12 and will be ignored.";
    private final static String ERROR_MESSAGE_30150 = "Column %s defined in Pivot %s in Source %s Dataset[%s] defines an expression likely to build an invalid pivot.  The length of the first source column in the expression plus the length of the row locator column must be less than 27";
    private final static String ERROR_MESSAGE_30151 = "Condition for SubQuery %s in Source %s in Dataset[%s] refers to unknown alias %s.";
    private final static String ERROR_MESSAGE_30152 = "Condition for SubQuery %s in Source %s in Dataset[%s] refers to unknown column %s.";

    private final static String ERROR_MESSAGE_30153 = "Condition for SubQuery %s in Source %s in Dataset[%s] refers to source by name %s; please use alias instead.";
    private final static String ERROR_MESSAGE_30154 = "Condition for SubQuery %s in Source %s in Dataset[%s] does not refer to driver.";
    private final static String ERROR_MESSAGE_30155 = "Condition not set for SubQuery %s in Source %s in Dataset[%s]; this must be supplied when Role is set to EXISTS or NOT_EXISTS";
    private final static String ERROR_MESSAGE_30156 = "SubQuery %s in Source %s in Dataset[%s] explicitly defines invalid execution location %s. Only Values SOURCE and WORK are allowed.";
    private final static String ERROR_MESSAGE_30200 = "Join condition %s of Source %s in Dataset[%s] refers to source %s by name even though it must be referenced by its alias.";
    private final static String ERROR_MESSAGE_30201 = "Join condition %s of Source %s in Dataset[%s] refers exclusively to other sources. Join condition must refer to this source with one part of the expression.";
    private final static String ERROR_MESSAGE_30204 = "Join condition %s of Source %s in Dataset[%s] refers to alias %s that is not in scope for this operation.";
    private final static String ERROR_MESSAGE_30206 = "Join condition %s of Source %s in Dataset[%s] is undefined. INNER and OUTER joins must define a valid join condition.";
    private final static String ERROR_MESSAGE_30213 = "Source %s in Dataset[%s] defines a cross join and defines illegal join condition %s. Only the alias of the data source to join with may be defined.";
    private final static String ERROR_MESSAGE_30214 = "Source %s in Dataset[%s] defines a natural join and defines illegal join condition %s. Only the alias of the data source to join with may be defined.";
    private final static String ERROR_MESSAGE_30220 = "Unpredicted error during calculation of join execution location for join at Source %s in Dataset[%s] occurred in plug-in %s.";
    private final static String ERROR_MESSAGE_30221 = "Join of Source %s in Dataset[%s] explicitly defines invalid execution location %s. Only Values SOURCE and WORK are allowed.";
    private final static String ERROR_MESSAGE_30222 = "Execution Location plugin %s attempted to set invalid join location %s for Source %s in Dataset[%s]. Only Values SOURCE and WORK are allowed.";
    private final static String ERROR_MESSAGE_30230 = "Join condition for Source %s in Dataset[%s] refers to non-existent source %s.";
    private final static String ERROR_MESSAGE_30231 = "Join condition for Source %s in Dataset[%s] refers to non-existent column %s on source %s.";
    private final static String ERROR_MESSAGE_30232 = "Join condition for Source %s in Dataset[%s] requires data type conversion between %s and %s.";
    private final static String ERROR_MESSAGE_30300 = "The explicitly defined LKM code %s of source %s in Dataset[%s] is not defined in KM rule Jodi properties file.";
    private final static String ERROR_MESSAGE_30301 = "The explicitly defined LKM option code %s at source %s in Dataset[%s] is not defined for the selected LKM.";
    private final static String ERROR_MESSAGE_30302 = "The illegal value %s is assigned to the explicitly defined LKM option code %s of source %s in Dataset[%s].";
    private final static String ERROR_MESSAGE_30303 = "An unpredicted error occurred in plug-in %s while determining the LKM code and LKM options of source %s in Dataset[%s].";
    private final static String ERROR_MESSAGE_30304 = "No suitable LKM was found for target technology %s for source %s in Dataset[%s].";
    private final static String ERROR_MESSAGE_30305 = "No LKM with name %s was loaded in ODI for source %s in Dataset[%s].";
    private final static String ERROR_MESSAGE_30306 = "KM plugin %s attempted to set LKM code %s for source %s in Dataset[%s] which is not defined in KM rule Jodi properties file.";
    private final static String ERROR_MESSAGE_30307 = "KM plugin %s attempted to set LKM option code %s for source %s in Dataset[%s] which is not defined for the selected LKM.";
    private final static String ERROR_MESSAGE_30308 = "KM plugin %s attempted to set illegal value %s for LKM option code %s of source %s in Dataset[%s].";
    private final static String ERROR_MESSAGE_31000 = "Lookup %s in Source %s contained in Dataset[%s] sets Subselect option to true. This option is only valid for temporary interfaces and will be ignored.";
    private final static String ERROR_MESSAGE_31010 = "Lookup data store %s in Source %s contained in Dataset[%s] does not exist in any defined model.";
    private final static String ERROR_MESSAGE_31011 = "Model %s was selected for lookup data store %s in Source %s contained in Dataset[%s] but lookup data store does not exist in the selected model.";
    private final static String ERROR_MESSAGE_31012 = "Lookup data store %s in Source %s contained in Dataset[%s] explicitly defines model code %s but it is not defined in properties file.";
    private final static String ERROR_MESSAGE_31013 = "Lookup data store %s in Source %s contained in Dataset[%s] explicitly defines model code %s but the lookup data store does not exist in this model.";
    private final static String ERROR_MESSAGE_31020 = "An unpredicted error occurred in plug-in %s while determining the model code for Lookup data store %s in source %s contained in Dataset[%s].";
    private final static String ERROR_MESSAGE_31200 = "Join expression %s of Lookup %s of Source %s in Dataset[%s] refers to source %s by name even though it must be referenced by its alias.";
    private final static String ERROR_MESSAGE_31201 = "Join expression %s of Lookup %s of Source %s in Dataset[%s] does not refer to the parent Source alias.";
    private final static String ERROR_MESSAGE_31202 = "Join expression %s of Lookup %s of Source %s in Dataset[%s] refers to unknown alias %s";
    private final static String ERROR_MESSAGE_31203 = "Join expression %s of Lookup %s of Source %s in Dataset[%s] contains illegal reference to alias %s.";
    private final static String ERROR_MESSAGE_31221 = "Join of Lookup %s of Source %s  in Dataset[%s] explicitly defines invalid execution location %s. Only Values SOURCE and WORK are allowed.";
    private final static String ERROR_MESSAGE_31222 = "Plugin strategy %s  for Lookup %s of Source %s  in Dataset[%s] attempted to set invalid execution location %s. Only Values SOURCE and WORK are allowed.";
    private final static String ERROR_MESSAGE_31230 = "Join condition for Lookup %s in Source %s in Dataset[%s] refers to non-existent source %s.";
    private final static String ERROR_MESSAGE_31231 = "Join condition for Lookup %s in Source %s in Dataset[%s] refers to non-existent column %s on source %s.";
    private final static String ERROR_MESSAGE_31232 = "Join condition for Lookup %s in Source %s in Dataset[%s] requires data type conversion between %s and  %s.";
    private final static String ERROR_MESSAGE_31240 = "Lookup type for Lookup %s in Source %s in Dataset[%s] specifies invalid type %s and ODI version %s";
    private final static String ERROR_MESSAGE_31241 = "Lookup type for Lookup %s in Source %s in Dataset[%s] is explicitly set but ignored as Source or Lookup is temporary and implemented with Resusable Mapping in ODI 12.";
    private final static String ERROR_MESSAGE_31300 = "Default row expression %s of Column %s of Lookup %s of Source %s in Dataset[%s] refers to source %s by name even though it must be referenced by its alias.";
    private final static String ERROR_MESSAGE_31302 = "Default row expression %s of Column %s of Lookup %s of Source %s in Dataset[%s] refers to unknown alias %s";
    private final static String ERROR_MESSAGE_31303 = "Default row expression %s of Column %s of Lookup %s of Source %s in Dataset[%s] refers to unknown column %s of source %s.";
    private final static String ERROR_MESSAGE_31304 = "Default row Column with name %s of Lookup %s of Source %s in Dataset[%s] does not exist on Lookup Datastore.";
    private final static String ERROR_MESSAGE_31305 = "NoMatchRows of Lookup %s of Source %s in Dataset[%s] fails to define Column with name %s.";
    private final static String ERROR_MESSAGE_40010 = "Target data store %s does not exist in any data model.";
    private final static String ERROR_MESSAGE_40011 = "Model %s was selected for target data store %s but this data store does not exist in the selected model.";
    private final static String ERROR_MESSAGE_40012 = "Target data store %s explicitly defines model code %s, which is not defined in properties file.";
    private final static String ERROR_MESSAGE_40013 = "Target data store %s explicitly defines model code %s but the target data store does not exist in this model.";
    private final static String ERROR_MESSAGE_40020 = "An unpredicted error occurred in plugin %s while determining the model code target data store %s.";
    private final static String ERROR_MESSAGE_40100 = "Jodi Properties file rule %s refers to unknown IKM code %s.";
    private final static String ERROR_MESSAGE_40110 = "The explicitly defined IKM code %s in Mappings is not defined in KM rule Jodi Properties file.";
    private final static String ERROR_MESSAGE_40111 = "The explicitly defined IKM option code %s at Mappings is not defined for the selected IKM.";
    private final static String ERROR_MESSAGE_40112 = "The illegal value %s is assigned to the explicitly defined IKM option code %s in Mappings.";
    private final static String ERROR_MESSAGE_40120 = "An unpredicted error occurred in plugin %s while determining the IKM code and IKM options of target data store %s.";
    private final static String ERROR_MESSAGE_40121 = "No suitable IKM was found for target %s with technology %s";
    private final static String ERROR_MESSAGE_40140 = "KM plugin attempted to set IKM code %s which is not defined in any KM rules of the Jodi properties file.";
    private final static String ERROR_MESSAGE_40141 = "KM plugin attempted to set IKM option code %s which is not defined for selected IKM";
    private final static String ERROR_MESSAGE_40142 = "KM plugin attempted to set IKM option code value %s which is invalid for the selected IKM and code %s.";
    private final static String ERROR_MESSAGE_40143 = "IKM %s is multi-connection enabled however no staging model has been set.";
    private final static String ERROR_MESSAGE_40144 = "An unpredicted error occurred in plugin %s while determining staging model for target data store %s.";
    private final static String ERROR_MESSAGE_40200 = "Jodi Properties file rule %s refers to unknown CKM code %s";
    private final static String ERROR_MESSAGE_40220 = "An unpredicted error occurred in plug-in %s while determining the CKM code and CKM options of target data store %s.";
    private final static String ERROR_MESSAGE_40221 = "No suitable CKM was found for target %s with technology %s";
    private final static String ERROR_MESSAGE_40210 = "The explicitly defined CKM code %s in Mappings is not defined in KM rule Jodi Properties file.";
    private final static String ERROR_MESSAGE_40211 = "The explicitly defined CKM option code %s at Mappings is not defined for the selected CKM. ";
    private final static String ERROR_MESSAGE_40212 = "The illegal value %s is assigned to the explicitly defined CKM option code %s in Mappings.";
    private final static String ERROR_MESSAGE_40240 = "KM plugin attempted to set CKM code %s which is not defined in any KM rules of the Jodi properties file.";
    private final static String ERROR_MESSAGE_40241 = "KM plugin attempted to set CKM option code %s which is not defined for selected CKM";
    private final static String ERROR_MESSAGE_40242 = "KM plugin attempted to set CKM option code value %s which is invalid for the selected CKM and code %s.";
    private final static String ERROR_MESSAGE_41000 = "Undefined column %s on target data store %s";
    private final static String ERROR_MESSAGE_41001 = "Target column %s of target data store %s mappings has a different type as column %s of source %s.";
    private final static String ERROR_MESSAGE_41002 = "The data type of target column %s of the target data store %s mappings has a smaller length or scale than its source column.";
    private final static String ERROR_MESSAGE_41003 = "Mapping Expression for target column %s of target data store %s mapping must be defined when the target column is defined.";
    private final static String ERROR_MESSAGE_41004 = "Incorrect number of expressions for target column %s of target data store %s mappings. Expected %s expressions but found %s.";
    private final static String ERROR_MESSAGE_41005 = "Expression[%s] for target column %s of target data store %s mappings does not appear valid.";
    private final static String ERROR_MESSAGE_41006 = "Target column %s is duplicated for %s mappings.";
    private final static String ERROR_MESSAGE_41010 = "Expression[%s] for target column %s of target data store %s mappings refers to source %s by name even though it must be referenced by its alias.";
    private final static String ERROR_MESSAGE_41011 = "Expression[%s] for target column %s of target data store %s mappings refers to alias %s that is not in scope for this operation.";
    private final static String ERROR_MESSAGE_41012 = "Expression[%s] for target column %s of target data store %s mappings refers to undefined column %s in source %s.";
    private final static String ERROR_MESSAGE_41013 = "Expression[%s] for target column %s of target data store %s mappings is undefined.";
    private final static String ERROR_MESSAGE_41016 = "Expression[%s] for target column %s of target data store %s mappings refers to alias %s that is not defined for this operation.";
    private final static String ERROR_MESSAGE_41017 = "Unpredicted error during mapping of target column %s of target data store %s";
    private final static String ERROR_MESSAGE_41020 = "Unpredicted error during calculation of Mappings execution location for target column %s in of target data store %s.";
    private final static String ERROR_MESSAGE_41030 = "Column properties must not be defined for target column %s of target data store %s mappings because target is not a temporary table.";
    private final static String ERROR_MESSAGE_41031 = "Column properties are required but are undefined for target column %s of temporary table %s mappings.";
    private final static String ERROR_MESSAGE_41032 = "Column properties must define a data type for target column %s of temporary table %s mappings.";
    private final static String ERROR_MESSAGE_42001 = "The column '%s' of type '%s' will be updated since '%s' is a dimension, and dimension columns are limited to types ADD_ROW_ON_CHANGE, OVERRIDE_ON_CHANGE.";
    private final static String ERROR_MESSAGE_50000 = "Knowledge Module rules specified in Jodi properties file contains errors.  See log file for details.";
    private final static String ERROR_MESSAGE_70001 = "Unpredicted error was raised was raised in during call to get model codes for CDC in journalizing strategy %s.";
    private final static String ERROR_MESSAGE_70002 = "Unpredicted error was raised was raised in during call to get JKM options in journalizing strategy %s.";
    private final static String ERROR_MESSAGE_70003 = "Unpredicted error was raised was raised in during call to get subscribers in journalizing strategy %s.";
    private final static String ERROR_MESSAGE_70010 = "Journalizing property for model code %s refers to JKM %s which is not loaded in ODI.";
    private final static String ERROR_MESSAGE_70011 = "Journalizing property for model code %s and JKM %s refers to undefined option code %s.";
    private final static String ERROR_MESSAGE_70012 = "Journalizing property for model code %s and JKM %s  and option code %s defines invalid value %s.";
    private final static String ERROR_MESSAGE_70014 = "Journalizing plugin %s for model code %s and JKM %s refers to undefined option code %s.";
    private final static String ERROR_MESSAGE_70015 = "Journalizing plugin %s for model code %s and JKM %s  and option code %s defines invalid value %s.";
    private final static String ERROR_MESSAGE_70020 = "Deprecate CDC property set on journalized run.";
    //private final static String ERROR_MESSAGE_99996 = "Variables shouldn't be referenced as binds (:VARNAME) any longer, and should be named (#PROJECT_NAME.VARIABLE).";
    private final static String ERROR_MESSAGE_71010 = "Tranformation with package sequence %s does not have valid %s %s";
    // duplicated since we don't want dependencies
    private final static String ERROR_MESSAGE_02040 = "Error initializing JournalizingExecutonContext %1$s.";
    private final static String ERROR_MESSAGE_02050 = "Unsuccessful attempt to parse name value pair";
    public final ErrorWarningMessageJodi errorWarningMessages;
    public final Boolean devMode;
    private final DatabaseMetadataService metadataService;
    private final ETLSubsystemService etlSubsystemProvider;
    private final JodiProperties properties;
    private final TreeMap<Integer, List<String>> errors;
    private final TreeMap<Integer, List<String>> warnings;
    private final PackageCache packageCache;
    private final EtlSubSystemVersion etlSubSystemVersion;
    private final JournalizingContext journalizingContext;
    // end duplication
    public HashMap<Integer, String> sequenceMap;
    public HashMap<String, Integer> nameMap;

    @Inject
    public ETLValidatorImpl(final DatabaseMetadataService metadataService,
                            final ETLSubsystemService etlSubsystemProvider,
                            final JodiProperties properties,
                            final PackageCache packageCache,
                            final ErrorWarningMessageJodi errorWarningMessages,
                            final @DevMode Boolean devMode,
                            final EtlSubSystemVersion etlSubSystemVersion,
                            final JournalizingContext journalizingContext) {
        this.metadataService = metadataService;
        this.properties = properties;
        this.etlSubsystemProvider = etlSubsystemProvider;
        sequenceMap = new HashMap<>();
        nameMap = new HashMap<>();
        errors = new TreeMap<>();
        warnings = new TreeMap<>();
        this.packageCache = packageCache;
        this.errorWarningMessages = errorWarningMessages;
        this.devMode = devMode;
        this.etlSubSystemVersion = etlSubSystemVersion;
        this.journalizingContext = journalizingContext;
    }

    @Override
    public void reset() {
        errors.clear();
        warnings.clear();
        nameMap.clear();
        sequenceMap.clear();
    }

    public void addErrorMessage(int packageSequence, String error) {
        errorWarningMessages.addMessage(packageSequence, error, MESSAGE_TYPE.ERRORS);
    }

    public void addErrorMessage(String error) {
        errorWarningMessages.addMessage(error, MESSAGE_TYPE.ERRORS);
    }

    public void addWarningMessage(int packageSequence, String warning) {
        errorWarningMessages.addMessage(packageSequence, warning, MESSAGE_TYPE.WARNINGS);
    }

    public void addWarningMessage(String warning) {
        errorWarningMessages.addMessage(warning, MESSAGE_TYPE.WARNINGS);
    }

    // used for one.jodi.core.validataion.etl.ETLValidatorImplTests
    public SortedMap<Integer, List<String>> getErrorMessages() {
        return Collections.unmodifiableSortedMap(errorWarningMessages.getErrorMessages());
    }

    public Map<Integer, List<String>> getWarningMessages() {
        return Collections.unmodifiableMap(errorWarningMessages.getWarningMessages());
    }
    // --- end methods added for testing

    @Override
    public boolean validatePackageAssociations(Transformation transformation) {
        boolean valid = true;
        if (transformation.getPackageList() == null ||
                transformation.getPackageList().length() < 1) {
            addErrorMessage(transformation.getPackageSequence(),
                    errorWarningMessages.formatMessage(10060, ERROR_MESSAGE_10060, this.getClass()));
            valid = false;
        } else if (!packageCache.getPackageAssociations().isEmpty()) {
            Set<String> packageAssociations = packageCache.getPackageAssociations();

            List<String> transformationAssociations = parsePackageList(transformation.getPackageList());
            transformationAssociations.stream()
                    .filter(transformationAssociation -> !packageAssociations
                            .contains(transformationAssociation))
                    .forEach(transformationAssociation -> {
                        StringBuilder sb = new StringBuilder();
                        for (String s : packageAssociations) {
                            sb.append(s).append(" ");
                        }
                        sb.append(newLine);
                        logger.debug(sb.toString());
                        addErrorMessage(transformation
                                        .getPackageSequence(),
                                errorWarningMessages
                                        .formatMessage(10061,
                                                ERROR_MESSAGE_10061,
                                                this.getClass(),
                                                transformationAssociation));
                    });
        }

        return valid;
    }

    @Override
    public boolean validateTransformationName(final Transformation transformation) {


        String transformationName = transformation.getFolderName() + "/" + transformation.getName();
        Integer packageSequence = nameMap.get(transformationName);
        boolean valid = true;

        if (packageSequence != null &&
                packageSequence != transformation.getPackageSequence()) {
            addErrorMessage(transformation.getPackageSequence(),
                    errorWarningMessages.formatMessage(10000,
                            ERROR_MESSAGE_10000, this.getClass(), transformationName,
                            packageSequence));
            valid = false;
        } else {
            nameMap.put(transformationName, transformation.getPackageSequence());
        }

        String targetDataStore = transformation.getMappings().getTargetDataStore() + "";
        // check for validity with Dataset number added
        // D1 for dataset 1 so now max length is 26 // 2 for reusable mappings + 2 for dataset
        if (targetDataStore.length() > 30) {
            addErrorMessage(transformation.getPackageSequence(),
                    errorWarningMessages.formatMessage(10050,
                            ERROR_MESSAGE_10050, this.getClass(), transformation.getName(),
                            transformation.getMappings().getTargetDataStore(),
                            transformation.getMappings().getTargetDataStore().length()));
            valid = false;
        }
        if (targetDataStore.length() > 28 && transformation.isTemporary()) {
            addWarningMessage(transformation.getPackageSequence(),
                    errorWarningMessages.formatMessage(10050,
                            ERROR_MESSAGE_10050, this.getClass(), transformation.getName(),
                            transformation.getMappings().getTargetDataStore(),
                            transformation.getMappings().getTargetDataStore().length()));
            valid = true;
        }
        return valid;
    }


    @Override
    public boolean validateFolderName(final Transformation transformation,
                                      final FolderNameStrategy strategy) {
        boolean valid = true;
        String folderName = transformation.getFolderName();
        if (folderName == null || folderName.trim() == null || folderName.trim().length() == 0) {
            addErrorMessage(transformation.getPackageSequence(),
                    errorWarningMessages.formatMessage(10040,
                            ERROR_MESSAGE_10040, this.getClass(), strategy.getClass().getName()));
            valid = false;
        }

        return valid;
    }

    @Override
    public boolean validatePackageSequence(int packageSequence, String fileName) {
        boolean valid = true;
        if (sequenceMap.containsKey(packageSequence)) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(
                            10001, ERROR_MESSAGE_10001,
                            this.getClass(),
                            packageSequence,
                            sequenceMap.get(packageSequence)));
            valid = false;
        } else {
            sequenceMap.put(packageSequence, fileName);
        }

        return valid;
    }

    // PRE
    @Override
    public boolean validateDataset(List<Dataset> datasets) {

        boolean valid = true;
        if (datasets.size() > 0) {
            Integer packageSequence = datasets.get(0).getParent().getPackageSequence();

            SetOperatorTypeEnum setOperator = datasets.get(0).getSetOperator() != null ? datasets
                    .get(0).getSetOperator() : SetOperatorTypeEnum.NOT_DEFINED;

            if (!SetOperatorTypeEnum.NOT_DEFINED.equals(setOperator)) {
                addWarningMessage(packageSequence,
                        errorWarningMessages.formatMessage(20002,
                                ERROR_MESSAGE_20002, this.getClass(), setOperator.name()));
                valid = false;
            }

            for (Dataset dataset : datasets) {
                Map<String, String> aliasTableMap = new HashMap<>();


                for (Source source : dataset.getSources()) {
                    String alias = (source.getAlias() != null) ? source.getAlias() : source.getName();

                    if (!aliasTableMap.containsKey(alias)) {
                        aliasTableMap.put(alias, source.getName());
                    } else {
                        if (aliasTableMap.get(alias).equalsIgnoreCase(source.getName())) {
                            addErrorMessage(packageSequence,
                                    errorWarningMessages.formatMessage(30030,
                                            ERROR_MESSAGE_30030, this.getClass(), getDatasetIndex(dataset),
                                            source.getName(), alias));
                        } else {
                            addErrorMessage(packageSequence,
                                    errorWarningMessages.formatMessage(30031,
                                            ERROR_MESSAGE_30031, this.getClass(), getDatasetIndex(dataset),
                                            aliasTableMap.get(alias), source.getName(), alias));
                        }
                        valid = false;
                    }
                }
            }
        }

        return valid;

    }

    // PRE
    @Override
    public boolean validateSubselect(Source source) {

        boolean valid = true;

        if (source.isSubSelect()
                && metadataService.isTemporaryTransformation(source.getName())) {
            Integer packageSequence = source.getParent().getParent()
                    .getPackageSequence();
            addWarningMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30000,
                            ERROR_MESSAGE_30000, this.getClass(),
                            source.getName(), getSourceIndex(source)));
            valid = false;
        }

        return valid;
    }

    private int getDatasetIndex(Dataset dataset) {
        List<Dataset> datasets = dataset.getParent().getDatasets();
        for (int i = 0; i < datasets.size(); i++) {
            if (dataset == datasets.get(i)) {
                return i;
            }
        }
        String msg = errorWarningMessages.formatMessage(2090, ERROR_MESSAGE_02090, this.getClass());
        errorWarningMessages.addMessage(dataset.getParent().getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
        throw new RuntimeException(msg);
    }

    private int getSourceIndex(Source source) {
        List<Source> sources = source.getParent().getSources();
        for (int i = 0; i < sources.size(); i++) {
            if (source == sources.get(i)) {
                return i;
            }
        }
        String msg = errorWarningMessages.formatMessage(2100, ERROR_MESSAGE_02100, this.getClass());
        errorWarningMessages.addMessage(
                source.getParent().getParent().getPackageSequence(), msg,
                MESSAGE_TYPE.ERRORS);
        throw new RuntimeException(msg);
    }

    private int getDatasetIndex(Source source) {
        return getDatasetIndex(source.getParent());
    }

    private int getDatasetIndex(Lookup lookup) {
        return getDatasetIndex(lookup.getParent());
    }

    // PRE
    @Override
    public boolean validateSourceDataStore(Source source) {
        List<DataStore> matchingDataStores = metadataService
                .findDataStoreInAllModels(source.getName());

        if (matchingDataStores.isEmpty() && !source.isTemporary()) {
            Integer packageSequence = source.getParent().getParent()
                    .getPackageSequence();

            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30010,
                            ERROR_MESSAGE_30010, this.getClass(), source.getName(),
                            getDatasetIndex(source)));
        }
        return !matchingDataStores.isEmpty();
    }

    @Override
    public boolean validateSourceDataStoreName(Source source) {
        // TODO create own validation messages
        String prefix = source.getName();
        if (prefix.length() > 30 && !source.isTemporary()) {
            // if the source is not temporary we 28 positions + 2 for datasets =
            // 30 oracle max
            addErrorMessage(
                    source.getParent().getParent().getPackageSequence(),
                    errorWarningMessages.formatMessage(10050,
                            ERROR_MESSAGE_10050, this.getClass(), source
                                    .getParent().getParent().getName(), prefix,
                            prefix.length()));
            return false;
        }
        if (prefix.length() > 30 && source.isTemporary()) {
            // if the source is temporary we need 26 positions + 2 for datasets +
            // 2 for journalizing = 30 oracle max.
            addWarningMessage(
                    source.getParent().getParent().getPackageSequence(),
                    errorWarningMessages.formatMessage(10050,
                            ERROR_MESSAGE_10050, this.getClass(), source
                                    .getParent().getParent().getName(), prefix,
                            prefix.length()));
            return true;
        }
        if ((source.getAlias() == null ||
                source.getAlias().length() == 0
        ) && source.getName().contains("$")) {
            addErrorMessage(
                    source.getParent().getParent().getPackageSequence(),
                    errorWarningMessages.formatMessage(30014,
                            ERROR_MESSAGE_30014, this.getClass(), source.getName()));
            return false;
        }
        if (source.getAlias() != null
                && source.getAlias().contains("$")) {
            addErrorMessage(
                    source.getParent().getParent().getPackageSequence(),
                    errorWarningMessages.formatMessage(30014,
                            ERROR_MESSAGE_30014, this.getClass(), source.getName()));
            return false;
        }
        return true;
    }

    // PRE
    @Override
    public boolean validateFilter(Source source) {
        boolean valid = true;
        HashMap<String, String> aliasesToNames = getAllAliases(source.getParent(), true);

        HashMap<String, String> invalidReferencesMap = new HashMap<>();

        int index = getDatasetIndex(source);
        String regex = JodiConstants.ALIAS_DOT_COLUMN_PREFIX_OR_VAR;

        // TODO - may want to extract this to not regenerate pattern each time.
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(source.getFilter());
        boolean referencesParent = source.getFilter() != null
                && source.getFilter().length() > 0 ? false : true;
        while (matcher.find()) {
            String columnName = matcher.group();
            String tableName = columnName.substring(0, columnName.indexOf("."));
            if (tableName.startsWith("#") ||
                    tableName.startsWith(":")
            ) {
                // this is not a table but a variable name
                String variableName = columnName.substring(columnName.indexOf(".") + 1, columnName.length());
                if (!metadataService.projectVariableExists(tableName.substring(1), variableName) && !tableName.toUpperCase().contains("#GLOBAL")) {
                    String msg = errorWarningMessages.formatMessage(30102, ERROR_MESSAGE_30102, this.getClass(), source.getFilter(), source.getAlias(), index, columnName);
                    invalidReferencesMap.put(tableName, msg);
                }
                continue;
            }
            if (aliasesToNames.values().contains(tableName)
                    && !aliasesToNames.keySet().contains(tableName)) {
                // "Filter condition %s of Source %s in Dataset[%s] refers to
                // source %s by name even though it must be referenced by its alias. ";
                invalidReferencesMap.put(
                        tableName,
                        errorWarningMessages.formatMessage(30100,
                                ERROR_MESSAGE_30100, this.getClass(), source.getFilter(),
                                source.getAlias(), index, tableName));
            } else if (!aliasesToNames.keySet().contains(tableName)) {
                invalidReferencesMap.put(
                        tableName,
                        errorWarningMessages.formatMessage(30103,
                                ERROR_MESSAGE_30103, this.getClass(), source.getFilter(),
                                source.getAlias(), index, tableName));
            }

            // Make sure that
            referencesParent |= source.getAlias().equalsIgnoreCase(tableName);
        }
        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();

        if (!referencesParent) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30101,
                            ERROR_MESSAGE_30101, this.getClass(), source.getFilter(),
                            source.getAlias(), index));
            valid = false;
        }

        for (String tableName : invalidReferencesMap.keySet()) {
            addErrorMessage(packageSequence,
                    invalidReferencesMap.get(tableName));
            valid = false;
        }
        return valid;
    }

    @Override
    public boolean validateFilterExecutionLocation(Source source) {
        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();
        int index = getDatasetIndex(source);

        if (ExecutionLocationtypeEnum.TARGET.equals(source
                .getFilterExecutionLocation())) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30121,
                            ERROR_MESSAGE_30121, this.getClass(), source.getAlias(), index,
                            source.getFilterExecutionLocation()));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean validateFilterExecutionLocation(Source source,
                                                   ExecutionLocationStrategy strategy) {
        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();
        int index = getDatasetIndex(source);

        if (ExecutionLocationtypeEnum.TARGET.equals(source
                .getFilterExecutionLocation())
                || source.getFilterExecutionLocation() == null) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30122, ERROR_MESSAGE_30122,
                            this.getClass(), strategy.getClass().getName(),
                            source.getFilterExecutionLocation(),
                            source.getAlias(), index));
            return false;
        } else
            return true;
    }


    /**
     * Return the alias map for a given flow item.  This will either be the parent Source or the previous Flow item.
     *
     * @param flow
     * @return map of aliases to their names (order 1)
     */
    private HashMap<String, String> getAliases(Flow flow) {
        HashMap<String, String> aliases = new HashMap<>();
        Source source = flow.getParent();
        int flowIndex = source.getFlows().indexOf(flow);
        if (flow instanceof SubQuery) {
            String filterAlias = ((SubQuery) flow).getFilterSource();
            aliases.put(filterAlias, ((SubQuery) flow).getParent().getName());
            filterAlias = ((SubQuery) flow).getName();
            aliases.put(filterAlias, ((SubQuery) flow).getParent().getName());
        }
        if (flowIndex == 0) {
            aliases.put(source.getAlias(), source.getName());
        } else {
            Flow previousFlow = source.getFlows().get(flowIndex - 1);
            if (previousFlow instanceof SubQuery) {
                String filterAlias = ((SubQuery) previousFlow).getFilterSource();
                aliases.put(filterAlias, ((SubQuery) previousFlow).getParent().getName());
                filterAlias = ((SubQuery) previousFlow).getName();
                aliases.put(filterAlias, ((SubQuery) previousFlow).getParent().getName());
            }
            aliases.put(previousFlow.getName(), previousFlow.getName());
        }
        return aliases;
    }


    private HashMap<String, String> getAllAliases(Transformation transformation) {
        HashMap<String, String> aliases = new HashMap<>();
        for (Dataset dataset : transformation.getDatasets()) {
            aliases.putAll(getAllAliases(dataset, false));
        }
        return aliases;
    }


    private HashMap<String, String> getAllAliases(Dataset dataset, boolean forFilter) {
        HashMap<String, String> aliases = new HashMap<>();
        for (Source source : dataset.getSources()) {
            if (source.getFlows().size() > 0 && !forFilter) {
                Flow lastFlow = source.getFlows().get(source.getFlows().size() - 1);
                aliases.put(lastFlow.getName(), lastFlow.getName());
            } else {
                aliases.put(source.getAlias(), source.getName());
                for (Lookup lookup : source.getLookups()) {
                    aliases.put(lookup.getAlias(), lookup.getLookupDataStore());
                }
            }

        }
        return aliases;
    }


    // PRE
    @Override
    public boolean validateJoin(Source source) {
        boolean valid = true;
        String regex = JodiConstants.ALIAS_DOT_COLUMN_PREFIX_OR_VAR;
        HashMap<String, String> aliasesToNames = getAllAliases(source.getParent(), false);
        HashMap<String, String> invalidReferencesMap = new HashMap<>();

        int datasetIndex = getDatasetIndex(source);

        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();

        //@TODO
        // add validation for flows.
        if (source.getFlows().size() != 0) {
            return true;
        }

        if (source.getJoin() != null && source.getJoin().length() > 0) {
            // TODO - may want to extract this to not regenerate pattern each
            // time.
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(source.getJoin());
            boolean referencesParent = false;
            while (matcher.find()) {
                String columnName = matcher.group();
                String tableName = columnName.substring(0,
                        columnName.indexOf("."));

                if (tableName.startsWith("#")) {
                    // this is not a table but a variable.
                    continue;
                }
                if (aliasesToNames.values().contains(tableName)
                        && !aliasesToNames.keySet().contains(tableName)) {
                    invalidReferencesMap
                            .put(tableName,
                                    errorWarningMessages.formatMessage(30200,
                                            ERROR_MESSAGE_30200, this.getClass(), source.getJoin(),
                                            source.getAlias(), datasetIndex,
                                            tableName));
                } else if (!aliasesToNames.keySet().contains(tableName)) {
                    invalidReferencesMap
                            .put(tableName,
                                    errorWarningMessages.formatMessage(30204,
                                            ERROR_MESSAGE_30204, this.getClass(), source.getJoin(),
                                            source.getAlias(), datasetIndex,
                                            tableName));
                }

                referencesParent |= source.getAlias().equalsIgnoreCase(
                        tableName);
            }

            if (!referencesParent) {
                addErrorMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(30201,
                                ERROR_MESSAGE_30201, this.getClass(), source.getJoin(), source.getAlias(),
                                datasetIndex));
                valid = false;
            }

            if (source.getJoin() != null && source.getJoin().length() > 0) {
                if (JoinTypeEnum.CROSS.equals(source.getJoinType())) {
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30213,
                                    ERROR_MESSAGE_30213, this.getClass(), source.getName(), datasetIndex,
                                    source.getJoin()));
                    valid = false;
                } else if (JoinTypeEnum.NATURAL.equals(source.getJoinType())) {
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30214,
                                    ERROR_MESSAGE_30214, this.getClass(), source.getName(), datasetIndex,
                                    source.getJoin()));
                    valid = false;
                }
            }

        } else if (JoinTypeEnum.INNER.equals(source.getJoinType())
                || JoinTypeEnum.LEFT_OUTER.equals(source.getJoinType())) {
            if (getSourceIndex(source) != 0) {
                addErrorMessage(packageSequence,
                        errorWarningMessages.formatMessage(30206,
                                ERROR_MESSAGE_30206, this.getClass(), "", source.getName(), datasetIndex));
                valid = false;
            }
        }
        if (source.getJoinType() != null && source.getJoinType().equals(JoinTypeEnum.LEFT_OUTER) && source.getFilter() != null && source.getFilter().length() > 0 && etlSubSystemVersion.isVersion11()) {
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(30104,
                            ERROR_MESSAGE_30104, this.getClass(), source.getFilter(), source.getName(), datasetIndex));
            valid = false;
        }

        for (String tableName : invalidReferencesMap.keySet()) {
            addErrorMessage(packageSequence,
                    invalidReferencesMap.get(tableName));
            valid = false;
        }
        return valid;
    }

    @Override
    public boolean validateJoinExecutionLocation(Source source) {
        boolean valid = true;
        int datasetIndex = getDatasetIndex(source);
        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();

        if (ExecutionLocationtypeEnum.TARGET.equals(source
                .getJoinExecutionLocation())) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30221,
                            ERROR_MESSAGE_30221, this.getClass(), source.getName(), datasetIndex,
                            source.getJoinExecutionLocation()));
            valid = false;
        }

        return valid;
    }

    @Override
    public boolean validateJoinExecutionLocation(Source source,
                                                 ExecutionLocationStrategy strategy) {
        boolean valid = true;
        int datasetIndex = getDatasetIndex(source);
        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();
        if (ExecutionLocationtypeEnum.TARGET.equals(source
                .getJoinExecutionLocation())
                || source.getJoinExecutionLocation() == null) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30222,
                            ERROR_MESSAGE_30222, this.getClass(), strategy.getClass().getName(),
                            source.getJoinExecutionLocation(),
                            source.getName(), datasetIndex));
            valid = false;
        }

        return valid;
    }

    // PRE
    @Override
    public boolean validateLKM(Source source) {
        int datasetIndex = getDatasetIndex(source);
        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();

        List<KMValidation> errors = validateKM(source.getLkm());
        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:

                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30300,
                                    ERROR_MESSAGE_30300, this.getClass(), error.values[0], source.getAlias(),
                                    datasetIndex));
                    break;
                case NOT_LOADED:
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30305,
                                    ERROR_MESSAGE_30305, this.getClass(), error.values[0], source.getName(),
                                    datasetIndex));
                    break;
                case INVALID_OPTION:
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30301,
                                    ERROR_MESSAGE_30301, this.getClass(), error.values[0], source.getName(),
                                    datasetIndex));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30302,
                                    ERROR_MESSAGE_30302, this.getClass(), error.values[1], error.values[0],
                                    source.getName(), datasetIndex));
                    break;
            }
        }

        return errors.isEmpty();
    }

    @Override
    public boolean validateLKM(Source source, KnowledgeModuleStrategy strategy) {
        int datasetIndex = getDatasetIndex(source);
        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();

        List<KMValidation> errors = validateKM(source.getLkm());
        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:

                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30306,
                                    ERROR_MESSAGE_30306, this.getClass(), strategy.getClass().getName(),
                                    error.values[0], source.getAlias(),
                                    datasetIndex));
                    break;
                case NOT_LOADED:
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30305,
                                    ERROR_MESSAGE_30305, this.getClass(), error.values[0], source.getName(),
                                    datasetIndex));
                    break;
                case INVALID_OPTION:
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30307,
                                    ERROR_MESSAGE_30307, this.getClass(), strategy.getClass().getName(),
                                    error.values[0], source.getName(), datasetIndex));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(30308,
                                    ERROR_MESSAGE_30308, this.getClass(), strategy.getClass().getName(),
                                    error.values[1], error.values[0],
                                    source.getName(), datasetIndex));
                    break;
            }
        }

        return errors.isEmpty();
    }

    // PRE
    @Override
    public boolean validateLookup(Lookup lookup) {
        boolean valid = true;
        int datasetIndex = getDatasetIndex(lookup);

        if (metadataService.isTemporaryTransformation(lookup
                .getLookupDataStore()) && lookup.isSubSelect()) {
            Integer packageSequence = lookup.getParent().getParent()
                    .getParent().getPackageSequence();

            addWarningMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(31000,
                            ERROR_MESSAGE_31000, this.getClass(), lookup.getAlias(), lookup.getParent()
                                    .getAlias(), datasetIndex));
        }
        valid &= validateLookupType(lookup);

        return valid;
    }

    @Override
    public boolean validateLookupName(Lookup lookup) {
        // TODO own error code
        String prefixLkup = lookup.getLookupDataStore();
        if (prefixLkup.length() > 30 && !lookup.isTemporary()) {
            // if the lookup is not temporary we need 28 positions + 2 for
            // datasets = 30 oracle max
            addErrorMessage(lookup.getParent().getParent().getParent()
                    .getPackageSequence(), errorWarningMessages.formatMessage(
                    10050, ERROR_MESSAGE_10050, this.getClass(), lookup
                            .getParent().getParent().getParent().getName(),
                    prefixLkup, prefixLkup.length()));
            return false;
        }
        if (prefixLkup.length() > 30 && lookup.isTemporary()) {
            // if the lookup is temporary we need 26 positions + 2 for
            // journalizing + 2 for datasets = 30 oracle max
            addWarningMessage(lookup.getParent().getParent().getParent()
                    .getPackageSequence(), errorWarningMessages.formatMessage(
                    10050, ERROR_MESSAGE_10050, this.getClass(), lookup
                            .getParent().getParent().getParent().getName(),
                    prefixLkup, prefixLkup.length()));
            return true;
        }
        if (lookup.getAlias() == null && lookup.getLookupDataStore().contains("$")) {
            addErrorMessage(
                    lookup.getParent().getParent().getParent().getPackageSequence(),
                    errorWarningMessages.formatMessage(30014,
                            ERROR_MESSAGE_30014, this.getClass(), lookup.getLookupDataStore()));
            return false;
        }
        if (lookup.getAlias() != null && lookup.getAlias().contains("$")) {
            addErrorMessage(
                    lookup.getParent().getParent().getParent().getPackageSequence(),
                    errorWarningMessages.formatMessage(30014,
                            ERROR_MESSAGE_30014, this.getClass(), lookup.getLookupDataStore()));
            return false;
        }
        return true;
    }

    // pre
    @Override
    public boolean validateIKM(Mappings mappings) {
        List<KMValidation> errors = validateKM(mappings.getIkm());
        Integer packageSequence = mappings.getParent().getPackageSequence();

        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40110,
                                    ERROR_MESSAGE_40110, this.getClass(), error.values[0]));
                    break;
                case NOT_LOADED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40100,
                                    ERROR_MESSAGE_40100, this.getClass(), error.values[0], error.values[1]));
                    break;
                case INVALID_OPTION:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40111,
                                    ERROR_MESSAGE_40111, this.getClass(), error.values[0]));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40112,
                                    ERROR_MESSAGE_40112, this.getClass(), error.values[1], error.values[0]));
                    break;
            }
        }

        return errors.isEmpty();
    }

    @Override
    public boolean validateIKM(Mappings mappings,
                               KnowledgeModuleStrategy strategy) {
        List<KMValidation> errors = validateKM(mappings.getIkm());
        Integer packageSequence = mappings.getParent().getPackageSequence();

        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40140,
                                    ERROR_MESSAGE_40140, this.getClass(), error.values[0]));
                    break;
                case NOT_LOADED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40100,
                                    ERROR_MESSAGE_40100, this.getClass(), error.values[0], error.values[1]));
                    break;
                case INVALID_OPTION:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40141,
                                    ERROR_MESSAGE_40141, this.getClass(), error.values[0]));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40142,
                                    ERROR_MESSAGE_40142, this.getClass(), error.values[1], error.values[0]));
                    break;
            }
        }

        return errors.isEmpty();
    }

    @Override
    public boolean validateCKM(Mappings mappings) {
        List<KMValidation> errors = validateKM(mappings.getCkm());
        Integer packageSequence = mappings.getParent().getPackageSequence();

        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40210,
                                    ERROR_MESSAGE_40210, this.getClass(), error.values[0]));
                    break;
                case NOT_LOADED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40200,
                                    ERROR_MESSAGE_40200, this.getClass(), error.values[0], error.values[1]));
                    break;
                case INVALID_OPTION:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40211,
                                    ERROR_MESSAGE_40211, this.getClass(), error.values[0]));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40212,
                                    ERROR_MESSAGE_40212, this.getClass(), error.values[1], error.values[0]));
                    break;
            }
        }

        return errors.isEmpty();
    }

    @Override
    public boolean validateCKM(Mappings mappings,
                               KnowledgeModuleStrategy strategy) {
        List<KMValidation> errors = validateKM(mappings.getCkm());
        Integer packageSequence = mappings.getParent().getPackageSequence();

        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40240,
                                    ERROR_MESSAGE_40240, this.getClass(), error.values[0]));
                    break;
                case NOT_LOADED:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40200,
                                    ERROR_MESSAGE_40200, this.getClass(), error.values[0], error.values[1]));
                    break;
                case INVALID_OPTION:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40241,
                                    ERROR_MESSAGE_40241, this.getClass(), error.values[0]));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(40242,
                                    ERROR_MESSAGE_40242, this.getClass(), error.values[1], error.values[0]));
                    break;
            }
        }

        return errors.isEmpty();
    }


    private List<KMValidation> validateKM(KmType km) {
        ArrayList<KMValidation> errors = new ArrayList<>();


        if (km != null && km.getName() != null) {
            String kmName = getProperty(km.getName() + ".name");
            if (kmName == null) {
                errors.add(new KMValidation(KMValidationEnum.UNDEFINED, km
                        .getName()));
            } else {
                errors.addAll(validateKM(km.getName(), kmName, km.getOptions()));
            }
        }

        return errors;
    }


    private List<KMValidation> validateKM(String code, String name, Map<String, String> options) {
        ArrayList<KMValidation> errors = new ArrayList<>();
        //  TODO check Boolean option type CHOICE and other types.
       // return errors;
		KnowledgeModule referenceKm = getKnowledgeModule(name);
		if (referenceKm != null) {
			for (String option : options.keySet()) {
				KnowledgeModule.KMOptionType referenceType = referenceKm.getOptions()
						.get(option);
				String optionValue = options.get(option);

				if (referenceType != null) {
                    switch (referenceType) {
                        case CHECKBOX:
                            if (!optionValue.toUpperCase().equalsIgnoreCase("TRUE")
                                    && !optionValue.toUpperCase().equalsIgnoreCase(
                                    "FALSE")) {
                                errors.add(new KMValidation(
                                        KMValidationEnum.INVALID_OPTION_VALUE,
                                        option, optionValue));
                            }
                            break;
                        case SHORT_TEXT:
                            if (optionValue.length() > 250) {
                                errors.add(new KMValidation(
                                        KMValidationEnum.INVALID_OPTION_VALUE,
                                        option, optionValue));
                            }
                            break;
                        case LONG_TEXT:
                            break;
                        case CHOICE:
                            break;
                    }
				} else {
					errors.add(new KMValidation(
							KMValidationEnum.INVALID_OPTION, option));
				}
			}
		} else {
			errors.add(new KMValidation(KMValidationEnum.NOT_LOADED, code + ".name", name));
		}
         return errors;
    }

    KnowledgeModule getKnowledgeModule(String kmName) {
        for (KnowledgeModule km : etlSubsystemProvider.getKMs()) {
            if (km.getName().equalsIgnoreCase(kmName)) {
                return km;
            }
        }

        return null;
    }

    private String getProperty(String propertyName) {
        try {
            return properties.getProperty(propertyName);
        } catch (JodiPropertyNotFoundException e) {
            return null;
        }
    }


    @Override
    public boolean validateJoinEnriched(Source source) {

        if (source.getFlows().size() > 0) return true;

        Dataset dataset = source.getParent();
        int datasetIndex = getDatasetIndex(source);
        Integer packageSequence = source.getParent().getParent().getPackageSequence();
        List<ExpressionError> errors = validateExpressionSources(source.getJoin(), source, null, false);
        errors.addAll(validateJoinTypes(source.getJoin(), dataset));

        for (ExpressionError error : errors) {
            switch (error.error) {
                case DATASTORE:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(30230,
                                    ERROR_MESSAGE_30230, this.getClass(), source.getAlias(),
                                    datasetIndex, error.values[0]));
                    break;
                case COLUMN:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(30231,
                                    ERROR_MESSAGE_30231, this.getClass(), source.getAlias(),
                                    datasetIndex, error.values[1], error.values[0]));
                    break;
                case JOIN_TYPE:
                    addWarningMessage(packageSequence,
                            errorWarningMessages.formatMessage(30232,
                                    ERROR_MESSAGE_30232, this.getClass(), source.getAlias(),
                                    datasetIndex, error.values[0],
                                    error.values[1]));
                    break;
                default:
                    break;
            }
        }

        return errors.size() == 0;
    }


    @Override
    public boolean validateFilterEnriched(Source source) {
        Dataset dataset = source.getParent();
        int datasetIndex = getDatasetIndex(source);
        Integer packageSequence = dataset.getParent().getPackageSequence();

        List<ExpressionError> errors = validateExpressionSources(source.getFilter(), source, null, true);

        for (ExpressionError error : errors) {
            switch (error.error) {
                case DATASTORE:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(30130,
                                    ERROR_MESSAGE_30130, this.getClass(), source.getAlias(),
                                    datasetIndex, error.values[0]));
                    break;
                case COLUMN:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(30131,
                                    ERROR_MESSAGE_30131, this.getClass(), source.getAlias(),
                                    datasetIndex, error.values[1], error.values[0]));
                    break;
                default:
                    break;
            }
        }

        return errors.size() == 0;
    }


    @Override
    public boolean validateJoinEnriched(Lookup lookup) {

        int datasetIndex = getDatasetIndex(lookup);
        Source source = lookup.getParent();
        Dataset dataset = source.getParent();
        Integer packageSequence = dataset.getParent().getPackageSequence();
        List<ExpressionError> errors = validateExpressionSources(lookup.getJoin(), lookup.getParent(), lookup, false);
        errors.addAll(validateJoinTypes(lookup.getJoin(), dataset));
        for (ExpressionError error : errors) {
            switch (error.error) {
                case DATASTORE:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(31230,
                                    ERROR_MESSAGE_31230, this.getClass(),
                                    lookup.getLookupDataStore(), source.getAlias(),
                                    datasetIndex, error.values[0]));
                    break;
                case COLUMN:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(31231,
                                    ERROR_MESSAGE_31231, this.getClass(),
                                    lookup.getLookupDataStore(), source.getAlias(),
                                    datasetIndex, error.values[1], error.values[0]));
                    break;
                case JOIN_TYPE:
                    addWarningMessage(packageSequence,
                            errorWarningMessages.formatMessage(31232,
                                    ERROR_MESSAGE_31232, this.getClass(),
                                    lookup.getLookupDataStore(), source.getAlias(),
                                    datasetIndex, error.values[0],
                                    error.values[1]));
                    break;
                default:
                    break;
            }
        }

        return errors.size() == 0;
    }

    private List<ExpressionError> validateExpressionSources(String expression, Source source, Lookup lookup, boolean isFilter) {
        ArrayList<ExpressionError> errors = new ArrayList<>();
        Dataset dataset = source != null ? source.getParent() : lookup.getParent().getParent();
        HashMap<String, String> aliasMap = getAllAliases(dataset, isFilter);

        String regex = JodiConstants.ALIAS_DOT_COLUMN_PREFIX;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(expression + "");
        while (matcher.find()) {
            String alias = matcher.group(1);
            String column = matcher.group(3);
            String name = aliasMap.get(alias);
            if (name != null) {

                try {
                    DataStore dataStore = findSourceDataStore(name, dataset);

                    if (!dataStore.isTemporary()) {
                        findSourceDataStoreColumn(dataStore, column, dataset.getParent().getPackageSequence());
                    }
                } catch (DataStoreNotInModelException de) {
                    errors.add(new ExpressionError(ExpressionErrorEnum.DATASTORE, name));
                } catch (ColumnNotInDataStoreException ce) {
                    errors.add(new ExpressionError(ExpressionErrorEnum.COLUMN, name, column));
                } catch (RuntimeException re) {
                    errors.add(new ExpressionError(ExpressionErrorEnum.DATASTORE, name));
                }
            }
        }

        return errors;
    }

    private List<ExpressionError> validateJoinTypes(String join, Dataset dataset) {

        ArrayList<ExpressionError> errors = new ArrayList<>();
        if (dataset.getSources().get(0).getFlows().size() > 0) {
            return errors;
        }
        String regex = "([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)\\s*=\\s*([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)";

        HashMap<String, String> scopeAliases = getAllAliases(dataset, false);

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(join + "");
        while (matcher.find()) {
            String leftAlias = matcher.group(1);
            String leftColumn = matcher.group(3);
            String rightAlias = matcher.group(4);
            String rightColumn = matcher.group(6);

            try {
                if (leftAlias.startsWith("#") || rightAlias.startsWith("#")) {
                    continue;
                }

                DataStore leftDataStore = findSourceDataStore(
                        scopeAliases.get(leftAlias), dataset);

                DataStoreColumn leftDataStoreColumn = findSourceDataStoreColumn(
                        leftDataStore, leftColumn, dataset.getParent().getPackageSequence());

                DataStore rightDataStore = findSourceDataStore(
                        scopeAliases.get(rightAlias), dataset);

                DataStoreColumn rightDataStoreColumn = findSourceDataStoreColumn(
                        rightDataStore, rightColumn, dataset.getParent().getPackageSequence());

                if (leftDataStoreColumn != null && rightDataStoreColumn != null && !leftDataStoreColumn.getColumnDataType().equals(rightDataStoreColumn.getColumnDataType())) {
                    errors.add(new ExpressionError(ExpressionErrorEnum.JOIN_TYPE,
                            leftAlias + "." + leftColumn, rightAlias + "." + rightColumn));
                }
            } catch (DataStoreNotInModelException | ColumnNotInDataStoreException de) {
                // validation should have already been performed on datastore existance.
            }
        }


        return errors;
    }

    @Override
    public boolean validateTargetColumn(Targetcolumn targetColumn) {
        Mappings mappings = targetColumn.getParent();
        Integer packageSequence = mappings.getParent().getPackageSequence();
        int errorCount = errors.get(packageSequence) != null ? errors.get(
                packageSequence).size() : 0;
        boolean temporary = mappings.getParent().isTemporary();
        DataStore targetDataStore = null;
        DataStoreColumn targetDataStoreColumn = null;

        if (!temporary) {
            targetDataStore = metadataService
                    .getTargetDataStoreInModel(mappings);
            targetDataStoreColumn = targetDataStore.getColumns().get(
                    targetColumn.getName());

            if (targetDataStoreColumn == null) {
                addErrorMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(41000,
                                ERROR_MESSAGE_41000, this.getClass(), targetColumn.getName(),
                                mappings.getTargetDataStore()));
            }

            if (targetColumn.getLength() > 0 || targetColumn.getScale() > 0
                    || targetColumn.getDataType() != null) {
                addWarningMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(41030,
                                ERROR_MESSAGE_41030, this.getClass(), targetColumn.getName(),
                                mappings.getTargetDataStore()));
            }
        } else {
            if ((targetColumn.getDataType() == null || targetColumn
                    .getDataType().length() == 0)
                    && targetColumn.getLength() == 0
                    && targetColumn.getScale() == 0) {
                addErrorMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(41031,
                                ERROR_MESSAGE_41031, this.getClass(), targetColumn.getName(),
                                mappings.getTargetDataStore()));
            } else if (targetColumn.getDataType() == null
                    || targetColumn.getDataType().length() == 0) {
                addErrorMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(41032,
                                ERROR_MESSAGE_41032, this.getClass(), targetColumn.getName(),
                                mappings.getTargetDataStore()));
            }
        }

        if (targetColumn.getMappingExpressions().size() == 0 && this.devMode) {
            addWarningMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(41003,
                            ERROR_MESSAGE_41003, this.getClass(), targetColumn.getName(),
                            mappings.getTargetDataStore()));
        } else if (targetColumn.getMappingExpressions().size() != mappings
                .getParent().getDatasets().size()) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(41004,
                            ERROR_MESSAGE_41004, this.getClass(), targetColumn.getName(), mappings
                                    .getTargetDataStore(), mappings.getParent()
                                    .getDatasets().size(), targetColumn
                                    .getMappingExpressions().size()));
        }

        if ((targetDataStore != null && targetDataStore.getDataStoreType() != null && (targetDataStore.getDataStoreType().equals(DataStoreType.DIMENSION)
                || targetDataStore.getDataStoreType().equals(DataStoreType.SLOWLY_CHANGING_DIMENSION))
                && this.properties.getPropertyKeys().contains("jodi.etl.insert_date")
                && targetColumn.getName().equals(this.properties.getProperty("jodi.etl.insert_date"))

        )) {
            addWarningMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(42001,
                            ERROR_MESSAGE_42001, this.getClass(), targetColumn.getName(), "jodi.etl.insert_date",
                            targetColumn.getParent().getTargetDataStore()));
        }
        if ((targetDataStore != null && targetDataStore.getDataStoreType() != null && (targetDataStore.getDataStoreType().equals(DataStoreType.DIMENSION)
                || targetDataStore.getDataStoreType().equals(DataStoreType.SLOWLY_CHANGING_DIMENSION))
                && this.properties.getPropertyKeys().contains("jodi.etl.etl_proc_wid")
                && targetColumn.getName().equals(this.properties.getProperty("jodi.etl.insert_date"))
        )) {
            addWarningMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(42001,
                            ERROR_MESSAGE_42001, this.getClass(), targetColumn.getName(), "jodi.etl.etl_proc_wid",
                            targetColumn.getParent().getTargetDataStore()));
        }

        HashMap<String, String> allAliases = getAllAliases(mappings.getParent());
        int index = 0;

        for (String expression : targetColumn.getMappingExpressions()) {
            if (mappings.getParent().getDatasets().size() <= index) continue;

            String regex = JodiConstants.ALIAS_DOT_COLUMN_PREFIX;
            ;

            // TODO - may want to extract this to not regenerate pattern each
            // time.
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(expression);

            if (expression == null || expression.length() < 1) {
                addErrorMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(41013,
                                ERROR_MESSAGE_41013, this.getClass(), index, targetColumn.getName(),
                                mappings.getTargetDataStore()));
                continue;
            } else if (expression.toUpperCase().contains("NEXTVAL")) {
                continue;
            }

            while (matcher.find()) {
                String aliasAndColumn = matcher.group();
                String alias = aliasAndColumn.substring(0,
                        aliasAndColumn.indexOf("."));
                String column = aliasAndColumn.substring(aliasAndColumn
                        .indexOf(".") + 1);
                HashMap<String, String> scopeAliases = getAllAliases(mappings.getParent().getDatasets().get(index), false);
                if (alias.startsWith("#")
                        || aliasAndColumn.toLowerCase().contains("out.print")
                        || alias.toLowerCase().contains("odiref")
                        || (this.properties != null &&
                        this.properties.getProjectCode() != null &&
                        alias != null &&
                        alias.toLowerCase().equalsIgnoreCase(this.properties.getProjectCode().toLowerCase()))) {
                    // alias is a variable or
                    // a workaround for analytical functions is implemented with out.print
                    // e.g. NVL(<?out.print("SUM");?>( S_INVOICELINE_I.INVL_AMOUNT ) OVER ( PARTITION BY S_INVOICELINE_I.INVL_CUST_CODE , SUBSTR( S_INVOICELINE_I.INVL_INVE_DATE_CODE , 0, 4) ORDER BY S_INVOICELINE_I.INVL_INVE_DATE_CODE, S_INVOICELINE_I.INVL_LINE_CODE ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING ) ,0)
                    // or alias is a odi api
                    continue;
                }
                if (!scopeAliases.containsKey(alias)
                        && scopeAliases.containsValue(alias)) {
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(41010,
                                    ERROR_MESSAGE_41010, this.getClass(),
                                    index, targetColumn.getName(),
                                    mappings.getTargetDataStore(), alias));
                } else if (!scopeAliases.containsKey(alias)
                        && allAliases.containsKey(alias)) {
                    addErrorMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(41011,
                                    ERROR_MESSAGE_41011, this.getClass(),
                                    index, targetColumn.getName(),
                                    mappings.getTargetDataStore(), alias));
                } else if (!allAliases.containsKey(alias) && !expression.contains("odiRef.")) {
                    addWarningMessage(
                            packageSequence,
                            errorWarningMessages.formatMessage(41016,
                                    ERROR_MESSAGE_41016, this.getClass(),
                                    index, targetColumn.getName(),
                                    mappings.getTargetDataStore(), alias));

                } else {
                    String tableName = scopeAliases.get(alias);
                    if (tableName == null) {
                        continue;
                    }
                    DataStore sourceDataStore = findSourceDataStore(tableName,
                            mappings.getParent().getDatasets().get(index));

                    DataStoreColumn sourceDataStoreColumn = sourceDataStore
                            .getColumns().get(column);

                    if (sourceDataStoreColumn != null
                            && targetDataStoreColumn != null) {
                        // For simple expressions make sure type, scale and
                        // length are acceptable.
                        if (aliasAndColumn.equals(expression)) {
                            if (targetDataStoreColumn.getLength() < sourceDataStoreColumn
                                    .getLength()
                                    || targetDataStoreColumn.getScale() < sourceDataStoreColumn
                                    .getScale()) {
                                addWarningMessage(
                                        packageSequence,
                                        errorWarningMessages.formatMessage(41002,
                                                ERROR_MESSAGE_41002, this.getClass(),
                                                targetColumn.getName(),
                                                mappings.getTargetDataStore()));
                            }

                            if (!targetDataStoreColumn.getColumnDataType()
                                    .equals(sourceDataStoreColumn
                                            .getColumnDataType())) {
                                addWarningMessage(
                                        packageSequence,
                                        errorWarningMessages.formatMessage(41001,
                                                ERROR_MESSAGE_41001, this.getClass(),
                                                targetColumn.getName(),
                                                mappings.getTargetDataStore(),
                                                column, tableName));
                            }
                        }
                    } else if (sourceDataStoreColumn != null && temporary) {
                        if (aliasAndColumn.equals(expression)) {
                            if ((targetColumn.getLength() != 0 && targetColumn
                                    .getLength() < sourceDataStoreColumn
                                    .getLength())
                                    || (targetColumn.getScale() != 0 && targetColumn
                                    .getScale() < sourceDataStoreColumn
                                    .getScale())) {
                                addWarningMessage(
                                        packageSequence,
                                        errorWarningMessages.formatMessage(41002,
                                                ERROR_MESSAGE_41002, this.getClass(),
                                                targetColumn.getName(),
                                                mappings.getTargetDataStore()));
                            }

                            if (targetColumn.getDataType() != null
                                    && targetColumn.getDataType().length() > 0
                                    && !targetColumn.getDataType().equals(
                                    sourceDataStoreColumn
                                            .getColumnDataType())) {
                                addWarningMessage(
                                        packageSequence,
                                        errorWarningMessages.formatMessage(41001,
                                                ERROR_MESSAGE_41001, this.getClass(),
                                                targetColumn.getName(),
                                                mappings.getTargetDataStore(),
                                                column, tableName));
                            }
                        }
                    } else if (sourceDataStoreColumn == null && !sourceDataStore.isTemporary()) {
                        addErrorMessage(
                                packageSequence,
                                errorWarningMessages.formatMessage(41012,
                                        ERROR_MESSAGE_41012, this.getClass(), index,
                                        targetColumn.getName(),
                                        mappings.getTargetDataStore(),
                                        column, tableName));
                    }
                }
            }
            index++;
        }

        return errors.get(packageSequence) != null ? errors
                .get(packageSequence).size() == errorCount : true;
    }

    @Override
    public boolean validateLookupJoin(Lookup lookup) {
        boolean valid = true;
        String sourceAlias = lookup.getParent().getAlias().toLowerCase();
        String sourceName = lookup.getParent().getName();
        String regex = JodiConstants.ALIAS_DOT_COLUMN_PREFIX;

        HashMap<String, String> aliasesToNames = getAllAliases(lookup.getParent().getParent(), false);

        HashMap<String, String> invalidReferencesMap = new HashMap<>();

        int datasetIndex = getDatasetIndex(lookup);

        Integer packageSequence = lookup.getParent().getParent().getParent().getPackageSequence();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lookup.getJoin());
        boolean referencesParent = false;
        while (matcher.find()) {
            String columnName = matcher.group();
            String tableName = columnName.substring(0,
                    columnName.indexOf("."));
            referencesParent |= sourceAlias.equalsIgnoreCase(tableName.toLowerCase());
            if (!sourceAlias.equalsIgnoreCase(tableName.toLowerCase())
                    && !lookup.getAlias().toLowerCase().equalsIgnoreCase(tableName.toLowerCase())) {
                if (sourceName.toLowerCase().equalsIgnoreCase(tableName.toLowerCase())
                        || lookup.getLookupDataStore().toLowerCase().equalsIgnoreCase(tableName.toLowerCase())) {
                    invalidReferencesMap.put(tableName.toLowerCase(),
                            errorWarningMessages.formatMessage(31200,
                                    ERROR_MESSAGE_31200, this.getClass(), lookup.getJoin(),
                                    lookup.getLookupDataStore(), sourceName,
                                    datasetIndex, tableName));
                } else if (aliasesToNames.values().contains(tableName) && !aliasesToNames.keySet().contains(tableName)) {
                    invalidReferencesMap.put(tableName.toLowerCase(),
                            errorWarningMessages.formatMessage(31203,
                                    ERROR_MESSAGE_31203, this.getClass(), lookup.getJoin(),
                                    lookup.getLookupDataStore(), sourceName,
                                    datasetIndex, tableName));
                } else if (lookup.getParent().getFlows().size() == 0) {
                    invalidReferencesMap.put(tableName.toLowerCase(),
                            errorWarningMessages.formatMessage(31202,
                                    ERROR_MESSAGE_31202, this.getClass(), lookup.getJoin(),
                                    lookup.getLookupDataStore(), sourceName,
                                    datasetIndex, tableName));
                }
            }
        }

        if (!referencesParent && lookup.getParent().getFlows().size() == 0) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(31201,
                            ERROR_MESSAGE_31201, this.getClass(), lookup.getJoin(),
                            lookup.getLookupDataStore(), sourceName,
                            datasetIndex));
            valid = false;
        }

        for (String tableName : invalidReferencesMap.keySet()) {
            addErrorMessage(packageSequence,
                    invalidReferencesMap.get(tableName));
            valid = false;
        }

        return valid;
    }

    private DataStoreColumn findSourceDataStoreColumn(DataStore dataStore, String name, int packageSequence) {
        for (String key : dataStore.getColumns().keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return dataStore.getColumns().get(key);
            }
        }
        if (!dataStore.isTemporary()) {
            String msg = errorWarningMessages.formatMessage(2110, ERROR_MESSAGE_02110, this.getClass(), dataStore.getDataStoreName(), name);
            //errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
            throw new ColumnNotInDataStoreException(msg);
        } else {
            return null;
        }
    }

    private DataStoreColumn createDataStoreColumn(final DataStore ds, final OutputAttribute oa, final int index) {
        return new DataStoreColumn() {
            @Override
            public DataStore getParent() {
                return ds;
            }

            @Override
            public String getName() {
                return oa.getName();
            }

            @Override
            public String getColumnDataType() {
                return null;
            }

            @Override
            public int getLength() {
                return 0;
            }

            @Override
            public int getScale() {
                return 0;
            }

            @Override
            public SCDType getColumnSCDType() {
                return null;
            }

            @Override
            public boolean hasNotNullConstraint() {
                return false;
            }

            @Override
            public String getDescription() {
                return "NA";
            }

            @Override
            public int getPosition() {
                return index;
            }
        };
    }

    // Pivot and Unpivot can be treated like datastores with columns and column properties.  TODO add column properties if/when they become added to OutputAttribute.
    private Map<String, DataStoreColumn> createDataStoreColumns(DataStore ds, List<OutputAttribute> oas) {
        LinkedHashMap<String, DataStoreColumn> map = new LinkedHashMap<>();
        for (OutputAttribute oa : oas) {
            map.put(oa.getName(), createDataStoreColumn(ds, oa, oas.indexOf(oa)));
        }
        return map;
    }

    private DataStore createDataStore(final Flow flow) {
        return new DataStore() {
            @Override
            public String getDataStoreName() {
                return flow.getName();
            }

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public Map<String, DataStoreColumn> getColumns() {
                return createDataStoreColumns(this, flow.getOutputAttributes());
            }

            @Override
            public DataStoreType getDataStoreType() {
                return DataStoreType.UNKNOWN;
            }

            @Override
            public List<DataStoreKey> getDataStoreKeys() {
                return Collections.emptyList();
            }

            @Override
            public DataStoreKey getPrimaryKey() {
                return null;
            }

            @Override
            public List<DataStoreForeignReference> getDataStoreForeignReference() {
                return null;
            }

            @Override
            public Map<String, Object> getDataStoreFlexfields() {
                return null;
            }

            @Override
            public String getDescription() {
                return "N/A";
            }

            @Override
            public DataModel getDataModel() {
                return null;
            }

            @Override
            public DataStoreKey getAlternateKey() {
                return null;
            }
        };
    }

    private DataStore findSourceDataStore(String name, Dataset dataset) {
        StringBuffer sourceNames = new StringBuffer();
        for (Source source : dataset.getSources()) {
            sourceNames.append(source.getName()).append(" ");
            if (source.getName().equalsIgnoreCase(name)) {
                return metadataService.getSourceDataStoreInModel(
                        source.getName(), source.getModel());
            } else {
                for (Lookup lookup : source.getLookups()) {
                    if (lookup.getLookupDataStore().equalsIgnoreCase(name)) {
                        return metadataService.getSourceDataStoreInModel(lookup.getLookupDataStore(), lookup.getModel());
                    }
                }
            }
        }

        for (Dataset d : dataset.getParent().getDatasets()) {
            for (Source source : d.getSources()) {
                for (Flow flow : source.getFlows()) {
                    if (name.equalsIgnoreCase(flow.getName())) {
                        return createDataStore(flow);
                    }
                }
            }
        }

        String msg = errorWarningMessages.formatMessage(2120, ERROR_MESSAGE_02120, this.getClass(), name, sourceNames);
        errorWarningMessages.addMessage(dataset.getParent().getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
        throw new RuntimeException(msg);
    }

    @Override
    public void handleTransformationName(Exception e,
                                         Transformation transformation) {
        int packageSequence = transformation.getPackageSequence();
        StackTraceElement ste = e.getStackTrace()[0];
        addErrorMessage(
                packageSequence,
                errorWarningMessages.formatMessage(10020,
                        ERROR_MESSAGE_10020, this.getClass(), transformation.getPackageSequence() + "",
                        ste != null ? ste.getClassName() : "unknown"));
    }

    @Override
    public void handleFolderName(Exception e, Transformation transformation) {
        int packageSequence = transformation.getPackageSequence();
        StackTraceElement ste = e.getStackTrace()[0];
        addErrorMessage(
                packageSequence,
                errorWarningMessages.formatMessage(10030,
                        ERROR_MESSAGE_10030, this.getClass(), transformation.getPackageSequence() + "",
                        ste != null ? ste.getClassName() : "unknown"));
    }


//	private DataStore findFlowDataStore(String name, Transformation transformation) {
//		for(Dataset dataset : transformation.getDatasets()) {
//			for(Source source : dataset.getSources()) {
//				for(Flow flow : source.getFlows()) {
//					if(name.equalsIgnoreCase(flow.getName())) {
//						return createDataStore(flow);
//					}
//				}
//			}
//		}
//		return null;
//	}
//	

    @Override
    public void handleModelCode(Exception e, Source source) {

        Integer packageSequence = source.getParent().getParent()
                .getPackageSequence();
        int datasetIndex = getDatasetIndex(source);

        if (e instanceof DataStoreNotInModelException) {
            if (source.getModel() != null && source.getModel().length() > 0) {
                addErrorMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(30013,
                                ERROR_MESSAGE_30013, this.getClass(), source.getName(), datasetIndex,
                                source.getModel()));
            } else { // derived
                addErrorMessage(
                        packageSequence,
                        errorWarningMessages.formatMessage(30011,
                                ERROR_MESSAGE_30011, this.getClass(), source.getModel(),
                                source.getName(), datasetIndex));
            }
        } else if (e instanceof NoModelFoundException) { // datastore not found
            // in any model
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(30010,
                            ERROR_MESSAGE_30010, this.getClass(), source.getName(), datasetIndex));
        } else if (e instanceof JodiPropertyNotFoundException) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30012,
                            ERROR_MESSAGE_30012, this.getClass(), source.getName(), datasetIndex,
                            source.getModel()));
        } else {
            StackTraceElement ste = e.getStackTrace()[0];
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30020,
                            ERROR_MESSAGE_30020, this.getClass(), ste.getClassName(),
                            source.getName(), getDatasetIndex(source)));
        }
    }

    @Override
    public void handleFilterExecutionLocation(Exception e, Source source) {
        int packageSequence = source.getParent().getParent()
                .getPackageSequence();
        StackTraceElement ste = e.getStackTrace()[0];
        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(20120,
                        ERROR_MESSAGE_20120, this.getClass(), source.getName(), getDatasetIndex(source),
                        ste.getClassName()));
    }

    @Override
    public void handleJoinExecutionLocation(Exception e, Source source) {
        int packageSequence = source.getParent().getParent()
                .getPackageSequence();
        StackTraceElement ste = e.getStackTrace()[0];
        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(30220, ERROR_MESSAGE_30220,
                        this.getClass(),
                        source.getName(), getDatasetIndex(source),
                        ste.getClassName()));
    }

    @Override
    public void handleLKM(Exception e, Source source) {
        int packageSequence = source.getParent().getParent()
                .getPackageSequence();
        if (e instanceof NoKnowledgeModuleFoundException) { // wrong - add
            // exception type
            DataStore ds = metadataService.getSourceDataStoreInModel(
                    source.getName(), source.getModel());
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(30304, ERROR_MESSAGE_30304,
                            this.getClass(),
                            ds.getDataModel().getDataServerTechnology(),
                            source.getName(), getDatasetIndex(source)));
        } else if (e instanceof KnowledgeModulePropertiesException) {
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(50000, ERROR_MESSAGE_50000, this.getClass()));
        } else {
            StackTraceElement ste = e.getStackTrace()[0];
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(30303, ERROR_MESSAGE_30303,
                            this.getClass(),
                            ste.getClassName(), source.getName(),
                            getDatasetIndex(source)));
        }

        throw (RuntimeException) e;
    }

    @Override
    public void handleModelCode(Exception e, Lookup lookup) {
        Integer packageSequence = lookup.getParent().getParent().getParent()
                .getPackageSequence();
        int datasetIndex = getDatasetIndex(lookup);

        if (e instanceof DataStoreNotInModelException) { // Explicitly set model
            // code is not
            // defined.
            if (lookup.getModel() == null || lookup.getModel().equals("")) {
                addErrorMessage(packageSequence,
                        errorWarningMessages.formatMessage(31013,
                                ERROR_MESSAGE_31013, this.getClass(), lookup.getLookupDataStore(),
                                lookup.getParent().getName(), datasetIndex,
                                lookup.getModel()));
            } else { // derived
                addErrorMessage(packageSequence,
                        errorWarningMessages.formatMessage(31011,
                                ERROR_MESSAGE_31011, this.getClass(), lookup.getModel(),
                                lookup.getLookupDataStore(),
                                lookup.getParent().getName(), datasetIndex));
            }
        } else if (e instanceof NoModelFoundException) { // datastore not found
            // in any model
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(31010,
                            ERROR_MESSAGE_31010, this.getClass(),
                            lookup.getLookupDataStore(),
                            lookup.getParent().getName(), datasetIndex));
        } else if (e instanceof JodiPropertyNotFoundException) {
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(31012,
                            ERROR_MESSAGE_31012, this.getClass(),
                            lookup.getLookupDataStore(),
                            lookup.getParent().getName(), datasetIndex,
                            lookup.getModel()));
        } else {
            StackTraceElement ste = e.getStackTrace()[0];
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(31020,
                            ERROR_MESSAGE_31020, this.getClass(),
                            ste.getClassName(),
                            lookup.getLookupDataStore(),
                            lookup.getParent().getName(),
                            this.getDatasetIndex(lookup)));
        }

    }

    @Override
    public void handleModelCode(Exception e, Mappings mappings) {
        Integer packageSequence = mappings.getParent().getPackageSequence();

        if (e instanceof DataStoreNotInModelException) {
            if (mappings.getModel() != null && mappings.getModel().length() > 0) {
                //explicit
                addErrorMessage(packageSequence,
                        errorWarningMessages.formatMessage(40013,
                                ERROR_MESSAGE_40013, this.getClass(),
                                mappings.getTargetDataStore(),
                                mappings.getModel()));
            } else {
                // implicitly derived
                addErrorMessage(packageSequence,
                        errorWarningMessages.formatMessage(40011,
                                ERROR_MESSAGE_40011, this.getClass(),
                                "UNKNOWN",
                                mappings.getTargetDataStore()));
            }

        } else if (e instanceof NoModelFoundException) {
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(40010,
                            ERROR_MESSAGE_40010, this.getClass(),
                            mappings.getTargetDataStore()));
        } else if (e instanceof JodiPropertyNotFoundException) {
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(40012,
                            ERROR_MESSAGE_40012, this.getClass(),
                            mappings.getTargetDataStore(),
                            mappings.getModel()));
        } else {
            StackTraceElement ste = e.getStackTrace()[0];
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(40020,
                            ERROR_MESSAGE_40020, this.getClass(),
                            ste.getClassName(),
                            mappings.getTargetDataStore()));
        }
    }

    // TODO - remove
    @Override
    public void handleExecutionLocation(Exception e, Mappings mappings) {
    }

    @Override
    public void handleIKM(Exception e, Mappings mappings) {
        int packageSequence = mappings.getParent().getPackageSequence();
        if (e instanceof NoKnowledgeModuleFoundException) {
            DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(40121,
                            ERROR_MESSAGE_40121, this.getClass(),
                            mappings.getTargetDataStore(),
                            ds.getDataModel().getDataServerTechnology()));
        } else if (e instanceof KnowledgeModulePropertiesException) {
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(50000,
                            ERROR_MESSAGE_50000, this.getClass()));
        } else {
            StackTraceElement ste = e.getStackTrace()[0];
            addErrorMessage(
                    packageSequence,
                    errorWarningMessages.formatMessage(40120,
                            ERROR_MESSAGE_40120,
                            this.getClass(),
                            ste.getClassName(),
                            mappings.getTargetDataStore()));
        }

        throw (RuntimeException) e;
    }

    @Override
    public void handleCKM(Exception e, Mappings mappings) {
        int packageSequence = mappings.getParent().getPackageSequence();

        if (e instanceof NoKnowledgeModuleFoundException) {
            DataStore ds = metadataService.getTargetDataStoreInModel(mappings);
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(40221,
                            ERROR_MESSAGE_40221, this.getClass(),
                            mappings.getTargetDataStore(),
                            ds.getDataModel().getDataServerTechnology()));
        } else if (e instanceof KnowledgeModulePropertiesException) {
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(50000, ERROR_MESSAGE_50000, this.getClass()));
        } else {
            StackTraceElement ste = e.getStackTrace()[0];
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(40220,
                            ERROR_MESSAGE_40220,
                            this.getClass(),
                            ste.getClassName(),
                            mappings.getTargetDataStore()));
        }
        throw (RuntimeException) e;
    }

    @Override
    public void handleColumnMapping(Exception e, Transformation transformation, String column) {
        Mappings mappings = transformation.getMappings();
        int packageSequence = mappings.getParent().getPackageSequence();
        String targetDataStore = mappings.getTargetDataStore();

        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(41017,
                        ERROR_MESSAGE_41017, this.getClass(), column,
                        targetDataStore));
    }

    @Override
    public void handleExecutionLocation(Exception e, Targetcolumn targetColumn) {
        Mappings mappings = targetColumn.getParent();
        int packageSequence = mappings.getParent().getPackageSequence();

        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(41020,
                        ERROR_MESSAGE_41020,
                        this.getClass(),
                        targetColumn.getName(),
                        mappings.getTargetDataStore()));
    }

    @Override
    public void handleTargetColumnFlags(Exception e, Targetcolumn targetColumn) {
        Mappings mappings = targetColumn.getParent();
        int packageSequence = mappings.getParent().getPackageSequence();

        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(41017,
                        ERROR_MESSAGE_41017,
                        this.getClass(),
                        targetColumn.getName(),
                        mappings.getTargetDataStore()));
    }

    @Override
    public void handleJournalizingDatastores(Exception e) {
        int packageSequence = 0;
        StackTraceElement ste = e.getStackTrace()[0];
        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(70001,
                        ERROR_MESSAGE_70001, this.getClass(),
                        ste.getClassName()));
    }

    @Override
    public void handleJournalizingOptions(Exception e) {
        int packageSequence = 0;
        StackTraceElement ste = e.getStackTrace()[0];
        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(70002,
                        ERROR_MESSAGE_70002, this.getClass(),
                        ste.getClassName()));
    }

    @Override
    public void handleJournalizingSubscribers(Exception e) {
        int packageSequence = 0;
        StackTraceElement ste = e.getStackTrace()[0];
        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(70003,
                        ERROR_MESSAGE_70003, this.getClass(),
                        ste.getClassName()));
    }

    private List<String> parsePackageList(String packageListString) {
        List<String> result = new ArrayList<>();
        String[] packageListItems = packageListString.trim().toUpperCase().split("\\s*,\\s*");

        for (String packageListItem : packageListItems) {
            if (packageListItem.trim().length() > 0)
                result.add(packageListItem);
        }

        return result;
    }

    @Override
    public boolean validateStagingModel(Mappings mappings) {
        boolean valid = true;
        KnowledgeModule odiKm = getKnowledgeModule(mappings.getIkm().getName());
        if (odiKm.isMultiTechnology() && (mappings.getStagingModel() == null || mappings.getStagingModel().length() < 1)) {
            int packageSequence = mappings.getParent().getPackageSequence();
            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(40143,
                            ERROR_MESSAGE_40143, this.getClass(),
                            mappings.getTargetDataStore()));
            valid = false;
        }

        return valid;
    }

    @Override
    public void handleStagingModel(Exception e, Mappings mappings) {
        StackTraceElement ste = e.getStackTrace()[0];
        int packageSequence = mappings.getParent().getPackageSequence();
        addErrorMessage(packageSequence,
                errorWarningMessages.formatMessage(40144,
                        ERROR_MESSAGE_40144, this.getClass(),
                        ste.getClassName(),
                        mappings.getTargetDataStore()));
    }

    @Override
    public boolean validateJournalized(Transformation transformation) {
        int journalizedCounter = 0;
        for (Dataset ds : transformation.getDatasets()) {
            for (Source source : ds.getSources()) {
                if (source.isJournalized())
                    journalizedCounter++;
                for (Lookup lookup : source.getLookups()) {
                    if (lookup.isJournalized())
                        journalizedCounter++;
                }
            }
        }

        if (journalizedCounter > 1) {
            int packageSequence = transformation.getPackageSequence();

            addErrorMessage(packageSequence,
                    errorWarningMessages.formatMessage(10070, ERROR_MESSAGE_10070,
                            this.getClass()));
        }

        return !(journalizedCounter > 1);
    }

    @Override
    public boolean validateJournalizingOptions(String modelCode, String jkm, Map<String, String> options) {
        List<KMValidation> errors = validateKM(jkm, jkm, options);

        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:
                    break;
                case NOT_LOADED:
                    addErrorMessage(errorWarningMessages.formatMessage(70010,
                            ERROR_MESSAGE_70010, this.getClass(),
                            modelCode, jkm));
                    break;
                case INVALID_OPTION:
                    addErrorMessage(errorWarningMessages.formatMessage(70011,
                            ERROR_MESSAGE_70011, this.getClass(),
                            modelCode, jkm, error.values[0]));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(errorWarningMessages.formatMessage(70012,
                            ERROR_MESSAGE_70012, this.getClass(),
                            modelCode, jkm, error.values[1],
                            error.values[0]));
                    break;
            }
        }

        if (properties.hasDeprecateCDCProperty()) {
            addErrorMessage(errorWarningMessages.formatMessage(70020,
                    ERROR_MESSAGE_70020, this.getClass()));
            return false;
        } else {
            return errors.isEmpty();
        }
    }

    @Override
    public boolean validateJournalizingOptions(String modelCode, String jkm, Map<String, String> options, String strategyClassname) {
        List<KMValidation> errors = validateKM(jkm, jkm, options);

        for (KMValidation error : errors) {
            switch (error.error) {
                case UNDEFINED:
                case NOT_LOADED: // Should have already been validated
                    break;
                case INVALID_OPTION:
                    addErrorMessage(errorWarningMessages.formatMessage(70014,
                            ERROR_MESSAGE_70014, this.getClass(),
                            strategyClassname, modelCode, jkm, error.values[0]));
                    break;
                case INVALID_OPTION_VALUE:
                    addErrorMessage(errorWarningMessages.formatMessage(70015,
                            ERROR_MESSAGE_70015, this.getClass(),
                            strategyClassname, modelCode, jkm, error.values[1],
                            error.values[0]));
                    break;
            }
        }

        return errors.isEmpty();
    }

    @Override
    public boolean validateExecutionLocation(SubQuery subquery) {
        Source source = subquery.getParent();
        int packageSequence = source.getParent().getParent().getPackageSequence();
        if (ExecutionLocationtypeEnum.TARGET.equals(subquery.getExecutionLocation())) {
            addErrorMessage(packageSequence, errorWarningMessages.formatMessage(
                    30156, ERROR_MESSAGE_30156, this.getClass(),
                    subquery.getName(), source.getAlias(),
                    getDatasetIndex(source), subquery.getExecutionLocation()));
            return false;
        }
        return true;
    }

    @Override
    public boolean validateExecutionLocation(Lookup lookup) {
        Source source = lookup.getParent();
        int packageSequence = source.getParent().getParent().getPackageSequence();
        if (ExecutionLocationtypeEnum.TARGET.equals(lookup.getJoinExecutionLocation())) {
            addErrorMessage(packageSequence, errorWarningMessages.formatMessage(
                    31221, ERROR_MESSAGE_31221, this.getClass(),
                    lookup.getLookupDataStore(), source.getAlias(),
                    getDatasetIndex(lookup), lookup.getJoinExecutionLocation()));
            return false;
        }
        return true;
    }

    @Override
    public boolean validateExecutionLocation(Lookup lookup,
                                             ExecutionLocationStrategy strategy) {
        Source source = lookup.getParent();
        int packageSequence = source.getParent().getParent().getPackageSequence();
        ExecutionLocationtypeEnum el = lookup.getJoinExecutionLocation();
        if (ExecutionLocationtypeEnum.TARGET.equals(el) || el == null) {
            addErrorMessage(packageSequence, errorWarningMessages.formatMessage(
                    31222, ERROR_MESSAGE_31222, this.getClass(),
                    strategy.getClass().getName(), lookup.getLookupDataStore(),
                    source.getAlias(), getDatasetIndex(lookup),
                    lookup.getJoinExecutionLocation()));
            return false;
        }
        return true;
    }

    @Override
    public boolean validateFlow(Flow flow) {
        Source source = flow.getParent();
        int datasetIndex = getDatasetIndex(source);
        int packageSequence = source.getParent().getParent().getPackageSequence();
        boolean valid = true;

        // TODO remove when ODI 11 is removed for Jodi 1.5.
        // Issue warning when Flows are used in combination with ODI 11.
        if (etlSubSystemVersion.isVersion11() && source.getFlows().indexOf(flow) == 0) {
            String msg = errorWarningMessages.formatMessage(30149, ERROR_MESSAGE_30149, this.getClass(), source.getAlias(), datasetIndex);
            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.WARNINGS);
        }

        validateFlowName:
        {
            for (Dataset d : source.getParent().getParent().getDatasets()) {
                for (Source s : d.getSources()) {
                    if (source.getAlias().equalsIgnoreCase(flow.getName())) {
                        String msg = errorWarningMessages.formatMessage(30140, ERROR_MESSAGE_30140, this.getClass(), flow.getName(), source.getAlias(), datasetIndex);
                        errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                        valid = false;
                    }
                    for (Flow f : s.getFlows()) {
                        if (flow != f && flow.getName().equalsIgnoreCase(f.getName())) {
                            String msg = errorWarningMessages.formatMessage(30140, ERROR_MESSAGE_30140, this.getClass(), f.getName(), source.getAlias(), datasetIndex);
                            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                            valid = false;
                            break validateFlowName;
                        }
                    }
                }
            }
        }


        if (flow instanceof Pivot) {
            valid &= validateRowLocator((Pivot) flow);
        } else if (flow instanceof SubQuery) {
            valid &= validateSubQuery((SubQuery) flow);
        }

        // Ensure OutputAttribute name uniqueness
        for (int i = 0; i < flow.getOutputAttributes().size(); i++) {
            for (int j = 0; j < i; j++) {
                if (flow.getOutputAttributes().get(i).getName().equalsIgnoreCase(flow.getOutputAttributes().get(j).getName())) {
                    String msg = errorWarningMessages.formatMessage(30148, ERROR_MESSAGE_30148, this.getClass(), flow.getOutputAttributes().get(i).getName(), flow.getName(), source.getAlias(), datasetIndex);
                    errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                    valid = false;
                    break;
                }
            }
        }


        String regex = "([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)";
        Pattern pattern = Pattern.compile(regex);
        for (OutputAttribute attribute : flow.getOutputAttributes()) {
            for (Map.Entry<String, String> entry : attribute.getExpressions().entrySet()) {
                String expression = entry.getValue();
                Matcher matcher = pattern.matcher(expression + "");
                while (matcher.find()) {
                    String alias = matcher.group(1);
                    String column = matcher.group(3);
                    String columnName = matcher.group();
                    String tableName = columnName.substring(0, columnName.indexOf("."));

                    if (columnName.toLowerCase().contains("out.print") || alias.toLowerCase().contains("odiref")) {
                        continue;
                    } else if (alias.startsWith("#")) {
                        String variableName = columnName.substring(columnName.indexOf(".") + 1, columnName.length());
                        if (!metadataService.projectVariableExists(tableName.substring(1), variableName) || (tableName.substring(1).contains("GLOABL") && !metadataService.globalVariableExists(variableName))) {
                            String msg = errorWarningMessages.formatMessage(30146, ERROR_MESSAGE_30146, this.getClass(), flow.getName(), source.getAlias(), datasetIndex, columnName);
                            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                            valid = false;
                        }
                    } else {

                        HashMap<String, String> scopeAliases = getAliases(flow);
                        if (scopeAliases.containsValue(alias) && !scopeAliases.containsKey(alias)) {
                            String msg = errorWarningMessages.formatMessage(30147, ERROR_MESSAGE_30147, this.getClass(), flow.getName(), source.getAlias(), datasetIndex, alias);
                            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                            valid = false;
                        } else if (!scopeAliases.containsKey(alias)) {
                            String msg = errorWarningMessages.formatMessage(30144, ERROR_MESSAGE_30144, this.getClass(), flow.getName(), source.getAlias(), datasetIndex, alias);
                            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                            valid = false;
                        }
                    }

                }
            }


        }

        return valid;
    }

    private boolean validateRowLocator(Pivot pivot) {
        Source source = pivot.getParent();
        int packageSequence = source.getParent().getParent().getPackageSequence();
        int datasetIndex = getDatasetIndex(source);
        String regex = JodiConstants.ALIAS_DOT_COLUMN_PREFIX;
        ;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pivot.getRowLocator());
        boolean valid = true;

        for (OutputAttribute oa : pivot.getOutputAttributes()) {

            for (String k : oa.getExpressions().keySet()) {
                if (k == null) continue;
                String expression = oa.getExpressions().get(k);
                Matcher m = pattern.matcher(expression);
                while (m.find()) {
                    String column = m.group(3);
                    if (column.length() + pivot.getRowLocator().length() > 27) {
                        String msg = errorWarningMessages.formatMessage(30150, ERROR_MESSAGE_30150, this.getClass(), oa.getName(), pivot.getName(), source.getAlias(), datasetIndex);
                        errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.WARNINGS);
                    }
                    break;
                }
            }
        }


        while (matcher.find()) {

            String aliasAndColumn = matcher.group();
            String alias = aliasAndColumn.substring(0,
                    aliasAndColumn.indexOf("."));
            String column = aliasAndColumn.substring(aliasAndColumn
                    .indexOf(".") + 1);
            HashMap<String, String> scopeAliases = getAliases(pivot);


            if (scopeAliases.containsValue(alias) && !scopeAliases.containsKey(alias)) {
                String msg = errorWarningMessages.formatMessage(30143, ERROR_MESSAGE_30143, this.getClass(), pivot.getName(), source.getAlias(), datasetIndex, alias);
                errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                valid = false;
            } else if (!scopeAliases.containsKey(alias)) {
                String msg = errorWarningMessages.formatMessage(30141, ERROR_MESSAGE_30141, this.getClass(), pivot.getName(), source.getAlias(), datasetIndex, alias);
                errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                valid = false;
            } else {
                // Validate column
                String tableName = scopeAliases.get(alias);
                if (tableName != null) {
                    DataStore dataStore = findSourceDataStore(tableName, source.getParent());
					/*
					DataStore dataStore = source.getFlows().indexOf(pivot) > 0 ?
							findFlowDataStore(tableName, source.getParent().getParent()) :
							findSourceDataStore(tableName,	source.getParent());
							*/
                    if (dataStore != null) {
                        DataStoreColumn dataStoreColumn = dataStore.getColumns().get(column);
                        if (dataStoreColumn == null) {
                            String msg = errorWarningMessages.formatMessage(30142, ERROR_MESSAGE_30142, this.getClass(), pivot.getName(), source.getAlias(), datasetIndex, column);
                            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                            valid = false;
                        }
                    }
                }
            }
        }

        return valid;
    }

    private boolean validateSubQueryCondition(SubQuery subquery) {
        Source source = subquery.getParent();
        int packageSequence = source.getParent().getParent().getPackageSequence();
        int datasetIndex = getDatasetIndex(source);
        String regex = "([a-zA-Z_$#0-9\"]+)([.]{1,1})([a-zA-Z_$#0-9\"]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(subquery.getCondition());
        boolean valid = true;
        boolean referencesParent = false;

        String driverAlias = "";
        int index = source.getFlows().indexOf(subquery);
        if (index > 0) {
            driverAlias = source.getFlows().get(index - 1).getName();
            ;
        } else {
            driverAlias = source.getAlias();
        }

        while (matcher.find()) {

            String aliasAndColumn = matcher.group();
            String alias = aliasAndColumn.substring(0,
                    aliasAndColumn.indexOf("."));
            String column = aliasAndColumn.substring(aliasAndColumn
                    .indexOf(".") + 1);

            referencesParent |= driverAlias.equals(alias);

            if (alias.equals(source.getName()) && !source.getAlias().equals(source.getName())) {
                // error refer to source by alias
                String msg = errorWarningMessages.formatMessage(30153, ERROR_MESSAGE_30153, this.getClass(), subquery.getName(), source.getAlias(), datasetIndex, alias);
                errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                valid = false;
            } else if (!alias.equals(driverAlias) && !alias.equals(subquery.getFilterSource())) {
                // unknown source (can only be parent source, a previous flow item and subquery itself.
                String msg = errorWarningMessages.formatMessage(30151, ERROR_MESSAGE_30151, this.getClass(), subquery.getName(), source.getAlias(), datasetIndex, alias);
                errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                valid = false;
            } else {
                // translate from alias if needed.
                DataStore datastore = null;
                if (alias.equals(subquery.getFilterSource())) {
                    datastore = metadataService.getSourceDataStoreInModel(subquery.getFilterSource(),
                            subquery.getFilterSourceModel());
                } else {
                    String tableName = alias.equals(source.getAlias()) ?
                            source.getName() : alias;
                    datastore = findSourceDataStore(tableName, source.getParent());

                }

                if (datastore != null) {
                    DataStoreColumn dataStoreColumn = datastore.getColumns().get(column);
                    if (dataStoreColumn == null) {
                        String msg = errorWarningMessages.formatMessage(30152, ERROR_MESSAGE_30152, this.getClass(), subquery.getName(), source.getAlias(), datasetIndex, column);
                        errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
                        valid = false;
                    }
                }
            }
        }

        if (!referencesParent && subquery.getCondition().length() > 1 && subquery.getParent().getFlows().size() == 1) {
            String msg = errorWarningMessages.formatMessage(30154, ERROR_MESSAGE_30154, this.getClass(), subquery.getName(), source.getAlias(), datasetIndex);
            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
            valid = false;
        }

        return valid;
    }

    public boolean validateSubQuery(SubQuery subquery) {
        boolean valid = true;
        valid &= validateSubQueryCondition(subquery);

        if (subquery.getCondition().trim().isEmpty() && (RoleEnum.EXISTS.equals(subquery.getRole()) || RoleEnum.NOT_EXISTS.equals(subquery.getRole()))) {
            Source source = subquery.getParent();
            int packageSequence = source.getParent().getParent().getPackageSequence();
            int datasetIndex = getDatasetIndex(source);

            String msg = errorWarningMessages.formatMessage(30155, ERROR_MESSAGE_30155, this.getClass(), subquery.getName(), source.getAlias(), datasetIndex);
            errorWarningMessages.addMessage(packageSequence, msg, MESSAGE_TYPE.ERRORS);
            valid = false;
        }

		/*
		// determine that correct number of expressions are used
		subquery.getOutputAttributes().forEach(f -> {
				if(f.getExpressions().keySet().stream().filter(k -> k == ExpressionSource.DRIVER.name()). > 1) {

				}
			}
		);
			*/


        return valid;


    }

    @Override
    public boolean validateBeginAndEndMapping(Transformation transformation) {
        boolean isValidBegin = true;
        boolean isValidEnd = true;
        if (transformation.getBeginMappingCommand() != null) {
            isValidBegin = validateMappingCommand(transformation, transformation.getBeginMappingCommand(), "beginmappingcommand");
        }
        if (transformation.getEndMappingCommand() != null) {
            isValidEnd = validateMappingCommand(transformation, transformation.getEndMappingCommand(), "endmappingcommand");
        }
        return (isValidBegin && isValidEnd);
    }

    private boolean validateMappingCommand(Transformation transformation, MappingCommand beginMappingCommand, String type) {
        boolean result = true;
        if (beginMappingCommand != null) {
            if (etlSubSystemVersion.isVersion11()) {
                String msg = errorWarningMessages.formatMessage(71010, ERROR_MESSAGE_71010, this.getClass(), transformation.getPackageSequence() + "", type, "Begin and End Mappings are not supported on ODI 11.");
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                result = false;
            }
            if (beginMappingCommand.getText().trim().endsWith("/") && beginMappingCommand.getTechnology().toUpperCase().equalsIgnoreCase("ORACLE")) {
                String msg = errorWarningMessages.formatMessage(71010, ERROR_MESSAGE_71010, this.getClass(), transformation.getPackageSequence() + "", type, "the mapping command shouldn't end with a '/'.");
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                result = false;
            }
            if (beginMappingCommand.getTechnology().trim().length() < 1) {
                String msg = errorWarningMessages.formatMessage(71010, ERROR_MESSAGE_71010, this.getClass(), transformation.getPackageSequence() + "", type, "the mapping command technology is not specified.");
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                result = false;
            }
            if (beginMappingCommand.getModel().trim().length() < 1 && beginMappingCommand.getTechnology().toLowerCase().equalsIgnoreCase("oracle")) {
                String msg = errorWarningMessages.formatMessage(71010, ERROR_MESSAGE_71010, this.getClass(), transformation.getPackageSequence() + "", type, "the mapping command model / location is not specified.");
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean validateLookupType(Lookup lookup) {
        String lookupType = lookup.getLookupType() != null ? lookup.getLookupType().name() : null;
        boolean valid = true;
        List<String> choices = etlSubSystemVersion.isVersion11() ?
                Arrays.asList("LEFT OUTER", "SCALAR") :
                Arrays.asList("LEFT_OUTER", "ALL_ROWS", "ANY_ROW", "ERROR_WHEN_MULTIPLE_ROW", "FIRST_ROW", "LAST_ROW");

        if (lookupType != null) {
            int index = getDatasetIndex(lookup);
            Source source = lookup.getParent();
            Transformation transformation = source.getParent().getParent();

            if (!choices.contains(lookupType)) {
                String msg = errorWarningMessages.formatMessage(31240, ERROR_MESSAGE_31240, this.getClass(), lookup.getLookupDataStore(), source.getAlias(), index, lookupType, etlSubSystemVersion.getVersion());
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.ERRORS);

                valid = false;
            }

            if (!etlSubSystemVersion.isVersion11() && (lookup.isTemporary() || lookup.getParent().isTemporary())) {
                String msg = errorWarningMessages.formatMessage(31240, ERROR_MESSAGE_31241, this.getClass(), lookup.getLookupDataStore(), source.getAlias(), index);
                errorWarningMessages.addMessage(transformation.getPackageSequence(), msg, MESSAGE_TYPE.WARNINGS);
                valid = false;
            }
        }


        return valid;
    }

    @Override
    public boolean validateNoMatchRows(Lookup lookup) {
        List<Boolean> valids = new ArrayList<Boolean>();
        int datasetIndex = getDatasetIndex(lookup);
        int packageSequence = lookup.getParent().getParent().getParent().getPackageSequence();

        if (lookup.getDefaultRowColumns().isEmpty())
            return true;

        DataStore ds = metadataService.getSourceDataStoreInModel(
                lookup.getLookupDataStore(), lookup.getModel());

        ds.getColumns().forEach((k, v) -> {
            if (!lookup.getDefaultRowColumns().containsKey(k)) {
                valids.add(false);
                String msg = errorWarningMessages.formatMessage(31305,
                        ERROR_MESSAGE_31305, this.getClass(),
                        lookup.getLookupDataStore(),
                        lookup.getParent().getAlias(),
                        datasetIndex, k);
                addErrorMessage(packageSequence, msg);
            }
        });

        lookup.getDefaultRowColumns().forEach((k, v) -> {
            if (!ds.getColumns().containsKey(k)) {
                valids.add(false);
                String msg = errorWarningMessages.formatMessage(31304,
                        ERROR_MESSAGE_31304, this.getClass(), k,
                        lookup.getLookupDataStore(),
                        lookup.getParent().getAlias(),
                        datasetIndex);
                addErrorMessage(packageSequence, msg);
            } else {
                boolean valid = validateLookupDefaultRowExpression(lookup, k, v);
                valids.add(valid);
            }
        });


        return !valids.contains(false);
    }

    private boolean validateLookupDefaultRowExpression(Lookup lookup, String column, String expression) {
        boolean valid = true;

        String sourceNameOrAlias = lookup.getParent().getAlias() != null ? lookup.getParent().getAlias().toLowerCase() : lookup.getParent().getName().toLowerCase();
        String lookupNameOrAlias = lookup.getAlias() != null ? lookup.getAlias().toLowerCase() : lookup.getLookupDataStore().toLowerCase();
        String regex = JodiConstants.ALIAS_DOT_COLUMN_PREFIX;
        int datasetIndex = getDatasetIndex(lookup);
        Integer packageSequence = lookup.getParent().getParent().getParent().getPackageSequence();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(expression);
        while (matcher.find()) {
            String columnName = matcher.group();
            String tableName = columnName.substring(0, columnName.indexOf("."));

            if (!sourceNameOrAlias.toLowerCase().equalsIgnoreCase(tableName.toLowerCase()) &&
                    !lookupNameOrAlias.equalsIgnoreCase(tableName.toLowerCase())) {
                String msg = errorWarningMessages.formatMessage(31300,
                        ERROR_MESSAGE_31300, this.getClass(), expression, column,
                        lookup.getLookupDataStore(), sourceNameOrAlias,
                        datasetIndex, tableName);
                addErrorMessage(packageSequence, msg);
                valid = false;
            }
            // aliases are ok.
//			else {
//				String msg = errorWarningMessages.formatMessage(31302,
//					ERROR_MESSAGE_31302, this.getClass(), expression, column,
//					lookup.getLookupDataStore(), sourceName,
//					datasetIndex, tableName);
//				addErrorMessage(packageSequence, msg);
//				valid = false;
//			}
        }

        Source source = lookup.getParent();
        Dataset dataset = lookup.getParent().getParent();
        List<ExpressionError> errors = validateExpressionSources(expression, source, lookup, false);
        errors.addAll(validateJoinTypes(expression, dataset));

        for (ExpressionError error : errors) {
            switch (error.error) {
                case DATASTORE:
                    String msg = errorWarningMessages.formatMessage(31302,
                            ERROR_MESSAGE_31302, this.getClass(), expression, column,
                            lookup.getLookupDataStore(), sourceNameOrAlias,
                            datasetIndex, error.values[0]);
                    addErrorMessage(packageSequence, msg);
                    break;
                case COLUMN:
                    addErrorMessage(packageSequence,
                            errorWarningMessages.formatMessage(31303,
                                    ERROR_MESSAGE_31303, this.getClass(), expression, column, source.getAlias(), lookup.getAlias(),
                                    datasetIndex, error.values[1], error.values[0]));
                    break;
                default:
                    break;
            }
        }

        valid &= (errors.size() == 0);

        return valid;
    }

    @Override
    public boolean validateJKMOptions(String modelCode, String jkm, Map<String, Object> options) {
        HashMap<String, String> map = new HashMap<>();
        for (String key : options.keySet()) {
            map.put(key, options.get(key).toString());
        }

        return validateJournalizingOptions(modelCode, jkm, map);
    }

    @Override
    public boolean validateJournalizing() {
        boolean valid = true;
//		JournalizingExecutionContext exc = getJournalizingExecutionContext();
//		List<String> defaultModelsForCDC = defaultStrategy
//				.getModelCodesEnabledForCDC(null, exc);
//		List<String> finalModelsForCDC = .;
//		if (customStrategy != null) {
//			finalModelsForCDC = customStrategy.getModelCodesEnabledForCDC(
//						defaultModelsForCDC, exc);
//		}
//		assert finalModelsForCDC != null ;
        for (JournalizingConfiguration jconfig : this.journalizingContext.getJournalizingConfiguration()) {
            String modelCode = jconfig.getModelCode();
            if (validateJKMOptions(modelCode, jconfig.getName(), jconfig.getJkmOptions())) {
                Map<String, Object> defaultJKMOptions = jconfig.getJkmOptions();
                if (validateJKMOptions(modelCode, jconfig.getName(), defaultJKMOptions)) {
                    Map<String, Object> finalJKMOptions = defaultJKMOptions;
                    valid &= validateJKMOptions(modelCode, jconfig.getName(), finalJKMOptions);
                } else {
                    valid = false;
                }
            } else {
                valid = false;
            }
        }
        return valid;
    }

    private JournalizingExecutionContext getJournalizingExecutionContext() {
        try {
            JournalizingExecutionContext exc = new JournalizingExecutionContext() {

                public Map<String, DataStore> getDatastores() {
                    Map<String, DataStore> cdc = new TreeMap<>();
                    for (ModelProperties modelCode : metadataService.getConfiguredModels()) {
                        cdc.putAll(metadataService
                                .getAllDataStoresInModel(modelCode.getCode()));
                    }
                    return Collections.unmodifiableMap(cdc);
                }

                @Override
                public Map<String, Object> getJKMOptions(String modelCode) {
                    for (ModelProperties modelProperties : metadataService
                            .getConfiguredModels()) {
                        if (modelProperties.getCode().equals(modelCode)
                                && modelProperties.isJournalized())
                            return Collections.unmodifiableMap(parseMap(modelProperties.getJkmoptions()));
                    }
                    return Collections.unmodifiableMap(new HashMap<>());
                }

                @Override
                public List<String> getModelCodesEnabledForCDC() {
                    List<String> cdcModels = metadataService
                            .getConfiguredModels().stream()
                            .filter(ModelProperties::isJournalized).map(ModelProperties::getCode)
                            .collect(Collectors.toList());
                    return Collections.unmodifiableList(cdcModels);
                }

                @Override
                public List<String> getSubscribers(String modelCode) {
                    List<String> subscribers = new ArrayList<>();
                    for (ModelProperties modelProperties : metadataService
                            .getConfiguredModels()) {
                        if (modelProperties.isJournalized() && modelCode.equals(modelProperties.getCode())) {
                            subscribers = modelProperties.getSubscribers();
                        }
                    }
                    if (subscribers == null || subscribers.size() == 0) {
                        subscribers = new ArrayList<>();
                        subscribers.add("SUNOPSIS");
                    }
                    return Collections.unmodifiableList(subscribers);
                }

                @Override
                public String getName(String modelCode) {
                    String name = null;
                    for (ModelProperties modelProperties : metadataService
                            .getConfiguredModels()) {
                        if (modelProperties.isJournalized() && modelCode.equals(modelProperties.getCode())) {
                            name = modelProperties.getJkm();
                            break;
                        }
                    }
                    return name;
                }

            };
            return exc;
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            String msg = errorWarningMessages.formatMessage(2040,
                    ERROR_MESSAGE_02040, this.getClass(), message);
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            logger.error(msg);
            throw new UnRecoverableException(msg, e);
        }
    }

    private Map<String, Object> parseMap(List<String> options) {
        HashMap<String, Object> map = new HashMap<>();
        for (String option : options) {
            String[] list = option.split(",");
            for (String s : list) {
                String[] keyValuePair = s.split(":", 2);
                if (keyValuePair.length != 2) {
                    String msg = errorWarningMessages.formatMessage(2050,
                            ERROR_MESSAGE_02050, this.getClass());
                    errorWarningMessages.addMessage(
                            errorWarningMessages.assignSequenceNumber(), msg,
                            MESSAGE_TYPE.ERRORS);
                    throw new UnRecoverableException(msg);
                } else {
                    try {
                        map.put(keyValuePair[0].trim(),
                                Integer.valueOf(keyValuePair[1].trim()));
                    } catch (NumberFormatException nfe) {
                        if ("true".equalsIgnoreCase(keyValuePair[1].trim())
                                || "false".equalsIgnoreCase(keyValuePair[1].trim())) {
                            map.put(keyValuePair[0].trim(),
                                    Boolean.valueOf(keyValuePair[1].trim()));
                        } else {
                            map.put(keyValuePair[0].trim(), keyValuePair[1].trim());
                        }
                    }
                }
            }
        }
        return map;
    }

    @Override
    public void validateTargetColumns(Mappings mapping) {
        Set<String> columNames = new HashSet<>();
        for (Targetcolumn tc : mapping.getTargetColumns()) {
            if (columNames.contains(tc.getName())) {
                String msg = errorWarningMessages.formatMessage(41006,
                        ERROR_MESSAGE_41006, this.getClass(), tc.getName(), mapping.getParent().getName());
                errorWarningMessages.addMessage(
                        mapping.getParent().getPackageSequence(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new UnRecoverableException(msg);
            }
            columNames.add(tc.getName());
        }
    }

    private enum ExpressionErrorEnum {
        DATASTORE, COLUMN, JOIN_TYPE, PROJECT_VARIABLE
    }

    private enum KMValidationEnum {
        UNDEFINED, NOT_LOADED, INVALID_OPTION, INVALID_OPTION_VALUE
    }

    private static class ExpressionError {
        ExpressionErrorEnum error;
        String[] values;

        ExpressionError(ExpressionErrorEnum error, String... values) {
            this.error = error;
            this.values = values;
        }
    }

    private static class ColumnNotInDataStoreException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        @SuppressWarnings("unused")
        String message;

        ColumnNotInDataStoreException(String message) {
            this.message = message;
        }
    }

    private class KMValidation {
        KMValidationEnum error;
        String[] values;

        KMValidation(KMValidationEnum error, String... values) {
            this.error = error;
            this.values = values;
        }
    }
}
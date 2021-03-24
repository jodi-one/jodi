package one.jodi.core.config;

import java.util.Arrays;
import java.util.List;

/**
 * Contains constants that are related to Jodi only.
 */
public abstract class JodiConstants {

    // data mart
    public static final String ROW_WID = "jodi.star.row_wid";
    public static final String DATA_MART_PREFIX = "jodi.star.prefix";
    public static final String DIMENSION_SUFFIX = "jodi.star.dimension_postfix";
    public static final String FACT_SUFFIX = "jodi.star.fact_postfix";
    public static final String HELPER_SUFFIX = "jodi.star.helper_postfix";

    // scd type 2
    public static final String CURRENT_FLG = "jodi.scr.current_flag";
    public static final String EFFECTIVE_DATE = "jodi.scd.effective_from_date";
    public static final String EXPIRATION_DATE = "jodi.scd.effective_to_date";

    // common columns for data mart
    public static final String W_INSERT_DT = "jodi.etl.insert_date";
    public static final String W_UPDATE_DT = "jodi.etl.update_date";
    public static final String ETL_PROC_WID = "jodi.etl.etl_proc_wid";

    // folders
    public static final String INITIAL_LOAD_FOLDER = "jodi.folder.bulk";
    // jkm
    public static final String INCREMENTALL_LOAD_FOLDER = "jodi.folder.incremental";
    public static final String INCREMENTALL_LOAD_FOLDER_DEFAULT = "Real";
    // end jkm

    // column mapping
    public static final String COLUMN_MATCH_REGEX = "jodi.column_match_default_strategy.regex";
    public static final String COLUMN_MATCH_SOURCE_IGNORE = "jodi.column_match_default_strategy.ignore";

    // ODI project properties
    public static final String ODI_PROJECT_CODE = "odi.project.code";
    public static final String JODI_INCLUDE_DETAIL = "jodi.include.detail";
    public static final String DEFAULT_PROPERTIES = "odi.properties";

    public static final String ALIAS_REGEXP_PREFIX = "[A-Za-z]{1,}[A-Za-z0-9_$#]{0,}(\\.){1,1}";
    public static final String COLUM_REGEXP = "[A-Za-z]{1,}[A-Za-z0-9_$#]{0,}";
    public static final String ALIAS_DOT_COLUMN_PREFIX = "([A-Za-z]{1,}[a-zA-Z_$#0-9\"]{0,})([.]{1,1})([A-Za-z]{1,}[a-zA-Z_$#0-9\"]{0,})";
    public static final String ALIAS_DOT_COLUMN_PREFIX_OR_VAR = "([A-Za-z#:]{1,}[a-zA-Z_$#0-9\"]{0,})([.]{1,1})([A-Za-z]{1,}[a-zA-Z_$#0-9\"]{0,})";

    public final static String XMLLOADPLANLOC = "loadPlans";
    public final static String XSDLOCPROPERTY = "xml.xsd.loadPlan";

    public static final String XSD_FILE_CONSTRAINTS = "jodi-constraints.v1.0.xsd";
    public static final String XSD_FILE_EXTENSIONS = "jodi-extensions.v1.0.xsd";
    public static final String XSD_FILE_LOADPLAN = "jodi-loadplan.v1.0.xsd";
    public static final String XSD_FILE_MODEL = "jodi-model.v1.1.xsd";
    public static final String XSD_FILE_PACKAGES = "jodi-packages.v1.1.xsd";
    public static final String XSD_FILE_PROCEDURE = "jodi-procedure.v1.0.xsd";
    public static final String XSD_FILE_SEQUENCES = "jodi-sequences.v1.0.xsd";
    public static final String XSD_FILE_VARIABLES = "jodi-variables.v1.0.xsd";
    public static final String JODI_CACHE_MAXSIZE = "jodi.cache.maxsize";
    public static final String TEMPORARY_MAPPING_REGEX_PROPERTY = "jodi.temporary_mapping_regex";
    public static final String VERSION_HEADER = "------- (Jodi Version: %s)";
    public static final String ERROR_FOOTER = "There were one or more"
            + " errors, to see them enable log4j logging with the -Dlog4j.info"
            + " VMArgument, and place a log4j.properties in your classpath.";
    public static final String USE_SCENARIOS_INSTEAD_OF_MAPPINGS = "odi12.useScenariosInsteadOfMappings";
    public final static String ODI12_GENERATE_SCENARIOS_FOR_MAPPINGS =
            "odi12.generateScenariosForMappings";
    public final static String ODI12_GENERATE_SCENARIOS_FOR_PROCEDURES =
            "odi12.generateScenariosForProcedures";
    public final static String ODI12_GENERATE_SCENARIOS_FOR_PACKAGES =
            "odi12.generateScenariosForPackages";

    public static List<String> getEmbeddedXSDFileNames() {
        return Arrays.asList(XSD_FILE_CONSTRAINTS, XSD_FILE_EXTENSIONS, XSD_FILE_LOADPLAN,
                XSD_FILE_PROCEDURE, XSD_FILE_SEQUENCES, XSD_FILE_VARIABLES);
    }

    public static String getScenarioNameFromObject(final String name, boolean isMapping) {
        return name.replace("(", "_")
                .replace(")", "_")
                .replace(",", "_")
                .replace(" ", "_")
                .toUpperCase();
    }

    public String getReusableMappingPrefix(final String interfacePrefix) {
        //assert(interfacePrefix!=null && interfacePrefix.length()>0);
        String result = "I_"; // default needed for package action runner
        if (interfacePrefix != null && interfacePrefix.trim().length() > 0) {
            result = interfacePrefix.toUpperCase().replace(" ", "").trim()
                    .charAt(0) + "_";
        }
        return result;
    }

}

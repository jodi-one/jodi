package one.jodi.core.config.km;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.util.StringUtils;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertiesParser;
import one.jodi.core.extensions.strategies.KnowledgeModulePropertiesException;
import one.jodi.core.metadata.ETLSubsystemService;
import one.jodi.core.metadata.types.KnowledgeModule;
import one.jodi.etl.km.KnowledgeModuleType;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses properties related to knowledge module definitions and performs some additional
 * validations.
 */
@Singleton //should be defined as eager Singleton in model
public class KnowledgeModulePropertiesProviderImpl implements KnowledgeModulePropertiesProvider {

    /**
     * Prefix that is used in properties file to describe properties related to
     * the model configuration.
     */
    public static final String TOPIC = "km";

    private static final Logger logger = LogManager.getLogger(KnowledgeModulePropertiesProviderImpl.class);
    private static final String newLine = System.getProperty("line.separator");
    private static final String ERROR_MESSAGE_00100 = "Errors found in Knowledge Module Configuration Rules %s";
    private static final String ERROR_MESSAGE_00110 =
            "Cannot map name(s) %s to KM loaded in ODI.  Please check that your" +
                    " ODI instance is loaded with KM names referred to in Jodi " + "properties file.";
    private static final String ERROR_MESSAGE_00120 = "Runtime exception on %s %s";
    private static final String ERROR_MESSAGE_00130 = "The KM properties contain %1$s configuration errors.";
    private static final String ERROR_MESSAGE_00140 = "Knowledge Module Configuration contains no %s rules.";
    private static final String ERROR_MESSAGE_00150 =
            "Invalid trg_temporary specification %s.  This must be {-1,0,-1}.";
    private static final String ERROR_MESSAGE_00160 =
            "Invalid name specification.  Multiple names are not permitted " + "for rules.";
    private static final String ERROR_MESSAGE_00170 = "Duplicated default rule found for technology %s";
    private static final String ERROR_MESSAGE_00180 = "Duplicated or missing order number %s";
    private static final String ERROR_MESSAGE_00190 = "KM '%s' is loaded in ODI as a multi-technology KM.  Rules for " +
            "multi-technology KMs must specify both src_technology and" + " trg_technology.";
    private static final String ERROR_MESSAGE_00200 =
            "Rule specifies src_technology however KM '%s' is not loaded in" + " ODI as multi-technology.";
    private static final String ERROR_MESSAGE_00210 =
            "Invalid DataStoreType specification (%s). DataType must be one" + " of %s";
    private static final String ERROR_MESSAGE_00220 = "Invalid field (%s) for rule of type %s";
    private static final String ERROR_MESSAGE_00230 = "Missing required field (%s for rule of type %s";
    private final JodiProperties jodiProperties;
    private final ETLSubsystemService etlSubsystemService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final String[] PARAMETER_DEFS =
            new String[]{"name[]!", "order", "global", "options{}", "trg_technology!", "src_technology", "default",
                         "trg_temporary", "trg_regex", "trg_layer[]", "trg_tabletype[]", "src_regex", "src_layer[]",
                         "src_tabletype[]"};
    private final String[] LOAD_PARAMETER_DEFS =
            new String[]{"id", "name[]!", "order", "global", "options{}", "trg_technology!", "src_technology!",
                         "default", "trg_regex", "trg_layer[]", "trg_tabletype[]", "src_regex", "src_layer[]",
                         "src_tabletype[]"};
    private final String[] CHECK_PARAMETER_DEFS =
            new String[]{"id", "name[]!", "order", "global", "options{}", "trg_technology!", "default", "trg_temporary",
                         "trg_regex", "trg_layer[]", "trg_tabletype[]"};
    private final String[] INTEGRATION_PARAMETER_DEFS =
            new String[]{"id", "name[]!", "order", "global", "options{}", "trg_technology!", "src_technology",
                         "default", "trg_temporary", "trg_regex", "trg_layer[]", "trg_tabletype[]"};
    private final Integer[] TRG_TEMPORARY_VALUES = {-1, 0, 1};
    private final HashMap<KnowledgeModuleType, List<KnowledgeModuleProperties>> propertiesMapList = new HashMap<>();
    private List<KnowledgeModule> constraints;

    private boolean built = false;

    // The list of fields that the KnowledgeModulePropertiesImpl class posesses
    private final ArrayList<String> knowledgeModulePropertiesFields = new ArrayList<>();

    // Key is group/id and value is error
    private final Map<String, HashSet<String>> errors = new HashMap<>();

    @Inject
    public KnowledgeModulePropertiesProviderImpl(final JodiProperties jodiProperties,
                                                 final ETLSubsystemService etlSubsystemService,
                                                 final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        logger.debug("Initializing KnowledgeModulePropertiesProvider ...");
        this.jodiProperties = jodiProperties;
        this.etlSubsystemService = etlSubsystemService;
        this.errorWarningMessages = errorWarningMessages;
    }

    private void addErrorMessage(String id, String error) {
        HashSet<String> idErrors = null;
        if (errors.containsKey(id)) {
            idErrors = errors.get(id);
        } else {
            idErrors = new HashSet<>();
            errors.put(id, idErrors);
        }

        idErrors.add(error);
    }

    private void normalizeName(KnowledgeModuleProperties p) {
        for (ListIterator<String> iterator = p.getName()
                                              .listIterator(); iterator.hasNext(); ) {
            iterator.set(iterator.next()
                                 .trim()
                                 .replaceAll("\\s{2,}", " "));
        }

    }

    // sets default to first model (presumably source) if not available
    // use default layer names to defer order information ???
    private void buildLists() {
        PropertiesParser<KnowledgeModulePropertiesImpl> parser =
                new PropertiesParser<>(jodiProperties, errorWarningMessages);

        String ID_FIELD = "id";
        for (KnowledgeModuleProperties kmp : parser.parseProperties(TOPIC, ID_FIELD, Arrays.asList(PARAMETER_DEFS),
                                                                    KnowledgeModulePropertiesImpl.class)) {

            normalizeName(kmp);

            KnowledgeModuleType kmType = type(kmp);
            if (kmType == KnowledgeModuleType.Unknown) {
                String msg = errorWarningMessages.formatMessage(110, ERROR_MESSAGE_00110, this.getClass(),
                                                                Arrays.toString(kmp.getName()
                                                                                   .toArray()));
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                addErrorMessage(kmp.getId(), msg);
            }
            propertiesMapList.putIfAbsent(kmType, new ArrayList<>());

            propertiesMapList.get(kmType)
                             .add(kmp);

        }

        Comparator<KnowledgeModuleProperties> comp = KnowledgeModuleProperties::compareOrderTo;

        for (KnowledgeModuleType t : propertiesMapList.keySet()) {
            Collections.sort(propertiesMapList.get(t), comp);
        }

        validate();
    }


    private void validate() {

        List<Integer> validTemporaries = Arrays.asList(this.TRG_TEMPORARY_VALUES);

        // Make sure required types are represented. LKM is not required since not all ETL projects cross DB boundaries
        for (KnowledgeModuleType type : new KnowledgeModuleType[]{KnowledgeModuleType.Integration}) {
            if (!propertiesMapList.containsKey(type)) {
                String msg =
                        errorWarningMessages.formatMessage(140, ERROR_MESSAGE_00140, this.getClass(), type.toString());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                addErrorMessage("", msg);
            }
        }


        for (KnowledgeModuleType kmType : propertiesMapList.keySet()) {
            List<KnowledgeModuleProperties> list = propertiesMapList.get(kmType);

            if (kmType == KnowledgeModuleType.Unknown) {
                continue;
            }

            for (KnowledgeModuleProperties p : list) {
                if (kmType.equals(KnowledgeModuleType.Integration)) {
                    this.validate(INTEGRATION_PARAMETER_DEFS, p);
                } else if (kmType.equals(KnowledgeModuleType.Loading)) {
                    this.validate(LOAD_PARAMETER_DEFS, p);
                } else if (kmType.equals(KnowledgeModuleType.Check)) {
                    this.validate(CHECK_PARAMETER_DEFS, p);
                }

                if (p.getTrg_temporary() != null && !validTemporaries.contains(p.getTrg_temporary())) {
                    String msg = errorWarningMessages.formatMessage(150, ERROR_MESSAGE_00150, this.getClass(),
                                                                    p.getTrg_temporary());
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                    MESSAGE_TYPE.ERRORS);
                    addErrorMessage(p.getId(), msg);
                }

                if (!p.isGlobal() && p.getName()
                                      .size() > 1) {
                    String msg = errorWarningMessages.formatMessage(160, ERROR_MESSAGE_00160, this.getClass(),
                                                                    p.getTrg_temporary());
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                    MESSAGE_TYPE.ERRORS);
                    addErrorMessage(p.getId(), msg);
                }

                validateDataStoreType(p.getTrg_tabletype(), p.getId(), "trg_tabletype");
                validateDataStoreType(p.getSrc_tabletype(), p.getId(), "src_tabletype");

                validateMultiTechnology(p.getName(), p.getId(), p.isGlobal(), p.getSrc_technology(),
                                        p.getTrg_technology());
            }


            // make sure we dont have more than one default per technology or, for LKM, technology pair
            // we dont need to check for null because trg_technology, and for LKM src_technology, are mandatory.
            HashSet<String> defaultSet = new HashSet<>();
            list.stream()
                .filter(KnowledgeModuleProperties::isDefault)
                .forEach(p -> {
                    String key = (type(p) == KnowledgeModuleType.Loading) ? p.getTrg_technology() + " " +
                            p.getSrc_technology() : p.getTrg_technology();
                    if (!defaultSet.add(key)) {
                        String msg = errorWarningMessages.formatMessage(170, ERROR_MESSAGE_00170, this.getClass(), key);
                        errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                        MESSAGE_TYPE.ERRORS);
                        addErrorMessage(p.getId(), msg);
                    }
                });


            // Make sure we have unique order values
            int previousOrder = Integer.MIN_VALUE;
            for (KnowledgeModuleProperties p : list) {
                if (p.getOrder() == null || p.getOrder() == previousOrder) {
                    String msg =
                            errorWarningMessages.formatMessage(180, ERROR_MESSAGE_00180, this.getClass(), p.getOrder());
                    errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                    MESSAGE_TYPE.ERRORS);
                    addErrorMessage(p.getId(), msg);
                    break;
                }
                previousOrder = p.getOrder();
            }

        }
    }


    private void validateMultiTechnology(List<String> names, String id, boolean isGlobal, String src, String trg) {

        if (isGlobal) {
            return;
        }

        for (String name : names) {
            for (KnowledgeModule km : constraints) {
                if (km.getName()
                      .equalsIgnoreCase(name)) {
                    if (km.isMultiTechnology() &&
                            (src == null || src.length() < 1 || trg == null || trg.length() < 1)) {
                        String msg =
                                errorWarningMessages.formatMessage(190, ERROR_MESSAGE_00190, this.getClass(), name);
                        errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                        MESSAGE_TYPE.ERRORS);
                        addErrorMessage(id, msg);
                    } else if (!km.isMultiTechnology() && km.getType()
                                                            .equals(KnowledgeModuleType.Integration)) {
                        if (src != null && src.length() > 0) {
                            String msg =
                                    errorWarningMessages.formatMessage(200, ERROR_MESSAGE_00200, this.getClass(), name);
                            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg,
                                                            MESSAGE_TYPE.ERRORS);
                            addErrorMessage(id, msg);
                        }
                    }
                    break;
                }
            }
        }
    }


    private void validateDataStoreType(List<String> dataStoreTypeList, String id, String property) {
        for (String dataStoreType : dataStoreTypeList) {
            try {
                Enum.valueOf(DataStoreType.class, dataStoreType);
            } catch (IllegalArgumentException iae) {
                String msg =
                        errorWarningMessages.formatMessage(210, ERROR_MESSAGE_00210, this.getClass(), dataStoreType,
                                                           Arrays.toString(DataStoreType.values()));
                logger.warn(msg);
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                addErrorMessage(id, msg);
            }
        }
    }


    /**
     * Validate that the name parameter contains valid KM specification of the same type.  If this fails, return Unknown.
     *
     * @param p
     * @return
     */

    private KnowledgeModuleType type(KnowledgeModuleProperties p) {
        HashSet<KnowledgeModuleType> types = new HashSet<>();
        for (String name : p.getName()) {
            KnowledgeModuleType type = KnowledgeModuleType.Unknown;
            for (KnowledgeModule km : constraints) {
                if (name.equalsIgnoreCase(km.getName())) {
                    type = km.getType();
                    break;
                }
            }
            types.add(type);
        }

        return types.size() != 1 ? KnowledgeModuleType.Unknown : types.iterator()
                                                                      .next();
    }


    /**
     * Since we cant tell if primitives have been set we let the caller pass in a default which is returned in the unknown case.
     *
     * @param p
     * @param field
     * @param defaultValue
     * @return
     */
    private boolean fieldIsPopulated(KnowledgeModuleProperties p, String field, boolean defaultValue) {
        boolean isList = field.contains("[]");
        boolean isMap = field.contains("{}");
        String properField = field.replace("{}", "")
                                  .replace("[]", "")
                                  .replace("!", "");

        // This is a hack but as long as we use a Java keyword this causes issues.
        if (properField.equals("isDefault")) {
            properField = "default";
        }

        try {
            PropertyDescriptor descriptor = new PropertyDescriptor(properField, KnowledgeModulePropertiesImpl.class);

            Method method = PropertyUtils.getReadMethod(descriptor);

            Object rv = method.invoke(p);

            // we should get something intelligible back
            boolean populated = true;
            if (isList) {
                if (rv == null || ((List<?>) rv).size() == 0) {
                    populated = false;
                }
            } else if (isMap) {
                if (rv == null || ((Map<?, ?>) rv).size() == 0) {
                    populated = false;
                }
            } else {
                if (method.getReturnType() == String.class) {
                    if (rv == null || ((String) rv).length() == 0) {
                        populated = false;
                    }
                } else if (method.getReturnType() == Integer.class) {
                    if (rv == null) {
                        populated = false;
                    }
                } else {
                    populated = defaultValue;
                }
            }
            return populated;
        } catch (Exception e) {

            String msg = errorWarningMessages.formatMessage(120, ERROR_MESSAGE_00120, this.getClass(), field,
                                                            e.getMessage());
            logger.error(msg);
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }
    }

    /**
     * @param fields
     * @param p
     */
    private void validate(String[] fields, KnowledgeModuleProperties p) {

        // Check subtractive differences
        ArrayList<String> fieldsList = new ArrayList<>();
        for (String field : fields) {
            fieldsList.add(field.replace("{}", "")
                                .replace("[]", "")
                                .replace("!", ""));
        }


        knowledgeModulePropertiesFields.stream()
                                       .filter(field -> !fieldsList.contains(field) &&
                                               fieldIsPopulated(p, field, false))
                                       .forEach(field -> {
                                           String msg = errorWarningMessages.formatMessage(220, ERROR_MESSAGE_00220,
                                                                                           this.getClass(), field,
                                                                                           type(p));
                                           errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                                                                           msg, MESSAGE_TYPE.ERRORS);
                                           addErrorMessage(p.getId(), msg);
                                       });

        // Check additive differences - at this point only for required fields as we cant add to bean,
        // only require that certain fields are present
        for (String field : fields) {
            if (field.endsWith("!") && !fieldIsPopulated(p, field, true)) {
                String param = field.replace("!", "")
                                    .replace("{}", "")
                                    .replace("[]", "");
                String msg =
                        errorWarningMessages.formatMessage(230, ERROR_MESSAGE_00230, this.getClass(), param, type(p));
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                addErrorMessage(p.getId(), msg);
            }
        }
    }


    @Override
    public List<KnowledgeModuleProperties> getProperties(KnowledgeModuleType type) {
        if (!built) {
            built = true;

            constraints = etlSubsystemService.getKMs();


            // Build the list of fields used by KnowledgeModulePropertiesImpl which have a read method (e.g. bean)
            for (Field field : KnowledgeModulePropertiesImpl.class.getDeclaredFields()) {
                PropertyDescriptor descriptor = null;
                try {
                    descriptor = new PropertyDescriptor(field.getName(), KnowledgeModulePropertiesImpl.class);
                    if (PropertyUtils.getReadMethod(descriptor) != null) {
                        knowledgeModulePropertiesFields.add(field.getName());
                    }

                } catch (IntrospectionException e) {
                    logger.debug(
                            "Cannot find method on KnowledgeModulePropertiesImpl.class, this method will be ignored.");
                }


            }

            buildLists();

            if (errors.size() > 0) {
                StringBuilder sb = new StringBuilder();
                //sb.append("Errors found in Knowledge Module Configuration Rules %s"+ newLine);
                for (String id : errors.keySet()) {
                    if (StringUtils.hasLength(id)) {
                        sb.append("Knowledge Module configuration rule (" + id + ") contains errors." + newLine);
                    }
                    for (String error : errors.get(id)) {
                        if (StringUtils.hasLength(id)) {
                            sb.append("\t");
                        }
                        sb.append(error + newLine);
                    }
                }
                String msg =
                        errorWarningMessages.formatMessage(100, ERROR_MESSAGE_00100, this.getClass(), sb.toString());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
            }

        }

        if (!errors.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(130, ERROR_MESSAGE_00130, this.getClass(),
                                                            getErrorMessages().size());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);

            throw new KnowledgeModulePropertiesException(getErrorMessages());
        }
        return propertiesMapList.get(type) != null ? propertiesMapList.get(type)
                                                   : Collections.<KnowledgeModuleProperties>emptyList();
    }

    @Override
    public List<String> getErrorMessages() {
        ArrayList<String> list = new ArrayList<>();
        for (String key : errors.keySet()) {
            list.addAll(errors.get(key)
                              .stream()
                              .map(error -> "Knowledge Module rule " +
                                      (StringUtils.isEmpty(key) ? "" : "(" + key + ") ") + "error.  " + error)
                              .collect(Collectors.toList()));
        }
        return list;
    }


}

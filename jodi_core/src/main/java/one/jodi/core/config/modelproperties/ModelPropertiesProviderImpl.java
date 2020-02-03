package one.jodi.core.config.modelproperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.PropertiesParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Parses properties related to model definitions and performs some additional
 * validations.
 */
@Singleton //should be defined as eager Singleton in model
public class ModelPropertiesProviderImpl implements ModelPropertiesProvider {

    /**
     * Prefix that is used in properties file to describe properties related to
     * the model configuration.
     */
    private static final String MODEL_TOPIC = "model";

    private final static Logger logger = LogManager.getLogger(ModelPropertiesProviderImpl.class);

    private final static String ERROR_MESSAGE_03030 = "The model properties contain %1$s configuration errors.";
    private final static String ERROR_MESSAGE_03040 = "Duplicate default flag defined for model code '%1$s'. Only one model may be the default model.";
    private final static String ERROR_MESSAGE_03050 = "The order number %1$s for model '%2$s' has been previously defined. Order numbers must be unique.";
    private final static String ERROR_MESSAGE_03060 = "The model code '%1$s' in group '%2$s' has been previously defined. Order numbers must be unique.";
    private final static String[] PARAMETER_DEFS = new String[]{"code!", "default",
            "ignoredbyheuristics", "order!", "layer", "prefix[]", "postfix[]", "journalized", "jkmoptions[]", "jkm", "subscribers[]"};
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final JodiProperties jodiProperties;
    private List<ModelProperties> mpList;
    private PropertiesParser<ModelPropertiesImpl> mpParser;

    // contains errors found during initialization
    private List<String> errorMessages = new ArrayList<>();

    @SuppressWarnings("unchecked")
    @Inject
    public ModelPropertiesProviderImpl(final JodiProperties jodiProperties,
                                       final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        logger.debug("initialize ModelPropertiesProvider ...");
        this.jodiProperties = jodiProperties;
        this.errorWarningMessages = errorWarningMessages;
        this.mpParser = new PropertiesParser<>(this.jodiProperties,
                errorWarningMessages);
        this.mpList = (List<ModelProperties>) (List<?>) mpParser
                .parseProperties(MODEL_TOPIC, Arrays.asList(PARAMETER_DEFS),
                        ModelPropertiesImpl.class);
        //sort list according to the order member.
        Comparator<ModelProperties> comp =
                ModelProperties::compareOrderTo;
        Collections.sort(mpList, comp);

        //make list unmodifiable
        this.mpList = Collections.unmodifiableList(mpList);
        validate();
    }

    // sets default to first model (presumably source) if not available
    // use default layer names to defer order information ???
    private void validate() {

        int countDefaultFlags = 0;
        //find first occurrence of a duplicate default flag
        for (ModelProperties mp : mpList) {
            if (mp.isDefault()) {
                countDefaultFlags++;
            }
            if (countDefaultFlags > 1) {
                String msg = errorWarningMessages.formatMessage(3040,
                        ERROR_MESSAGE_03040, this.getClass(), mp.getCode());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                errorMessages.add(msg);
                break;
            }
        }

        int previousOrder = Integer.MIN_VALUE;
        for (ModelProperties mp : mpList) {
            if (mp.getOrder() == previousOrder) {
                String msg = errorWarningMessages.formatMessage(3050,
                        ERROR_MESSAGE_03050, this.getClass(), mp.getOrder(), mp.getCode());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                errorMessages.add(msg);
                break;
            }
            previousOrder = mp.getOrder();
        }

        Set<String> foundModels = new HashSet<>();
        for (ModelProperties mp : mpList) {
            if (foundModels.contains(mp.getCode())) {
                String msg = errorWarningMessages.formatMessage(3060,
                        ERROR_MESSAGE_03060, this.getClass(), mp.getCode(), mp.getLayer());
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                errorMessages.add(msg);
                break;
            }
            foundModels.add(mp.getCode());
        }
    }

    @Override
    public List<ModelProperties> getConfiguredModels() {

        if (!errorMessages.isEmpty()) {
            String msg = errorWarningMessages.formatMessage(3030,
                    ERROR_MESSAGE_03030, this.getClass(), errorMessages.size());
            errorWarningMessages.addMessage(
                    errorWarningMessages.assignSequenceNumber(), msg,
                    MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        }

        return mpList;
    }

    @Override
    public List<ModelProperties> getConfiguredModels(List<String> layers) {

        List<ModelProperties> modelsInLayer = new ArrayList<>();

        for (ModelProperties mp : getConfiguredModels()) {
            for (String layerName : layers) {
                if ((mp.getLayer() != null) && (!mp.getLayer().equals("")) && (mp.getLayer().equalsIgnoreCase(layerName))) {
                    modelsInLayer.add(mp);
                    break;
                }
            }
        }

        return Collections.unmodifiableList(modelsInLayer);
    }

}

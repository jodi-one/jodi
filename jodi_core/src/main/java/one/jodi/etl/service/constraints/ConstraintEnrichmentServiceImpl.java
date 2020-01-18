package one.jodi.etl.service.constraints;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.util.Version;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.etl.internalmodel.ConditionConstraint;
import one.jodi.etl.internalmodel.Constraint;
import one.jodi.etl.internalmodel.KeyConstraint;
import one.jodi.etl.internalmodel.ReferenceConstraint;
import one.jodi.etl.internalmodel.impl.ConstraintImpl;
import one.jodi.etl.internalmodel.impl.ReferenceConstraintImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class ConstraintEnrichmentServiceImpl implements ConstraintEnrichmentService {

    private final static String newLine = System.getProperty("line.separator");
    private final JodiProperties properties;
    private final ModelPropertiesProvider modelPropertiesProvider;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final Logger logger = LogManager.getLogger(ConstraintEnrichmentServiceImpl.class);


    @Inject
    protected ConstraintEnrichmentServiceImpl(final JodiProperties properties,
                                              ModelPropertiesProvider modelPropertiesProvider,
                                              ErrorWarningMessageJodi errorWarningMessages) {
        this.properties = properties;
        this.modelPropertiesProvider = modelPropertiesProvider;
        this.errorWarningMessages = errorWarningMessages;
    }

    protected void enrich(KeyConstraint constraint) {
    }


    protected void enrich(ConditionConstraint constraint) {

    }

    protected void enrich(ReferenceConstraint constraint) {

        if (properties.getPropertyKeys().contains(constraint.getPrimaryModel())) {
            ((ReferenceConstraintImpl) constraint).setPrimaryModel(properties.getProperty(constraint.getPrimaryModel()));
        }
    }

    public void enrich(Constraint constraint) {
        if (properties.getPropertyKeys().contains(constraint.getModel())) {
            ((ConstraintImpl) constraint).setModel(properties.getProperty(constraint.getModel()));
        }

        if (properties.includeDetails()) {
            StringBuilder description = new StringBuilder();
            description.append("Constraint created by bulk operation from file ");
            description.append(constraint.getFileName());
            description.append(newLine);
            description.append(".  Imported by ");
            description.append(System.getProperty("user.name"));
            description.append(" on ");
            DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date date = new Date();
            description.append(dateFormat.format(date));
            description.append(newLine);
            description.append("Created by Jodi version ");
            description.append(Version.getProductVersion());
            description.append(" with build date ");
            description.append(Version.getBuildDate());
            description.append(" ");
            description.append(Version.getBuildTime());
            ((ConstraintImpl) constraint).setComments(description.toString());
        }

        if (constraint instanceof ReferenceConstraint) {
            enrich((ReferenceConstraint) constraint);
        }
    }

    private String getModelCode(String model) {

        Optional<ModelProperties> mopro = modelPropertiesProvider.getConfiguredModels().stream()
                .filter(mp -> {
                    return mp.getCode() != null && mp.getCode().trim().equals(model);
                })
                .sorted((mp1, mp2) -> Integer.compare(mp1.getOrder(), mp2.getOrder()))
                .findFirst();

        if (!mopro.isPresent()) {
            String models = modelPropertiesProvider.getConfiguredModels()
                    .stream()
                    .map(m -> m.getCode())
                    .reduce((t, u) -> t + "," + u)
                    .get();
            String msg = "Cannot map model '" + model + "' to a model code amongst (" + models + ").  Configure Jodi Properties.";
            logger.error(msg);
            errorWarningMessages.addMessage(msg, MESSAGE_TYPE.ERRORS);
            throw new RuntimeException(msg);
        } else {
            return mopro.get().getModelID() + ".code";
        }

    }

    @Override
    public void reduce(Constraint constraint) {
        ((ConstraintImpl) constraint).setModel(getModelCode(constraint.getSchema()));

        if (constraint instanceof ReferenceConstraint) {
            reduce((ReferenceConstraint) constraint);
        }

    }


    protected void reduce(KeyConstraint constraint) {

    }

    protected void reduce(ConditionConstraint constraint) {
    }


    protected void reduce(ReferenceConstraint constraint) {
        ((ReferenceConstraintImpl) constraint).setPrimaryModel(getModelCode(constraint.getPrimaryModel()));
    }


}

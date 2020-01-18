package one.jodi.etl.builder.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class DictionaryModelLogicalSchemaImpl implements DictionaryModelLogicalSchema {

    private final ModelPropertiesProvider modelPropertiesProvider;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final SchemaMetaDataProvider metadataProvider;

    @Inject
    public DictionaryModelLogicalSchemaImpl(final ModelPropertiesProvider modelPropertiesProvider,
                                            final ErrorWarningMessageJodi errorWarningMessages,
                                            final SchemaMetaDataProvider metadataProvider) {
        this.modelPropertiesProvider = modelPropertiesProvider;
        this.errorWarningMessages = errorWarningMessages;
        this.metadataProvider = metadataProvider;
    }

    @Override
    public String translateToLogicalSchema(final String logicalSchemaInproperties) {
        Optional<ModelProperties> modelProperties =
                this.modelPropertiesProvider
                        .getConfiguredModels()
                        .stream()
                        .filter(mp -> (mp.getModelID() + ".code")
                                .equalsIgnoreCase(logicalSchemaInproperties))
                        .findFirst();
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String calledFrom = stackTraceElements[2].getClassName() + "." +
                stackTraceElements[2].getMethodName();
        if (!modelProperties.isPresent()) {
            String message = String.format("Can't find logical model for property %s called from %s.",
                    logicalSchemaInproperties,
                    calledFrom
            );
            String msg = this.errorWarningMessages.formatMessage(85123, message,
                    this.getClass());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return logicalSchemaInproperties;
        }

        Map<String, String> modelsAndSchemas = this.metadataProvider
                .translateModelToLogicalSchema();
        Optional<Entry<String, String>> codeToSchemaName =
                modelsAndSchemas.entrySet()
                        .stream()
                        .filter(e -> e.getKey()
                                .equals(modelProperties.get().getCode()))
                        .findFirst();
        if (!codeToSchemaName.isPresent()) {
            String message = String.format("Can't find logical schema in ODI " +
                            "repository with model code %s.",
                    modelProperties.get().getCode());
            String msg = this.errorWarningMessages.formatMessage(85124, message,
                    this.getClass());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return logicalSchemaInproperties;
        }

        return codeToSchemaName.get().getValue();
    }

}

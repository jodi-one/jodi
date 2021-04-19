package one.jodi.odi12.variables;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.service.SequenceServiceException;
import one.jodi.etl.builder.impl.DictionaryModelLogicalSchema;
import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.internalmodel.Variables;
import one.jodi.etl.internalmodel.impl.VariableImpl;
import one.jodi.etl.internalmodel.impl.VariablesImpl;
import one.jodi.etl.service.variables.VariableServiceProvider;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.domain.project.OdiVariable;
import oracle.odi.domain.topology.OdiLogicalSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

public class Odi12VariableServiceProvider implements VariableServiceProvider {
    private static final String ERROR_MESSAGE_6003 = "Variable type unknown.";
    private static final String ERROR_MESSAGE_6004 = "Can't find logicalschema for variable: %s.";
    private static final Logger logger = LogManager.getLogger(Odi12VariableServiceProvider.class);
    private final ErrorWarningMessageJodi errorWarningMessageJodi;
    private final OdiVariableAccessStrategy accessStrategy;
    private final DictionaryModelLogicalSchema dictionaryModelLogicalSchema;

    @Inject
    public Odi12VariableServiceProvider(final OdiVariableAccessStrategy accessStrategy,
                                        final DatabaseMetadataService databaseMetadataService,
                                        final ErrorWarningMessageJodi errorWarningMessageJodi,
                                        final ModelPropertiesProvider mpp,
                                        final DictionaryModelLogicalSchema dictionaryModelLogicalSchema) {
        this.accessStrategy = accessStrategy;
        this.errorWarningMessageJodi = errorWarningMessageJodi;
        this.dictionaryModelLogicalSchema = dictionaryModelLogicalSchema;
    }

    @Override
    public void create(Variable iV, String projectCode, String dateFormat) {
        this.accessStrategy.create(iV, projectCode, dateFormat);
    }

    @Override
    public void delete(Variable internalVariable, String projectCode) {
        this.accessStrategy.delete(internalVariable, projectCode);
    }

    @Override
    public Variables findAll() {
        Collection<OdiVariable> odiVariables = this.accessStrategy.findAllVariables();
        odiVariables.addAll(this.accessStrategy.findAllGlobalVariables());
        return transform(odiVariables);
    }

    private Variables transform(Collection<OdiVariable> allVariables) {
        Collection<Variable> variables = new ArrayList<Variable>();
        allVariables.stream().forEach(odiVariable -> variables.add(transform(odiVariable)));
        return new VariablesImpl(variables);
    }

    private Variable transform(OdiVariable odiVariable) {
        if (odiVariable == null) {
            return null;
        }
        assert (odiVariable.getName() != null);
        assert (odiVariable.getDataType() != null);
        assert (odiVariable.getLogicalSchema().getName() != null);
        assert (odiVariable.getValuePersistence() != null);
        return new VariableImpl(odiVariable.getName(), mapFrom(odiVariable.getDataType()),
                getModelFromSchema(odiVariable.getLogicalSchema(), odiVariable.getName()),
                odiVariable.getRefreshQuery() != null ? odiVariable.getRefreshQuery().getAsString() : "",
                odiVariable.isGlobal(),
                odiVariable.getDefaultValue() == null ? null : odiVariable.getDefaultValue().toString(),
                odiVariable.getDescription(), mapFrom(odiVariable.getValuePersistence()));
    }

    private String getModelFromSchema(OdiLogicalSchema logicalSchema, String variableName) {
        if (logicalSchema == null || logicalSchema.getName() == null) {
            String message = errorWarningMessageJodi.formatMessage(6004, ERROR_MESSAGE_6004, this.getClass(), variableName);
            errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            return "";
        }
        return this.dictionaryModelLogicalSchema.translateToLogicalSchema(logicalSchema.getName());
    }

    private Variable.Datatype mapFrom(final OdiVariable.DataType dataType) {
        if (dataType.equals(OdiVariable.DataType.DATE)) {
            return Variable.Datatype.DATE;
        } else if (dataType.equals(OdiVariable.DataType.LONG_TEXT)) {
            return Variable.Datatype.TEXT;
        } else if (dataType.equals(OdiVariable.DataType.SHORT_TEXT)) {
            return Variable.Datatype.ALPHANUMERIC;
        } else if (dataType.equals(OdiVariable.DataType.NUMERIC)) {
            return Variable.Datatype.NUMERIC;
        } else {
            String message = this.errorWarningMessageJodi.formatMessage(6003, ERROR_MESSAGE_6003, this.getClass());
            logger.error(message);
            errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new SequenceServiceException(message);
        }
    }

    private Variable.Keephistory mapFrom(final OdiVariable.ValuePersistence valuePersistence) {
        if (valuePersistence.equals(OdiVariable.ValuePersistence.HISTORIZE)) {
            return Variable.Keephistory.ALL_VALUES;
        } else if (valuePersistence.equals(OdiVariable.ValuePersistence.LATEST_VALUE)) {
            return Variable.Keephistory.LATEST_VALUE;
        } else if (valuePersistence.equals(OdiVariable.ValuePersistence.NON_PERSISTENT)) {
            return Variable.Keephistory.NO_HISTORY;
        } else if (valuePersistence.equals(OdiVariable.ValuePersistence.NON_TRACKABLE)) {
            return Variable.Keephistory.NO_HISTORY;
        } else {
            String message = this.errorWarningMessageJodi.formatMessage(6003, ERROR_MESSAGE_6003, this.getClass());
            logger.error(message);
            errorWarningMessageJodi.addMessage(message, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new SequenceServiceException(message);
        }
    }
}

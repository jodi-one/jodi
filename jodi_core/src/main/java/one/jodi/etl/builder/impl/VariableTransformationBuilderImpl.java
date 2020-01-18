package one.jodi.etl.builder.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.variables.DataTypeType;
import one.jodi.core.variables.KeepHistoryType;
import one.jodi.core.variables.Variables;
import one.jodi.etl.builder.VariableTransformationBuilder;
import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.internalmodel.impl.VariableImpl;
import one.jodi.etl.internalmodel.impl.VariablesImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

public class VariableTransformationBuilderImpl implements VariableTransformationBuilder {

    private final Logger logger =
            LogManager.getLogger(VariableTransformationBuilderImpl.class);
    private final ErrorWarningMessageJodi errorWarningMessageJodi;
    private final DictionaryModelLogicalSchema dictionaryModelLogicalSchema;

    @Inject
    public VariableTransformationBuilderImpl(final ErrorWarningMessageJodi errorWarningMessageJodi,
                                             final DictionaryModelLogicalSchema dictionaryModelLogicalSchema) {
        this.errorWarningMessageJodi = errorWarningMessageJodi;
        this.dictionaryModelLogicalSchema = dictionaryModelLogicalSchema;
    }

    @Override
    public VariablesImpl transmute(Variables externalVariables) {
        Collection<Variable> internalVars = new ArrayList<>();
        externalVariables.getVariable().forEach(ev -> {
            internalVars.add(transmute(ev));
        });
        return new VariablesImpl(internalVars);
    }

    private Variable transmute(one.jodi.core.variables.Variable eV) {
        String name = eV.getName();
        Variable.Datatype dataType =
                Variable.Datatype.valueOf(eV.getDataType().name());
        final String schema = getSchemaFromModel(eV.getModel(), eV.getName());
        String query = eV.getQuery();
        Boolean global = eV.isGlobal();
        String defaultValue = eV.getDefaultValue();
        String description = eV.getDescription();
        Variable.Keephistory keephistory = eV.getKeepHistory() != null ?
                Variable.Keephistory.valueOf(eV.getKeepHistory().name()) : Variable.Keephistory.LATEST_VALUE;
        return new VariableImpl(name, dataType, schema, query, global, defaultValue,
                description, keephistory);
    }

    private String getSchemaFromModel(String model, String name) {
        return this.dictionaryModelLogicalSchema.translateToLogicalSchema(model);
    }

    @Override
    public Variables transmute(one.jodi.etl.internalmodel.Variables inV) {
        Variables exV =
                new Variables();
        inV.getVariables()
                .forEach(aInVar -> exV.getVariable()
                        .add(transformVarIntoExternalModel(aInVar)));

        return exV;
    }

    private one.jodi.core.variables.Variable transformVarIntoExternalModel(Variable internalVar) {
        one.jodi.core.variables.Variable external =
                new one.jodi.core.variables.Variable();
        external.setDataType(mapFrom(internalVar.getDataType()));
        external.setDefaultValue(internalVar.getDefaultValue());
        external.setDescription(internalVar.getDescription());
        external.setGlobal(internalVar.getGlobal());
        external.setKeepHistory(mapFrom(internalVar.getKeephistory()));
        external.setName(internalVar.getName());
        external.setQuery(internalVar.getQuery());
        external.setModel(internalVar.getSchema());
        return external;
    }

    private KeepHistoryType mapFrom(Variable.Keephistory keephistory) {
        if (keephistory.equals(Variable.Keephistory.ALL_VALUES)) {
            return KeepHistoryType.ALL_VALUES;
        } else if (keephistory.equals(Variable.Keephistory.LATEST_VALUE)) {
            return KeepHistoryType.LATEST_VALUE;
        } else if (keephistory.equals(Variable.Keephistory.NO_HISTORY)) {
            return KeepHistoryType.NO_HISTORY;
        } else {
            String msg = "Can't map Odi model for " + "variable KeepHistoryType into GBU " +
                    "model for " + "variable KeepHistoryType.";
            logger.error(msg);
            this.errorWarningMessageJodi.addMessage(msg,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new UnsupportedOperationException(msg);
        }
    }

    private DataTypeType mapFrom(Variable.Datatype dataType) {
        if (dataType.equals(Variable.Datatype.ALPHANUMERIC)) {
            return DataTypeType.ALPHANUMERIC;
        } else if (dataType.equals(Variable.Datatype.DATE)) {
            return DataTypeType.DATE;
        } else if (dataType.equals(Variable.Datatype.NUMERIC)) {
            return DataTypeType.NUMERIC;
        } else if (dataType.equals(Variable.Datatype.TEXT)) {
            return DataTypeType.TEXT;
        } else {
            String msg = "Can't map Odi model for " + "variable datatype into GBU " +
                    "model for " + "variable datatype.";
            logger.error(msg);
            this.errorWarningMessageJodi.addMessage(msg,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new UnsupportedOperationException(msg);
        }
    }
}

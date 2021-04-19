package one.jodi.odi12.variables;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.util.Version;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.service.VariableServiceException;
import one.jodi.etl.internalmodel.Variable;
import one.jodi.etl.internalmodel.impl.VariableImpl;
import one.jodi.odi.variables.OdiVariableAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.OdiVariable;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.project.finder.IOdiVariableFinder;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.finder.IOdiLogicalSchemaFinder;
import oracle.odi.domain.xrefs.expression.Expression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public class Odi12VariableAccessStrategy implements OdiVariableAccessStrategy {

    private static final Logger logger = LogManager.getLogger(Odi12VariableAccessStrategy.class);
    private static final String newLine = System.getProperty("line.separator");
    private final OdiInstance odiInstance;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final String projectCode;
    private final JodiProperties jodiProperties;

    @Inject
    public Odi12VariableAccessStrategy(final OdiInstance odiInstance, final JodiProperties jodiProperties,
                                       final ErrorWarningMessageJodi errorWarningMessageJodi) {
        this.odiInstance = odiInstance;
        this.projectCode = jodiProperties.getProjectCode();
        this.errorWarningMessages = errorWarningMessageJodi;
        this.jodiProperties = jodiProperties;
    }

    @Override
    public OdiVariable findProjectVariable(final String projectVariableName, final String projectCode) {
        return ((IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                .getFinder(OdiVariable.class)).findByName(projectVariableName,
                                                                                          projectCode);
    }

    @Override
    public Collection<OdiVariable> findAllVariables() {
        IOdiVariableFinder finder = (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiVariable.class);
        Collection<OdiVariable> all = finder.findByProject(this.projectCode);
        return all;
    }

    @Override
    public Collection<OdiVariable> findAllGlobalVariables() {
        IOdiVariableFinder finder = (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiVariable.class);
        return finder.findAllGlobals();
    }

    @Override
    public Collection<OdiVariable> findAllProjectVariables(final String projectCode) {
        IOdiVariableFinder finder = (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiVariable.class);
        return finder.findByProject(projectCode);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void create(final Variable internalVar, final String projectCode, final String dateFormat) {
        OdiProject project = internalVar.getGlobal() ? null : findProject(projectCode);
        boolean exists = variableExist(project, internalVar.getName());
        OdiVariable odiVariable = findOrCreateVariable(project, internalVar.getName());
        odiVariable.setDataType(mapFrom(internalVar.getDataType()));
        if (internalVar.getDataType()
                       .equals(Variable.Datatype.TEXT)) {
            odiVariable.setDefaultValue(internalVar.getDefaultValue());
        } else if (internalVar.getDataType()
                              .equals(Variable.Datatype.ALPHANUMERIC)) {
            odiVariable.setDefaultValue(internalVar.getDefaultValue());
        } else if (internalVar.getDataType()
                              .equals(Variable.Datatype.NUMERIC)) {
            if (internalVar.getDefaultValue() != null) {
                odiVariable.setDefaultValue(Long.parseLong(internalVar.getDefaultValue()));
            }
        } else if (internalVar.getDataType()
                              .equals(Variable.Datatype.DATE)) {
            if (internalVar.getDefaultValue() != null) {
                SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
                try {
                    odiVariable.setDefaultValue(formatter.parse(internalVar.getDefaultValue()));
                } catch (ParseException e) {
                    String msg =
                            "Can't parse default value of date format of " + "VariableImpl " + internalVar.getName();
                    logger.error(msg, e);
                    this.errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
                    throw new VariableServiceException(msg, e);
                }
            }
        } else {
            String msg = "Can't determine datatype for variable." + internalVar.getName();
            logger.error(msg);
            this.errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new VariableServiceException(msg);
        }
        String originalDescription = internalVar.getDescription() != null ? internalVar.getDescription() : "";
        if (jodiProperties.includeDetails()) {
            StringBuilder description = new StringBuilder();
            description.append("Variable created by bulk operation from file ");
            description.append(newLine);
            description.append(".  Imported by ");
            description.append(System.getProperty("user.name"));
            description.append(" on ");
            DateFormat dateFormat1 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date date = new Date();
            description.append(dateFormat1.format(date));
            description.append(newLine);
            description.append("Created by Jodi version ");
            description.append(Version.getProductVersion());
            description.append(" with build date ");
            description.append(Version.getBuildDate());
            description.append(" ");
            description.append(Version.getBuildTime());
            ((VariableImpl) internalVar).setDescription(description.toString() + newLine + originalDescription);
        }
        odiVariable.setDescription(internalVar.getDescription());
        odiVariable.setLogicalSchema(findLogicalSchema(internalVar.getSchema()));
        odiVariable.setName(internalVar.getName());
        odiVariable.setRefreshQuery(new Expression(internalVar.getQuery(), null, Expression.SqlGroupType.NONE));
        odiVariable.setValuePersistence(mapFrom(internalVar.getKeephistory()));
        if (exists) {
            odiInstance.getTransactionalEntityManager()
                       .merge(odiVariable);
        } else {
            odiInstance.getTransactionalEntityManager()
                       .persist(odiVariable);
        }
        logger.info(
                "Created " + (odiVariable.isGlobal() ? "global " : " ") + "variable:" + internalVar.getName() + ".");
    }


    private OdiVariable findOrCreateVariable(OdiProject project, String variableName) {
        IOdiVariableFinder finder = (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiVariable.class);
        final OdiVariable var;
        if (project == null) {
            var = finder.findGlobalByName(variableName);
        } else {
            var = finder.findByName(variableName, project.getCode());
        }
        if (var != null) {
            return var;
        }
        return new OdiVariable(project, variableName);
    }

    private boolean variableExist(OdiProject project, String variableName) {
        IOdiVariableFinder finder = (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiVariable.class);
        final OdiVariable var;
        if (project == null) {
            var = finder.findGlobalByName(variableName);
        } else {
            var = finder.findByName(variableName, project.getCode());
        }
        return var != null;
    }

    // transaction semantics defined in calling method
    private void deleteVariableScenario(final OdiVariable variable) {
        assert (odiInstance.getTransactionalEntityManager()
                           .isOpen());
        assert (variable != null);
        Number variableId = variable.getVariableId();
        Collection<OdiScenario> odiScenarios = ((IOdiScenarioFinder) odiInstance.getTransactionalEntityManager()
                                                                                .getFinder(
                                                                                        OdiScenario.class)).findBySourceVariable(
                variableId);
        assert (odiScenarios != null);
        if (!odiScenarios.isEmpty()) {
            logger.debug("Attempt to remove " + odiScenarios.size() + " scenario for: " + variable.getName());
        }
        odiScenarios.stream()
                    .peek(s -> logger.debug("deleted scenario '" + s.getName()))
                    .forEach(s -> odiInstance.getTransactionalEntityManager()
                                             .remove(s));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void delete(final Variable internalVar, final String projectCode) {
        IOdiVariableFinder finder = (IOdiVariableFinder) odiInstance.getTransactionalEntityManager()
                                                                    .getFinder(OdiVariable.class);
        final OdiVariable var;
        if (internalVar.getGlobal()) {
            var = finder.findGlobalByName(internalVar.getName());
        } else {
            var = finder.findByName(internalVar.getName(), projectCode);
        }
        if (var == null) {
            return;
        }
        deleteVariableScenario(var);
        odiInstance.getTransactionalEntityManager()
                   .remove(var);
        logger.info("Removed variable:" + internalVar.getName());
    }

    protected OdiVariable.DataType mapFrom(Variable.Datatype dataType) {
        if (dataType.equals(Variable.Datatype.ALPHANUMERIC)) {
            return OdiVariable.DataType.SHORT_TEXT;
        } else if (dataType.equals(Variable.Datatype.DATE)) {
            return OdiVariable.DataType.DATE;
        } else if (dataType.equals(Variable.Datatype.NUMERIC)) {
            return OdiVariable.DataType.NUMERIC;
        } else if (dataType.equals(Variable.Datatype.TEXT)) {
            return OdiVariable.DataType.LONG_TEXT;
        } else {
            String msg = "Can't map Odi model for variable datatype into GBU " + "model for variable datatype.";
            logger.error(msg);
            this.errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new UnsupportedOperationException(msg);
        }
    }

    protected OdiVariable.ValuePersistence mapFrom(Variable.Keephistory keephistory) {
        if (keephistory.equals(Variable.Keephistory.ALL_VALUES)) {
            return OdiVariable.ValuePersistence.HISTORIZE;
        } else if (keephistory.equals(Variable.Keephistory.LATEST_VALUE)) {
            return OdiVariable.ValuePersistence.LATEST_VALUE;
        } else if (keephistory.equals(Variable.Keephistory.NO_HISTORY)) {
            return OdiVariable.ValuePersistence.NON_PERSISTENT;
        } else { // VariableImpl.Keephistory.NONE and others
            String msg = "Can't map Odi model for variable keephistory into GBU " + "model for variable keephistory.";
            logger.error(msg);
            this.errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new UnsupportedOperationException(msg);
        }
    }


    @Cached
    protected OdiLogicalSchema findLogicalSchema(String schema) {
        IOdiLogicalSchemaFinder finder = (IOdiLogicalSchemaFinder) odiInstance.getTransactionalEntityManager()
                                                                              .getFinder(OdiLogicalSchema.class);
        OdiLogicalSchema lSchema = finder.findByName(schema);
        assert (lSchema != null);
        return lSchema;
    }

    @Cached
    protected OdiProject findProject(String projectCode) {
        IOdiProjectFinder finder = (IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                                                                  .getFinder(OdiProject.class);
        OdiProject project = finder.findByCode(projectCode);
        assert (project != null);
        return project;
    }
   
   /*public void deleteScenario(String scenarioName) {
       scenarioName = scenarioName.toUpperCase();
       OdiScenario odiScenario = ((IOdiScenarioFinder) odiInstance
               .getTransactionalEntityManager().getFinder(OdiScenario.class))
               .findLatestByName(scenarioName);
       if (odiScenario != null) {
           odiInstance.getTransactionalEntityManager().remove(odiScenario);
           logger.debug("deleted scenario: " + scenarioName);
       } else {
           logger.debug("Scenario not found: " + scenarioName);
       }
   }*/
}
package one.jodi.odi12.procedure;

import com.google.inject.Inject;
import one.jodi.base.annotations.Cached;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.procedure.Option;
import one.jodi.core.procedure.OptionType;
import one.jodi.core.procedure.impl.OptionImpl;
import one.jodi.etl.builder.impl.DictionaryModelLogicalSchema;
import one.jodi.etl.internalmodel.procedure.*;
import one.jodi.etl.internalmodel.procedure.impl.CommandInternalImpl;
import one.jodi.etl.internalmodel.procedure.impl.CommandInternalImpl.Location;
import one.jodi.etl.internalmodel.procedure.impl.ProcedureInternalImpl;
import one.jodi.etl.internalmodel.procedure.impl.TaskInternalImpl;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.procedure.ProcedureServiceProvider;
import one.jodi.logging.OdiLogHandler;
import one.jodi.odi12.folder.Odi12FolderServiceProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.core.persistence.IOdiEntityManager;
import oracle.odi.core.persistence.transaction.ITransactionDefinition;
import oracle.odi.core.persistence.transaction.ITransactionManager;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.TransactionSystemException;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.project.*;
import oracle.odi.domain.project.OdiProcedureLineCmd.IsolationLevel;
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.project.finder.IOdiUserProcedureFinder;
import oracle.odi.domain.runtime.scenario.OdiScenario;
import oracle.odi.domain.runtime.scenario.finder.IOdiScenarioFinder;
import oracle.odi.domain.topology.OdiContext;
import oracle.odi.domain.topology.OdiLogicalSchema;
import oracle.odi.domain.topology.OdiTechnology;
import oracle.odi.domain.topology.finder.IOdiContextFinder;
import oracle.odi.domain.topology.finder.IOdiLogicalSchemaFinder;
import oracle.odi.domain.topology.finder.IOdiTechnologyFinder;
import oracle.odi.domain.xrefs.expression.Expression;
import oracle.odi.domain.xrefs.expression.Expression.SqlGroupType;
import oracle.odi.generation.IOdiScenarioGenerator;
import oracle.odi.generation.OdiScenarioGeneratorException;
import oracle.odi.generation.support.OdiScenarioGeneratorImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static one.jodi.odi12.folder.Odi12FolderHelper.getFolderPath;

public class Odi12ProcedureServiceProvider implements ProcedureServiceProvider {

    final static Logger logger =
            LogManager.getLogger(Odi12ProcedureServiceProvider.class);

    private final String ERROR_MESSAGE_85020 =
            "Unable to find user procedures in ODI project with code %1$s.";
    private final String ERROR_MESSAGE_85100 =
            "When creating procedure '%1$s' the selected %2$s technology '%3$s' was not " +
                    "recognized. The default value 'ORACLE' is used instead.";
    private final String ERROR_MESSAGE_85110 =
            "When creating procedure '%1$s' the value '%2$s' for transaction isolation" +
                    " was not recognized. The value 'NONE' is used instead.";
    private final String ERROR_MESSAGE_85130 =
            "When creating procedure '%1$s' the logical schema '%2$s' was not " +
                    "found. Any logical schema is selected.";
    private final String ERROR_MESSAGE_85140 =
            "The scenario for procedure '%1$s' was not created: %2$s.";
    private final String ERROR_MESSAGE_85150 =
            "When creating procedure '%1$s' the log counter value '%2$s' was not " +
                    "recognized. The default value 'NONE' is used instead.";
    private final String ERROR_MESSAGE_85160 =
            "The scenario for procedure '%1$s' was not created due to errors while " +
                    "compiling a procedure: %2$s.";

    private final OdiInstance odiInstance;
    private final Odi12FolderServiceProvider folderService;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final DictionaryModelLogicalSchema dictionaryModelLogicalSchema;
    // this is due to protected constructor.
    private final ProcedureOptionBuilder procedureOptionBuilder;


    @Inject
    Odi12ProcedureServiceProvider(final OdiInstance odiInstance,
                                  final Odi12FolderServiceProvider folderService,
                                  final ErrorWarningMessageJodi errorWarningMessages,
                                  final DictionaryModelLogicalSchema dictionaryModelLogicalSchema,
                                  final ProcedureOptionBuilder procedureOptionBuilder) {
        super();
        this.odiInstance = odiInstance;
        this.folderService = folderService;
        this.errorWarningMessages = errorWarningMessages;
        this.dictionaryModelLogicalSchema = dictionaryModelLogicalSchema;
        this.procedureOptionBuilder = procedureOptionBuilder;
        // suppress ODI logger messages when generating scenarios
        java.util.logging.Logger odiLogger =
                java.util.logging.Logger.getLogger("oracle.odi.scenario.generation");
        odiLogger.setLevel(Level.WARNING);
        OdiLogHandler odiHandler = new OdiLogHandler(errorWarningMessages);
        odiHandler.setLevel(Level.WARNING);
        odiLogger.addHandler(odiHandler);
    }

    //
    // CRUD operations
    //

    @Cached
    protected OdiContext findContext(final String name) {
        final IOdiContextFinder finder =
                ((IOdiContextFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiContext.class));
        return finder.findByCode(name);
    }

    @Cached
    protected OdiContext findDefaultContext() {
        final IOdiContextFinder finder =
                ((IOdiContextFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiContext.class));
        @SuppressWarnings("unchecked")
        Collection<OdiContext> contexts = finder.findAll();
        return contexts.stream()
                .filter(c -> c.isDefaultContext())
                .findFirst().orElseGet(null);
    }

    @Cached
    protected OdiTechnology findOdiTechnology(final String name) {
        final IOdiTechnologyFinder finder =
                ((IOdiTechnologyFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiTechnology.class));
        return finder.findByCode(name);
    }

    @Cached
    public Collection<OdiTechnology> getOdiTechnology() {
        final IOdiTechnologyFinder finder =
                ((IOdiTechnologyFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiTechnology.class));
        @SuppressWarnings("unchecked")
        Collection<OdiTechnology> technologies = finder.findAll();
        //technologies.forEach(t -> System.err.println(t.getQualifiedName()));
        return technologies;
    }

    @Cached
    protected OdiLogicalSchema findOdiLogicalSchema(final String name) {
        final IOdiLogicalSchemaFinder finder =
                ((IOdiLogicalSchemaFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiLogicalSchema.class));
        return finder.findByName(name);
    }

    @Cached
    protected OdiProject findProject(final String projectCode) {
        final IOdiProjectFinder finder =
                ((IOdiProjectFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiProject.class));
        final OdiProject project = finder.findByCode(projectCode);
        return project;
    }

    //
    // Procedures
    //

    // extract procedure

    public CommandInternal extractCommand(final TaskInternal parent,
                                          final OdiProcedureLineCmd command,
                                          final Location location) {
        String technology = command.getTechnology() != null
                ? command.getTechnology().getName()
                : "<undefined>";
        String logicalSchema = command.getLogicalSchema() != null
                ? command.getLogicalSchema().getName()
                : "<undefined>";
        String expression = command.getExpression() != null
                ? command.getExpression().getAsString()
                : "<undefined>";
        String context = command.getExecutionContext() != null &&
                command.getExecutionContext().getCode() != null
                ? command.getExecutionContext().getCode()
                : "<undefined>";
        boolean doNotTranslate = true;
        return new CommandInternalImpl(parent, technology, logicalSchema,
                context, expression,
                location, Collections.emptySet(),
                this.dictionaryModelLogicalSchema,
                doNotTranslate
        );
    }

    private boolean isEmptyCommand(final OdiProcedureLineCmd command) {
        return command == null || command.getExpression() == null ||
                command.getExpression().getAsString() == null ||
                command.getExpression().getAsString().trim().isEmpty();
    }

    private TaskInternal extractTask(final ProcedureHeader parent,
                                     final OdiUserProcedureLine odiLine) {
        TaskInternalImpl task = new TaskInternalImpl(parent, odiLine.getName(),
                odiLine.isCleanUp(),
                odiLine.isIgnoreError());
        if (!isEmptyCommand(odiLine.getOnSourceCommand())) {
            extractCommand(task, odiLine.getOnSourceCommand(), Location.SOURCE);
        }
        if (!isEmptyCommand(odiLine.getOnTargetCommand())) {
            extractCommand(task, odiLine.getOnTargetCommand(), Location.TARGET);
        }
        return task;
    }

    private ProcedureInternal extractProcedure(final OdiUserProcedure odiProcedure,
                                               final String folderPathToProcedure) {
        String[] ppath = folderPathToProcedure.split("/");
        List<String> path = Arrays.asList(ppath)
                .subList(0, ppath.length - 1);

        ProcedureInternalImpl procedure =
                new ProcedureInternalImpl(path, odiProcedure.getName(),
                        odiProcedure.getDescription(),
                        odiProcedure.isMultiConnectionSupported(),
                        odiProcedure.isCleanupOnError(),
                        odiProcedure.isConcurrent(),
                        transform(odiProcedure.getOptions()),
                        null);

        for (OdiUserProcedureLine odiLine : odiProcedure.getLines()) {
            extractTask(procedure, odiLine);
        }
        return procedure;
    }

    // create procedure

    private List<Option> transform(List<ProcedureOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        List<Option> externalOptions = new ArrayList<>();
        options.forEach(o -> externalOptions.add(transform(o)));
        return externalOptions;
    }

    private Option transform(ProcedureOption o) {
        OptionImpl externalOption = new OptionImpl();
        externalOption.setName(o.getName());
        externalOption.setCondition(o.getConditionExpression());
        externalOption.setType(mapFrom(oracle.odi.domain.project.ProcedureOption.OptionType.valueOf(o.getProcedureOptionType())));
        externalOption.setDefaultValue(o.getDefaultOptionValue());
        externalOption.setDescription(o.getDescription());

        //TODO can't find this option in ODI12
        //externalOption.setDirectExecutionValue(o.get);
        logger.info(String.format("Can't set direct execution value for option %s.", o.getName()));

        externalOption.setHelp(o.getHelp());
        externalOption.setType(mapFrom(o.getOptionType()));
        return externalOption;
    }

    private OptionType mapFrom(oracle.odi.domain.project.ProcedureOption.OptionType optionType) {
        final OptionType option;
        switch (optionType) {
            case CHOICE:
                option = OptionType.CHOICE;
                break;
            case CHECKBOX:
                option = OptionType.BOOLEAN;
                break;
            default:
            case LONG_TEXT:
            case SHORT_TEXT:
                option = OptionType.TEXT;
                break;
        }
        return option;
    }

    @SuppressWarnings("deprecation")
    private OdiProcedureLineCmd createCommand(final CommandInternal command,
                                              final boolean isTarget) {
        OdiProcedureLineCmd cmd = new OdiProcedureLineCmd();
        cmd.setAutoCommitMode();

        // expression
        assert (command.getCommand() != null && !command.getCommand().isEmpty());
        Expression expr = new Expression(command.getCommand(), null, SqlGroupType.NONE);
        cmd.setExpression(expr);

        // technology
        OdiTechnology technology = findOdiTechnology(command.getTechnology());
        if (technology == null) {
            String msg = this.errorWarningMessages
                    .formatMessage(85100, ERROR_MESSAGE_85100, this.getClass(),
                            command.getParent().getParent().getName(),
                            isTarget ? "target" : "source",
                            command.getTechnology());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            technology = findOdiTechnology("ORACLE");
        }
        cmd.setTechnology(technology);

        // isolation
        IsolationLevel isolationLevel;
        try {
            isolationLevel = IsolationLevel.valueOf(command.getTransactionIsolation());
        } catch (IllegalArgumentException | NullPointerException e) {
            String msg = this.errorWarningMessages
                    .formatMessage(85110, ERROR_MESSAGE_85110, this.getClass(),
                            command.getParent().getParent().getName(),
                            command.getTransactionIsolation());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            isolationLevel = IsolationLevel.NONE;
        }
        cmd.setIsolationLevel(isolationLevel);

        // context
        OdiContext context;
        if (command.getExecutionContext() != null
                && command.getExecutionContext().equalsIgnoreCase("DEFAULT_CONTEXT")) {
            context = findDefaultContext();
        } else if (command.getExecutionContext() != null
                && command.getExecutionContext().equalsIgnoreCase("EXECUTION_CONTEXT")) {
            context = null;
        } else if (command.getExecutionContext() != null) {
            context = findContext(command.getExecutionContext());
        } else {
            context = null;
        }
        cmd.setExecutionContext(context);

        // logical schema
        OdiLogicalSchema schema = findOdiLogicalSchema(command.getLogicalSchema());
        if (schema == null) {
            String msg = this.errorWarningMessages
                    .formatMessage(85130, ERROR_MESSAGE_85130, this.getClass(),
                            command.getParent().getParent().getName(),
                            command.getLogicalSchema());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
        } else {
            cmd.setLogicalSchema(schema);
        }
        return cmd;
    }

    private void addTask(final OdiUserProcedure parent, final TaskInternal task) {
        OdiProcedureLineCmd srcCommand = null;
        OdiProcedureLineCmd trgCommand = null;
        if (task.getSourceCommand().isPresent()) {
            srcCommand = createCommand(task.getSourceCommand().get(), false);
        }
        if (task.getTargetCommand().isPresent()) {
            trgCommand = createCommand(task.getTargetCommand().get(), true);
        }

        OdiUserProcedureLine line = parent.addLine(task.getName(), trgCommand, srcCommand,
                null);
        line.setCleanUp(task.isCleanup());
        line.setIgnoreError(task.isIgnoreErrors());
        line.setLogLevel(task.getLogLevel());
        line.setLogFinalCommand(task.isLogFinalCommand());

        OdiProcedureLine.LogCounter logCounter = OdiProcedureLine.LogCounter.NONE;
        try {
            logCounter = OdiProcedureLine.LogCounter.valueOf(task.getLogCounter());
        } catch (IllegalArgumentException | NullPointerException e) {
            String msg = this.errorWarningMessages
                    .formatMessage(85150, ERROR_MESSAGE_85150, this.getClass(),
                            parent.getName(), task.getLogCounter());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
        }
        line.setLogCounter(logCounter);
    }

    // transaction semantics defined in calling method
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private OdiScenario createProcedureScenario(final OdiUserProcedure procedure) {
        assert (procedure != null);
        assert (odiInstance.getTransactionalEntityManager().isOpen());
        IOdiScenarioGenerator scenarioGenerator = new OdiScenarioGeneratorImpl(odiInstance);
        OdiScenario scenario = null;
        String scenarioName = JodiConstants.getScenarioNameFromObject(procedure.getName(),
                true);
        try {
            scenario = scenarioGenerator.generateScenario(procedure, scenarioName, "001");
        } catch (OdiScenarioGeneratorException e) {
            String msg = this.errorWarningMessages
                    .formatMessage(85140, ERROR_MESSAGE_85140, this.getClass(),
                            procedure.getName(), e.getMessage());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg, e);
        } catch (Throwable e) { // TODO catch all in case names are duplicated
            String msg = this.errorWarningMessages
                    .formatMessage(85160, ERROR_MESSAGE_85160, this.getClass(),
                            procedure.getName(), e.getMessage());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg, e);
        }
        return scenario;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected OdiUserProcedure addProcedure(final ProcedureInternal procedure,
                                            final OdiFolder folder,
                                            final boolean generateScenarios,
                                            final String projectCode) {
        assert (folder != null && procedure != null) :
                "folder: " + folder + " procedure: " + procedure;
        // do not change this transaction control,
        // it has impact on performance.
        final DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition(ITransactionDefinition.PROPAGATION_MANDATORY);
        final ITransactionManager tm = odiInstance.getTransactionManager();
        final ITransactionStatus txnStatus = tm.getTransaction(txnDef);
        final IOdiEntityManager tme = odiInstance.getTransactionalEntityManager();
        OdiUserProcedure userProcedure = new OdiUserProcedure(folder, procedure.getName());
        if (procedure.getDescription().isPresent()) {
            userProcedure.setDescription(procedure.getDescription().get());
        }
        userProcedure.setMultiConnectionSupported(procedure.isMultiConnectionSupported());
        userProcedure.setCleanupOnError(procedure.isRemoveTemporaryObjectsonError());
        userProcedure.setConcurrent(procedure.isUseUniqueTemporaryObjectNames());

        if (procedure.isMultiConnectionSupported()) {
            OdiTechnology sourceTech = findOdiTechnology(procedure.getSourceTechnology());
            if (sourceTech == null) {
                String msg = this.errorWarningMessages
                        .formatMessage(85100, ERROR_MESSAGE_85100, this.getClass(),
                                procedure.getName(), "default source",
                                procedure.getSourceTechnology());
                errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                        .ERRORS);
                logger.error(msg);
                sourceTech = findOdiTechnology("ORACLE");
            }
            userProcedure.setDefaultSourceTechnology(sourceTech);
        }

        OdiTechnology targetTech = findOdiTechnology(procedure.getSourceTechnology());
        if (targetTech == null) {
            String msg = this.errorWarningMessages
                    .formatMessage(85100, ERROR_MESSAGE_85100, this.getClass(),
                            procedure.getName(), "default source",
                            procedure.getSourceTechnology());
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE
                    .ERRORS);
            logger.error(msg);
            targetTech = findOdiTechnology("ORACLE");
        }
        userProcedure.setDefaultTargetTechnology(targetTech);

        // add options
        for (OptionInternal o : procedure.getOptions()) {
            addOption(userProcedure, o);
        }

        // add task lines
        for (TaskInternal task : procedure.getTasks()) {
            addTask(userProcedure, task);
        }
        if (generateScenarios) {
            createProcedureScenario(userProcedure);
        }
        tme.persist(userProcedure);
        tm.commit(txnStatus);
        logger.info("Created procedure " + procedure.getName() + " in folder : " + folder.getQualifiedName() + ".");
        return userProcedure;
    }

    private void addOption(OdiUserProcedure userProcedure, OptionInternal optionInternal) {
        // this is due to protected constructor.
        this.procedureOptionBuilder.build(userProcedure, optionInternal);
    }

    // transaction semantics defined in calling method
    private void deleteProcedureScenario(final OdiUserProcedure userProcedure,
                                         final String folderPath) {
        assert (odiInstance.getTransactionalEntityManager().isOpen());
        assert (userProcedure != null);
        assert (folderPath != null && !folderPath.isEmpty());
        Number procedureID = userProcedure.getProcedureId();
        Collection<OdiScenario> odiScenarios =
                ((IOdiScenarioFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiScenario.class))
                        .findBySourceUserProcedure(procedureID);
        assert (odiScenarios != null);
        if (!odiScenarios.isEmpty()) {
            logger.debug("Attempt to remove " + odiScenarios.size() + " scenarios for: " +
                    userProcedure.getName() + " in folder " + folderPath);
        }
        odiScenarios.stream()
                .peek(s -> logger.debug("deleted scenario '" + s.getName() +
                        "' in folder " + folderPath))
                .forEach(s -> odiInstance.getTransactionalEntityManager().remove(s));
    }

    // assumption: transaction was started before
    protected void deleteProcedure(final OdiUserProcedure userProcedure,
                                   final String folderPath, final String projectCode) {
        assert (odiInstance.getTransactionalEntityManager().isOpen());
        deleteProcedureScenario(userProcedure, folderPath);
        //odiInstance.getTransactionalEntityManager().merge(userProcedure);
        odiInstance.getTransactionalEntityManager()
                .remove(userProcedure);
        logger.info("Removed procedure " + userProcedure.getName() +
                " in folder " + folderPath + ".");
    }

    private Collection<OdiUserProcedure> findProcedures(final String projectCode)
            throws ResourceNotFoundException {
        IOdiUserProcedureFinder finder =
                (IOdiUserProcedureFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiUserProcedure.class);
        Collection<OdiUserProcedure> userProcedures = finder.findByProject(projectCode);
        if (userProcedures == null) {
            String msg = this.errorWarningMessages
                    .formatMessage(85020, ERROR_MESSAGE_85020, this.getClass(),
                            projectCode);
            errorWarningMessages.addMessage(msg, ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            logger.error(msg);
        }
        return userProcedures;
    }

    //
    // APIs
    //

    @Override
    public Collection<OdiUserProcedure> findProcedures(final String procedureName,
                                                       final String projectCode) {
        return ((IOdiUserProcedureFinder)
                odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiUserProcedure.class))
                .findByName(procedureName, projectCode);
    }


    @Override
    public Map<String, OdiUserProcedure> findFolderPathsAndProcedures(
            final String projectCode) {
        try {
            return findProcedures(projectCode)
                    .stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(p -> getFolderPath(p.getFolder()) +
                                            "/" + p.getName(),
                                    p -> p),
                            Collections::unmodifiableMap));
        } catch (ResourceNotFoundException re) {
            throw new UnRecoverableException("Unable to find procedures.", re);
        }
    }

    @Override
    public void createProcedures(final List<ProcedureInternal> procedures,
                                 final boolean generateScenarios,
                                 final String projectCode) {
        List<String> folderPaths = procedures.stream()
                .map(p -> p.getFolderPath())
                .distinct()
                .collect(Collectors.toList());
        Map<String, OdiFolder> folders = this.folderService
                .findOrCreateFolders(folderPaths,
                        projectCode);
        procedures.forEach(p -> {
            try {
                addProcedure(p, folders.get(p.getFolderPath()),
                        generateScenarios, projectCode);
            } catch (TransactionSystemException e) {
                logger.error("Scenario creation failed for procedure " +
                        p.getName(), e);
            }
        });
    }

    @Override
    public List<ProcedureInternal> extractProcedures(final String projectCode) {
        Map<String, OdiUserProcedure> folderProcedureMap;
        folderProcedureMap = findFolderPathsAndProcedures(projectCode);
        return folderProcedureMap.entrySet()
                .stream()
                .map(e -> extractProcedure(e.getValue(), e.getKey()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        Collections::unmodifiableList));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteProcedures(final List<ProcedureHeader> procedures,
                                 final String projectCode) {
        Map<String, OdiUserProcedure> procedureMap =
                findFolderPathsAndProcedures(projectCode);
        // filter out existing procedures and only attempt to delete those
        procedures.stream()
                .filter(p -> procedureMap.get(p.getFolderPath() +
                        "/" + p.getName()) != null)
                .forEach(p -> deleteProcedure(procedureMap.get(p.getFolderPath() +
                                "/" + p.getName()),
                        p.getFolderPath(), projectCode));
    }

}

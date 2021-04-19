package one.jodi.etl.builder.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.service.metadata.SchemaMetaDataProvider;
import one.jodi.core.procedure.CommandType;
import one.jodi.core.procedure.Procedure;
import one.jodi.core.procedure.Task;
import one.jodi.etl.builder.ProcedureTransformationBuilder;
import one.jodi.etl.internalmodel.procedure.CommandInternal;
import one.jodi.etl.internalmodel.procedure.ProcedureHeader;
import one.jodi.etl.internalmodel.procedure.ProcedureInternal;
import one.jodi.etl.internalmodel.procedure.TaskInternal;
import one.jodi.etl.internalmodel.procedure.Validate;
import one.jodi.etl.internalmodel.procedure.impl.CommandInternalImpl;
import one.jodi.etl.internalmodel.procedure.impl.ProcedureHeaderImpl;
import one.jodi.etl.internalmodel.procedure.impl.ProcedureInternalImpl;
import one.jodi.etl.internalmodel.procedure.impl.TaskInternalImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ProcedureTransformationBuilderImpl implements ProcedureTransformationBuilder {

    private static final Logger logger = LogManager.getLogger(ProcedureTransformationBuilderImpl.class);
    private final SchemaMetaDataProvider metadataProvider;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final DictionaryModelLogicalSchema dictionaryModelLogicalSchema;

    @Inject
    public ProcedureTransformationBuilderImpl(final SchemaMetaDataProvider metadataProvider,
                                              final ErrorWarningMessageJodi errorWarningMessages,
                                              final DictionaryModelLogicalSchema dictionaryModelLogicalSchema) {
        this.metadataProvider = metadataProvider;
        this.errorWarningMessages = errorWarningMessages;
        this.dictionaryModelLogicalSchema = dictionaryModelLogicalSchema;
    }

    private String findDefaultTechnology(final CommandInternalImpl.Location location, final TaskInternal parent) {
        final String technology;
        ProcedureInternal procedure = (ProcedureInternal) parent.getParent();
        if (location == CommandInternalImpl.Location.SOURCE) {
            technology = procedure.getSourceTechnology();
        } else {
            technology = procedure.getTargetTechnology();
        }
        return technology == null ? "Oracle" : technology;
    }

    private CommandInternal build(final TaskInternal parent, final CommandType extCommand,
                                  final CommandInternalImpl.Location location, final Set<String> logicalModelNames) {
        assert (extCommand != null);
        // registers itself to parent
        String technology;
        if (extCommand.getTechnology() == null) {
            technology = findDefaultTechnology(location, parent);
        } else {
            technology = extCommand.getTechnology()
                                   .value();
        }
        boolean doNotTranslate = false;
        return new CommandInternalImpl(parent, technology, extCommand.getLogicalSchema(),
                                       extCommand.getExecutionContext(), extCommand.getCommand(), location,
                                       logicalModelNames, this.dictionaryModelLogicalSchema, doNotTranslate);
    }


    private TaskInternal build(final ProcedureHeader parent, final Task extTask, final Set<String> logicalModelNames) {
        TaskInternal task =
                new TaskInternalImpl(parent, extTask.getName(), extTask.isCleanup(), extTask.isIgnoreErrors());
        if (extTask.getSourceCommand() != null) {
            // registers itself to parent
            build(task, extTask.getSourceCommand(), CommandInternalImpl.Location.SOURCE, logicalModelNames);
        }
        if (extTask.getTargetCommand() != null) {
            // registers itself to parent
            build(task, extTask.getTargetCommand(), CommandInternalImpl.Location.TARGET, logicalModelNames);
        }
        return task;
    }

    private List<String> getFolderPath(final Procedure extProcedure) {
        if (extProcedure.getFolderName() != null) {
            return Arrays.asList(extProcedure.getFolderName()
                                             .split("/"));
        } else {
            return null;
        }
    }

    @Override
    public Optional<ProcedureInternal> build(final Procedure extProcedure, final String filePath) {

        final Set<String> logicalModelNames = metadataProvider.getLogicalSchemaNames();
        ProcedureInternal procedure = new ProcedureInternalImpl(getFolderPath(extProcedure), extProcedure.getName(),
                                                                extProcedure.getDescription(),
                                                                extProcedure.isMultiConnectionSupported(),
                                                                extProcedure.isRemoveTemporaryObjectsonError(),
                                                                extProcedure.isUseUniqueTemporaryObjectNames(),
                                                                extProcedure.getOptions() != null
                                                                ? extProcedure.getOptions()
                                                                              .getOption() : Collections.EMPTY_LIST,
                                                                filePath);

        if (extProcedure.getTasks() != null) {
            extProcedure.getTasks()
                        .getTask()
                        .stream()
                        // each task registers itself to parent
                        .forEach(t -> build(procedure, t, logicalModelNames));
        }

        if (!((Validate) procedure).validate(this.errorWarningMessages)) {
            return Optional.empty();
        }
        return Optional.of(procedure);
    }

    @Override
    public Optional<ProcedureHeader> buildHeader(final Procedure extProcedure, final String filePath) {
        ProcedureHeader header = new ProcedureHeaderImpl(getFolderPath(extProcedure), extProcedure.getName(), filePath);
        if (!((Validate) header).validate(this.errorWarningMessages)) {
            return Optional.empty();
        }
        return Optional.of(header);
    }

}

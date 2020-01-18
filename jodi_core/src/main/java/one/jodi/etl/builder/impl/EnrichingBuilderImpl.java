package one.jodi.etl.builder.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.exception.UnRecoverableException;
import one.jodi.base.util.Version;
import one.jodi.core.automapping.ColumnMappingContext;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.executionlocation.ExecutionLocationContext;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.folder.FolderNameContext;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.targetcolumn.FlagsContext;
import one.jodi.core.transformation.TransformationNameContext;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.builder.DeleteTransformationContext;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.impl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/* *
 * Implementation of EnrichingBuilder
 *
 *
 */
public class EnrichingBuilderImpl implements EnrichingBuilder {

    private final static Logger logger = LogManager.getLogger(EnrichingBuilderImpl.class);
    private final static String newLine = System.getProperty("line.separator");

    private final static String ERROR_MESSAGE_03240 =
            "KnowledgeModuleConfiguration can't be null.";
    private final static String ERROR_MESSAGE_08070 =
            "convertUDFs(Set<UserDefinedFlag>): Error parsing UDF number from name %s";

    private final JodiProperties properties;
    private final ColumnMappingContext columnMappingContext;
    private final ExecutionLocationContext executionLocationContext;
    private final FlagsContext flagsContext;
    private final FolderNameContext folderNameContext;
    private final JournalizingContext journalizingContext;
    private final KnowledgeModuleContext knowledgeModuleContext;
    private final ModelCodeContext modelCodeContext;
    private final TransformationNameContext transformationNameContext;
    private final ETLValidator etlValidator;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private final Pattern udfNumberRegex = Pattern.compile("ud(\\d+)\\z",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private final Pattern tempNameTableMatcher;

    @Inject
    public EnrichingBuilderImpl(
            final ColumnMappingContext columnMappingContext,
            final ExecutionLocationContext executionLocationContext,
            final FlagsContext flagsContext,
            final FolderNameContext folderNameContext,
            final JournalizingContext journalizingContext,
            final KnowledgeModuleContext knowledgeModuleContext,
            final ModelCodeContext modelCodeContext,
            final TransformationNameContext transformationNameContext,
            final JodiProperties properties, final ETLValidator etlValidator,
            final ErrorWarningMessageJodi errorWarningMessages) {
        super();
        this.columnMappingContext = columnMappingContext;
        this.executionLocationContext = executionLocationContext;
        this.flagsContext = flagsContext;
        this.folderNameContext = folderNameContext;
        this.journalizingContext = journalizingContext;
        this.knowledgeModuleContext = knowledgeModuleContext;
        this.modelCodeContext = modelCodeContext;
        this.transformationNameContext = transformationNameContext;
        this.properties = properties;
        this.etlValidator = etlValidator;
        this.errorWarningMessages = errorWarningMessages;
        this.tempNameTableMatcher = Pattern.compile(this.properties.getTemporaryInterfacesRegex());
    }


    private String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    @Override
    public DeleteTransformationContext createDeleteContext(
            Transformation existingTransformation, boolean isJournalizedData) {

        logger.debug("------------------------------------------------");
        logger.debug(" createDeleteContext.");
        logger.debug("------------------------------------------------");
        final TransformationImpl transformation = new TransformationImpl();
        transformation.setExtension(existingTransformation.getExtension());
        transformation.setName(existingTransformation.getName());
        transformation.setOriginalFolderPath(existingTransformation.getOriginalFolderPath());
        MappingsImpl mappings = new MappingsImpl();
        mappings.setModel(existingTransformation.getMappings().getModel());
        mappings.setParent(transformation);
        mappings.setTargetDataStore(existingTransformation.getMappings()
                .getTargetDataStore());
        transformation.setMappings(mappings);

        // Target columns need to be added to compute folder name when target is temporary (for context)
        for (Targetcolumn tc : existingTransformation.getMappings().getTargetColumns()) {
            TargetcolumnImpl targetcolumn = new TargetcolumnImpl();
            targetcolumn.setName(tc.getName());
            targetcolumn.setDataType(tc.getDataType());
            targetcolumn.setLength(tc.getLength());
            targetcolumn.setScale(tc.getScale());
            targetcolumn.setExtension(tc.getExtension());
            targetcolumn.setPosition(tc.getPosition());
            targetcolumn.addMappingExpressions(tc.getMappingExpressions());

            targetcolumn.setParent(mappings);
            mappings.addTargetcolumns(targetcolumn);
        }
        transformation.setPackageSequence(existingTransformation
                .getPackageSequence());
        logger.debug("Computing delete package sequence "
                + transformation.getPackageSequence());
        if (!isTemporaryTransformation(transformation.getMappings()
                .getTargetDataStore())) {
            transformation.setTemporary(false);

            ((MappingsImpl) transformation.getMappings())
                    .setTargetDataStore(transformation.getMappings()
                            .getTargetDataStore());
            ((MappingsImpl) transformation.getMappings())
                    .setModel(modelCodeContext.getModelCode(transformation
                            .getMappings()));
            ((TransformationImpl) transformation)
                    .setFolderName(folderNameContext.getFolderName(transformation, isJournalizedData));
            ((TransformationImpl) transformation)
                    .setName(transformationNameContext
                            .getTransformationName(transformation));
        } else {
            ((TransformationImpl) transformation).setTemporary(true);
            modelCodeContext.getModelCode(transformation.getMappings());
            folderNameContext.getFolderName(transformation, isJournalizedData);

            ((TransformationImpl) transformation).setFolderName(folderNameContext.getFolderName(transformation, isJournalizedData));
            ((TransformationImpl) transformation).setName(transformationNameContext.getTransformationName(transformation));
			
/*
			transformation.setTemporary(true);
			logger.debug(transformation.getPackageSequence()
					+ " Temporary Target DS : "
					+ ((MappingsImpl) transformation.getMappings())
							.getTargetDataStore());
			String tempDs = transformationNameContext
					.getTemporaryDSTarget(transformation);
			if (((MappingsImpl) transformation.getMappings()).getTargetDataStore().length() < transformation.getMappings().getTargetDataStore().length())
				((MappingsImpl) transformation.getMappings()).setTargetDataStore(tempDs);
			logger.debug(transformation.getPackageSequence()
					+ " Target DS : "
					+ ((MappingsImpl) transformation.getMappings())
							.getTargetDataStore());
			((MappingsImpl) transformation.getMappings())
					.setModel(modelCodeContext.getModelCode(transformation
					.getMappings()));
			((TransformationImpl) transformation).setName(tempDs);
			((TransformationImpl) transformation).setFolderName(folderNameContext.getFolderName(transformation, isJournalizedData));
			*/
        }
        return new DeleteTransformationContext() {
            @Override
            public String getName() {
                return transformation.getName();
            }

            @Override
            public String getDataStoreName() {
                return transformation.getMappings().getTargetDataStore();
            }

            @Override
            public String getModel() {
                return transformation.getMappings().getModel();
            }

            @Override
            public int getPackageSequence() {
                return transformation.getPackageSequence();
            }

            @Override
            public boolean isTemporary() {
                return transformation.isTemporary();
            }

            @Override
            public String getFolderName() {
                return transformation.getFolderName();
            }

        };

    }

    private List<ExecutionLocationtypeEnum> convert(
            List<ExecutionLocationType> in) {
        ArrayList<ExecutionLocationtypeEnum> out = in.stream()
                .map(el -> ExecutionLocationtypeEnum
                        .fromValue(el.name()))
                .collect(Collectors
                        .toCollection(ArrayList::new));

        return out;
    }

    @Override
    public Transformation enrich(Transformation transformation,
                                 boolean isJournalizedData) {
        logger.debug("------------------------------------------------");
        logger.debug(transformation.getPackageSequence()
                + " enriching input model with Jodi derived information using plugins.");
        logger.debug("------------------------------------------------");

        if (transformation.getFolderName() != null && transformation.getName() != null) {
            logger.debug(transformation.getPackageSequence() + " transformation name, " +
                    "model and folder already set, returning without deriving");
            return transformation;
        }

        ((TransformationImpl) transformation)
                .setTemporary(isTemporaryTransformation(transformation
                        .getMappings().getTargetDataStore()));
        modelCodeContext.getModelCode(transformation.getMappings());
        folderNameContext.getFolderName(transformation, isJournalizedData);

        transformationNameContext.getTransformationName(transformation);

        etlValidator.validatePackageAssociations(transformation);

        etlValidator.validateDataset(transformation.getDatasets());

        // Set models for each source (required by ExecutionLocation plugin to
        // compute isSameModelAsTransformation and for the various KM
        // operations)
        int dataSetNumber = 0;
        for (Dataset dataset : transformation.getDatasets()) {
            boolean firstSource = true;
            for (Source source : dataset.getSources()) {
                if (firstSource) {
                    ((DatasetImpl) dataset).setName(source.getName() + "_" + dataSetNumber);
                    firstSource = false;
                }
                ((SourceImpl) source)
                        .setTemporary(isTemporaryTransformation(source
                                .getName()));
                transformationNameContext.setSourceName(source);

                modelCodeContext.getModelCode(source);

                etlValidator.validateFilter(source);
                etlValidator.validateJoin(source);

                for (Lookup lookup : source.getLookups()) {
                    ((LookupImpl) lookup)
                            .setTemporary(isTemporaryTransformation(lookup
                                    .getLookupDataStore()));
                    transformationNameContext.setLookupName(lookup);

                    // must be executed after name is set for temp table due to
                    // required lookup in cache with derived name
                    // ((LookupImpl)
                    // lookup).setModel(modelCodeContext.getModelCode(lookup));
                    modelCodeContext.getModelCode(lookup);
                }
            }
            dataSetNumber++;
        }
        etlValidator.validateBeginAndEndMapping(transformation);
        // depends on model code
        knowledgeModuleContext.getIKMConfig(transformation);
        // ((MappingsImpl)
        //transformation.getMappings()).setIkm(generateKMType(ikm));

        knowledgeModuleContext.getStagingModel(transformation);
        // ((MappingsImpl)
        // transformation.getMappings()).setStagingModel(stagingModel);

        for (Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {

                executionLocationContext.getFilterExecutionLocation(source);
                executionLocationContext.getJoinExecutionLocation(source);

                if (isTemporaryTransformation(source.getName())) {
                    ((SourceImpl) source).setTemporary(true);
                }

                logger.debug("Journalized in enrichment:"
                        + ((SourceImpl) source).isJournalized());

                knowledgeModuleContext.getLKMConfig(source, "");

                ((SourceImpl) source).setSubSelect(source.isSubSelect());
                etlValidator.validateSourceDataStore(source);
                etlValidator.validateSourceDataStoreName(source);

                ((SourceImpl) source).setJournalizedType(journalizingContext.isJournalizedSource(source));
                if (journalizingContext.isJournalizedSource(source)) {
                    journalizingContext.getJournalizingConfiguration().stream()
                            .filter(journalizingConfig -> journalizingConfig
                                    .getModelCode()
                                    .equals(source.getModel()))
                            .forEach(journalizingConfig -> ((SourceImpl) source)
                                    .setSubscribers(journalizingConfig
                                            .getSubscribers()));
                }
                for (Lookup lookup : source.getLookups()) {
                    if (isJournalizedData) {
                        ((LookupImpl) lookup)
                                .setJournalized(journalizingContext
                                        .isJournalizedLookup(lookup));
                        if (journalizingContext.isJournalizedLookup(lookup)) {
                            journalizingContext.getJournalizingConfiguration()
                                    .stream()
                                    .filter(journalizingConfig -> journalizingConfig
                                            .getModelCode()
                                            .equals(lookup.getModel()))
                                    .forEach(journalizingConfig -> ((LookupImpl) lookup)
                                            .setSubscribers(journalizingConfig
                                                    .getSubscribers()));
                        }
                    }
                    if (isTemporaryTransformation(lookup.getLookupDataStore())) {
                        ((LookupImpl) lookup).setTemporary(true);
                    } else {
                        ((LookupImpl) lookup).setTemporary(false);
                    }

                    executionLocationContext.getLookupExecutionLocation(lookup);
                    ((LookupImpl) lookup).setSubSelect(lookup.isSubSelect());
                    etlValidator.validateLookupJoin(lookup);
                    etlValidator.validateLookup(lookup);
                    etlValidator.validateLookupName((LookupImpl) lookup);
                    etlValidator.validateNoMatchRows(lookup);

                }


                source.getFlows()
                        .stream()
                        .filter(c -> c instanceof SubQuery)
                        .map(SubQueryImpl.class::cast)
                        .peek(modelCodeContext::getModelCode)
                        .peek(etlValidator::validateExecutionLocation)
                        .peek(executionLocationContext::getSubQueryExecutionLocation)
                        .peek(sq -> sq.setTemporary(isTemporaryTransformation(sq.getFilterSource())))
                        .forEach(etlValidator::validateFlow);
                source.getFlows().stream()
                        .filter(c -> !(c instanceof SubQuery))
                        .forEach(etlValidator::validateFlow);

//				source.getFlows().stream().filter(c -> c instanceof SubQuery).map(SubQuery.class::cast).forEach(modelCodeContext::getModelCode);
//				source.getFlows().forEach(etlValidator::validateFlow);

            }
        }

        knowledgeModuleContext.getCKMConfig(transformation);

        columnMappingContext.getMappings(transformation);

        // Now process flags
        int position = 0;
        for (Targetcolumn targetColumn : transformation.getMappings()
                .getTargetColumns()) {
            position++;
            TargetColumnFlags flags = flagsContext
                    .getTargetColumnFlags(targetColumn);
            ((TargetcolumnImpl) targetColumn).setMandatory(flags.isMandatory());
            ((TargetcolumnImpl) targetColumn).setInsert(flags.isInsert());
            ((TargetcolumnImpl) targetColumn).setUpdate(flags.isUpdate());
            ((TargetcolumnImpl) targetColumn).setUpdateKey(flags.isUpdateKey());
            if (flags.isUpdateKey() == null) {
                logger.error(targetColumn.getParent().getTargetDataStore() + "." +
                        targetColumn.getName() + " is " + flags.isUpdateKey());
            }
            ((TransformationImpl) transformation).setUseExpressions(flags.useExpression());

            Set<one.jodi.core.extensions.types.UserDefinedFlag> userDefinedFlags = flagsContext
                    .getUserDefinedFlags(targetColumn);
            if (userDefinedFlags != null) {
                ((TargetcolumnImpl) targetColumn)
                        .setUserDefinedFlags(convertUDFs(userDefinedFlags));
            }
            ((TargetcolumnImpl) targetColumn).setPosition(position);
            etlValidator.validateTargetColumn(targetColumn);
        }
        etlValidator.validateTargetColumns(transformation.getMappings());
        // Compute Execution Location for each target column's mapping
        // expressions
        for (Targetcolumn targetColumn : transformation.getMappings()
                .getTargetColumns()) {

            List<ExecutionLocationType> executionLocations = executionLocationContext
                    .getTargetColumnExecutionLocation(targetColumn);
            ((TargetcolumnImpl) targetColumn)
                    .setExecutionLocations(convert(executionLocations));
        }

        etlValidator.validateJournalized(transformation);

        for (Dataset dataset : transformation.getDatasets()) {
            for (Source source : dataset.getSources()) {
                etlValidator.validateFilterEnriched(source);
                etlValidator.validateJoinEnriched(source);

                source.getLookups().forEach(etlValidator::validateJoinEnriched);
            }
        }

        // add build information
        StringBuilder description = new StringBuilder();
        if (transformation.getComments() != null) {
            description.append(transformation.getComments());
        }
        if (properties.includeDetails()) {
            if (transformation.getComments() != null
                    && !transformation.getComments().trim().equals("")) {
                description.append(newLine);
                description.append(newLine);
            }
            description.append("Bulk creation operation for ");
            description.append(transformation.getMappings()
                    .getTargetDataStore());
            description.append(" with sequence number ");
            description.append(transformation.getPackageSequence());
            description.append(".  Imported by ");
            description.append(System.getProperty("user.name"));
            description.append(" at ");
            description.append(getCurrentDate());
            description.append(newLine);
            description.append("Created by Jodi version ");
            description.append(Version.getProductVersion());
            description.append(" with build date ");
            description.append(Version.getBuildDate());
            description.append(" ");
            description.append(Version.getBuildTime());
        }
        ((TransformationImpl) transformation).setComments(description
                .toString());

        // packageCache.addTransformation(transformation);
        //logger.info("Transformation: "+ transformation.getName() + " : "+ transformation.getBeginMappingCommand());
        if (transformation.getBeginMappingCommand() != null) {
            ((TransformationImpl) transformation).setBeginMappingCommand(getMappingCmd(transformation.getBeginMappingCommand()));
        }
        if (transformation.getEndMappingCommand() != null) {
            ((TransformationImpl) transformation).setEndMappingCommand(getMappingCmd(transformation.getEndMappingCommand()));
        }
        if (transformation.isAsynchronous()) {
            ((TransformationImpl) transformation).setAsynchronous(transformation.isAsynchronous());
        }
        return transformation;
    }

    private MappingCommand getMappingCmd(final MappingCommand beginOrEndMappingCommand) {
        return new MappingCommand() {

            @Override
            public String getText() {
                return beginOrEndMappingCommand.getText().trim();
            }

            @Override
            public String getTechnology() {
                return beginOrEndMappingCommand.getTechnology();
            }

            @Override
            public String getModel() {
                String model = beginOrEndMappingCommand.getModel();
                model = model.trim();
                return model == null ? null : modelCodeContext.getModelCode(beginOrEndMappingCommand.getModel());
            }
        };
    }


    @SuppressWarnings("unused")
    private KmType generateKMType(KnowledgeModuleConfiguration kmc) {
        if (kmc == null) {
            String msg = errorWarningMessages.formatMessage(3240,
                    ERROR_MESSAGE_03240, this.getClass());
            logger.error(msg);
            throw new UnRecoverableException(msg);
        }
        if (kmc == KnowledgeModuleConfiguration.Null) {
            return null;
        }

        KmTypeImpl type = new KmTypeImpl();
        type.setName(kmc.getName());
        for (String k : kmc.getOptionKeys()) {
            type.addOption(k, kmc.getOptionValue(k) + "");
        }
        return type;
    }

    private Set<one.jodi.etl.internalmodel.UserDefinedFlag> convertUDFs(
            Set<one.jodi.core.extensions.types.UserDefinedFlag> externalFlags) {
        Set<one.jodi.etl.internalmodel.UserDefinedFlag> result =
                new HashSet<>(
                        externalFlags.size());

        for (one.jodi.core.extensions.types.UserDefinedFlag udf : externalFlags) {
            try {
                Matcher regexMatcher = udfNumberRegex.matcher(udf.getName());
                if (regexMatcher.matches()) {
                    int udfNumber = Integer.parseInt(regexMatcher.group(1));
                    result.add(new UserDefinedFlagImpl(
                            udfNumber, udf.getValue()));
                }
            } catch (NumberFormatException | PatternSyntaxException e) {
                String msg = errorWarningMessages.formatMessage(8070, ERROR_MESSAGE_08070, this.getClass(), udf.getName());
                errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(), msg, MESSAGE_TYPE.ERRORS);
                logger.error(msg);
            }
        }
        return result;
    }

    @Override
    public boolean isTemporaryTransformation(String tableName) {
        return tempNameTableMatcher.matcher(tableName).find();
    }
}

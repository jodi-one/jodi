package one.jodi.etl.builder.impl;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.config.modelproperties.ModelProperties;
import one.jodi.core.config.modelproperties.ModelPropertiesProvider;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.sequences.*;
import one.jodi.core.service.SequenceServiceException;
import one.jodi.etl.builder.SequenceTransformationBuilder;
import one.jodi.etl.internalmodel.NativeSequence;
import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.internalmodel.SpecificSequence;
import one.jodi.etl.internalmodel.StandardSequence;
import one.jodi.etl.internalmodel.impl.NativeSequenceImpl;
import one.jodi.etl.internalmodel.impl.SequencesImpl;
import one.jodi.etl.internalmodel.impl.SpecificSequenceImpl;
import one.jodi.etl.internalmodel.impl.StandardSequenceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SequenceTransformationBuilderImpl implements SequenceTransformationBuilder {

    private static final Logger logger =
            LogManager.getLogger(SequenceTransformationBuilderImpl.class);
    private final ErrorWarningMessageJodi errorWarningMessageJodi;
    private final ModelPropertiesProvider modelPropertiesProvider;
    private final String ERROR_MESSAGE_4008 =
            "Error translating model properties into ETL Subsystem ModelID for variable %s schema %s.";
    private final DatabaseMetadataService databaseMetadataService;
    private final DictionaryModelLogicalSchema dictionaryModelLogicalSchema;

    @Inject
    public SequenceTransformationBuilderImpl(final ErrorWarningMessageJodi errorWarningMessageJodi,
                                             final ModelPropertiesProvider modelPropertiesProvider,
                                             final DatabaseMetadataService databaseMetadataService,
                                             final DictionaryModelLogicalSchema dictionaryModelLogicalSchema) {
        this.errorWarningMessageJodi = errorWarningMessageJodi;
        this.modelPropertiesProvider = modelPropertiesProvider;
        this.databaseMetadataService = databaseMetadataService;
        this.dictionaryModelLogicalSchema = dictionaryModelLogicalSchema;
    }

    @Override
    public Sequences transmute(one.jodi.etl.internalmodel.Sequences internalSequences) {
        Sequences externalSequences = new Sequences();
        internalSequences.getSequences()
                .forEach(is -> externalSequences.getSequence()
                        .add(transmute(is)));
        return externalSequences;
    }

    private JAXBElement<? extends SequenceType> transmute(Sequence inS) {
        ObjectFactory factory = new ObjectFactory();
        final SequenceType exS;
        if (inS instanceof StandardSequenceImpl) {
            exS = new SequenceStandardtype();
            exS.setGlobal(((StandardSequence) inS).getGlobal());
            exS.setIncrement(BigInteger.valueOf(Long.parseLong(((StandardSequence) inS).getIncrement() +
                    "")));
            exS.setName(((StandardSequence) inS).getName());
            return factory.createStandard((SequenceStandardtype) exS);
        } else if (inS instanceof SpecificSequenceImpl) {
            exS = new SequenceSpecifictype();
            exS.setGlobal(((SpecificSequence) inS).getGlobal());
            exS.setIncrement(BigInteger.valueOf(Long.parseLong(((SpecificSequence) inS).getIncrement() +
                    "")));
            exS.setName(((SpecificSequence) inS).getName());
            ((SequenceSpecifictype) exS).setModel(getModelFromSchema(((SpecificSequence) inS).getSchema(),
                    inS.getName()));
            ((SequenceSpecifictype) exS).setTable(((SpecificSequence) inS).getTable());
            ((SequenceSpecifictype) exS).setColumn(((SpecificSequence) inS).getColumn());
            ((SequenceSpecifictype) exS).setFilter(((SpecificSequence) inS).getFilter());
            return factory.createSpecific((SequenceSpecifictype) exS);
        } else if (inS instanceof NativeSequenceImpl) {
            exS = new SequenceNativetype();
            exS.setGlobal(((NativeSequenceImpl) inS).getGlobal());
            exS.setIncrement(BigInteger.valueOf(Long.parseLong(((NativeSequenceImpl) inS).getIncrement() +
                    "")));
            exS.setName(((NativeSequenceImpl) inS).getName());
            ((SequenceNativetype) exS).setModel(getModelFromSchema(((NativeSequence) inS).getSchema(),
                    inS.getName()));
            ((SequenceNativetype) exS).setNativeName(((NativeSequence) inS).getNativeName());
            return factory.createNative((SequenceNativetype) exS);
        } else {
            String message =
                    "Type of sequence not recognised for " + "sequence; " + inS.getName();
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new SequenceServiceException();
        }
    }

    @Override
    public one.jodi.etl.internalmodel.Sequences transmute(Sequences sequences) {
        final Collection<Sequence> internalSequences = new ArrayList<Sequence>();
        sequences.getSequence()
                .forEach(s -> internalSequences.add(transformFromExternall(s.getValue())));
        return new SequencesImpl(internalSequences);
    }

    private String getSchemaFromModel(String model, String name) {
        return this.dictionaryModelLogicalSchema.translateToLogicalSchema(model);
    }

    private String getModelFromSchema(String schema, String name) {
        List<ModelProperties> models =
                modelPropertiesProvider.getConfiguredModels().stream()
                        .filter(m -> (m.getCode()).equals(schema))
                        .collect(Collectors.toList());
        if (models.size() == 1) {
            return models.get(0).getModelID() + ".code";
        } else {
            String message = this.errorWarningMessageJodi.formatMessage(4008, ERROR_MESSAGE_4008, this.getClass(), name, schema);
            this.errorWarningMessageJodi.addMessage(message, MESSAGE_TYPE.ERRORS);
            throw new UnsupportedOperationException(message);
        }
    }

    private Sequence transformFromExternall(SequenceType sequence) {
        final Sequence inS;
        Integer intValue =
                sequence.getIncrement() == null ? null : sequence.getIncrement().intValue();
        Boolean global = sequence.isGlobal();
        if (sequence instanceof SequenceStandardtype) {
            inS =
                    new StandardSequenceImpl(sequence.getName(), intValue, global);
        } else if (sequence instanceof SequenceNativetype) {
            final String schema =
                    getSchemaFromModel(((SequenceNativetype) sequence).getModel(),
                            sequence.getName());
            inS =
                    new NativeSequenceImpl(sequence.getName(), intValue, global, schema,
                            ((SequenceNativetype) sequence).getNativeName());
        } else if (sequence instanceof SequenceSpecifictype) {
            final String schema =
                    getSchemaFromModel(((SequenceSpecifictype) sequence).getModel(),
                            sequence.getName());
            inS =
                    new SpecificSequenceImpl(sequence.getName(), intValue, global, schema,
                            ((SequenceSpecifictype) sequence).getTable(),
                            ((SequenceSpecifictype) sequence).getColumn(),
                            ((SequenceSpecifictype) sequence).getFilter());
        } else {
            String message = "Can't find sequence type for sequence " + sequence.getName() +
                    " with type; " + sequence.getClass().getName();
            logger.error(message);
            this.errorWarningMessageJodi.addMessage(message,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new SequenceServiceException(message);
        }
        return inS;
    }
}

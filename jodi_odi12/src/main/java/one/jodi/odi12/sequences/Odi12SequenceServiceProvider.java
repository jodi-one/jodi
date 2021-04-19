package one.jodi.odi12.sequences;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.core.service.SequenceServiceException;
import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.internalmodel.Sequences;
import one.jodi.etl.internalmodel.impl.NativeSequenceImpl;
import one.jodi.etl.internalmodel.impl.SequencesImpl;
import one.jodi.etl.internalmodel.impl.SpecificSequenceImpl;
import one.jodi.etl.internalmodel.impl.StandardSequenceImpl;
import one.jodi.etl.service.sequences.SequenceServiceProvider;
import one.jodi.odi.sequences.OdiSequenceAccessStrategy;
import oracle.odi.domain.project.OdiSequence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

public class Odi12SequenceServiceProvider implements SequenceServiceProvider {

    private static final Logger logger =
            LogManager.getLogger(Odi12SequenceServiceProvider.class);

    private static final String ERROR_MESSAGE_5000 =
            "SequenceImpl type unknown for sequence %s for type; %s.";

    private final OdiSequenceAccessStrategy accessStrategy;
    private final ErrorWarningMessageJodi errorWarningMessages;

    @Inject
    public Odi12SequenceServiceProvider(final OdiSequenceAccessStrategy accessStrategy,
                                        final ErrorWarningMessageJodi errorWarningMessages) {
        this.accessStrategy = accessStrategy;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public void create(final Sequence sequence) {
        accessStrategy.create(sequence);
    }

    @Override
    public void delete(final Sequence sequence) {
        accessStrategy.delete(sequence);
    }

    @Override
    public Sequences findAll() {
        Collection<OdiSequence> odiSequences = this.accessStrategy.findAll();
        odiSequences.addAll(this.accessStrategy.findAllGlobals());
        return transform(odiSequences);
    }

    private Sequences transform(
            final Collection<OdiSequence> allOdiSequences) {
        Collection<Sequence> internalSequences = new ArrayList<Sequence>();
        allOdiSequences.forEach(s -> internalSequences.add(transform(s)));
        return new SequencesImpl(internalSequences);
    }

    private Sequence transform(final OdiSequence pOdiSequence) {
        final Sequence sequence;
        if (pOdiSequence.getType().equals(OdiSequence.SequenceType.NATIVE)) {
            sequence = new NativeSequenceImpl(pOdiSequence.getName(),
                    pOdiSequence.getIncrementValue(),
                    pOdiSequence.isGlobal(),
                    pOdiSequence.getLogicalSchemaName(),
                    pOdiSequence.getNativeName());
        } else if (pOdiSequence.getType().equals(OdiSequence.SequenceType.SPECIFIC)) {
            sequence =
                    new SpecificSequenceImpl(pOdiSequence.getName(),
                            pOdiSequence.getIncrementValue(),
                            pOdiSequence.isGlobal(),
                            pOdiSequence.getLogicalSchemaName(),
                            pOdiSequence.getTableName(),
                            pOdiSequence.getColumnName(),
                            pOdiSequence.getRowFilterExpression() != null
                                    ? pOdiSequence.getRowFilterExpression()
                                    .getAsString()
                                    : "");
        } else if (pOdiSequence.getType().equals(OdiSequence.SequenceType.STANDARD)) {
            sequence = new StandardSequenceImpl(pOdiSequence.getName(),
                    pOdiSequence.getIncrementValue(),
                    pOdiSequence.isGlobal());
        } else {
            String message =
                    this.errorWarningMessages.formatMessage(5000, ERROR_MESSAGE_5000,
                            this.getClass(),
                            pOdiSequence.getName(),
                            pOdiSequence.getType().name());
            logger.error(message);
            errorWarningMessages.addMessage(message,
                    ErrorWarningMessageJodi.MESSAGE_TYPE.ERRORS);
            throw new SequenceServiceException(message);
        }
        return sequence;
    }

}

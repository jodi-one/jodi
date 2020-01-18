package one.jodi.odi.sequences;

import one.jodi.etl.internalmodel.Sequence;
import oracle.odi.domain.project.OdiSequence;

import java.util.Collection;


/**
 * Created by duvanl on 5/3/2016.
 */
public interface OdiSequenceAccessStrategy {
    Collection<OdiSequence> findAll();

    Collection<OdiSequence> findAllGlobals();

    void create(Sequence internalSequence);

    void delete(Sequence sequence);

}
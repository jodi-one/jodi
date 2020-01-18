package one.jodi.etl.service.sequences;

import one.jodi.etl.internalmodel.Sequence;
import one.jodi.etl.internalmodel.Sequences;

public interface SequenceServiceProvider {

    void create(Sequence sequence);

    void delete(Sequence sequence);

    Sequences findAll();
}

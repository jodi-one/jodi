package one.jodi.etl.service.constraints;

import one.jodi.etl.internalmodel.Constraint;


public interface ConstraintEnrichmentService {

    public void enrich(Constraint constraint);

    public void reduce(Constraint constraint);

}

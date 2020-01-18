package one.jodi.etl.service.constraints;

import one.jodi.etl.internalmodel.Constraint;

import java.util.List;

public interface ConstraintServiceProvider {

    void create(Constraint constraint);

    void delete(Constraint constraint);

    List<Constraint> findAll();
}

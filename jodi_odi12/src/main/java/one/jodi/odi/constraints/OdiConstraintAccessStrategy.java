package one.jodi.odi.constraints;

import one.jodi.etl.internalmodel.Constraint;
import one.jodi.etl.service.ResourceNotFoundException;
import oracle.odi.domain.model.OdiCondition;
import oracle.odi.domain.model.OdiKey;
import oracle.odi.domain.model.OdiReference;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface OdiConstraintAccessStrategy {
    Collection<OdiCondition> findAllConditions();

    Collection<OdiKey> findAllKeys();

    Collection<OdiReference> findAllReferences();

    OdiCondition findCondition(String name, String model) throws ResourceNotFoundException;

    OdiKey findKey(String name, String table, String model) throws ResourceNotFoundException;

    OdiReference findReference(String name, String table, String model) throws ResourceNotFoundException;

    Set<String> getModelNames();

    void delete(Constraint constraint);

    void create(Constraint constraint);

    List<Constraint> findAll();

}

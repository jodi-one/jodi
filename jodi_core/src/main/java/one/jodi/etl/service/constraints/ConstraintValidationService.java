package one.jodi.etl.service.constraints;


import one.jodi.etl.internalmodel.Constraint;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public interface ConstraintValidationService {

    public boolean validate(List<Constraint> internalConstraints);

    boolean enrichable(Constraint constraint, Set<String> schemas);
//	public boolean enrichable(ConditionConstraint constraint);
//	public boolean enrichable(KeyConstraint constraint);
//	public boolean enrichable(ReferenceConstraint constraint);

    public boolean deleteable(Constraint constraint, LinkedHashSet<String> nameSet);
//	public boolean deleteable(ConditionConstraint constraint);
//	public boolean deleteable(KeyConstraint constraint);
//	public boolean deleteable(ReferenceConstraint constraint);

    public boolean createable(Constraint constraint);


//	public boolean createable(ConditionConstraint constraint);
//	public boolean createable(KeyConstraint constraint);
//	public boolean createable(ReferenceConstraint constraint);
}


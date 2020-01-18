package one.jodi.etl.service.constraints;

import one.jodi.core.constraints.ConstraintType;
import one.jodi.core.constraints.Constraints;
import one.jodi.etl.internalmodel.Constraint;

import java.util.List;

public interface ConstraintTransformationService {
    /**
     * Transform from external to internal model
     *
     * @param constraints
     * @return list of constraints
     */
    public List<Constraint> transform(String filename,
                                      Constraints constraints);

    /**
     * Transform from external to internal
     *
     * @param filename
     * @param constraint
     * @return internal model
     */
    public Constraint transform(String filename,
                                ConstraintType external);

    public Constraints transform(List<Constraint> internal);
}

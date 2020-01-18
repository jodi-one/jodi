package one.jodi.odi12.constraints;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.Constraint;
import one.jodi.etl.service.constraints.ConstraintServiceProvider;
import one.jodi.odi.constraints.OdiConstraintAccessStrategy;

import java.util.List;

public class Odi12ConstraintServiceProvider implements ConstraintServiceProvider {

    @SuppressWarnings("unused")
    private final JodiProperties properties;
    private final OdiConstraintAccessStrategy constraintAccessStrategy;

    @Inject
    public Odi12ConstraintServiceProvider(final OdiConstraintAccessStrategy constraintsAccessStrategy,
                                          final JodiProperties properties) {
        this.properties = properties;
        this.constraintAccessStrategy = constraintsAccessStrategy;
    }

    @Override
    public void create(Constraint constraint) {
        this.constraintAccessStrategy.create(constraint);
    }

    @Override
    public List<Constraint> findAll() {
        return this.constraintAccessStrategy.findAll();
    }

    @Override
    public void delete(Constraint constraint) {
        this.constraintAccessStrategy.delete(constraint);
    }

}

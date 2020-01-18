package one.jodi.etl.service.constraints;

import com.google.inject.Inject;
import one.jodi.core.constraints.*;
import one.jodi.etl.builder.impl.DictionaryModelLogicalSchema;
import one.jodi.etl.internalmodel.ConditionConstraint;
import one.jodi.etl.internalmodel.Constraint;
import one.jodi.etl.internalmodel.KeyConstraint;
import one.jodi.etl.internalmodel.ReferenceConstraint;
import one.jodi.etl.internalmodel.impl.ConditionConstraintImpl;
import one.jodi.etl.internalmodel.impl.KeyConstraintImpl;
import one.jodi.etl.internalmodel.impl.ReferenceAttributeImpl;
import one.jodi.etl.internalmodel.impl.ReferenceConstraintImpl;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.stream.Collectors;

public class ConstraintTransformationServiceImpl implements ConstraintTransformationService {

    private final DictionaryModelLogicalSchema dictionaryModelLogicalSchema;

    @Inject
    public ConstraintTransformationServiceImpl(final DictionaryModelLogicalSchema dictionaryModelLogicalSchema) {
        this.dictionaryModelLogicalSchema = dictionaryModelLogicalSchema;
    }

    @Override
    public List<Constraint> transform(String filename,
                                      Constraints external) {
        return external.getConstraint().stream()
                .map(c -> transform(filename, c.getValue())).collect(Collectors.toList());
    }


    @Override
    public Constraint transform(String filename, ConstraintType constraint) {
        Constraint internal = null;
        if (constraint instanceof KeyConstraintType) {
            internal = transform(filename, (KeyConstraintType) constraint);
        } else if (constraint instanceof ReferenceConstraintType) {
            internal = transform(filename, (ReferenceConstraintType) constraint);
        } else if (constraint instanceof ConditionConstraintType) {
            internal = transform(filename, (ConditionConstraintType) constraint);
        }

        assert (internal != null);
        return internal;
    }


    private ConditionConstraintImpl transform(String filename, ConditionConstraintType c) {
        return new ConditionConstraintImpl(filename, c.getName(), dictionaryModelLogicalSchema.translateToLogicalSchema(c.getModel()), c.getTable(),
                c.isDefinedInDatabase(), c.isActive(), c.isFlow(), c.isStatic(),
                transform(c.getType()), c.getWhere(), c.getMessage(), c.getModel());
    }


    private ConditionConstraint.Type transform(ConditionConstraintEnum c) {
        return ConditionConstraint.Type.valueOf(c.name());
    }


    private KeyConstraintImpl transform(String filename, KeyConstraintType c) {
        KeyConstraintImpl constraint = new KeyConstraintImpl(filename, c.getName(), dictionaryModelLogicalSchema.translateToLogicalSchema(c.getModel()), c.getTable(),
                c.isDefinedInDatabase(), c.isActive(), c.isFlow(), c.isStatic(), transform(c.getType()), c.getModel());
        c.getAttributes().getKeyAttribute().forEach(ka -> {
            constraint.getAttributes().add(ka);
        });
        return constraint;
    }

    private KeyConstraint.KeyType transform(KeyType external) {
        return KeyConstraint.KeyType.valueOf(external.name());
    }


    private ReferenceConstraintImpl transform(String filename, ReferenceConstraintType c) {
        ReferenceConstraintImpl constraint = new ReferenceConstraintImpl(filename, c.getName(), dictionaryModelLogicalSchema.translateToLogicalSchema(c.getModel()), c.getTable(),
                c.isDefinedInDatabase(), c.isActive(), c.isFlow(), c.isStatic(),
                ReferenceConstraint.Type.valueOf(c.getType().name()), c.getPrimaryTable(),
                c.getPrimaryModel(), c.getExpression(), transform(c.getDeleteBehavior()),
                transform(c.getUpdateBehavior()), c.getModel());
        if (c.getAttributes() != null) {
            c.getAttributes().getReferenceAttribute().forEach(ra -> {
                constraint.getReferenceAttributes().add(new ReferenceAttributeImpl(ra.getFKColumn(), ra.getPKColumn()));
            });
        }

        return constraint;
    }


    private ReferenceConstraint.Action transform(ReferenceBehaviorOnActionType c) {
        return ReferenceConstraint.Action.valueOf(c.name());
    }

    @Override
    public Constraints transform(List<Constraint> internal) {
        ObjectFactory factory = new ObjectFactory();
        Constraints constraints = factory.createConstraints();
        internal.forEach(c -> {
            if (c instanceof ConditionConstraint) {
                constraints.getConstraint().add(transform((ConditionConstraint) c));
            } else if (c instanceof KeyConstraint) {
                constraints.getConstraint().add(transform((KeyConstraint) c));
            } else if (c instanceof ReferenceConstraint) {
                constraints.getConstraint().add(transform((ReferenceConstraint) c));
            } else throw new RuntimeException("Uknown constraint type.");
        });

        return constraints;
    }

    private void transform(Constraint internal, ConstraintType external) {
        external.setName(internal.getName());
        external.setTable(internal.getTable());
        external.setModel(internal.getSchema());
        external.setActive(internal.isActive());
        external.setDefinedInDatabase(internal.isDefinedInDatabase());
        external.setFlow(internal.isFlow());
        external.setStatic(internal.isStatic());
    }


    private JAXBElement<KeyConstraintType> transform(KeyConstraint c) {
        KeyConstraintType kc = new KeyConstraintType();
        transform(c, kc);
        kc.setType(KeyType.valueOf(c.getKeyType().name()));
        KeyAttributesType kat = new KeyAttributesType();
        kat.getKeyAttribute().addAll(c.getAttributes());

        kc.setAttributes(kat);

        ObjectFactory factory = new ObjectFactory();
        return factory.createKeyConstraint(kc);
    }

    private JAXBElement<ReferenceConstraintType> transform(ReferenceConstraint c) {

        ReferenceConstraintType rc = new ReferenceConstraintType();
        transform(c, rc);

        rc.setDeleteBehavior(ReferenceBehaviorOnActionType.valueOf(c.getDeleteBehavior().name()));
        rc.setUpdateBehavior(ReferenceBehaviorOnActionType.valueOf(c.getUpdateBehavior().name()));
        rc.setExpression(c.getExpression());
        rc.setPrimaryTable(c.getPrimaryTable());
        rc.setPrimaryModel(c.getPrimaryModel());
        rc.setType(ReferenceType.valueOf(c.getReferenceType().name()));
        rc.setAttributes(new ReferenceAttributesType());
        c.getReferenceAttributes().forEach(ra -> {
            ReferenceAttributeType rat = new ReferenceAttributeType();
            rat.setFKColumn(ra.getFKColumnName());
            rat.setPKColumn(ra.getPKColumnName());
            rc.getAttributes().getReferenceAttribute().add(rat);
        });

        ObjectFactory factory = new ObjectFactory();
        return factory.createReferenceConstraint(rc);
    }

    private JAXBElement<ConditionConstraintType> transform(ConditionConstraint c) {
        ConditionConstraintType cc = new ConditionConstraintType();
        transform(c, cc);

        cc.setType(ConditionConstraintEnum.valueOf(c.getType().name()));
        cc.setWhere(c.getWhere());

        ObjectFactory factory = new ObjectFactory();
        return factory.createConditionConstraint(cc);

    }

}

package one.jodi.odi.constraints;

import com.google.inject.Inject;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.ConditionConstraint;
import one.jodi.etl.internalmodel.Constraint;
import one.jodi.etl.internalmodel.KeyConstraint;
import one.jodi.etl.internalmodel.ReferenceConstraint;
import one.jodi.etl.internalmodel.ReferenceConstraint.Type;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.etl.service.constraints.ConstraintValidationService;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import oracle.odi.domain.model.OdiDataStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConstraintValidationServiceImpl implements ConstraintValidationService {

    final static String ERROR_MESSAGE_8501 = "Schema %s in Constraint %s located in file %s is not defined in Jodi properties file.";
    final static String ERROR_MESSAGE_8502 = "Table %s in Constraint %s located in file %s is not found in repository";
    final static String ERROR_MESSAGE_8503 = "Schema %s in Constraint %s located in file %s is not found in repository.";
    final static String ERROR_MESSAGE_8504 = "Schema %s in Constraint %s located in file %s is not defined in Jodi properties file.";
    final static String ERROR_MESSAGE_8505 = "PrimaryTable %s in ReferenceConstraint %s located in file %s is not found in repository.";
    final static String ERROR_MESSAGE_8506 = "PrimaryModel %s in ReferenceConstraint %s located in file %s is not found in repository.";
    final static String ERROR_MESSAGE_8507 = "Reference Constraint %s located in file %s defines an Expression for type %s; this will be ignored.";
    final static String ERROR_MESSAGE_8508 = "Reference Constraint %s located in file %s fails to define Expression, which is required when Type is set to COMPLEX_USER_REFERENCE";
    final static String ERROR_MESSAGE_8509 = "Reference Constraint %s located in file %s defines Attributes for type COMPLEX_USER_REFERENCE; these will be ignored.";
    final static String ERROR_MESSAGE_8510 = "KeyConstraint %s located in %s defines KeyAttribute %s which does not exist on datastore %s in model %s.";
    final static String ERROR_MESSAGE_8511 = "ReferenceConstraint %s located in %s defines %s %s which does not exist on datastore %s in model %s.";
    final static String ERROR_MESSAGE_8520 = "ConditionConstraint %s with Model %s defined in file %s already exists in ODI.";
    final static String ERROR_MESSAGE_8521 = "KeyConstraint %s on Table %s and Model %s defined in file %s already exists in ODI.";
    final static String ERROR_MESSAGE_8522 = "ReferenceConstraint %s on PrimaryTable %s and PrimaryModel %s defined in file %s already exists in ODI.";
    final static String ERROR_MESSAGE_8530 = "Constraint %s located in file %s uses Name that has previously been defined. This will be ignored.";
    final static String ERROR_MESSAGE_8555 = "Constraints not valid are deletable %s  is createable: %s isenrichable; %s. ";
    @SuppressWarnings("rawtypes")
    private final OdiTransformationAccessStrategy transformationAccessStrategy;

    ;
    private final OdiConstraintAccessStrategy constraintAccessStrategy;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final Logger logger = LogManager.getLogger(ConstraintValidationServiceImpl.class);

    @SuppressWarnings("rawtypes")
    @Inject
    protected ConstraintValidationServiceImpl(
            JodiProperties properties,
            final ErrorWarningMessageJodi errorWarningMessages,
            OdiTransformationAccessStrategy transformationAccessStrategy,
            OdiConstraintAccessStrategy constraintAccessStrategy) {
        this.properties = properties;
        this.errorWarningMessages = errorWarningMessages;
        this.transformationAccessStrategy = transformationAccessStrategy;
        this.constraintAccessStrategy = constraintAccessStrategy;
    }

    private void addError(MESSAGE_TYPE type, String msg, int code, Object... args) {
        try {
            String message = errorWarningMessages.formatMessage(code, msg, this.getClass(), args);
            errorWarningMessages.addMessage(message, type);
            logger.info(message);
        } catch (IllegalArgumentException e) {
            logger.error(e);
            assert (false) : "configuration error in " + this.getClass();
            throw new RuntimeException(e);
        }
    }

    private SourceValidationEnum validateSource(String table, String model) {
        try {
            if (transformationAccessStrategy.findModel(model) == null) {
                return SourceValidationEnum.MODEL_NOT_FOUND;
            } else {
                transformationAccessStrategy.findDataStore(table, model);
            }
        } catch (ResourceNotFoundException nfe) {
            return SourceValidationEnum.DATASTORE_NOT_FOUND;
        }

        return SourceValidationEnum.VALID;
    }

    @Override
    public boolean enrichable(Constraint constraint, Set<String> schemas) {
        boolean valid = true;
        if (!schemas.contains(constraint.getSchema())) {
            addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8501, 8501, constraint.getSchema(), constraint.getName(), constraint.getFileName());
            valid = false;
        }

        if (constraint instanceof ReferenceConstraint) {
            valid &= enrichable((ReferenceConstraint) constraint, schemas);
        }
        return valid;
    }

    protected boolean enrichable(ConditionConstraint constraint) {
        return true;
    }

    protected boolean enrichable(KeyConstraint constraint) {
        return true;
    }

    protected boolean enrichable(ReferenceConstraint constraint, Set<String> schemas) {
        boolean valid = true;
        if (!schemas.contains(constraint.getPrimaryModel())) {
            addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8506, 8506, constraint.getPrimaryModel(), constraint.getName(), constraint.getFileName());
            valid = false;
        }

        return valid;
    }

    @Override
    public boolean deleteable(Constraint constraint, LinkedHashSet<String> nameSet) {
        boolean valid = true;

        switch (validateSource(constraint.getTable(), constraint.getSchema())) {
            case MODEL_NOT_FOUND:
                addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8503, 8503, constraint.getSchema(), constraint.getName(), constraint.getFileName());
                valid = false;
                break;
            case DATASTORE_NOT_FOUND:
                addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8502, 8502, constraint.getTable(), constraint.getName(), constraint.getFileName());
                valid = false;
                break;
            default:
                break;
        }

        if (nameSet.contains(constraint.getName())) {
            //"Non-unique constraint name %s located in file %s will be ignored.";
            addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8530, 8530, constraint.getName(), constraint.getFileName());
            valid = false;
        } else {
            nameSet.add(constraint.getName());
        }

        if (constraint instanceof ReferenceConstraint) {
            valid &= deleteable((ReferenceConstraint) constraint);
        }

        return valid;
    }

    protected boolean deleteable(ConditionConstraint constraint) {
        return true;
    }

    protected boolean deleteable(KeyConstraint constraint) {
        return true;
    }

    protected boolean deleteable(ReferenceConstraint constraint) {
        boolean valid = true;
        switch (validateSource(constraint.getPrimaryTable(), constraint.getPrimaryModel())) {
            case MODEL_NOT_FOUND:
                addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8506, 8506, constraint.getPrimaryModel(), constraint.getName(), constraint.getFileName());
                valid = false;
                break;
            case DATASTORE_NOT_FOUND:
                addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8505, 8505, constraint.getPrimaryTable(), constraint.getName(), constraint.getFileName());
                valid = false;
                break;
            default:
                break;
        }

        return valid;
    }

    private boolean referenceErrors(ReferenceConstraint constraint, OdiDataStore ds, List<String> attributes, String fieldName) {
        attributes.removeAll(ds.getColumns()
                .stream()
                .map(col -> col.getName())
                .collect(Collectors.toList()));

        attributes.forEach(attr -> {
            addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8511, 8511, constraint.getName(), constraint.getFileName(),
                    fieldName, attr, ds.getName(), ds.getModel().getName());
        });

        return attributes.size() == 0;
    }

    @Override
    public boolean createable(Constraint constraint) {
        boolean valid = true;
        if (constraint instanceof ConditionConstraint) {
            valid &= createable((ConditionConstraint) constraint);
        } else if (constraint instanceof KeyConstraint) {
            valid &= createable((KeyConstraint) constraint);
        } else if (constraint instanceof ReferenceConstraint) {
            valid &= createable((ReferenceConstraint) constraint);
        }
        return valid;
    }

    // this method is subject to sequencing,
    // why you check for something that might still be created after?
    protected boolean createable(ConditionConstraint constraint) {
        return true;
	   /*
		boolean valid = true;
		try {
			if(constraintAccessStrategy.findCondition(constraint.getName(), constraint.getSchema()) != null) {
				addError(MESSAGE_TYPE.ERRORS, 8520, constraint.getName(), constraint.getSchema(), constraint.getFileName());
				valid = false;
			}
		} catch (ResourceNotFoundException e) {
			// previously validated
		}

		return valid;
		*/
    }

    // this method is subject to sequencing,
    // why you check for something that might still be created after?
    // also there is an error message added that actually doesn't change the value of valid.
    protected boolean createable(KeyConstraint constraint) {
        return true;
	   /*
		boolean valid = true;

		try {
		   // this is sequencing,
		   // you can't search for something that could be created after.
			if(constraintAccessStrategy.findKey(constraint.getName(), constraint.getTable(), constraint.getSchema()) != null) {
				addError(MESSAGE_TYPE.ERRORS, 8521, constraint.getName(), constraint.getTable(), constraint.getSchema(), constraint.getFileName());
				valid = false;
			}
		} catch (ResourceNotFoundException e) {
			// already tested in deleteable(KeyConstraint)
		}

		try {
			final OdiDataStore ds = transformationAccessStrategy
					.findDataStore(constraint.getTable(), constraint.getSchema());
			List<String> attributes = constraint.getAttributes()
					.stream()
					.collect(Collectors.toList());
			attributes.removeAll(ds.getColumns()
					.stream()
					.map(col -> col.getName())
					.collect(Collectors.toList()));
			valid &= attributes.size() == 0;
			attributes.forEach(attr -> {
				addError(MESSAGE_TYPE.ERRORS, 8510, constraint.getName(),
						constraint.getFileName(), attr, constraint.getTable(), constraint.getSchema());
			});
			// so what does this do? it adds error messages, but doesn't set valid?
		} catch (ResourceNotFoundException e1) {
			// already tested in deleteable(KeyConstraint)
		}
		logger.info("creatable :"+ valid);
		return valid;
		*/
    }

    // this method is subject to sequencing,
    // why you check for something that might still be created after?
    protected boolean createable(ReferenceConstraint constraint) {

        boolean valid = true;

        // // this method is subject to sequencing,
        // why you check for something that might still be created after?
		/*
		try {
			if(constraintAccessStrategy.findReference(constraint.getName(), constraint.getPrimaryTable(), constraint.getPrimaryModel()) != null) {
				addError(MESSAGE_TYPE.ERRORS, 8522, constraint.getName(), constraint.getPrimaryTable(), constraint.getPrimaryModel(), constraint.getFileName());
				valid = false;
			}
		} catch (ResourceNotFoundException e) {
			// already tested in deleteable(ReferenceConstraint)
		}
	   */
        if (Type.COMPLEX_USER_REFERENCE.equals(constraint.getReferenceType()) && constraint.getReferenceAttributes().size() > 0) {
            // Any attributes provided will go unused.
            addError(MESSAGE_TYPE.WARNINGS, ERROR_MESSAGE_8509, 8509, constraint.getName(), constraint.getFileName());
            valid = false;
            logger.info(valid);
        } else {
            try {
                final OdiDataStore foreignDS = transformationAccessStrategy
                        .findDataStore(constraint.getTable(), constraint.getSchema());
                List<String> fkAttributes = constraint.getReferenceAttributes().stream()
                        .map(ra -> ra.getFKColumnName())
                        .collect(Collectors.toList());
                valid &= referenceErrors(constraint, foreignDS, fkAttributes, "FKColumn");

                final OdiDataStore primaryDS = transformationAccessStrategy
                        .findDataStore(constraint.getPrimaryTable(), constraint.getPrimaryModel());
                List<String> pkAttributes = constraint.getReferenceAttributes().stream()
                        .map(ra -> ra.getPKColumnName())
                        .collect(Collectors.toList());
                valid &= referenceErrors(constraint, primaryDS, pkAttributes, "PKColumn");
            } catch (ResourceNotFoundException e1) {
                assert (false) : "Development error - datastore should be validated at this point.";
            }
        }


        if (constraint.getReferenceType() == Type.COMPLEX_USER_REFERENCE) {
            if (constraint.getExpression() == null || constraint.getExpression().length() < 2) {
                addError(MESSAGE_TYPE.ERRORS, ERROR_MESSAGE_8508, 8508, constraint.getName(), constraint.getFileName());
                valid = false;
            }
        } else if (constraint.getExpression() != null && constraint.getExpression().length() > 1) {
            addError(MESSAGE_TYPE.WARNINGS, ERROR_MESSAGE_8507, 8507, constraint.getName(), constraint.getFileName(), constraint.getReferenceType().name());
        }

        return valid;
    }

    @Override
    public boolean validate(List<Constraint> allConstraints) {
        LinkedHashSet<String> nameSet = new LinkedHashSet<String>();
        Set<String> schemas = this.constraintAccessStrategy.getModelNames();
        boolean isDeletable = allConstraints.stream().filter(c -> !deleteable(c, nameSet)).count() > 0 ? false : true;
        boolean isCreatable = allConstraints.stream().filter(c -> !createable(c)).count() > 0 ? false : true;
        boolean isEnrichable = allConstraints.stream().filter(c -> !enrichable(c, schemas)).count() > 0 ? false : true;
        boolean isValid = isDeletable && isCreatable && isEnrichable;
        if (!isValid) {
            String message = errorWarningMessages.formatMessage(8555, ERROR_MESSAGE_8555, this.getClass(), isDeletable + "", isCreatable + "", isEnrichable + "");
            errorWarningMessages.addMessage(message, MESSAGE_TYPE.ERRORS);
        }
        return isValid;
    }

    private enum SourceValidationEnum {VALID, MODEL_NOT_FOUND, DATASTORE_NOT_FOUND}

}
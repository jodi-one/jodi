package one.jodi.odi12.constraints;

import com.google.inject.Inject;
import one.jodi.core.annotations.TransactionAttribute;
import one.jodi.core.annotations.TransactionAttributeType;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.ConditionConstraint;
import one.jodi.etl.internalmodel.Constraint;
import one.jodi.etl.internalmodel.KeyConstraint;
import one.jodi.etl.internalmodel.ReferenceConstraint;
import one.jodi.etl.internalmodel.ReferenceConstraint.Type;
import one.jodi.etl.internalmodel.impl.ConditionConstraintImpl;
import one.jodi.etl.internalmodel.impl.KeyConstraintImpl;
import one.jodi.etl.internalmodel.impl.ReferenceAttributeImpl;
import one.jodi.etl.internalmodel.impl.ReferenceConstraintImpl;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.odi.constraints.OdiConstraintAccessStrategy;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.IOdiEntity;
import oracle.odi.domain.model.*;
import oracle.odi.domain.model.OdiCondition.ConditionType;
import oracle.odi.domain.model.OdiReference.CascadingRule;
import oracle.odi.domain.model.OdiReference.ReferenceType;
import oracle.odi.domain.model.finder.*;
import oracle.odi.domain.xrefs.expression.ExpressionStringBuilder;
import oracle.odi.languages.support.LanguageProviderImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class Odi12ConstraintsAccessStrategy implements OdiConstraintAccessStrategy {
    private final OdiInstance odiInstance;
    private final JodiProperties properties;
    private final Logger logger = LogManager.getLogger(Odi12ConstraintsAccessStrategy.class);
    private final HashMap<String, OdiDataStore> cacheDataStores = new HashMap<>();
    private Collection<OdiKey> keyCache;
    private Collection<OdiReference> refCache;

    @Inject
    public Odi12ConstraintsAccessStrategy(final OdiInstance odiInstance,
                                          final JodiProperties properties) {
        this.odiInstance = odiInstance;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public Collection<OdiKey> findAllKeys() {
        IOdiKeyFinder finder = ((IOdiKeyFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiKey.class));
        return finder.findAll();
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public Collection<OdiReference> findAllReferences() {
        IOdiReferenceFinder finder =
                ((IOdiReferenceFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiReference.class));
        return finder.findAll();
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public Collection<OdiCondition> findAllConditions() {
        IOdiConditionFinder finder =
                ((IOdiConditionFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiCondition.class));
        return finder.findAll();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiKey findKey(String name, String table,
                          String model) throws ResourceNotFoundException {
        IOdiKeyFinder finder = ((IOdiKeyFinder) odiInstance.getTransactionalEntityManager()
                .getFinder(OdiKey.class));
        return finder.findByName(name,
                findDatastoreByNameAndModel(table,
                        model).getDataStoreId());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiCondition findCondition(String name,
                                      String model) throws ResourceNotFoundException {
        IOdiConditionFinder finder =
                ((IOdiConditionFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiCondition.class));
        OdiCondition retValue = finder.findByName(name, model);
        logger.info("Condition with name: " + name + " in model: " + model + " found: " +
                (retValue != null));
        // assert(retValue != null);
        return retValue;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OdiReference findReference(String name, String table,
                                      String model) throws ResourceNotFoundException {
        IOdiReferenceFinder finder =
                ((IOdiReferenceFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiReference.class));
        return finder.findByName(name,
                findDatastoreByNameAndModel(table,
                        model).getDataStoreId());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void delete(Constraint constraint) {
        logger.info("Deleting constraint " + constraint.getName() + " of type " +
                constraint.getClass().getName());
        IOdiEntity artifact = null;
        try {
            if (constraint instanceof KeyConstraint) {
                artifact = findKey(constraint.getName(), constraint.getTable(),
                        constraint.getSchema());
            } else if (constraint instanceof ConditionConstraint) {
                artifact = findCondition(constraint.getName(), constraint.getSchema());
            } else if (constraint instanceof ReferenceConstraint) {
                artifact =
                        findReference(constraint.getName(),
                                ((ReferenceConstraint) constraint).getPrimaryTable(),
                                ((ReferenceConstraint) constraint).getPrimaryModel());
            } else throw new IllegalArgumentException("Unknown class type");
        } catch (ResourceNotFoundException rnfe) {
            logger.error("Datastore behind constraint not found", rnfe);
            throw new RuntimeException(rnfe);
        }
        if (artifact != null) {
            odiInstance.getTransactionalEntityManager().remove(artifact);
        } else {
            logger.debug("Can't find artifact: " + constraint.getName());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getModelNames() {
        IOdiModelFinder finder =
                (IOdiModelFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiModel.class);
        Collection<OdiModel> model = finder.findAll();
        return model.stream().map(s -> s.getName()).collect(Collectors.toSet());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected OdiDataStore findDatastoreByNameAndModel(String name, String model) {
        IOdiDataStoreFinder finder =
                ((IOdiDataStoreFinder) odiInstance.getTransactionalEntityManager()
                        .getFinder(OdiDataStore.class));
        return finder.findByName(name, model);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void create(Constraint constraint) {
        logger.info("Creating constraint " + constraint.getName() + " type " +
                constraint.getClass().getName());

        IOdiEntity artifact = null;

        if (constraint instanceof KeyConstraint) {
            artifact = createOdiKey((KeyConstraint) constraint);
        } else if (constraint instanceof ConditionConstraint) {
            artifact = createOdiCondition((ConditionConstraint) constraint);
        } else if (constraint instanceof ReferenceConstraint) {
            artifact = createOdiReference((ReferenceConstraint) constraint);
        }
        odiInstance.getTransactionalEntityManager().persist(artifact);
    }

    protected OdiKey createOdiKey(KeyConstraint keyConstraint) {
        final OdiDataStore ds = findDatastoreByNameAndModel(keyConstraint.getTable(),
                keyConstraint.getSchema());

        OdiKey odiKey = new OdiKey(ds, keyConstraint.getName());
        odiKey.setInDatabase(keyConstraint.isDefinedInDatabase());
        odiKey.setFlowCheckEnabled(keyConstraint.isFlow());
        odiKey.setStaticCheckEnabled(keyConstraint.isStatic());
        odiKey.setActive(keyConstraint.isActive());
        odiKey.setKeyType(oracle.odi.domain.model.OdiKey.KeyType.valueOf(keyConstraint.getKeyType()
                .getValue()));
        keyConstraint.getAttributes().stream().map(name -> ds.getColumn(name))
                .forEach(col -> {
                    odiKey.addColumn(col);
                });

        return odiKey;
    }

    protected OdiCondition createOdiCondition(ConditionConstraint conditionConstraint) {
        final OdiDataStore ds =
                findDatastoreByNameAndModel(conditionConstraint.getTable(),
                        conditionConstraint.getSchema());

        OdiCondition odiCondition = new OdiCondition(ds, conditionConstraint.getName());
        odiCondition.setInDatabase(conditionConstraint.isDefinedInDatabase());
        odiCondition.setFlowCheckEnabled(conditionConstraint.isFlow());
        odiCondition.setStaticCheckEnabled(conditionConstraint.isStatic());
        odiCondition.setActive(conditionConstraint.isActive());
        LanguageProviderImpl languageProvider = new LanguageProviderImpl(odiInstance);
        oracle.odi.domain.xrefs.expression.Expression expression =
                (new ExpressionStringBuilder(languageProvider.getDefaultSnpsLanguage())).append(conditionConstraint.getWhere())
                        .toExpression();
        odiCondition.setWhereClause(expression);
        odiCondition.setConditionType(translate(conditionConstraint.getType()));
        if (conditionConstraint.getMessage() != null)
            odiCondition.setMessage(conditionConstraint.getMessage());
        return odiCondition;
    }

    protected OdiReference createOdiReference(ReferenceConstraint referenceConstraint) {
        final OdiDataStore foreignDS =
                findDatastoreByNameAndModel(referenceConstraint.getTable(),
                        referenceConstraint.getSchema());
        final OdiDataStore primaryDS =
                findDatastoreByNameAndModel(referenceConstraint.getPrimaryTable(),
                        referenceConstraint.getPrimaryModel());
        OdiReference odiReference =
                new OdiReference(foreignDS, primaryDS, referenceConstraint.getName());
        odiReference.setFlowCheckEnabled(referenceConstraint.isFlow());
        odiReference.setStaticCheckEnabled(referenceConstraint.isStatic());
        odiReference.setActive(referenceConstraint.isActive());
        odiReference.setDeleteAction(translate(referenceConstraint.getDeleteBehavior()));
        odiReference.setUpdateAction(translate(referenceConstraint.getUpdateBehavior()));
        odiReference.setReferenceType(translate(referenceConstraint.getReferenceType()));
        if (!Type.COMPLEX_USER_REFERENCE.equals(referenceConstraint.getReferenceType())) {
            referenceConstraint.getReferenceAttributes().forEach(ra -> {
                OdiColumn fkColumn = findColumn(foreignDS, ra.getFKColumnName());
                OdiColumn pkColumn = findColumn(primaryDS, ra.getPKColumnName());
                new ReferenceColumn(odiReference, fkColumn, pkColumn);
                // odiReference.getReferenceColumns().add(rc);
            });
        } else {
            LanguageProviderImpl languageProvider = new LanguageProviderImpl(odiInstance);
            oracle.odi.domain.xrefs.expression.Expression expression =
                    (new ExpressionStringBuilder(languageProvider.getDefaultSnpsLanguage())).append(referenceConstraint.getExpression())
                            .toExpression();

            odiReference.setComplexSqlExpression(expression);
        }

        return odiReference;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    protected OdiColumn findColumn(OdiDataStore datastore, String column) {
        Optional<OdiColumn> optional =
                datastore.getColumns().stream().filter(c -> column.equals(c.getName()))
                        .findFirst();
        return optional.isPresent() ? optional.get() : null;
    }

    protected ReferenceType translate(ReferenceConstraint.Type rc) {
        return ReferenceType.valueOf(rc.getValue());
    }

    protected CascadingRule translate(ReferenceConstraint.Action action) {
        return CascadingRule.valueOf(action.name());
    }

    protected ConditionType translate(ConditionConstraint.Type type) {
        return ConditionType.valueOf(type.getValue());
    }

    @Override
    public List<Constraint> findAll() {
        Collection<OdiKey> odiKeys = findAllKeys();
        Collection<OdiReference> odiReferences = findAllReferences();
        Collection<OdiCondition> odiConditions = findAllConditions();
        List<Constraint> list = odiKeys.stream().map(odiKey -> transform(odiKey))
                .collect(Collectors.toList());
        list.addAll(odiReferences.stream().map(ref -> transform(ref))
                .collect(Collectors.toList()));
        list.addAll(odiConditions.stream().map(ref -> transform(ref))
                .collect(Collectors.toList()));

        return list;
    }

    protected ReferenceConstraint transform(OdiReference odiReference) {
        OdiDataStore foreignDatastore = odiReference.getForeignDataStore();
        OdiModel foreignModel = foreignDatastore.getModel();
        OdiDataStore primaryDatastore = odiReference.getPrimaryDataStore();
        OdiModel primaryModel = primaryDatastore.getModel();

        String expression =
                OdiReference.ReferenceType.COMPLEX_REFERENCE == odiReference.getReferenceType() ? odiReference.getComplexSqlExpression()
                        .getAsString()
                        : "";
        ReferenceConstraintImpl referenceConstraint =
                new ReferenceConstraintImpl("", odiReference.getName(),
                        foreignModel.getLogicalSchema().getName(),
                        foreignDatastore.getName(), false,
                        odiReference.isActive(),
                        odiReference.isFlowCheckEnabled(),
                        odiReference.isStaticCheckEnabled(),
                        transform(odiReference.getReferenceType()),
                        primaryDatastore.getName(),
                        primaryModel.getCode(), expression,
                        transform(odiReference.getDeleteAction()),
                        transform(odiReference.getUpdateAction()),
                        foreignModel.getName());

        odiReference.getReferenceColumns().forEach(rcol -> {
            referenceConstraint.getReferenceAttributes()
                    .add(new ReferenceAttributeImpl(rcol.getForeignKeyColumn()
                            .getName(),
                            rcol.getPrimaryKeyColumn()
                                    .getName()));
        });

        return referenceConstraint;
    }

    protected ReferenceConstraint.Type transform(ReferenceType referenceType) {
        return ReferenceConstraint.Type.fromValue(referenceType.name());
    }

    protected ReferenceConstraint.Action transform(CascadingRule cr) {
        return ReferenceConstraint.Action.valueOf(cr != null ? cr.name()
                : ReferenceConstraint.Action.NO_ACTION.name());
    }

    protected KeyConstraint transform(OdiKey odiKey) {
        OdiDataStore datastore = odiKey.getDataStore();
        OdiModel model = datastore.getModel();
        KeyConstraint keyConstraint =
                new KeyConstraintImpl("", odiKey.getName(), model.getLogicalSchema().getName(),
                        datastore.getName(), odiKey.isInDatabase(),
                        odiKey.isActive(), odiKey.isFlowCheckEnabled(),
                        odiKey.isStaticCheckEnabled(),
                        transform(odiKey.getKeyType()),
                        model.getName());
        odiKey.getColumns().forEach(col -> {
            keyConstraint.getAttributes().add(col.getName());
        });

        return keyConstraint;
    }

    protected ConditionConstraint transform(OdiCondition odiCondition) {
        OdiDataStore ds = odiCondition.getDataStore();
        OdiModel dm = ds.getModel();
        ConditionConstraintImpl conditionConstraint =
                new ConditionConstraintImpl("", odiCondition.getName(), dm.getLogicalSchema().getName(),
                        ds.getName(), odiCondition.isInDatabase(),
                        odiCondition.isActive(),
                        odiCondition.isFlowCheckEnabled(),
                        odiCondition.isStaticCheckEnabled(),
                        transform(odiCondition.getConditionType()),
                        odiCondition.getWhereClauseString(),
                        odiCondition.getMessage(),
                        dm.getName());
        return conditionConstraint;
    }

    protected ConditionConstraint.Type transform(ConditionType type) {
        return ConditionConstraint.Type.fromValue(type.name());
    }

    protected KeyConstraint.KeyType transform(oracle.odi.domain.model.OdiKey.KeyType k) {
        return KeyConstraint.KeyType.fromValue(k.name());
    }
}
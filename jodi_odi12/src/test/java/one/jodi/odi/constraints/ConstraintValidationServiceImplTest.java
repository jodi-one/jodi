package one.jodi.odi.constraints;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.config.JodiProperties;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.ReferenceConstraint.Type;
import one.jodi.etl.service.ResourceNotFoundException;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import oracle.odi.domain.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConstraintValidationServiceImplTest {

    ConstraintValidationServiceImpl fixture;
    @Mock
    OdiTransformationAccessStrategy transformationAccessStrategy;
    @Mock
    OdiConstraintAccessStrategy constraintAccessStrategy;
    @Mock
    JodiProperties properties;
    ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * protected ConstraintValidationServiceImpl(
     * JodiProperties properties,
     * final ErrorWarningMessageJodi errorWarningMessages,
     * OdiTransformationAccessStrategy transformationAccessStrategy)
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        fixture = new ConstraintValidationServiceImpl(properties,
                errorWarningMessages, transformationAccessStrategy, constraintAccessStrategy);

        errorWarningMessages.clear();

    }

    @Test
    public void testPreValidateSuccess() {
        KeyConstraint kc = configure(KeyConstraint.class, "CONSTRAINT", "TABLE", "MODEL");
        when(properties.getPropertyKeys()).thenReturn(Arrays.asList("MODEL.CODE"));
        fixture.enrichable(kc);

    }

    @Test
    public void testPreValidateFailure() {
        KeyConstraint kc = configure(KeyConstraint.class, "CONSTRAINT", "TABLE", "MODEL");
        fixture.enrichable(kc);
    }

    @After
    public void after() {
        errorWarningMessages.printMessages();
    }

    @Test
    public void testKeyDeleteableNoModel() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        KeyConstraint kc = configure(KeyConstraint.class, name, table, model);

        when(transformationAccessStrategy.findModel(model)).thenReturn(null);

        fixture.deleteable((Constraint) kc, new LinkedHashSet<String>());

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    @Test
    public void testKeyDeleteableNoTable() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        KeyConstraint kc = configure(KeyConstraint.class, name, table, model);

        OdiModel odiModel = mock(OdiModel.class);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model))
                .thenThrow(new ResourceNotFoundException(""));

        fixture.deleteable((Constraint) kc, new LinkedHashSet<String>());

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    @Test
    public void testConditionDeleteableNoModel() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        ConditionConstraint kc = configure(ConditionConstraint.class, name, table, model);

        when(transformationAccessStrategy.findModel(model)).thenReturn(null);

        fixture.deleteable((Constraint) kc, new LinkedHashSet<String>());

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    @Test
    public void testConditionDeleteableNoTable() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        ConditionConstraint kc = configure(ConditionConstraint.class, name, table, model);

        OdiModel odiModel = mock(OdiModel.class);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model))
                .thenThrow(new ResourceNotFoundException(""));

        fixture.deleteable((Constraint) kc, new LinkedHashSet<String>());

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    @Test
    public void testReferenceDeleteableNoModel() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        ReferenceConstraint kc = configure(ReferenceConstraint.class, name, table, model);
        when(kc.getPrimaryModel()).thenReturn(model);
        when(kc.getPrimaryTable()).thenReturn(table);

        when(transformationAccessStrategy.findModel(model)).thenReturn(null);

        fixture.deleteable(kc);

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    @Test
    public void testReferenceDeleteableNoTable() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        ReferenceConstraint kc = configure(ReferenceConstraint.class, name, table, model);
        when(kc.getPrimaryModel()).thenReturn(model);
        when(kc.getPrimaryTable()).thenReturn(table);

        OdiModel odiModel = mock(OdiModel.class);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model))
                .thenThrow(new ResourceNotFoundException(""));

        fixture.deleteable(kc);

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    //@Test
    public void testKeyAttributes() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        KeyConstraint kc = configure(KeyConstraint.class, name, table, model);
        List<String> attributes = Arrays.asList("C1", "UNKNOWN");
        when(kc.getAttributes()).thenReturn(attributes);

        OdiModel odiModel = mock(OdiModel.class);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);
        OdiColumn odiColumn = mock(OdiColumn.class);
        when(odiColumn.getName()).thenReturn("C1");
        List<OdiColumn> odiColumns = Arrays.asList(odiColumn);
        when(ds.getColumns()).thenReturn(odiColumns);


        fixture.createable(kc);

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    @Test
    public void testReferenceAttributes() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        ReferenceConstraint kc = configure(ReferenceConstraint.class, name, table, model);
        when(kc.getPrimaryTable()).thenReturn(table);
        when(kc.getPrimaryModel()).thenReturn(model);
        ReferenceAttribute ra1 = mock(ReferenceAttribute.class);
        when(ra1.getFKColumnName()).thenReturn("C1");
        when(ra1.getPKColumnName()).thenReturn("C1");
        ReferenceAttribute ra2 = mock(ReferenceAttribute.class);
        when(ra2.getFKColumnName()).thenReturn("UNKNOWN");
        when(ra2.getPKColumnName()).thenReturn("UNKNOWN");
        List<ReferenceAttribute> attributes = Arrays.asList(ra1, ra2);
        when(kc.getReferenceAttributes()).thenReturn(attributes);

        OdiModel odiModel = mock(OdiModel.class);
        when(odiModel.getName()).thenReturn(model);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(ds.getName()).thenReturn(table);
        when(ds.getModel()).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);
        OdiColumn odiColumn = mock(OdiColumn.class);
        when(odiColumn.getName()).thenReturn("C1");
        List<OdiColumn> odiColumns = Arrays.asList(odiColumn);
        when(ds.getColumns()).thenReturn(odiColumns);

        fixture.createable(kc);

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }

    //@Test
    public void testConditionCreateable() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        ConditionConstraint kc = configure(ConditionConstraint.class, name, table, model);


        OdiModel odiModel = mock(OdiModel.class);
        when(odiModel.getName()).thenReturn(model);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(ds.getName()).thenReturn(table);
        when(ds.getModel()).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);

        OdiCondition odiCondition = mock(OdiCondition.class);
        when(odiCondition.getName()).thenReturn(name);
        when(constraintAccessStrategy.findCondition(name, model)).thenReturn(odiCondition);

        assert (!fixture.createable(kc));

    }

    //@Test
    public void testKeyCreateable() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        KeyConstraint kc = configure(KeyConstraint.class, name, table, model);


        OdiModel odiModel = mock(OdiModel.class);
        when(odiModel.getName()).thenReturn(model);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(ds.getName()).thenReturn(table);
        when(ds.getModel()).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);

        OdiKey odiKey = mock(OdiKey.class);
        when(odiKey.getName()).thenReturn(name);
        when(constraintAccessStrategy.findKey(name, table, model)).thenReturn(odiKey);

        assert (!fixture.createable(kc));
        assert (this.errorWarningMessages.getErrorMessages().size() > 0);

    }

    // already created
    //@Test
    public void testReferenceCreateable() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        String primaryTable = "PRIMARYTABLE";
        String primaryModel = "PRIMARYMODEL";
        ReferenceConstraint referenceConstraint = configure(ReferenceConstraint.class, name, table, model);
        when(referenceConstraint.getPrimaryTable()).thenReturn(primaryTable);
        when(referenceConstraint.getPrimaryModel()).thenReturn(primaryModel);

        OdiModel odiModel = mock(OdiModel.class);
        when(odiModel.getName()).thenReturn(model);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(ds.getName()).thenReturn(table);
        when(ds.getModel()).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);

        OdiModel odiPrimaryModel = mock(OdiModel.class);
        when(odiPrimaryModel.getName()).thenReturn(primaryModel);
        when(transformationAccessStrategy.findModel(primaryModel)).thenReturn(odiPrimaryModel);
        OdiDataStore odiPrimaryDataStore = mock(OdiDataStore.class);
        when(odiPrimaryDataStore.getName()).thenReturn(primaryTable);
        when(odiPrimaryDataStore.getModel()).thenReturn(odiPrimaryModel);
        when(transformationAccessStrategy.findDataStore(primaryTable, primaryModel)).thenReturn(odiPrimaryDataStore);


        OdiReference odiReference = mock(OdiReference.class);
        when(odiReference.getName()).thenReturn(name);
        when(constraintAccessStrategy.findReference(name, primaryTable, primaryModel)).thenReturn(odiReference);

        assert (!fixture.createable(referenceConstraint));
    }

    //@Test
    public void testUniqueName() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        KeyConstraint kc1 = configure(KeyConstraint.class, name, table, model);
        KeyConstraint kc2 = configure(KeyConstraint.class, name, table, model);


        OdiModel odiModel = mock(OdiModel.class);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore odiDataStore = mock(OdiDataStore.class);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(odiDataStore);

        assert (fixture.deleteable((Constraint) kc1, new LinkedHashSet<String>()));
        assert (!fixture.deleteable((Constraint) kc2, new LinkedHashSet<String>()));

        assert (errorWarningMessages.getErrorMessages().size() > 0);
    }


    @Test
    public void testReferenceCreateableComplexType() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        String primaryTable = "PRIMARYTABLE";
        String primaryModel = "PRIMARYMODEL";
        ReferenceConstraint referenceConstraint = configure(ReferenceConstraint.class, name, table, model);
        when(referenceConstraint.getPrimaryTable()).thenReturn(primaryTable);
        when(referenceConstraint.getPrimaryModel()).thenReturn(primaryModel);

        OdiModel odiModel = mock(OdiModel.class);
        when(odiModel.getName()).thenReturn(model);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(ds.getName()).thenReturn(table);
        when(ds.getModel()).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);

        OdiModel odiPrimaryModel = mock(OdiModel.class);
        when(odiPrimaryModel.getName()).thenReturn(primaryModel);
        when(transformationAccessStrategy.findModel(primaryModel)).thenReturn(odiPrimaryModel);
        OdiDataStore odiPrimaryDataStore = mock(OdiDataStore.class);
        when(odiPrimaryDataStore.getName()).thenReturn(primaryTable);
        when(odiPrimaryDataStore.getModel()).thenReturn(odiPrimaryModel);
        when(transformationAccessStrategy.findDataStore(primaryTable, primaryModel)).thenReturn(odiPrimaryDataStore);

        when(referenceConstraint.getExpression()).thenReturn(null);
        when(referenceConstraint.getReferenceType()).thenReturn(Type.COMPLEX_USER_REFERENCE);
        assert (!fixture.createable(referenceConstraint));
    }

    //@Test
    public void testReferenceCreateableComplexTypeWithAttributes_warning() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        String primaryTable = "PRIMARYTABLE";
        String primaryModel = "PRIMARYMODEL";
        ReferenceConstraint referenceConstraint = configure(ReferenceConstraint.class, name, table, model);
        when(referenceConstraint.getPrimaryTable()).thenReturn(primaryTable);
        when(referenceConstraint.getPrimaryModel()).thenReturn(primaryModel);

        ReferenceAttribute ra = mock(ReferenceAttribute.class);
        when(ra.getFKColumnName()).thenReturn("FKCol");
        when(ra.getPKColumnName()).thenReturn("PKCol");
        List<ReferenceAttribute> ras = Arrays.asList(ra);
        when(referenceConstraint.getReferenceAttributes()).thenReturn(ras);

        OdiModel odiModel = mock(OdiModel.class);
        when(odiModel.getName()).thenReturn(model);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(ds.getName()).thenReturn(table);
        when(ds.getModel()).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);

        OdiModel odiPrimaryModel = mock(OdiModel.class);
        when(odiPrimaryModel.getName()).thenReturn(primaryModel);
        when(transformationAccessStrategy.findModel(primaryModel)).thenReturn(odiPrimaryModel);
        OdiDataStore odiPrimaryDataStore = mock(OdiDataStore.class);
        when(odiPrimaryDataStore.getName()).thenReturn(primaryTable);
        when(odiPrimaryDataStore.getModel()).thenReturn(odiPrimaryModel);
        when(transformationAccessStrategy.findDataStore(primaryTable, primaryModel)).thenReturn(odiPrimaryDataStore);

        when(referenceConstraint.getExpression()).thenReturn("MY EXPRESSION");
        when(referenceConstraint.getReferenceType()).thenReturn(Type.COMPLEX_USER_REFERENCE);

        boolean valid = fixture.createable(referenceConstraint);
        assert (errorWarningMessages.existsWarningMessageWithCode(8509));
        assert (valid);
    }

    @Test
    public void testReferenceCreateableNonComplexType() throws ResourceNotFoundException {
        String name = "CONSTRAINT";
        String table = "TABLE";
        String model = "MODEL";
        String primaryTable = "PRIMARYTABLE";
        String primaryModel = "PRIMARYMODEL";
        ReferenceConstraint referenceConstraint = configure(ReferenceConstraint.class, name, table, model);
        when(referenceConstraint.getPrimaryTable()).thenReturn(primaryTable);
        when(referenceConstraint.getPrimaryModel()).thenReturn(primaryModel);

        OdiModel odiModel = mock(OdiModel.class);
        when(odiModel.getName()).thenReturn(model);
        when(transformationAccessStrategy.findModel(model)).thenReturn(odiModel);
        OdiDataStore ds = mock(OdiDataStore.class);
        when(ds.getName()).thenReturn(table);
        when(ds.getModel()).thenReturn(odiModel);
        when(transformationAccessStrategy.findDataStore(table, model)).thenReturn(ds);

        OdiModel odiPrimaryModel = mock(OdiModel.class);
        when(odiPrimaryModel.getName()).thenReturn(primaryModel);
        when(transformationAccessStrategy.findModel(primaryModel)).thenReturn(odiPrimaryModel);
        OdiDataStore odiPrimaryDataStore = mock(OdiDataStore.class);
        when(odiPrimaryDataStore.getName()).thenReturn(primaryTable);
        when(odiPrimaryDataStore.getModel()).thenReturn(odiPrimaryModel);
        when(transformationAccessStrategy.findDataStore(primaryTable, primaryModel)).thenReturn(odiPrimaryDataStore);

        when(referenceConstraint.getExpression()).thenReturn("some expression");
        when(referenceConstraint.getReferenceType()).thenReturn(Type.DATABASE_REFERENCE);
        assert (fixture.createable(referenceConstraint));
        assert (errorWarningMessages.getWarningMessages().size() > 0);
    }


    private <T extends Constraint> T configure(Class<T> classToMock, String name, String table, String model) {
        T constraint = mock(classToMock);
        when(constraint.getName()).thenReturn(name);
        when(constraint.getTable()).thenReturn(table);
        when(constraint.getSchema()).thenReturn(model);
        when(constraint.getFileName()).thenReturn("CONSTRAINTS.XML");

        return constraint;
    }
}

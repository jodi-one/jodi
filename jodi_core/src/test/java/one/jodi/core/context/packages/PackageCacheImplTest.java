package one.jodi.core.context.packages;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.util.Register;
import one.jodi.etl.internalmodel.PackageStep;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.PackageStepImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageCacheImplTest {
    ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Mock
    Register register;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
    }

    //@Test (expected = PackageCacheException.class)
    public void testTransformationNameNotSet() {
        PackageCacheImpl fixture = new PackageCacheImpl(register, errorWarningMessages);
        Transformation transformation = mock(Transformation.class);
        fixture.addTransformationToPackages(transformation);
    }

    @Test
    public void testPackage2TransformationAssociations() {
        PackageCacheImpl fixture = new PackageCacheImpl(register, errorWarningMessages);
        String transformationName = "TRANSFORMATION";
        String package1 = "PACKAGE1";
        String package2 = "PACKAGE2";
        String transformation1PackageList = package1 + "," + package2;
        String transformation2PackageList = package2;
        Transformation transformation1 = createMockTransformation(transformationName, 1, transformation1PackageList);

        Transformation transformation2 = createMockTransformation(transformationName, 2, transformation2PackageList);

        fixture.addTransformationToPackages(transformation1);
        fixture.addTransformationToPackages(transformation2);

        assert (fixture.getTransformationsForPackage(package1, "FOLDER_NAME").size() == 1);
        assert (contains(fixture.getTransformationsForPackage(package1, "FOLDER_NAME"), 1));

        assert (fixture.getTransformationsForPackage(package2, "FOLDER_NAME").size() == 2);
        assert (contains(fixture.getTransformationsForPackage(package2, "FOLDER_NAME"), 2));
        assert (contains(fixture.getTransformationsForPackage(package2, "FOLDER_NAME"), 1));

        print(package1, fixture.getTransformationsForPackage(package1, "FOLDER_NAME"));
        print(package2, fixture.getTransformationsForPackage(package2, "FOLDER_NAME"));
    }

    private boolean contains(List<TransformationCacheItem> items, int packageSequence) {
        boolean found = false;
        for (TransformationCacheItem item : items) {
            if (item.getPackageSequence() == packageSequence) {
                found = true;
            }
        }
        return found;
    }

    private void print(String packageName, List<TransformationCacheItem> items) {
        System.out.print(packageName + " --> ");
        for (TransformationCacheItem item : items) {
            System.out.print(item.getName() + "(" + item.getPackageSequence() + ") ");
        }
        System.out.println("");
    }

    private Transformation createMockTransformation(String transformationName, int packageSequence, String packageList) {
        Transformation transformation = mock(Transformation.class);
        when(transformation.getName()).thenReturn(transformationName + "__" + packageSequence);
        when(transformation.getPackageList()).thenReturn(packageList);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);
        when(transformation.getFolderName()).thenReturn("FOLDER_NAME");
        return transformation;
    }
	
	/*
	@Test
	public void testGetPackagesInCreationOrder() {
		PackageCacheImpl fixture = new PackageCacheImpl();
		ETLPackage package1 = mock(ETLPackage.class);
		ETLPackage package2 = mock(ETLPackage.class);
		ETLPackage package3 = mock(ETLPackage.class);

		when(package1.getPackageName()).thenReturn("package1");
		when(package2.getPackageName()).thenReturn("package2");
		when(package3.getPackageName()).thenReturn("package3");
		when(package1.getPackageListItems()).thenReturn(Arrays.asList("A", "B", "C"));
		when(package2.getPackageListItems()).thenReturn(Arrays.asList("X", "Y", "Z"));
		when(package3.getPackageListItems()).thenReturn(Arrays.asList(""));

		ETLStep packageStep = createPackageStepTypeMock("package3");

		when(package1.getFirstStep()).thenReturn(packageStep);

		fixture.initializePackageCache(Arrays.asList(package1, package2, package3));

		List<ETLPackage> result = fixture.getPackagesInCreationOrder();
		assertNotNull(result);
		assertThat(result, IsIterableContainingInOrder.<ETLPackage> contains(
				package1, package2, package3));

	}*/

    @SuppressWarnings("unused")
    private PackageStep createPackageStepTypeMock(String name) {
        PackageStepImpl mock = mock(PackageStepImpl.class);
        when(mock.getName()).thenReturn(name);
        return mock;
    }
	
	/*
	@Test
	public void testInitializePackageCache() {
		PackageCacheImpl fixture = new PackageCacheImpl();
		ETLPackage package1 = mock(ETLPackage.class);
		ETLPackage package2 = mock(ETLPackage.class);

		when(package1.getPackageName()).thenReturn("package1");
		when(package2.getPackageName()).thenReturn("package2");
		when(package1.getPackageListItems()).thenReturn(Arrays.asList("A", "B", "C"));
		when(package2.getPackageListItems()).thenReturn(Arrays.asList("X", "Y", "Z"));

		fixture.initializePackageCache(Arrays.asList(package1, package2));
		verify(package1).getPackageListItems();
		verify(package2).getPackageListItems();
		verify(package1).getPackageName();
		verify(package2).getPackageName();
		verify(package1).getFirstStep();
		verify(package2).getFirstStep();
//		verify(package1).getFirstAfterStep();
//		verify(package2).getFirstAfterStep();
	}

	@Test
	public void testAddTransformationToPackages() {
		PackageCacheImpl fixture = new PackageCacheImpl();
		ETLPackage package1 = mock(ETLPackage.class);
		ETLPackage package2 = mock(ETLPackage.class);
		ETLPackage package3 = mock(ETLPackage.class);

		when(package1.getPackageName()).thenReturn("package1");
		when(package2.getPackageName()).thenReturn("package2");
		when(package3.getPackageName()).thenReturn("package3");
		when(package1.getPackageListItems()).thenReturn(Arrays.asList("A", "B", "C"));
		when(package2.getPackageListItems()).thenReturn(Arrays.asList("X", "Y", "Z"));
		when(package3.getPackageListItems()).thenReturn(Arrays.asList(""));

		fixture.initializePackageCache(Arrays.asList(package1, package2, package3));

		Transformation transformation = mock(Transformation.class);
		when(transformation.getName()).thenReturn("Transformation1");
		when(transformation.getPackageSequence()).thenReturn(1);
		when(transformation.getPackageList()).thenReturn("W,T,z");
		fixture.addTransformationToPackages(transformation);
		List<Transformation> result = fixture
				.getTransformationsForPackage("package2");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Transformation1", result.get(0).getName());
		assertEquals(1, result.get(0).getPackageSequence());

		result = fixture.getTransformationsForPackage("package1");
		assertNotNull(result);
		assertEquals(0, result.size());

		result = fixture.getTransformationsForPackage("package3");
		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void testGetTransformationsForPackageInvalidPackageName() {
		PackageCacheImpl fixture = new PackageCacheImpl();
		ETLPackage package1 = mock(ETLPackage.class);
		ETLPackage package2 = mock(ETLPackage.class);
		ETLPackage package3 = mock(ETLPackage.class);

		when(package1.getPackageName()).thenReturn("package1");
		when(package2.getPackageName()).thenReturn("package2");
		when(package3.getPackageName()).thenReturn("package3");
		when(package1.getPackageListItems()).thenReturn(Arrays.asList("A", "B", "C"));
		when(package2.getPackageListItems()).thenReturn(Arrays.asList("X", "Y", "Z"));
		when(package3.getPackageListItems()).thenReturn(Arrays.asList(""));

		fixture.initializePackageCache(Arrays.asList(package1, package2, package3));

		Transformation transformation = mock(Transformation.class);
		when(transformation.getName()).thenReturn("Transformation1");
		when(transformation.getPackageSequence()).thenReturn(1);
		when(transformation.getPackageList()).thenReturn("W,T,z");
		fixture.addTransformationToPackages(transformation);

		thrown.expect(PackageCacheException.class);
		thrown.expectMessage("No package found with name package4");
		fixture.getTransformationsForPackage("package4");
	}

	@Test
	public void testGetTransformationsForPackageInvalidPackageNameUninitialized() {
		PackageCacheImpl fixture = new PackageCacheImpl();
		List<Transformation> result = fixture.getTransformationsForPackage("package4");
		assertNotNull(result);
		assertEquals(0, result.size());
	}

	@Test
	public void testGetPackagesInCreationOrder() {
		PackageCacheImpl fixture = new PackageCacheImpl();
		ETLPackage package1 = mock(ETLPackage.class);
		ETLPackage package2 = mock(ETLPackage.class);
		ETLPackage package3 = mock(ETLPackage.class);

		when(package1.getPackageName()).thenReturn("package1");
		when(package2.getPackageName()).thenReturn("package2");
		when(package3.getPackageName()).thenReturn("package3");
		when(package1.getPackageListItems()).thenReturn(Arrays.asList("A", "B", "C"));
		when(package2.getPackageListItems()).thenReturn(Arrays.asList("X", "Y", "Z"));
		when(package3.getPackageListItems()).thenReturn(Arrays.asList(""));

		ETLStep packageStep = createPackageStepTypeMock("package3");

		when(package1.getFirstStep()).thenReturn(packageStep);

		fixture.initializePackageCache(Arrays.asList(package1, package2, package3));

		List<ETLPackage> result = fixture.getPackagesInCreationOrder();
		assertNotNull(result);
		assertThat(result, IsIterableContainingInOrder.<ETLPackage> contains(
				package3, package1, package2));

	}

	private PackageStep createPackageStepTypeMock(String name) {
	    PackageStepImpl mock = mock(PackageStepImpl.class);
		when(mock.getName()).thenReturn(name);
		return mock;
	}

	public static void main(String[] args) {
		new org.junit.runner.JUnitCore().run(PackageCacheImplTest.class);
	}
	*/
}

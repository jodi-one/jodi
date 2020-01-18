package one.jodi.tools.dependency.impl;

import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.tools.dependency.DirectedGraph;
import one.jodi.tools.dependency.MappingDependenciesComparator;
import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class MappingDependenciesComparatorTest {

    String model = "MODEL";

    @Mock
    DirectedGraph<String> dependencies;
    ArrayList<String> dataStores = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * U a -> b
     * V b -> c
     * W c -> d
     * X e - > f
     * <p>
     * assume data stores sorted as a,e,b,f,c
     * <p>
     * sorted order should be U,X,V,W
     */
    @Test
    public void testSortingOrder() {
        createDataStoresInOrder("a", "e", "b", "f", "c", "f", "d");

        when(dependencies.topologicalSort()).thenReturn(dataStores);
        MappingDependenciesComparator comparator = new MappingDependenciesComparator(dependencies);
        MappingHolder t1 = createMappingHolder("b", "a");
        MappingHolder t2 = createMappingHolder("c", "b");
        MappingHolder t3 = createMappingHolder("d", "c");
        MappingHolder t4 = createMappingHolder("f", "e");

        ArrayList<MappingHolder> MappingHolders = new ArrayList<MappingHolder>();
        MappingHolders.add(t4);
        MappingHolders.add(t2);
        MappingHolders.add(t3);
        MappingHolders.add(t1);


        Collections.sort(MappingHolders, comparator);
        assertTrue(MappingHolders.indexOf(t3) > MappingHolders.indexOf(t2));
        assertTrue(MappingHolders.indexOf(t2) > MappingHolders.indexOf(t1));
        assertTrue(MappingHolders.indexOf(t2) > MappingHolders.indexOf(t1));
    }


    /**
     * U a,b -> c
     * V a,d -> e
     * W c,e -> f
     * X f,g -> h
     * <p>
     * datastores sorted assumed to be a,b,d,g,c,e,f,h
     * should either be in order U,V,W,X or V,U,W,X
     */
    @Test
    public void testSortingOnDependents() {
        createDataStoresInOrder("a", "b", "d", "g", "c", "e", "f", "h");


        when(dependencies.topologicalSort()).thenReturn(dataStores);
        MappingDependenciesComparator comparator = new MappingDependenciesComparator(dependencies);
        MappingHolder t1 = createMappingHolder("c", "a", "b");
        MappingHolder t2 = createMappingHolder("e", "a", "d");
        MappingHolder t3 = createMappingHolder("f", "c", "e");
        MappingHolder t4 = createMappingHolder("h", "g", "f");

        ArrayList<MappingHolder> MappingHolders = new ArrayList<MappingHolder>();
        MappingHolders.add(t4);
        MappingHolders.add(t2);
        MappingHolders.add(t1);
        MappingHolders.add(t3);

        Collections.sort(MappingHolders, comparator);
        assertEquals(MappingHolders.get(3), t4);
        assertTrue(MappingHolders.indexOf(t3) > MappingHolders.indexOf(t2));
        assertTrue(MappingHolders.indexOf(t3) > MappingHolders.indexOf(t1));

    }

    @SuppressWarnings("unused")
    private DataStore createDataStore(String model, String name) {
        DataModel dm = mock(DataModel.class);
        DataStore ds = mock(DataStore.class);
        when(ds.getDataStoreName()).thenReturn(name);
        when(ds.getDataModel()).thenReturn(dm);
        when(dm.getModelCode()).thenReturn(model);
        return ds;
    }


    private void createDataStoresInOrder(String... names) {
        for (String name : names) {
            dataStores.add(name);
        }
    }

    private String getDataStore(String name) {
        for (String ds : dataStores) {
            if (name.equals(ds)) {
                return ds;
            }
        }

        throw new RuntimeException("DS not found.  Problem with Unit Test configuration.");
    }

    private MappingHolder createMappingHolder(String target, String... sources) {
        ArrayList<String> sourceDSs = new ArrayList<String>();
        for (String source : sources) {
            String ds = getDataStore(source);
            sourceDSs.add(ds);
        }
        String targetDS = getDataStore(target);
        return new MappingHolder(null, sourceDSs, targetDS, MappingType.Indeterminate);

    }


}

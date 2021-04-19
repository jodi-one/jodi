package one.jodi.tools;

import one.jodi.core.model.Dataset;
import one.jodi.core.model.Datasets;
import one.jodi.core.model.Lookup;
import one.jodi.core.model.Lookups;
import one.jodi.core.model.Source;
import one.jodi.core.model.Transformation;
import one.jodi.core.model.impl.DatasetImpl;
import one.jodi.core.model.impl.DatasetsImpl;
import one.jodi.core.model.impl.LookupImpl;
import one.jodi.core.model.impl.LookupsImpl;
import one.jodi.core.model.impl.MappingExpressionsImpl;
import one.jodi.core.model.impl.MappingsImpl;
import one.jodi.core.model.impl.SourceImpl;
import one.jodi.core.model.impl.TargetcolumnImpl;
import one.jodi.core.model.impl.TransformationImpl;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.builder.impl.TransformationBuilderImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ModelMethodsTest {

    private static final Logger logger = LogManager.getLogger(ModelMethodsTest.class);
    Transformation transformation;
    HashMap<String, List<String>> map = new HashMap<>();
    ArrayList<String> skipColumns = new ArrayList<>();
    /*
     * Used for easily building equivalent internal models.
     */ TransformationBuilder transformationBuilder = new TransformationBuilderImpl(null, null);

    @Before
    public void setup() {

        map.put("C1", Arrays.asList("S1.C1"));
        map.put("C3", Arrays.asList("S1.C2", "S1.C3"));


        transformation = new TransformationImpl();
        DatasetsImpl datasets = new DatasetsImpl();
        DatasetImpl dataset = new DatasetImpl();
        datasets.getDataset()
                .add(dataset);
        transformation.setDatasets(datasets);
        for (int sourceName : new int[]{1, 2}) {
            SourceImpl source = new SourceImpl();
            source.setName("source_" + sourceName);
            dataset.getSource()
                   .add(source);

            LookupsImpl lookups = new LookupsImpl();
            source.setLookups(lookups);
            for (int lookupName : new int[]{3, 4}) {
                LookupImpl lookup = new LookupImpl();
                lookup.setLookupDataStore("lookup_" + lookupName);
                lookups.getLookup()
                       .add(lookup);
            }
        }

        MappingsImpl mappings = new MappingsImpl();
        transformation.setMappings(mappings);

        for (String columnName : map.keySet()) {
            TargetcolumnImpl column = new TargetcolumnImpl();
            column.setName(columnName);
            mappings.getTargetColumn()
                    .add(column);
            MappingExpressionsImpl expressions = new MappingExpressionsImpl();
            column.setMappingExpressions(expressions);
            column.getMappingExpressions()
                  .getExpression()
                  .addAll(map.get(columnName));
        }


        ModelMethods.setCommonParent(transformation);

    }


    @Test
    public void testSetCommonParent() {

        Datasets datasets = transformation.getDatasets();
        assertEquals(transformation, datasets.getParent());

        Dataset dataset = datasets.getDataset()
                                  .get(0);
        assertEquals(datasets, dataset.getParent());

        for (Source source : dataset.getSource()) {
            assertEquals(dataset, source.getParent());
            Lookups lookups = source.getLookups();
            assertEquals(source, lookups.getParent());
            for (Lookup lookup : lookups.getLookup()) {
                assertEquals(lookups, lookup.getParent());
            }
        }
    }


    private void assertColumnsEqual(Map<String, List<String>> map1, Map<String, List<String>> map2) {
        assertArrayEquals("key set not equal", map1.entrySet()
                                                   .toArray(), map2.entrySet()
                                                                   .toArray());

        for (String key : map1.keySet()) {
            assertArrayEquals(map1.get(key)
                                  .toArray(), map2.get(key)
                                                  .toArray());
        }
    }


    @SuppressWarnings("unused")
    private void print(String name, Map<String, List<String>> ml) {
        logger.info("map with name = " + name);
        for (String key : ml.keySet()) {
            logger.info("  " + key + ":");
            for (String value : ml.get(key)) {
                logger.info(" " + value);
            }
            logger.info("");
        }
    }

    @Test
    public void testGetTargetColumns() {
        assertColumnsEqual(map, ModelMethods.getTargetcolumns(transformation));
    }

    @Test
    public void testGetTargetColumnsInternal() {
        assertColumnsEqual(map, ModelMethods.getTargetcolumns(transformationBuilder.transmute(transformation, 1)));
    }

    @Test
    public void testRemoveAll() {
        one.jodi.etl.internalmodel.Transformation internalTransformation =
                transformationBuilder.transmute(transformation, 1);

        ModelMethods.removeSuperfluousTargetColumns(transformation, internalTransformation, skipColumns);

        assertEquals(0, ModelMethods.getTargetcolumns(transformation)
                                    .keySet()
                                    .size());
    }

    @Test
    public void testRemoveAllNotInSkipColumns() {
        one.jodi.etl.internalmodel.Transformation internalTransformation =
                transformationBuilder.transmute(transformation, 1);

        skipColumns.add("C1");
        ModelMethods.removeSuperfluousTargetColumns(transformation, internalTransformation, skipColumns);

        assertEquals(1, ModelMethods.getTargetcolumns(transformation)
                                    .keySet()
                                    .size());
    }

    @Test
    public void testLeaveBasedOnSingleExpression() {
        one.jodi.etl.internalmodel.Transformation internalTransformation =
                transformationBuilder.transmute(transformation, 1);

        internalTransformation.getMappings()
                              .getTargetColumns()
                              .get(0)
                              .getMappingExpressions()
                              .set(0, "NULL");

        ModelMethods.removeSuperfluousTargetColumns(transformation, internalTransformation, skipColumns);

        assertEquals(1, ModelMethods.getTargetcolumns(transformation)
                                    .keySet()
                                    .size());

    }


    @Test
    public void testLeaveBasedOnMultipleExpression() {
        one.jodi.etl.internalmodel.Transformation internalTransformation =
                transformationBuilder.transmute(transformation, 1);

        internalTransformation.getMappings()
                              .getTargetColumns()
                              .get(1)
                              .getMappingExpressions()
                              .set(0, "NULL");

        ModelMethods.removeSuperfluousTargetColumns(transformation, internalTransformation, skipColumns);

        assertEquals(1, ModelMethods.getTargetcolumns(transformation)
                                    .keySet()
                                    .size());
    }


    @Test
    public void testLeaveBasedOnColumnName() {
        one.jodi.etl.internalmodel.Transformation internalTransformation =
                transformationBuilder.transmute(transformation, 1);

        ((one.jodi.etl.internalmodel.impl.TargetcolumnImpl) internalTransformation.getMappings()
                                                                                  .getTargetColumns()
                                                                                  .get(0)).setName("C0");

        ModelMethods.removeSuperfluousTargetColumns(transformation, internalTransformation, skipColumns);

        assertEquals(1, ModelMethods.getTargetcolumns(transformation)
                                    .keySet()
                                    .size());


    }


    @Test
    public void testGeneral() {
        one.jodi.etl.internalmodel.Transformation internalTransformation =
                transformationBuilder.transmute(transformation, 1);


        for (one.jodi.etl.internalmodel.Targetcolumn tc : internalTransformation.getMappings()
                                                                                .getTargetColumns()) {
            logger.info("targetcolumn name = " + tc.getName());
        }
    }


}

package one.jodi.tools.impl;

import one.jodi.core.etlmodel.Packages;
import one.jodi.core.model.Transformation;
import one.jodi.core.model.impl.*;
import one.jodi.tools.RenderingWriter;
import one.jodi.tools.TransformationCache;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class RendererImplTest {

    @Test
    public void test() throws XpathException, IOException, SAXException {
        StringWriter writer = new StringWriter();

        final ArrayList<Transformation> transformations = new ArrayList<Transformation>();
        transformations.add(createTransformation("MYTRANSFORMATION", "MYSOURCE", "MYTARGET", "COLUMN1", "COLUMN2"));

        TransformationCache transformationCache = new TransformationCache() {

            @Override
            public void registerTransformation(Transformation transformation) {

            }

            @Override
            public void clear() {

            }

            @Override
            public List<Transformation> getTransformations() {
                return transformations;
            }

            @Override
            public int getPackageSequence(Transformation transformation) {
                return 11;
            }
        };

        RendererImpl renderer = new RendererImpl(new TestRenderingWriter(writer), transformationCache);


        renderer.writeTransformations();

        String xml = writer.getBuffer().toString();

        XMLAssert.assertXpathExists("/Transformation[1]", xml);
        XMLAssert.assertXpathExists("/Transformation/Datasets/Dataset/Source/Name", xml);
        XMLAssert.assertXpathEvaluatesTo("MYSOURCE", "/Transformation[1]/Datasets/Dataset/Source/Name", xml);
        XMLAssert.assertXpathEvaluatesTo("MYTARGET", "/Transformation[1]/Mappings/TargetDataStore", xml);
        XMLAssert.assertXpathEvaluatesTo("MYSOURCE.COLUMN1", "/Transformation[1]/Mappings/TargetColumn[Name/text() = 'COLUMN1']/MappingExpressions/Expression[1]", xml);


        XMLAssert.assertXpathNotExists("//Transformation/Dataset[position() = 1]/SetOperator", xml);
        XMLAssert.assertXpathNotExists("//Source/Subselect[text() = 'false']", xml);
        XMLAssert.assertXpathNotExists("Source/Journalized[text() = 'false']", xml);
        XMLAssert.assertXpathNotExists("//Mappings/Distinct[text() = 'false']", xml);

    }

    private Transformation createTransformation(String name, String sourceName, String targetName, String... columns) {
        TransformationImpl transformation = new TransformationImpl();
        transformation.setName(name);
        transformation.setPackageList("package_list");
        DatasetsImpl datasets = new DatasetsImpl();
        DatasetImpl dataset = new DatasetImpl();
        datasets.getDataset().add(dataset);
        transformation.setDatasets(datasets);
        SourceImpl source = new SourceImpl();
        source.setName(sourceName);
        source.setAlias(sourceName);
        dataset.getSource().add(source);


        MappingsImpl mappings = new MappingsImpl();
        transformation.setMappings(mappings);
        mappings.setTargetDataStore(targetName);
        for (String column : columns) {
            TargetcolumnImpl tc = new TargetcolumnImpl();
            tc.setName(column);
            MappingExpressionsImpl mei = new MappingExpressionsImpl();
            mei.getExpression().add(sourceName + "." + column);
            tc.setMappingExpressions(mei);
            mappings.getTargetColumn().add(tc);
        }

        return transformation;
    }

    private static class TestRenderingWriter implements RenderingWriter {

        Writer writer;

        TestRenderingWriter(Writer writer) {
            this.writer = writer;
        }

        @Override
        public Writer create(Transformation transformation) {
            return writer;
        }


        @Override
        public Writer create(Packages packages) {
            return writer;
        }

    }
}

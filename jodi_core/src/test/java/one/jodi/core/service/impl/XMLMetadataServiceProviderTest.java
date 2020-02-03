package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.service.files.FileCollectorImpl;
import one.jodi.base.service.files.FileException;
import one.jodi.base.util.CollectXmlObjectsUtil;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.service.MetadataServiceProvider.TransformationMetadataHandler;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.builder.PackageBuilder;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.internalmodel.Transformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

/**
 * The class <code>XMLMetadataServiceProviderTest</code> contains tests for the
 * class <code>{@link XMLMetadataServiceProvider}</code>.
 */
public class XMLMetadataServiceProviderTest {

    /**
     * DOCUMENT ME!
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    JodiProperties properties;
    @Mock
    PackageCache packageCache;
    @Mock
    FileCollector fileCollector;
    @Mock
    TransformationBuilder transformationBuilder;
    @Mock
    PackageBuilder packageBuilder;
    @Mock
    ETLValidator etlValidator;
    String projectCode = "PCODE";
    XMLMetadataServiceProvider fixture;
    String metaDataFolder = "11_mdfolder";
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Creates a new XMLMetadataServiceProviderTest instance.
     */
    public XMLMetadataServiceProviderTest() {
    }

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(properties.getProjectCode()).thenReturn(projectCode);
        fixture = new XMLMetadataServiceProvider(properties, metaDataFolder, packageCache,
                fileCollector, transformationBuilder,
                packageBuilder, etlValidator,
                errorWarningMessages, "/tmp/properties.properties");
    }

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown() throws Exception {
        errorWarningMessages.printMessages();
        errorWarningMessages.clear();
    }

    /**
     * Run the ETL getETLMetaData() method test.
     *
     * @throws Exception
     */
    @Test
    public void testGetETLMetaDataFileNotFound() throws Exception {
        FileCollector fileCollector = new FileCollectorImpl(errorWarningMessages);
        fixture = new XMLMetadataServiceProvider(properties, metaDataFolder, packageCache,
                fileCollector, transformationBuilder,
                packageBuilder, etlValidator,
                errorWarningMessages, "/tmp/properties.properties");
        thrown.expect(FileException.class);
        thrown.expectMessage("FATAL: directory");
        thrown.expectMessage("not found");
        fixture.getPackages(false);
    }

    /**
     * Run the void provideTransformationMetadata(TransformationMetadataHandler)
     * method test.
     *
     * @throws Exception
     */
    @Test // this scenario should never occur
    public void testProvideTransformationMetadataInvalidFilename() throws Exception {
        File mockDir = mock(File.class);
        when(mockDir.isDirectory()).thenReturn(false);
        when(mockDir.getName()).thenReturn("###_abc.xml");

        Transformation internalTransformation =
                mock(Transformation.class);
        when(internalTransformation.getName()).thenReturn("###_abc.xml");


        Path path = Paths.get("/dir/###_abc.xml");
        List<Path> paths = new ArrayList<>();
        paths.add(path);
        when(fileCollector.collectInPath(any(Path.class),
                any(TransformationFileVisitor.class)))
                .thenReturn(paths);

        @SuppressWarnings("unchecked")
        CollectXmlObjectsUtil<one.jodi.core.model.Transformation> transCollectUtil =
                mock(CollectXmlObjectsUtil.class);
        Map<Path, one.jodi.core.model.Transformation> parsedTrans = new HashMap<>();
        when(transCollectUtil.collectObjectsFromFiles(anyListOf(Path.class)))
                .thenReturn(parsedTrans);
        fixture = new XMLMetadataServiceProvider(properties, metaDataFolder, packageCache,
                fileCollector, transformationBuilder,
                packageBuilder, etlValidator,
                errorWarningMessages, "/tmp/properties.properties");
        fixture.setTransformationCollectUtil(transCollectUtil);

        TransformationMetadataHandler handler = mock(TransformationMetadataHandler.class);

        fixture.provideTransformationMetadata(handler);

        InOrder inOrder = inOrder(handler);

        inOrder.verify(handler).pre();
        inOrder.verify(handler).preDESC();
        inOrder.verify(handler, never()).handleTransformationDESC(internalTransformation);
        inOrder.verify(handler).postDESC();
        inOrder.verify(handler).preASC();
        inOrder.verify(handler).postASC();
        inOrder.verify(handler).post();
    }

    @Test
    public void testProvideTransformationMetadataTooShortFilename() throws Exception {
        File mockDir = mock(File.class);
        when(mockDir.isDirectory()).thenReturn(false);
        when(mockDir.getName()).thenReturn("5_abc.xml");

        Transformation internalTransformation =
                mock(Transformation.class);
        when(internalTransformation.getName()).thenReturn("5_abc.xml");


        List<Path> paths = new ArrayList<>();
        when(fileCollector.collectInPath(any(Path.class),
                any(TransformationFileVisitor.class)))
                .thenReturn(paths);

        @SuppressWarnings("unchecked")
        CollectXmlObjectsUtil<one.jodi.core.model.Transformation> transCollectUtil =
                mock(CollectXmlObjectsUtil.class);
        Map<Path, one.jodi.core.model.Transformation> parsedTrans = new HashMap<>();
        when(transCollectUtil.collectObjectsFromFiles(anyListOf(Path.class)))
                .thenReturn(parsedTrans);
        fixture = new XMLMetadataServiceProvider(properties, metaDataFolder, packageCache,
                fileCollector, transformationBuilder,
                packageBuilder, etlValidator,
                errorWarningMessages, "/tmp/properties.properties");
        fixture.setTransformationCollectUtil(transCollectUtil);


        TransformationMetadataHandler handler = mock(TransformationMetadataHandler.class);

        fixture.provideTransformationMetadata(handler);

        InOrder inOrder = inOrder(handler);

        inOrder.verify(handler).pre();
        inOrder.verify(handler).preDESC();
        inOrder.verify(handler, never()).handleTransformationDESC(internalTransformation);
        inOrder.verify(handler).postDESC();
        inOrder.verify(handler).preASC();
        inOrder.verify(handler).postASC();
        inOrder.verify(handler).post();
    }

    /**
     * Run the void provideTransformationMetadata(TransformationMetadataHandler)
     * method test.
     *
     * @throws Exception
     */
    @Test
    public void testProvideTransformationMetadataSingleFile() throws Exception {
        File mockDir = mock(File.class);
        when(mockDir.isDirectory()).thenReturn(false);
        when(mockDir.getName()).thenReturn("12345_abc.xml");

        final one.jodi.core.model.Transformation transformation = mock(one.jodi.core.model.Transformation.class);

        Transformation internalTransformation =
                mock(Transformation.class);
        when(transformationBuilder.transmute(transformation, 12345))
                .thenReturn(internalTransformation);
        when(internalTransformation.getName()).thenReturn("12345_abc");


        Path path = Paths.get("/dir/12345_abc.xml");
        List<Path> paths = new ArrayList<>();
        paths.add(path);
        when(fileCollector.collectInPath(any(Path.class),
                any(TransformationFileVisitor.class)))
                .thenReturn(paths);

        @SuppressWarnings("unchecked")
        CollectXmlObjectsUtil<one.jodi.core.model.Transformation> transCollectUtil =
                mock(CollectXmlObjectsUtil.class);
        Map<Path, one.jodi.core.model.Transformation> parsedTrans = new HashMap<>();
        parsedTrans.put(path, transformation);
        when(transCollectUtil.collectObjectsFromFiles(anyListOf(Path.class)))
                .thenReturn(parsedTrans);
        fixture = new XMLMetadataServiceProvider(properties, metaDataFolder, packageCache,
                fileCollector, transformationBuilder,
                packageBuilder, etlValidator,
                errorWarningMessages, "/tmp/properties.properties");
        fixture.setTransformationCollectUtil(transCollectUtil);


        TransformationMetadataHandler handler = mock(TransformationMetadataHandler.class);

        fixture.provideTransformationMetadata(handler);

        InOrder inOrder = inOrder(handler);

        inOrder.verify(handler).pre();
        inOrder.verify(handler).preDESC();
        inOrder.verify(handler, times(1)).handleTransformationDESC(internalTransformation);
        inOrder.verify(handler).postDESC();
        inOrder.verify(handler).preASC();
        inOrder.verify(handler).postASC();
        inOrder.verify(handler).post();
    }

}

package one.jodi.core.service.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.base.util.XMLParserUtil;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.ObjectFactory;
import one.jodi.core.model.Transformation;
import one.jodi.core.service.impl.StreamingXMLMetadataServiceProvider.XMLStreamProvider;
import one.jodi.etl.builder.TransformationBuilder;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The class <code>StreamingXMLMetadataServiceProviderTest</code> contains tests
 * for the class <code>{@link StreamingXMLMetadataServiceProvider}</code>.
 *
 */
public class StreamingXMLMetadataServiceProviderTest {
    @Mock
    JodiProperties properties;
    @Mock
    XMLStreamProvider xmlStreamProvider;
    @Mock
    TransformationBuilder transformationBuilder;
    String projectCode = "PCODE";
    StreamingXMLMetadataServiceProvider fixture;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    /**
     * Perform pre-test initialization.
     *
     * @throws Exception if the initialization fails for some reason
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(properties.getProjectCode()).thenReturn(projectCode);

        fixture = new StreamingXMLMetadataServiceProvider(xmlStreamProvider, properties, transformationBuilder, errorWarningMessages);
    }

    /**
     * Perform post-test clean-up.
     *
     * @throws Exception if the clean-up fails for some reason
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Run the ETL getETLMetaData() method test.
     *
     * @throws Exception
     */
    @Test(expected = NotImplementedException.class)
    public void testGetETLMetaData() throws Exception {
        fixture.getPackages(false);
    }

    /**
     * Run the Transformation getTransformation(InputStream) method test.
     *
     * @throws Exception
     */
//	@Test
//	public void testGetTransformation() throws Exception {
//		InputStream stream = mock(InputStream.class);
//        Transformation transformation = mock(Transformation.class);
//        XMLParserUtil<Transformation, one.jodi.core.model.ObjectFactory> parser =
//                mock(XMLParserUtil.class);
//
//        when(parser.loadObjectFromXMLAndValidate(any(InputStream.class),
//                anyString())).thenReturn(transformation);
////        when(jodiObjectFactory.createXMLParserUtil(properties,
////                Transformation.class,
////                one.jodi.core.model.ObjectFactory.class)).thenReturn(
////            parser);
//		Transformation result = fixture.getTransformation(stream);
//		assertNotNull(result);
//		assertSame(transformation, result);
//	}

    /**
     * Run the Object parseEntity(XMLParserUtil<T,O>,InputStream) method test.
     *
     * @throws Exception
     */
    @Test
    public void testParseEntity() throws Exception {
        try (InputStream stream = mock(InputStream.class)) {
            @SuppressWarnings("unchecked")
            XMLParserUtil<Transformation, ObjectFactory> parser =
                    mock(XMLParserUtil.class);
            Transformation transformation = mock(Transformation.class);

            when(parser.loadObjectFromXMLAndValidate(any(InputStream.class),
                    anyString())).thenReturn(transformation);
            Transformation result = fixture.parseEntity(parser, stream);

            assertNotNull(result);
            assertSame(transformation, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run the void provideTransformationMetadata(TransformationMetadataHandler)
     * method test.
     *
     * @throws Exception
     *
     * @generatedBy CodePro at 9/6/13 12:11 PM
     */
//	@Test
//	public void testProvideTransformationMetadata() throws Exception {
//        TransformationMetadataHandler handler = mock(
//                TransformationMetadataHandler.class);
//		InputStream stream1 = mock(InputStream.class);
//		InputStream stream2 = mock(InputStream.class);
////        XMLParserUtil<Transformation, one.jodi.core.model.ObjectFactory> parser =
////                mock(XMLParserUtil.class);
//
//        Transformation t1 = mock(Transformation.class);
//        Transformation t2 = mock(Transformation.class);
////        when(jodiObjectFactory.createXMLParserUtil(properties,
////                Transformation.class,
////                one.jodi.core.model.ObjectFactory.class)).thenReturn(
////            parser);
// 
//        when(parser.loadObjectFromXMLAndValidate(any(InputStream.class),
//                anyString())).thenReturn(t1, t2);
//		when(xmlStreamProvider.getNextTransformationStream()).thenReturn(stream1, stream2, null);
//		fixture.provideTransformationMetadata(handler);
//		
//		verify(handler).handleTransformation(t1);
//		verify(handler).handleTransformation(t2);
//	}

}
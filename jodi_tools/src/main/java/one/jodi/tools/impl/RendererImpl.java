package one.jodi.tools.impl;


import com.google.inject.Inject;
import one.jodi.core.etlmodel.Packages;
import one.jodi.core.etlmodel.impl.PackagesImpl;
import one.jodi.core.model.Transformation;
import one.jodi.core.model.impl.TransformationImpl;
import one.jodi.tools.Renderer;
import one.jodi.tools.RenderingWriter;
import one.jodi.tools.TransformationCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.Writer;

public class RendererImpl implements Renderer {

    private static final String TransformationXSLFile = "/remove_default_values_from_transformation.xsl";
    private final RenderingWriter renderingFileNamer;
    private final TransformationCache transformationCache;
    private final Logger logger = LogManager.getLogger(RendererImpl.class);


    @Inject
    public RendererImpl(RenderingWriter renderingFileNamer, TransformationCache transformationCache) {
        this.renderingFileNamer = renderingFileNamer;
        this.transformationCache = transformationCache;
    }


    @Override
    public void writePackages(Packages packages) {

        try {
            Writer writer = renderingFileNamer.create(packages);
            JAXBContext context = JAXBContext.newInstance(PackagesImpl.class);
            Marshaller jaxbMarshaller = context.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(packages, writer);
        } catch (JAXBException e) {
            logger.error("Cannot marshall Package XML to directory");
            e.printStackTrace();
            throw new RuntimeException("Cannot marshall packages instance to file.");
        }


    }


    @Override
    public void writeTransformations() {
        try {
            JAXBContext context = JAXBContext.newInstance(TransformationImpl.class);

            Marshaller jaxbMarshaller = context.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            InputStream xslInputStream = getClass().getResourceAsStream(TransformationXSLFile);
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xslInputStream));
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");

            for (Transformation transformation : transformationCache.getTransformations()) {
                Writer writer = renderingFileNamer.create(transformation);
                JAXBSource source = new JAXBSource(context, transformation);

                //jaxbMarshaller.marshal(transformation, file);

                transformer.transform(source, new StreamResult(writer));
            }

        } catch (Exception e) {
            logger.error("Cannot marshall Transformation to file", e);
            e.printStackTrace();
            throw new RuntimeException("Cannot write transformation instance to file.");
        }
    }


}

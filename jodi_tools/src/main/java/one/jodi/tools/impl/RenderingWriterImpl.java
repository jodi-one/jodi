package one.jodi.tools.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import one.jodi.base.annotations.XmlFolderName;
import one.jodi.core.etlmodel.Packages;
import one.jodi.core.model.Transformation;
import one.jodi.tools.RenderingWriter;
import one.jodi.tools.TransformationCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

@Singleton
public class RenderingWriterImpl implements RenderingWriter {

    final String rootDirectory;


    private final TransformationCache transformationCache;
    private final Logger logger = LogManager.getLogger(RenderingWriterImpl.class);


    @Inject
    public RenderingWriterImpl(final @XmlFolderName String rootDirectory,
                               final TransformationCache transformationCache) {
        this.rootDirectory = rootDirectory;
        this.transformationCache = transformationCache;

        File dir = new File(rootDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("Cannot create RenderingWriterImpl as [" + rootDirectory + "] is not a writeable directory.");
        }

        logger.info("Writing output to directory " + rootDirectory);
    }

    @Override
    public Writer create(Transformation transformation) {

        File folder = new File(rootDirectory + File.separator + transformation.getPackageList());
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String resourceName = transformation.getName();

        int ps = transformationCache.getPackageSequence(transformation);
        String filename = rootDirectory + File.separator + transformation.getPackageList() + File.separator + ps + "_" + resourceName + ".xml";

        logger.info("output file " + filename + " created for writing");

        try {
            return new FileWriter(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create output file " + filename);
        }
    }

    public Writer create(Packages packges) {
        String filename = rootDirectory + File.separator + "0.xml";
        try {
            return new FileWriter(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create output file " + filename);
        }
    }


}

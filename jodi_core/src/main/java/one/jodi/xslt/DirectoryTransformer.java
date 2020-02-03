package one.jodi.xslt;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.error.ErrorWarningMessageJodiImpl;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectoryTransformer {

    private final static String ERROR_MESSAGE_00730 = "IOException: %s";
    private static final String OPTION_HELP = "help";
    private static final String DEFAULT_FILTER = ".*\\.xml$";
    private static final String OPTION_FILTER = "filter";
    private static final String OPTION_OUTPUT = "output";
    private static final String OPTION_SOURCE = "input";
    private static final String OPTION_STYLESHEET = "stylesheet";
    private static final String OPTION_DEBUG = "debug";
    private static ErrorWarningMessageJodi errorWarningMessages = ErrorWarningMessageJodiImpl.getInstance();
    private static Logger logger = LogManager.getLogger(DirectoryTransformer.class);

    public static void main(String[] args) {

        DirectoryTransformer transformer = new DirectoryTransformer();
        DirectoryTransformer.RunConfig runConfig = null;
        try {
            runConfig = DirectoryTransformer.getConfiguration(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            transformer.transformFiles(runConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Options createOptions() {
        Options opts = new Options();

        opts.addOption(OPTION_HELP, false, "prints this message");
        opts.addOption(OPTION_DEBUG, false, "debug mode");

        opts.addOption("i", OPTION_SOURCE, true, "source directory path");

        opts.addOption("o", OPTION_OUTPUT, true, "output directory path");
        opts.addOption("s", OPTION_STYLESHEET, true, "styleshet file path");

        opts.addOption("f", OPTION_FILTER, true,
                "file match regular expression filter. Defaults to " + DEFAULT_FILTER);

        return opts;
    }

    public static RunConfig getConfiguration(String args[]) throws IOException {
        Options opts = createOptions();

        CommandLineParser parser = new PosixParser();
        CommandLine cmdLine = null;

        try {
            cmdLine = parser.parse(opts, args);
        } catch (ParseException e) {
            usage("Unexpected exception:" + e.getMessage(), opts, -1);
        }

        if (cmdLine == null || cmdLine.hasOption(OPTION_HELP)) {
            usage(null, opts, 1);
        }

        RunConfig result = new RunConfig();

        String sourceDir = cmdLine.getOptionValue(OPTION_SOURCE);
        if (sourceDir == null) {
            usage("The '" + OPTION_SOURCE + "' option is required", opts, 1);
        }

        File sourceFile = new File(sourceDir);
        if (!sourceFile.exists()) {
            usage("The '" + sourceDir + "' directory does not exist", opts, 1);
        }
        result.sourceDirectory = sourceFile;

        String outputDir = cmdLine.getOptionValue(OPTION_OUTPUT);
        if (outputDir == null) {
            usage("The '" + OPTION_OUTPUT + "' option is required", opts, 1);
        }

        File outputFile = new File(outputDir);
        if (!outputFile.exists()) {
            usage("The '" + outputDir + "' directory does not exist", opts, 1);
        }
        result.outputDirectory = outputFile;

        String stylesheetPath = cmdLine.getOptionValue(OPTION_STYLESHEET);
        if (stylesheetPath == null) {
            usage("The '" + OPTION_STYLESHEET + "' option is required", opts, 1);
        }

        File stylesheetFile = new File(stylesheetPath);
        if (!stylesheetFile.exists()) {
            usage("The '" + stylesheetPath + "' file does not exist", opts, 1);
        }
        result.styleSheet = stylesheetFile;
        result.fileMatchPattern = cmdLine.getOptionValue(OPTION_FILTER, DEFAULT_FILTER);
        result.verbose = cmdLine.hasOption(OPTION_DEBUG);

        return result;
    }

    private static void usage(final String header, final Options opts, final int exitCode) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(" ", header, opts, null);
        logger.info("Directorytransformer exited with exit code; " + exitCode);
    }

    public void transformFiles(RunConfig runConfig) throws IOException,
            TransformerConfigurationException, ParserConfigurationException {
        XslTransformer transformer = new XslTransformer(runConfig.styleSheet);
        JodiDirectoryWalker walker = new JodiDirectoryWalker(
                runConfig.sourceDirectory, runConfig.outputDirectory,
                runConfig.fileMatchPattern, transformer, runConfig.verbose);
        walker.walk();
    }

    public static class RunConfig {
        File sourceDirectory;
        File outputDirectory;
        String fileMatchPattern;
        File styleSheet;
        boolean verbose;
    }

    private static class JodiDirectoryWalker {
        private final File baseDirectory;
        private final File outputDirectory;
        private final XslTransformer xslTransformer;
        private final boolean debug;
        private final String fileFilterRegExp;

        public JodiDirectoryWalker(File baseDirectory, File outputDirectory,
                                   String fileFilter, XslTransformer xslTransformer, boolean debug) {
            this.fileFilterRegExp = fileFilter;//super(fileFilter, -1);
            this.baseDirectory = baseDirectory;
            this.outputDirectory = outputDirectory;
            this.xslTransformer = xslTransformer;
            this.debug = debug;
        }


        public void walk() throws IOException {
            Collection<File> processedFiles = new ArrayList<>();
            walk(baseDirectory, processedFiles);
            for (File f : processedFiles) {
                handleFile(f, 0, Collections.<File>emptyList());
            }
        }

        private void walk(File baseDirectory, Collection<File> processedFiles) {
            if (baseDirectory == null || processedFiles == null || baseDirectory.listFiles() == null) {
                return;
            }
            for (File f : baseDirectory.listFiles()) {
                if (f == null) {
                    continue;
                }
                Pattern p = Pattern.compile(fileFilterRegExp);
                Matcher m = p.matcher(f.getName());
                if (m.find()) {
                    processedFiles.add(f);
                }
                if (f.isDirectory()) {
                    walk(f, processedFiles);
                }
            }
        }


        protected void handleFile(File file, int depth, Collection<File> results)
                throws IOException {
            String relativePath = baseDirectory.toURI().relativize(file.toURI())
                    .getPath();

            File outputFile = new File(outputDirectory, relativePath);

            if (debug) {
                logger.info("----------------");
                logger.info("Source file: " + file.getAbsolutePath());
                logger.info("Destination file: "
                        + outputFile.getAbsolutePath());
            }

            if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
                logger.info("Couldn't create directory.");
            }
            try {
                xslTransformer.transformFile(file, outputFile);
            } catch (SAXException | TransformerException e) {
                String msg = errorWarningMessages.formatMessage(730,
                        ERROR_MESSAGE_00730, this.getClass(), e);
                errorWarningMessages.addMessage(
                        errorWarningMessages.assignSequenceNumber(), msg,
                        MESSAGE_TYPE.ERRORS);
                throw new IOException(msg);
            }
        }
    }

    private static class XslTransformer {
        DocumentBuilder builder;
        Transformer transformer;

        public XslTransformer(File stylesheet)
                throws ParserConfigurationException,
                TransformerConfigurationException {
            DocumentBuilderFactory factory = DocumentBuilderFactory
                    .newInstance();
            builder = factory.newDocumentBuilder();
            TransformerFactory tFactory = TransformerFactory.newInstance();
            StreamSource styleSource = new StreamSource(stylesheet);
            transformer = tFactory.newTransformer(styleSource);

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                    "2");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            //transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS,
            //                              "Expression Join Filter Comments IkmOptionValue");
        }

        public void transformFile(File sourceFile, File targetFile)
                throws SAXException, IOException, TransformerException {
            Document document = builder.parse(sourceFile);

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(targetFile);
            transformer.transform(source, result);
        }
    }
}

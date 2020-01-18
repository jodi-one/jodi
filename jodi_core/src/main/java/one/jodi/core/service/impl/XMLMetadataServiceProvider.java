package one.jodi.core.service.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.Nullable;
import one.jodi.base.annotations.PropertyFileName;
import one.jodi.base.annotations.XmlFolderName;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodi.MESSAGE_TYPE;
import one.jodi.base.graph.CycleDetectedException;
import one.jodi.base.graph.Graph;
import one.jodi.base.graph.Node;
import one.jodi.base.service.files.FileCollector;
import one.jodi.base.util.CollectXmlObjectsUtil;
import one.jodi.base.util.CollectXmlObjectsUtilImpl;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.etlmodel.Package;
import one.jodi.core.etlmodel.*;
import one.jodi.core.model.Transformation;
import one.jodi.core.service.MetadataServiceProvider;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.builder.PackageBuilder;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.etl.internalmodel.ETLPackage;
import one.jodi.etl.internalmodel.ETLPackageHeader;
import one.jodi.etl.internalmodel.impl.ETLPackageHeaderImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXBElement;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the {@link MetadataServiceProvider} interface.
 *
 */
public class XMLMetadataServiceProvider implements MetadataServiceProvider {

    private final static Logger logger =
            LogManager.getLogger(XMLMetadataServiceProvider.class);
    private final static String ERROR_MESSAGE_00070 =
            "Ignoring file due to error deriving package sequence: %s, %s";
    private final static String FILE_EXTENSION = ".xml";
    private final static String PACKAGE_DEFAULT_NAME = "0";
    private final static String PACKAGE_JOURNALIZED_NAME = "1";
    private final String metadataFolder;
    private final TransformationBuilder transformationBuilder;
    private final ETLValidator etlValidator;
    private final PackageCache packageCache;
    private final FileCollector fileCollector;
    private final PackageBuilder packageBuilder;
    private final JodiProperties properties;
    private final ErrorWarningMessageJodi errorWarningMessages;
    private final Path propFile;
    private CollectXmlObjectsUtil<Packages> packageCollectUtil;
    private CollectXmlObjectsUtil<Transformation> transCollectUtil;
    @Inject
    public XMLMetadataServiceProvider(final JodiProperties properties,
                                      final @Nullable @XmlFolderName String metadataFolder,
                                      final PackageCache packageCache,
                                      final FileCollector fileCollector,
                                      final TransformationBuilder transformationBuilder,
                                      final PackageBuilder packageBuilder,
                                      final ETLValidator etlValidator,
                                      final ErrorWarningMessageJodi errorWarningMessages,
                                      final @PropertyFileName String propFile) {
        this.metadataFolder = metadataFolder;
        this.properties = properties;
        this.packageCache = packageCache;
        this.fileCollector = fileCollector;
        this.transformationBuilder = transformationBuilder;
        this.packageBuilder = packageBuilder;
        this.etlValidator = etlValidator;
        this.errorWarningMessages = errorWarningMessages;

        this.packageCollectUtil =
                new CollectXmlObjectsUtilImpl<Packages,
                        ObjectFactory>(
                        ObjectFactory.class,
                        properties.getPackageSchemaLocation(),
                        errorWarningMessages);
        this.transCollectUtil =
                new CollectXmlObjectsUtilImpl<Transformation,
                        one.jodi.core.model.ObjectFactory>(
                        one.jodi.core.model.ObjectFactory.class,
                        properties.getInputSchemaLocation(),
                        errorWarningMessages);
        this.propFile = Paths.get(propFile);

    }

    // test purposes only
    protected void setPackageCollectUtil(
            final CollectXmlObjectsUtil<Packages> packageCollectUtil) {
        assert (packageCollectUtil != null);
        this.packageCollectUtil = packageCollectUtil;
    }

    // test purposes only
    protected void setTransformationCollectUtil(
            final CollectXmlObjectsUtil<Transformation> transCollectUtil) {
        assert (transCollectUtil != null);
        this.transCollectUtil = transCollectUtil;
    }

    private int getLeadingInteger(final Path path) {
        try {
            int lead = TransformationNameHelper.getLeadingInteger(path);
            return lead;
        } catch (NumberFormatException e) {
            // this code should never be executed; otherwise a programming error is detected
            String msg = errorWarningMessages.formatMessage(70, ERROR_MESSAGE_00070,
                    this.getClass(),
                    path, e.getMessage());
            errorWarningMessages.addMessage(errorWarningMessages.assignSequenceNumber(),
                    msg, MESSAGE_TYPE.WARNINGS);
            logger.debug(msg, e);
            return -1;
        }
    }

    @Override
    public void provideTransformationMetadata(final TransformationMetadataHandler handler) {
        final List<Path> paths =
                this.fileCollector
                        .collectInPath(Paths.get(this.metadataFolder),
                                new TransformationFileVisitor(errorWarningMessages))
                        .stream()
                        .sorted(new DescSequenceComparator())
                        .collect(Collectors.toList());
        Map<Path, Transformation> transformations =
                this.transCollectUtil.collectObjectsFromFiles(paths);

        ArrayDeque<TransformationInfo> transformationStack =
                new ArrayDeque<>(paths.size() + 1);
        handler.pre();
        handler.preDESC();

        for (Path path : paths) {
            Integer packageSequence = getLeadingInteger(path);
            if (packageSequence == -1) {
                continue;
            }
            etlValidator.validatePackageSequence(packageSequence, path.toFile().getName());

            Transformation transformation = transformations.get(path);
            logger.debug("transmuting Transformation from external to internal model");

            one.jodi.etl.internalmodel.Transformation internalTransformation =
                    transformationBuilder.transmute(transformation, packageSequence);
            transformationStack.push(new TransformationInfo(transformation,
                    internalTransformation,
                    packageSequence));
            handler.handleTransformationDESC(internalTransformation);
        }

        handler.postDESC();

        handler.preASC();

        while (!transformationStack.isEmpty()) {
            TransformationInfo ti = transformationStack.pop();
            handler.handleTransformationASC(ti.internalTransformation, ti.packageSequence);
            if (ti.internalTransformation.getName() != null) {
                packageCache.addTransformationToPackages(ti.internalTransformation);
            }
        }

        handler.postASC();
        handler.post();
    }

    private Stream<Entry<Path, Packages>> getPackagesStream(final boolean journalized) {
        Path path = Paths.get(this.metadataFolder);

        String fileName = PACKAGE_DEFAULT_NAME;
        if (journalized) {
            fileName = PACKAGE_JOURNALIZED_NAME;
        }

        final List<Path> files =
                this.fileCollector.collectInPath(path, fileName + "-", FILE_EXTENSION,
                        fileName + FILE_EXTENSION);
        return packageCollectUtil.collectObjectsFromFiles(files)
                .entrySet()
                .stream()
                .sorted(new DepthFirstComparator<>())
                .peek(e -> logger.debug(e.getKey().toFile().getAbsolutePath()));
    }

    private Stream<JAXBElement<? extends StepType>> getStepStream(Steps steps) {
        return steps == null ? Stream.empty() : steps.getStep().stream();
    }

    private Set<String> getExecPackages(final Package pck,
                                        final Set<String> knownPackageNames) {
        // determine calls to packages from before, after and failure steps
        return Stream.concat(Stream.concat(getStepStream(pck.getBefore()),
                getStepStream(pck.getAfter())),
                getStepStream(pck.getFailure()))
                .filter(s -> s.getValue() instanceof ExecPackageType &&
                        knownPackageNames.contains(s.getValue().getName()))
                .map(s -> s.getValue().getName())
                .collect(Collectors.toSet());
    }

    private List<Package> orderPackages(final Packages packages) {

        Map<String, Node<Package>> packageNodeMap = new HashMap<>();
        Graph<Package> graph = new Graph<>();

        // create nodes for defined packages
        for (Package pck : packages.getPackage()) {
            Node<Package> node = graph.addNode(pck);
            packageNodeMap.put(pck.getPackageName(), node);
        }

        for (Package pck : packages.getPackage()) {
            final Node<Package> callerNode = packageNodeMap.get(pck.getPackageName());
            for (String calledPackage : getExecPackages(pck, packageNodeMap.keySet())) {
                final Node<Package> calledNode = packageNodeMap.get(calledPackage);
                // called node exists in the scope of this Packages element
                assert (calledNode != null);
                // reverse call direction in graph to ensure that the
                graph.addEdge(calledNode, callerNode);
            }
        }

        List<Package> result = Collections.emptyList();
        try {
            result = graph.topologicalSort();
        } catch (CycleDetectedException e) {
            String msg = "Cycle detected that includes packages " +
                    e.getCycleNodes()
                            .stream()
                            .map(n -> ((Package) n.getValue()).getPackageName())
                            .collect(Collectors.joining(", "));
            logger.error(msg, e);
            throw e;
        }
        return result;
    }

    private List<Package> getOrderedPackages(final boolean journalized) {
        return getPackagesStream(journalized)
                .map(e -> orderPackages(e.getValue()))
                .collect(ArrayList::new, List::addAll, List::addAll);
    }

    @Override
    public List<ETLPackageHeader> getPackageHeaders(boolean journalized) {
        int creationOrder = 0;
        List<ETLPackageHeader> headers =
                getOrderedPackages(journalized)
                        .stream()
                        .map(p -> new ETLPackageHeaderImpl(p.getPackageName(),
                                p.getFolderCode(),
                                properties.getProjectCode(),
                                p.getPackageListItem(),
                                p.getComments()))
                        .peek(p -> logger.debug(p.getFolderCode() + "/" + p.getPackageName()))
                        .collect(Collectors.toList());

        for (ETLPackageHeader header : headers) {
            packageCache.addPackageAssociation(header.getPackageName(),
                    header.getPackageListItems(),
                    creationOrder++);
        }
        return Collections.unmodifiableList(headers);
    }

    private List<ETLPackage> loadETLPackages(final Packages packages) {
        if (packages == null || packages.getPackage() == null) {
            return Collections.emptyList();
        }
        return orderPackages(packages)
                .stream()
                .map(p -> packageBuilder.transmute(p))
                .collect(Collectors.toList());
    }

    @Override
    public List<ETLPackage> getPackages(final boolean journalized) {

        List<ETLPackage> packages = getPackagesStream(journalized)
                .map(e -> loadETLPackages(e.getValue()))
                .collect(ArrayList::new, List::addAll,
                        List::addAll);
        return Collections.unmodifiableList(packages);
    }

    @Override
    public List<one.jodi.etl.internalmodel.Transformation> getInternaTransformations() {
        final List<Path> paths =
                this.fileCollector
                        .collectInPath(Paths.get(this.metadataFolder),
                                new TransformationFileVisitor(errorWarningMessages))
                        .stream()
                        .sorted(new DescSequenceComparator())
                        .collect(Collectors.toList());
        Map<Path, Transformation> transformations =
                this.transCollectUtil.collectObjectsFromFiles(paths);

        List<one.jodi.etl.internalmodel.Transformation> transformationStack =
                new ArrayList<>();

        for (Path path : paths) {
            Integer packageSequence = getLeadingInteger(path);
            if (packageSequence == -1) {
                continue;
            }

            Transformation transformation = transformations.get(path);
            logger.debug("transmuting Transformation from external to internal model");

            one.jodi.etl.internalmodel.Transformation internalTransformation =
                    transformationBuilder.transmute(transformation, packageSequence);

            transformationStack.add(internalTransformation);
        }
        return transformationStack;
    }

    private static class TransformationInfo {

        one.jodi.etl.internalmodel.Transformation internalTransformation;
        int packageSequence;

        TransformationInfo(final Transformation transformation,
                           final one.jodi.etl.internalmodel.Transformation internalTransformation,
                           final int packageSequence) {
            this.internalTransformation = internalTransformation;
            this.packageSequence = packageSequence;
        }
    }

}

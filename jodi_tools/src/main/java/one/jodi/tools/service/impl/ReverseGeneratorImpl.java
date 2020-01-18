package one.jodi.tools.service.impl;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.etlmodel.impl.PackageImpl;
import one.jodi.core.etlmodel.impl.PackagesImpl;
import one.jodi.core.model.Transformation;
import one.jodi.core.model.impl.TransformationImpl;
import one.jodi.etl.builder.EnrichingBuilder;
import one.jodi.etl.builder.TransformationBuilder;
import one.jodi.tools.*;
import one.jodi.tools.dependency.MappingHolder;
import one.jodi.tools.dependency.MappingProvider;
import oracle.odi.core.OdiInstance;
import oracle.odi.domain.adapter.AdapterException;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.exception.MapComponentException;
import oracle.odi.domain.mapping.exception.MapConnectionException;
import oracle.odi.domain.mapping.exception.MappingException;
import oracle.odi.domain.mapping.finder.IMappingFinder;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.finder.IOdiFolderFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;


public class ReverseGeneratorImpl implements ReverseGenerator {
    private final OdiInstance odiInstance;
    private final Logger logger = LogManager.getLogger(ReverseGeneratorImpl.class);
    private final JodiProperties properties;
    private final Renderer renderer;
    private final MappingProvider provider;
    private final TransformationBuilder transformationBuilder;
    private final EnrichingBuilder enrichingBuilder;
    private final ModelBuildingStep transformationNameBuildingStep;
    private final ModelBuildingStep kmBuildingStep;
    private final ModelBuildingStep executionLocationBuildingStep;
    private final ModelBuildingStep flagsBuildingStep;
    private final ModelBuildingStep targetColumnBuildingStep;
    private final ModelBuildingStep sourceBuildingStep;

    private final TransformationCache transformationCache;


    @Inject
    public ReverseGeneratorImpl(
            final EnrichingBuilder enrichingBuilder,
            final TransformationBuilder transformationBuilder,
            final MappingProvider provider,
            final OdiInstance odiInstance, final JodiProperties properties,
            final Renderer renderer,
            final TransformationCache transformationCache,
            final @Named("TransformationNameBuildingStep") ModelBuildingStep transformationNameBuildingStep,
            final @Named("KMBuildingStep") ModelBuildingStep kmBuildingStep,
            final @Named("ExecutionLocationBuildingStep") ModelBuildingStep executionLocationBuildingStep,
            final @Named("FlagsBuildingStep") ModelBuildingStep flagsBuildingStep,
            final @Named("TargetColumnBuildingStep") ModelBuildingStep targetColumnBuildingStep,
            final @Named("SourceBuildingStep") ModelBuildingStep sourceBuildingStep) {
        super();
        this.odiInstance = odiInstance;
        this.properties = properties;
        this.renderer = renderer;
        this.provider = provider;
        this.transformationBuilder = transformationBuilder;
        this.enrichingBuilder = enrichingBuilder;
        this.transformationNameBuildingStep = transformationNameBuildingStep;
        this.kmBuildingStep = kmBuildingStep;
        this.executionLocationBuildingStep = executionLocationBuildingStep;
        this.flagsBuildingStep = flagsBuildingStep;
        this.targetColumnBuildingStep = targetColumnBuildingStep;
        this.sourceBuildingStep = sourceBuildingStep;
        this.transformationCache = transformationCache;
    }

    private void generate(OdiFolder folder) {
        logger.warn("Generating Jodi specifications for folder " + folder.getName());
        IMappingFinder finder = (IMappingFinder) odiInstance.getTransactionalEntityManager().getFinder(oracle.odi.domain.mapping.Mapping.class);
        for (MappingHolder mappingHolder : provider.getMappingSequence(folder.getName())) {
            Mapping mapping = finder.findByName(folder, mappingHolder.getMapping());
            generate(folder, mapping, mappingHolder);
        }

    }

    private void generate(OdiFolder folder, Mapping mapping, MappingHolder mappingHolder) {
        //logger.warn("creating for " + mapping.getName());
        try {
            if (mapping.getTargets().size() > 1) {
                logger.error("Jodi generation for Mapping " + mapping.getName() + " in folder " + folder.getName() + " cannot be performed.");
            }
            createTransformation(folder, mapping, mappingHolder);
            //reportIKMs(folder, mapping);
        } catch (MapComponentException e) {
            e.printStackTrace();
        } catch (MappingException e) {
            e.printStackTrace();
        }
    }


    private void createTransformation(OdiFolder folder, Mapping mapping, MappingHolder mappingHolder) throws MapComponentException, AdapterException, MappingException {

        TransformationImpl transformation = new TransformationImpl();
        logger.info("generating Jodi transformation for mapping " + mapping.getName());
        transformation.setPackageList(folder.getName());
        transformation.setComments(mapping.getDescription());

        transformationNameBuildingStep.processPreEnrichment(transformation, mapping, mappingHolder);

        sourceBuildingStep.processPreEnrichment(transformation, mapping, mappingHolder);

        targetColumnBuildingStep.processPreEnrichment(transformation, mapping, mappingHolder);

        flagsBuildingStep.processPreEnrichment(transformation, mapping, mappingHolder);

        kmBuildingStep.processPreEnrichment(transformation, mapping, mappingHolder);

        logger.info("Adding transformation to " + transformation.getMappings().getTargetDataStore());

        ModelMethods.setCommonParent(transformation);

        transformationCache.registerTransformation(transformation);

        one.jodi.etl.internalmodel.Transformation internalTransformation = transformationBuilder.transmute(transformation, transformationCache.getPackageSequence(transformation));

        //((one.jodi.etl.internalmodel.impl.MappingsImpl) internalTransformation.getMappings()).clearTargetcolumns();

        ArrayList<one.jodi.etl.internalmodel.Targetcolumn> removes = new ArrayList<one.jodi.etl.internalmodel.Targetcolumn>();
        for (one.jodi.etl.internalmodel.Targetcolumn tc : internalTransformation.getMappings().getTargetColumns()) {
            if (tc.getMappingExpressions().size() > 0) {
                if (isSimpleSql(tc.getMappingExpressions().get(0))) {
                    removes.add(tc);
                }
            }
        }
        ((one.jodi.etl.internalmodel.impl.MappingsImpl) internalTransformation.getMappings()).removeAllTargetcolumns(removes);

        // Now run enrichment process on transformation and begin comparing to either original model
        // or ODI repository itself.
        enrichingBuilder.enrich(internalTransformation, false);

        transformationNameBuildingStep.processPostEnrichment(transformation, internalTransformation, mapping, mappingHolder);

        executionLocationBuildingStep.processPostEnrichment(transformation, internalTransformation, mapping, mappingHolder);

        flagsBuildingStep.processPostEnrichment(transformation, internalTransformation, mapping, mappingHolder);

        targetColumnBuildingStep.processPostEnrichment(transformation, internalTransformation, mapping, mappingHolder);

    }


    private boolean isSimpleSql(String sql) {
        boolean isSimple = true;
        int positionOfFirstDot = sql.indexOf(".") + 1;
        int positionOfSecondDot = sql.indexOf(".", positionOfFirstDot);
        if (sql.contains(")") || sql.contains("(") || sql.trim().contains(" ")
                || sql.trim().contains(":") || sql.trim().contains(">")
                || sql.trim().contains("<") || sql.trim().contains("-")
                || sql.trim().contains("+") || sql.trim().contains("*")
                || sql.trim().contains("/") || sql.trim().contains("=")) {
            isSimple = false;
        } else if (positionOfSecondDot > 0) {
            isSimple = false;
        } else if (positionOfFirstDot == 0) {
            isSimple = false;
        } else if (sql.contains("||") || sql.toLowerCase().contains("row_loc")) {
            /* for unpivot */
            isSimple = false;
        }
        return isSimple;
    }


    @Override
    public void generate() {

        IOdiFolderFinder folderFinder = (IOdiFolderFinder) odiInstance.getTransactionalEntityManager().getFinder(OdiFolder.class);

        for (String folderName : provider.getFolderSequence()) {
            for (OdiFolder folder : folderFinder.findByName(folderName, properties.getProjectCode())) {
                generate(folder);
            }
        }

        renderer.writeTransformations();
        generatePackages();
        transformationCache.clear();
    }


    private void generatePackages() {

        HashSet<String> packageListItems = new HashSet<String>();
        for (Transformation transformation : transformationCache.getTransformations()) {
            String packageListItem = transformation.getPackageList();
            packageListItems.add(packageListItem);
        }

        PackagesImpl packages = new PackagesImpl();
        for (String packageListItem : packageListItems) {
            PackageImpl pkg = new PackageImpl();
            pkg.setPackageName(packageListItem);
            pkg.setFolderCode(packageListItem);
            pkg.setPackageListItem(packageListItem);
            packages.getPackage().add(pkg);
        }

        renderer.writePackages(packages);

    }
	
	/*
	 *
	 * <Packages xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="../../../../../../jodi_core/src/main/resources/jodi-packages.v1.1.xsd">
		<Package>
			<FolderCode>ZAnalytics</FolderCode>
			<PackageName>PKG_INC_AM</PackageName>
			<PackageListItem>ZAnalytics</PackageListItem>
  		</Package>
  		</Packages>
	 */


    protected void traverseFlow(IMapComponent root) {
        try {
            for (IMapComponent imc : root.getDownstreamConnectedLeafComponents()) {
                logger.info(" -> " + imc.getName() + "(" + imc.getTypeName() + ") ");
                traverseFlow(imc);
            }
        } catch (MapConnectionException e) {
            e.printStackTrace();
        }
    }


}

package one.jodi.core.transformation.impl;

import com.google.inject.Inject;
import one.jodi.base.annotations.DefaultStrategy;
import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.model.types.DataStore;
import one.jodi.core.annotations.InterfacePrefix;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.config.JodiConstants;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.TransformationNameExecutionContext;
import one.jodi.core.extensions.strategies.TransformationNameStrategy;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.transformation.TransformationNameContext;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Mappings;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.etl.internalmodel.impl.LookupImpl;
import one.jodi.etl.internalmodel.impl.MappingsImpl;
import one.jodi.etl.internalmodel.impl.SourceImpl;
import one.jodi.etl.internalmodel.impl.TransformationImpl;
import one.jodi.model.extensions.TransformationExtension;

import java.util.Map;

/**
 * The class manages a default strategy and a custom strategy for determining
 * the name of the transformation. The default strategy is always executed first
 * and the result is passed to the custom strategy, where it may be overwritten.
 * <p>
 * This class is also responsible for building the execution context object that
 * is passed into strategies.
 * <p>
 * The class is the Context that participates in the Strategy Pattern.
 */
public class TransformationNameContextImpl implements TransformationNameContext {

    private final DatabaseMetadataService databaseMetadataService;
    private final String prefix;

    // default strategy is created without DI because it is
    // considered to be hard-coded and is not designed to be
    // modified or extended at this time
    private final TransformationNameStrategy defaultStrategy;
    // custom strategy by default will be a ID strategy that returns
    // the default value; it is configured through Guice injection
    private final TransformationNameStrategy customStrategy;

    private final ETLValidator validator;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private final JodiConstants jodiConstants = new JodiConstants() {
    };

    /**
     * @param prefix                  the string that is passed to Jodi via the command line and is
     *                                intended to be a prefix to the ODI interface name.
     * @param etlProvider             refers to a service for accessing functionality of the ETL
     *                                tool
     * @param databaseMetadataService refers to a service for building an execution context
     * @param defaultStrategy         defines the default strategy with the core business rules
     * @param customStrategy          defines non-null custom strategy (typically an ID strategy)
     */
    @Inject
    public TransformationNameContextImpl(
            final @InterfacePrefix String prefix,
            final DatabaseMetadataService databaseMetadataService,
            final @DefaultStrategy TransformationNameStrategy defaultStrategy,
            final TransformationNameStrategy customStrategy,
            final ETLValidator validator,
            final ErrorWarningMessageJodi errorWarningMessages) {
        this.prefix = prefix;
        this.databaseMetadataService = databaseMetadataService;
        this.customStrategy = customStrategy;
        this.defaultStrategy = defaultStrategy;
        this.validator = validator;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public String getTransformationName(final Transformation transformation) {
        if (transformation.isTemporary()) return getTemporaryDSTarget(transformation);

        final Mappings mappings = transformation
                .getMappings();
        final String targetDataStore = mappings.getTargetDataStore();
        final DataStore ds = databaseMetadataService
                .getTargetDataStoreInModel(mappings);

        // create execution context object
        TransformationNameExecutionContext exc = new TransformationNameExecutionContext() {

            @Override
            public String getPrefix() {
                return prefix;
            }

            @Override
            public boolean isTargetDistinct() {
                return transformation.getMappings().isDistinct();
            }

            @Override
            public DataStore getTargetDataStore() {
                return ds;
            }

            @Override
            public Map<String, PropertyValueHolder> getProperties() {
                return databaseMetadataService.getCoreProperties();
            }

            @Override
            public TransformationExtension getTransformationExtension() {
                // Implemented defensive cloning to avoid that plug-in code
                // alters content of extension object
                ClonerUtil<TransformationExtension> cloner =
                        new ClonerUtil<>(errorWarningMessages);
                return cloner.clone(transformation.getExtension());
            }
        };

        // execute default strategy
        String defaultName = defaultStrategy.getTransformationName(
                transformation.getName(), exc);
        assert ((defaultName != null) && (!defaultName.equals("")));

        // execute custom strategy
        String name = defaultName;
        // We assume here that the custom strategy may not exist in which case
        // the result of the default strategy is applied.
        // Note: Guice forces the definition of a custom strategy unless the
        // @Nullable annotation (JSR305) is used in the constructor

        // for now we do not permit customization of temporary interfaces
        // for now since we use the "_Sxx" postfix to communicate that a
        // temporary table is used
        if (this.customStrategy != null
                && !databaseMetadataService
                .isTemporaryTransformation(targetDataStore)) {

            name = customStrategy.getTransformationName(defaultName, exc);
            ((TransformationImpl) transformation).setName(name);

            validator.validateTransformationName(transformation);
        }
        return name;
    }


    @Override
    public String getTemporaryDSLookup(
            Lookup lookup) {
        return jodiConstants.getReusableMappingPrefix(prefix)
                + lookup.getLookupDataStore();
    }

    @Override
    public String getTemporaryDSSource(Source source) {
        return jodiConstants.getReusableMappingPrefix(prefix)
                + source.getName();
    }


    //@Override
    public String getTemporaryDSTarget(
            Transformation transformation) {
        String name = jodiConstants.getReusableMappingPrefix(prefix)
                + transformation.getMappings().getTargetDataStore();
        ((MappingsImpl) transformation.getMappings()).setTargetDataStore(name);
		
		/*
		if(((MappingsImpl) transformation.getMappings()).getTargetDataStore() == null)
			((MappingsImpl) transformation.getMappings()).setTargetDataStore(name);
		*/

        ((TransformationImpl) transformation).setName(name);
        //((MappingsImpl) transformation.getMappings()).setTemporaryDataStore(name);
        return name;
    }

    @Override
    public String setSourceName(Source source) {
        if (source.isTemporary()) {
            String name = jodiConstants.getReusableMappingPrefix(prefix) + source.getName();
            ((SourceImpl) source).setName(name);
            //((SourceImpl) source).setTemporaryDataStore(name);
        }

        return source.getName();
    }

    @Override
    public String setLookupName(Lookup lookup) {
        if (lookup.isTemporary()) {
            ((LookupImpl) lookup).setLookupDatastore(jodiConstants.getReusableMappingPrefix(prefix)
                    + lookup.getLookupDataStore());
        }

        return lookup.getLookupDataStore();
    }
}
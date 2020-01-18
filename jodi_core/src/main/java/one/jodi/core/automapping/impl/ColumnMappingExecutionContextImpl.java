package one.jodi.core.automapping.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.core.common.ClonerUtil;
import one.jodi.core.config.PropertyValueHolder;
import one.jodi.core.extensions.contexts.ColumnMappingExecutionContext;
import one.jodi.core.extensions.types.DataStoreWithAlias;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.Lookup;
import one.jodi.etl.internalmodel.Source;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.model.extensions.MappingsExtension;
import one.jodi.model.extensions.SourceExtension;
import one.jodi.model.extensions.TransformationExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ColumnMappingExecutionContextImpl
        implements ColumnMappingExecutionContext {

    private final DatabaseMetadataService databaseMetadataService;
    private final ErrorWarningMessageJodi errorWarningMessages;

    private final Dataset dataset;
    private final Transformation transformation;

    private final BinaryOperator<List<String>> merge =
            (old, current) -> {
                old.addAll(current);
                return old;
            };
    Map<String, List<String>> allColumnMapping = null;

    public ColumnMappingExecutionContextImpl(final Dataset dataset,
                                             final DatabaseMetadataService databaseMetadataService,
                                             final ErrorWarningMessageJodi errorWarningMessages) {
        this.dataset = dataset;
        this.transformation = dataset.getParent();
        this.databaseMetadataService = databaseMetadataService;
        this.errorWarningMessages = errorWarningMessages;
    }

    @Override
    public DataStore getTargetDataStore() {
        return databaseMetadataService
                .getTargetDataStoreInModel(transformation.getMappings());
    }

    @Override
    public List<DataStoreWithAlias> getDataStores() {
        ArrayList<DataStoreWithAlias> list = new ArrayList<>();

        for (final Source source : dataset.getSources()) {
            final DataStore sourceDataStore = databaseMetadataService
                    .getSourceDataStoreInModel(source.getName(),
                            source.getModel());
            list.add(new DataStoreWithAlias() {

                @Override
                public String getAlias() {
                    return source.getAlias();
                }

                @Override
                public DataStore getDataStore() {
                    return sourceDataStore;
                }

                @Override
                public Type getType() {
                    return DataStoreWithAlias.Type.Source;
                }

                @Override
                public SourceExtension getSourceExtension() {
                    ClonerUtil<SourceExtension> cloner =
                            new ClonerUtil<>(errorWarningMessages);
                    return cloner.clone(source.getExtension());
                }
            });
            for (final Lookup lookup : source.getLookups()) {
                final DataStore lookupDataStore = databaseMetadataService
                        .getSourceDataStoreInModel(lookup.getLookupDataStore(),
                                lookup.getModel());
                list.add(new DataStoreWithAlias() {

                    @Override
                    public String getAlias() {
                        return lookup.getAlias();
                    }

                    @Override
                    public DataStore getDataStore() {
                        return lookupDataStore;
                    }

                    @Override
                    public Type getType() {
                        return DataStoreWithAlias.Type.Lookup;
                    }

                    @Override
                    public SourceExtension getSourceExtension() {
                        ClonerUtil<SourceExtension> cloner =
                                new ClonerUtil<>(errorWarningMessages);
                        return cloner.clone(source.getExtension());
                    }
                });
            }
        }

        return Collections.unmodifiableList(list);
    }

    @Override
    public MappingsExtension getMappingsExtension() {
        ClonerUtil<MappingsExtension> cloner =
                new ClonerUtil<>(errorWarningMessages);
        return cloner.clone(transformation.getMappings().getExtension());
    }

    @Override
    public Map<String, PropertyValueHolder> getCoreProperties() {
        return databaseMetadataService.getCoreProperties();
    }

    @Override
    public TransformationExtension getTransformationExtension() {
        ClonerUtil<TransformationExtension> cloner =
                new ClonerUtil<>(errorWarningMessages);
        return cloner.clone(transformation.getExtension());
    }

    @Override
    public Dataset getDataset() {
        return dataset;
    }

    private Map<String, String> getColumns(final Source source) {
        DataStore ds = databaseMetadataService
                .getSourceDataStoreInModel(source.getName(),
                        source.getModel());
        Map<String, String> result = Collections.emptyMap();
        if (ds != null) {
            result = ds.getColumns()
                    .values()
                    .stream()
                    .collect(Collectors.toMap(DataStoreColumn::getName,
                            c -> source.getAlias() + "." +
                                    c.getName()));
        }
        return result;
    }

    private Map<String, String> getColumns(final Lookup lookup) {
        DataStore ds = databaseMetadataService
                .getSourceDataStoreInModel(lookup.getLookupDataStore(),
                        lookup.getModel());
        Map<String, String> result = Collections.emptyMap();
        if (ds != null) {
            result = ds.getColumns()
                    .values()
                    .stream()
                    .collect(Collectors.toMap(DataStoreColumn::getName,
                            c -> lookup.getAlias() + "." +
                                    c.getName()));
        }
        return result;
    }

    private Map<String, List<String>> getSourceColumnToAlias() {
        return dataset
                .getSources()
                .stream()
                .flatMap(s -> getColumns(s).entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey,
                        e -> {
                            List<String> l = new ArrayList<>();
                            l.add(e.getValue());
                            return l;
                        },
                        this.merge));
    }

    private Map<String, List<String>> getLookupColumnToAlias() {
        return dataset
                .getSources()
                .stream()
                .flatMap(s -> s.getLookups().stream())
                .flatMap(s -> getColumns(s).entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey,
                        e -> {
                            List<String> l = new ArrayList<>();
                            l.add(e.getValue());
                            return l;
                        },
                        this.merge));
    }

    // collect all column names and map to alias column names in scope
    @Override
    public Map<String, List<String>> getAllColumnToAlias() {
        // calculate once and store for better performance
        if (this.allColumnMapping == null) {
            this.allColumnMapping =
                    Stream.of(getSourceColumnToAlias(), getLookupColumnToAlias())
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                                    this.merge));
        }

        return this.allColumnMapping;
    }

}

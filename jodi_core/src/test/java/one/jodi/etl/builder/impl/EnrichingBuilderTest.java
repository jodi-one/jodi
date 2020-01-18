package one.jodi.etl.builder.impl;

import one.jodi.base.error.ErrorWarningMessageJodi;
import one.jodi.base.error.ErrorWarningMessageJodiHelper;
import one.jodi.core.automapping.ColumnMappingContext;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.context.packages.PackageCache;
import one.jodi.core.datastore.ModelCodeContext;
import one.jodi.core.executionlocation.ExecutionLocationContext;
import one.jodi.core.extensions.types.ExecutionLocationType;
import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.core.extensions.types.TargetColumnFlags;
import one.jodi.core.folder.FolderNameContext;
import one.jodi.core.journalizing.JournalizingContext;
import one.jodi.core.km.KnowledgeModuleContext;
import one.jodi.core.km.impl.KnowledgeModuleConfigurationImpl;
import one.jodi.core.metadata.DatabaseMetadataService;
import one.jodi.core.targetcolumn.FlagsContext;
import one.jodi.core.transformation.TransformationNameContext;
import one.jodi.core.validation.etl.ETLValidator;
import one.jodi.etl.builder.DeleteTransformationContext;
import one.jodi.etl.internalmodel.*;
import one.jodi.etl.internalmodel.impl.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public class EnrichingBuilderTest {

    @Mock
    ColumnMappingContext columnMappingContext;
    @Mock
    ExecutionLocationContext executionLocationContext;
    @Mock
    FlagsContext flagsContext;
    @Mock
    FolderNameContext folderNameContext;
    @Mock
    JournalizingContext journalizingContext;
    @Mock
    KnowledgeModuleContext knowledgeModuleContext;
    @Mock
    ModelCodeContext modelCodeContext;
    @Mock
    TransformationNameContext transformationNameContext;
    @Mock
    JodiProperties properties;
    @Mock
    DatabaseMetadataService databaseMetadataService;
    @Mock
    ETLValidator validator;
    @Mock
    PackageCache packageCache;
    EnrichingBuilderImpl fixture = null;
    private ErrorWarningMessageJodi errorWarningMessages =
            ErrorWarningMessageJodiHelper.getTestErrorWarningMessages();

    @Before
    public void setUp()
            throws Exception {
        MockitoAnnotations.initMocks(this);
        when(properties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");
        fixture = new EnrichingBuilderImpl(
                columnMappingContext,
                executionLocationContext,
                flagsContext,
                folderNameContext,
                journalizingContext,
                knowledgeModuleContext,
                modelCodeContext,
                transformationNameContext,
                properties,
                validator, errorWarningMessages);
    }

    private SourceImpl createSource(DatasetImpl dataset, String postfix) {
        SourceImpl source = new SourceImpl();
        source.setName("source");
        source.setAlias("alias");
        DatasetImpl ds = new DatasetImpl();
        ds.addSource(source);
        source.setParent(ds);
        ds.setParent(dataset.getParent());

        LookupImpl lookup1 = new LookupImpl();
        lookup1.setParent(source);
        lookup1.setLookupDatastore("lookupDataStore");
        source.addLookup(lookup1);

        LookupImpl lookup2 = new LookupImpl();
        lookup2.setParent(source);
        lookup2.setLookupDatastore("lookupDataStore");
        source.addLookup(lookup2);

        return source;
    }

    // Test not only that correct values are set but also sequencing of plugin calls
    private void testUtility(String target, boolean sourceIsTemporary, boolean lookupIsTemporary, boolean isJournalized) {
        when(properties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");
        boolean targetIsTemporary = fixture.isTemporaryTransformation(target);
        TransformationImpl transformation = new TransformationImpl();
        MappingsImpl mappings = new MappingsImpl();
        transformation.setMappings(mappings);
        mappings.setParent(transformation);
        mappings.setTargetDataStore(target);
        TargetcolumnImpl targetcolumn = new TargetcolumnImpl();
        targetcolumn.setName("c1");
        targetcolumn.addMappingExpression("explicit");
        targetcolumn.setParent(mappings);
        mappings.addTargetcolumns(targetcolumn);

        DatasetImpl dataset = new DatasetImpl();
        dataset.setParent(transformation);
        SourceImpl source1 = createSource(dataset, "1");
        SourceImpl source2 = createSource(dataset, "2");
        dataset.addSource(source1);
        dataset.addSource(source2);
        transformation.getDatasets().add(dataset);


        final String transformationName = "transformationName";
        final String folderName = "folderName";
        String model = "model";
        ExecutionLocationType elt = ExecutionLocationType.SOURCE;
        boolean journalizing = true;
        final KnowledgeModuleConfigurationImpl kmc = new KnowledgeModuleConfigurationImpl();
        kmc.setName("km");
        kmc.putOption("option", "value");
        //when(transformationNameContext.getTransformationName(transformation)).thenReturn(transformationName);
        when(transformationNameContext.getTransformationName(transformation)).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TransformationImpl ti = (TransformationImpl) args[0];
                ti.setName(transformationName);
                return transformationName;
            }
        });
        when(transformationNameContext.getTemporaryDSTarget(transformation)).thenReturn("temporaryDataStore");
        for (Source s : dataset.getSources()) {
            if (sourceIsTemporary)
                when(transformationNameContext.getTemporaryDSSource(s)).thenReturn("temporaryDataStore");
            when(modelCodeContext.getModelCode(s)).thenReturn(model);
            when(executionLocationContext.getFilterExecutionLocation(s)).thenReturn(elt);
            when(executionLocationContext.getJoinExecutionLocation(s)).thenReturn(elt);
            when(journalizingContext.isJournalizedSource(s)).thenReturn(journalizing);
            when(knowledgeModuleContext.getLKMConfig(s, "")).thenReturn(kmc);
            when(databaseMetadataService.isTemporaryTransformation(s.getName())).thenReturn(sourceIsTemporary);

            for (Lookup l : s.getLookups()) {
                if (lookupIsTemporary)
                    when(transformationNameContext.getTemporaryDSLookup(l)).thenReturn("temporaryDataStore");
                when(modelCodeContext.getModelCode(l)).thenReturn(model);
                when(executionLocationContext.getLookupExecutionLocation(l)).thenReturn(elt);
                when(journalizingContext.isJournalizedLookup(l)).thenReturn(journalizing);
                when(databaseMetadataService.isTemporaryTransformation(l.getLookupDataStore())).thenReturn(lookupIsTemporary);

            }
        }


        //when(folderNameContext.getFolderName(transformation, isJournalized)).thenReturn("folderName");
        when(folderNameContext.getFolderName(transformation, isJournalized)).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TransformationImpl ti = (TransformationImpl) args[0];
                ti.setFolderName(folderName);
                return transformationName;
            }
        });
        when(knowledgeModuleContext.getCKMConfig(transformation)).thenAnswer(new Answer<KnowledgeModuleConfiguration>() {
            @Override
            public KnowledgeModuleConfiguration answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TransformationImpl ti = (TransformationImpl) args[0];
                ((MappingsImpl) ti.getMappings()).setCkm(generateKMType(kmc));
                return kmc;
            }
        });
        when(knowledgeModuleContext.getIKMConfig(transformation)).thenAnswer(new Answer<KnowledgeModuleConfiguration>() {
            @Override
            public KnowledgeModuleConfiguration answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TransformationImpl ti = (TransformationImpl) args[0];
                ((MappingsImpl) ti.getMappings()).setIkm(generateKMType(kmc));
                return kmc;
            }
        });

        final HashMap<String, List<String>> columnMappings = new HashMap<String, List<String>>();
        columnMappings.put("c1", Arrays.asList("source.c1"));
        columnMappings.put("c2", Arrays.asList("source.c2"));

        HashMap<String, TargetColumnFlags> flagsMap = new HashMap<String, TargetColumnFlags>();
        flagsMap.put("c1", new TargetColumnFlags() {
            @Override
            public Boolean isInsert() {
                return true;
            }

            @Override
            public Boolean isUpdate() {
                return true;
            }

            @Override
            public Boolean isUpdateKey() {
                return true;
            }

            @Override
            public Boolean isMandatory() {
                return true;
            }

            @Override
            public Boolean useExpression() {
                return true;
            }
        });
        flagsMap.put("c2", new TargetColumnFlags() {
            @Override
            public Boolean isInsert() {
                return false;
            }

            @Override
            public Boolean isUpdate() {
                return false;
            }

            @Override
            public Boolean isUpdateKey() {
                return false;
            }

            @Override
            public Boolean isMandatory() {
                return false;
            }

            @Override
            public Boolean useExpression() {
                return true;
            }
        });

        when(flagsContext.getTargetColumnFlags(mappings)).thenReturn(flagsMap);

        when(flagsContext.getTargetColumnFlags(any(Targetcolumn.class))).thenReturn(flagsMap.get("c1"));
        when(flagsContext.getTargetColumnFlags(any(Targetcolumn.class))).thenAnswer(new Answer<TargetColumnFlags>() {
            @Override
            public TargetColumnFlags answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TargetcolumnImpl tc = (TargetcolumnImpl) args[0];
                if ("c1".equals(tc.getName())) {
                    return new TargetColumnFlags() {
                        @Override
                        public Boolean isInsert() {
                            return true;
                        }

                        @Override
                        public Boolean isUpdate() {
                            return true;
                        }

                        @Override
                        public Boolean isUpdateKey() {
                            return true;
                        }

                        @Override
                        public Boolean isMandatory() {
                            return true;
                        }

                        @Override
                        public Boolean useExpression() {
                            return true;
                        }
                    };
                }
                if ("c2".equals(tc.getName())) {
                    return new TargetColumnFlags() {
                        @Override
                        public Boolean isInsert() {
                            return false;
                        }

                        @Override
                        public Boolean isUpdate() {
                            return false;
                        }

                        @Override
                        public Boolean isUpdateKey() {
                            return false;
                        }

                        @Override
                        public Boolean isMandatory() {
                            return false;
                        }

                        @Override
                        public Boolean useExpression() {
                            return true;
                        }
                    };
                } else throw new RuntimeException("bad test config, unknown column");
            }
        });

        when(columnMappingContext.getMappings(transformation)).thenAnswer(new Answer<Map<String, List<String>>>() {
            @Override
            public Map<String, List<String>> answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TransformationImpl ti = (TransformationImpl) args[0];
                TargetcolumnImpl targetColumn = new TargetcolumnImpl();
                targetColumn.setName("c2");
                targetColumn.addMappingExpressions(Arrays.asList("source.c2"));
                targetColumn.setParent(ti.getMappings());
                ((MappingsImpl) ti.getMappings()).addTargetcolumns(targetColumn);
                return columnMappings;
            }
        });

        when(databaseMetadataService.isTemporaryTransformation(mappings.getTargetDataStore())).thenReturn(targetIsTemporary);
        when(databaseMetadataService.isTemporaryTransformation("temporaryDataStore")).thenReturn(targetIsTemporary);

        List<ExecutionLocationType> els = Collections.singletonList(ExecutionLocationType.TARGET);
        when(executionLocationContext.getTargetColumnExecutionLocation(any(Targetcolumn.class))).thenReturn(els);

        fixture.enrich(transformation, isJournalized);


        if (targetIsTemporary) {
            InOrder inOrder = inOrder(modelCodeContext, folderNameContext);
            inOrder.verify(modelCodeContext).getModelCode(transformation.getMappings());
            inOrder.verify(folderNameContext).getFolderName(transformation, isJournalized);

            // inOrder.verify(transformationNameContext).getTemporaryDSTarget(transformation); // is not idempotent

            assertEquals("transformationName", transformation.getName());
        } else {
            assertEquals(transformationName, transformation.getName());
        }

        assertEquals(folderName, transformation.getFolderName());

        if (!isJournalized) {
            verify(journalizingContext, never()).isJournalizedLookup(any(Lookup.class));
            verify(journalizingContext, never()).isJournalizedSource(any(Source.class));
        }

        InOrder inOrder = inOrder(modelCodeContext, transformationNameContext, executionLocationContext, knowledgeModuleContext, journalizingContext);
        for (Dataset ds : transformation.getDatasets()) {
            for (Source s : ds.getSources()) {
                if (sourceIsTemporary) {
                    inOrder.verify(transformationNameContext, atLeastOnce()).getTemporaryDSSource(s);
                }
                inOrder.verify(modelCodeContext).getModelCode(s);

                for (Lookup l : s.getLookups()) {
                    if (lookupIsTemporary) {
                        inOrder.verify(transformationNameContext, atLeastOnce()).getTemporaryDSLookup(l);
                    }
                    inOrder.verify(modelCodeContext).getModelCode(l);
                }
            }
        }

        for (Dataset ds : transformation.getDatasets()) {
            for (Source s : ds.getSources()) {
                inOrder.verify(executionLocationContext).getFilterExecutionLocation(s);
                inOrder.verify(executionLocationContext).getJoinExecutionLocation(s);

                if (isJournalized)
                    inOrder.verify(journalizingContext).isJournalizedSource(s);

                inOrder.verify(knowledgeModuleContext).getLKMConfig(s, "");

                for (Lookup l : s.getLookups()) {
                    if (isJournalized)
                        inOrder.verify(journalizingContext).isJournalizedLookup(l);

                    inOrder.verify(executionLocationContext).getLookupExecutionLocation(l);

                }
            }
        }


        for (Dataset ds : transformation.getDatasets()) {
            for (Source s : ds.getSources()) {
                assertEquals(ExecutionLocationtypeEnum.SOURCE, s.getFilterExecutionLocation());
                assertEquals(ExecutionLocationtypeEnum.SOURCE, s.getJoinExecutionLocation());
                assertEquals(model, s.getModel());
                if (isJournalized)
                    assertEquals(journalizing, s.isJournalized());
                assertEquals(kmc.getName(), s.getLkm().getName());
                assertEquals(1, s.getLkm().getOptions().size());
                assertEquals(kmc.getOptionValue("option"), s.getLkm().getOptions().get("option"));
                //assertEquals(sourceIsTemporary == true ? "temporaryDataStore" : null, s.getTemporaryDataStore());
                for (Lookup l : s.getLookups()) {
                    assertEquals(ExecutionLocationtypeEnum.SOURCE, l.getJoinExecutionLocation());
                    assertEquals(model, l.getModel());
                    if (isJournalized)
                        assertEquals(journalizing, l.isJournalized());
                    //assertEquals(lookupIsTemporary == true ? "temporaryDataStore" : null, l.getTemporaryDataStore());
                }
            }
        }


        assertEquals(kmc.getName(), mappings.getCkm().getName());
        assertEquals(1, mappings.getCkm().getOptions().size());
        assertEquals(kmc.getOptionValue("option"), mappings.getCkm().getOptions().get("option"));

        assertEquals(kmc.getName(), mappings.getIkm().getName());
        assertEquals(1, mappings.getIkm().getOptions().size());
        assertEquals(kmc.getOptionValue("option"), mappings.getIkm().getOptions().get("option"));

        // confirm explicitly defined column hasnt been modified
        assertEquals("explicit", mappings.getTargetColumns().get(0).getMappingExpressions().get(0));
        assertEquals("c1", mappings.getTargetColumns().get(0).getName());

        // confirm sure undefined column was mapped
        assertEquals("source.c2", mappings.getTargetColumns().get(1).getMappingExpressions().get(0));
        assertEquals("c2", mappings.getTargetColumns().get(1).getName());

        // confirm flags set properly on target c1 and c2 columns
        for (Targetcolumn tc : mappings.getTargetColumns()) {
            if ("c1".equals(tc.getName())) {
                assertTrue(tc.isInsert());
                assertTrue(tc.isMandatory());
                assertTrue(tc.isUpdate());
                assertTrue(tc.isUpdateKey());
            } else if ("c2".equals(tc.getName())) {
                assertFalse(tc.isInsert());
                assertFalse(tc.isMandatory());
                assertFalse(tc.isUpdate());
                assertFalse(tc.isUpdateKey());
            }
            assertEquals(ExecutionLocationtypeEnum.TARGET, tc.getExecutionLocations().get(0));
        }

    }

    @Test
    public void testBuilder() {
        testUtility("Movies", false, false, false);
    }

    @Test
    //TODO validate, correct and reactivate
    public void testBuilder_target_temporary() {
        testUtility("S01", false, false, false);
    }

    @Test
    public void testBuilder_source_temporary() {
        testUtility("Movies", true, false, false);
    }

    @Test
    public void testBuilder_lookup_temporary() {
        testUtility("Movies", false, true, false);
    }

    @Test
    public void testBuilder_journalized() {
        testUtility("Movies", false, false, true);
    }


    private Transformation setup_DeleteContext(final int packageSequence, final String targetDataStore, final String model, final String folderName, final String transformationName, final String temporaryTransformationName) {

        Transformation transformation = mock(Transformation.class);
        Mappings mappings = mock(Mappings.class);
        when(mappings.getTargetDataStore()).thenReturn(targetDataStore);
        when(transformation.getMappings()).thenReturn(mappings);
        when(transformation.getPackageSequence()).thenReturn(packageSequence);

        //when(folderNameContext.getFolderName(any(Transformation.class), anyBoolean())).thenReturn(folderName);
        when(folderNameContext.getFolderName(any(Transformation.class), anyBoolean())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TransformationImpl ti = (TransformationImpl) args[0];
                ti.setFolderName(folderName);
                return folderName;
            }
        });

        when(modelCodeContext.getModelCode(any(Mappings.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                MappingsImpl mi = (MappingsImpl) args[0];
                mi.setModel(model);
                return model;
            }
        });
        when(transformationNameContext.getTransformationName(any(Transformation.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TransformationImpl ti = (TransformationImpl) args[0];
                String name = temporaryTransformationName != null ? temporaryTransformationName : transformationName;
                ti.setName(name);
                return name;
            }
        });

        if (temporaryTransformationName == null) {
            when(databaseMetadataService.isTemporaryTransformation(targetDataStore)).thenReturn(false);
        } else {
            when(databaseMetadataService.isTemporaryTransformation(targetDataStore)).thenReturn(true);
        }

        return transformation;
    }

    @Test
    public void testDeleteContext_nontemporary() {
        int packageSequence = 11;
        String targetDataStore = "targetDataStore";
        String model = "model";
        String folderName = "folderName";
        String transformationName = "transformationName";
        when(properties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");
        Transformation transformation = setup_DeleteContext(packageSequence, targetDataStore, model, folderName, transformationName, null);

        DeleteTransformationContext context = fixture.createDeleteContext(transformation, false);
        assertEquals(targetDataStore, context.getDataStoreName());
        assertEquals(transformationName, context.getName());
        assertEquals(model, context.getModel());
        assertEquals(context.getPackageSequence(), packageSequence);
        assertEquals(false, context.isTemporary());
    }

    @Test
    public void testDeleteContext_temporary() {
        int packageSequence = 11;
        String targetDataStore = "_S01";
        String model = "model";
        String folderName = "folderName";
        String transformationName = "transformationName";
        String temporaryTransformationName = "temporaryTransformationName";

        Transformation transformation = setup_DeleteContext(packageSequence, targetDataStore, model, folderName, transformationName, temporaryTransformationName);
        when(properties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");
        DeleteTransformationContext context = fixture.createDeleteContext(transformation, false);
        //assertEquals(temporaryTransformationName, context.getDataStoreName()); // targetDataStore == temporaryTransformationame
        assertEquals(temporaryTransformationName, context.getName());
        assertEquals(model, context.getModel());
        assertEquals(context.getPackageSequence(), packageSequence);
        assertEquals(true, context.isTemporary());
        assertEquals(folderName, context.getFolderName());
    }

    @Test
    public void testTemporaryByName() {
        when(properties.getTemporaryInterfacesRegex()).thenReturn("(_S)[0-9]{2,2}$");
        assertEquals(false, fixture.isTemporaryTransformation("Table"));
        assertEquals(true, fixture.isTemporaryTransformation("_S02"));
    }

    private KmType generateKMType(KnowledgeModuleConfiguration kmc) {

        KmTypeImpl type = new KmTypeImpl();
        type.setName(kmc.getName());
        for (String k : kmc.getOptionKeys()) {
            type.addOption(k, kmc.getOptionValue(k) + "");
        }
        return type;
    }

}

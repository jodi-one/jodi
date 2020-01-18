package one.jodi.odi12.etl.impl;

import one.jodi.InputModelMockHelper;
import one.jodi.etl.internalmodel.Dataset;
import one.jodi.etl.internalmodel.Targetcolumn;
import one.jodi.etl.internalmodel.Transformation;
import one.jodi.odi12.etl.EtlOperators;
import oracle.odi.domain.mapping.MapConnectorPoint;
import oracle.odi.domain.mapping.MapRootContainer;
import oracle.odi.domain.mapping.component.SetComponent;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TestSetBuilderImpl {


    @Test
    public void test() throws Exception {
        SetComponent sc = mock(SetComponent.class);

        SetBuilderImpl setBuilder = new SetBuilderImpl() {
            protected SetComponent createSetComponent(MapRootContainer mapping) {
                return sc;
            }
        };
        EtlOperators etlOperators = mock(EtlOperators.class);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation();

        setBuilder.addSetComponent(transformation, null, true, etlOperators);
        verify(etlOperators, times(1)).addSetComponent(any(SetComponent.class));

        verify(sc, times(transformation.getMappings().getTargetColumns().size()))
                .addSetAttribute(any(MapConnectorPoint.class),
                        any(String.class), any(String[].class));

        for (Targetcolumn tc : transformation.getMappings().getTargetColumns()) {
            verify(sc).addSetAttribute(null, tc.getName(), new String[]{"", ""});
        }

    }


    @Test
    public void test_singleDataset() throws Exception {
        SetComponent sc = mock(SetComponent.class);

        SetBuilderImpl setBuilder = new SetBuilderImpl() {
            protected SetComponent createSetComponent(MapRootContainer mapping) {
                return sc;
            }
        };
        EtlOperators etlOperators = mock(EtlOperators.class);

        Transformation transformation = InputModelMockHelper.createMockETLTransformation();
        List<Dataset> datasets = Arrays.asList(transformation.getDatasets().get(0));
        when(transformation.getDatasets()).thenReturn(datasets);
        setBuilder.addSetComponent(transformation, null, true, etlOperators);
        verify(etlOperators, times(0)).addSetComponent(any(SetComponent.class));

    }
}

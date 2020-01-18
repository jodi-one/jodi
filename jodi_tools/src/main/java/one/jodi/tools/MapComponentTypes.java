package one.jodi.tools;

import oracle.odi.domain.mapping.component.Dataset;

/**
 * Convenience enumeration.  The ODI type strings are distributed through various classes.
 */
public enum MapComponentTypes {

    AggregateComponent(oracle.odi.domain.mapping.component.AggregateComponent.COMPONENT_TYPE_NAME),
    DatasetComponent(Dataset.COMPONENT_TYPE_NAME),
    DatastoreComponent(oracle.odi.domain.mapping.component.DatastoreComponent.COMPONENT_TYPE_NAME),
    DistinctComponent(oracle.odi.domain.mapping.component.DistinctComponent.COMPONENT_TYPE_NAME),
    ExpressionComponent(oracle.odi.domain.mapping.component.ExpressionComponent.COMPONENT_TYPE_NAME),
    FileComponent(oracle.odi.domain.mapping.component.FileComponent.COMPONENT_TYPE_NAME),
    FilterComponent(oracle.odi.domain.mapping.component.FilterComponent.COMPONENT_TYPE_NAME),
    InputSignature(oracle.odi.domain.mapping.component.InputSignature.COMPONENT_TYPE_NAME),
    JoinComponent(oracle.odi.domain.mapping.component.JoinComponent.COMPONENT_TYPE_NAME),
    LookupComponent(oracle.odi.domain.mapping.component.LookupComponent.COMPONENT_TYPE_NAME),
    OutputSignature(oracle.odi.domain.mapping.component.OutputSignature.COMPONENT_TYPE_NAME),
    PivotComponent(oracle.odi.domain.mapping.component.PivotComponent.COMPONENT_TYPE_NAME),
    ReusableMappingComponent(oracle.odi.domain.mapping.component.ReusableMappingComponent.COMPONENT_TYPE_NAME),
    SetComponent(oracle.odi.domain.mapping.component.SetComponent.COMPONENT_TYPE_NAME),
    SorterComponent(oracle.odi.domain.mapping.component.SorterComponent.COMPONENT_TYPE_NAME),
    SplitterComponent(oracle.odi.domain.mapping.component.SplitterComponent.COMPONENT_TYPE_NAME),
    SubqueryFilterComponent(oracle.odi.domain.mapping.component.SubqueryFilterComponent.COMPONENT_TYPE_NAME),
    TableFunctionComponent(oracle.odi.domain.mapping.component.TableFunctionComponent.COMPONENT_TYPE_NAME),
    UnpivotComponent(oracle.odi.domain.mapping.component.UnpivotComponent.COMPONENT_TYPE_NAME);

    String type;

    MapComponentTypes(String type) {
        this.type = type;
    }


}

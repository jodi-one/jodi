package one.jodi.odi12.etl;

import one.jodi.etl.internalmodel.Source;
import one.jodi.odi.interfaces.OdiTransformationAccessStrategy;
import oracle.odi.domain.mapping.IMapComponent;
import oracle.odi.domain.mapping.component.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The class EtlOperators represent all operators in odi used; for instance if a
 * mapping has two sources, the two sources are not in the EtlOperator class,
 * but getJoiners method will return 1 JoinComponent.
 * <p>
 * All get methods return a list for uniformity although they may hold none, one
 * or more than one component.
 * <p>
 * If there is one filter used in a mapping then the getFilterComponents will
 * yield one Map with key the name of the filter component. and as value the
 * actual filter component. The key of the map; the filters name, has naming
 * conventions to extract to which source the filter applies.
 *
 */
public class EtlOperators {

    private final List<JoinComponent> joiners;
    private final List<LookupComponent> lookups;
    private final Map<String, FilterComponent> filterComponents;
    //private final List<AggregateComponent> aggregateComponents;
    private final List<AggregateComponent> aggregateComponents;
    private final Map<Source, List<IMapComponent>> flows;
    private final OdiTransformationAccessStrategy odiAccessStrategy;
    private IMapComponent targetComponents;
    private ExpressionComponent targetExpressions;
    private SetComponent setComponents;
    private DistinctComponent distinctComponents;

    /**
     * Constructor of EtlOperators, normally this is just to construct the
     * object; the final objects are created with empty lists.
     *
     * @param targetComponents targetComponent
     * @param targetExpressions targetExpressions
     * @param joiners joiners
     * @param lookups lookups
     * @param setComponents setc omponents
     * @param filterComponents filter components
     * @param distinctComponents distinct components
     * @param aggregateComponents aggregate components
     */
    public EtlOperators(final IMapComponent targetComponents, final ExpressionComponent targetExpressions,
                        final List<JoinComponent> joiners, final List<LookupComponent> lookups, final SetComponent setComponents,
                        final Map<String, FilterComponent> filterComponents, final DistinctComponent distinctComponents,
                        final List<AggregateComponent> aggregateComponents, final Map<Source, List<IMapComponent>> flows,
                        final OdiTransformationAccessStrategy odiAccessStrategy) {
        this.targetComponents = targetComponents;
        this.targetExpressions = targetExpressions;
        this.joiners = joiners;
        this.lookups = lookups;
        this.setComponents = setComponents;
        this.filterComponents = filterComponents;
        this.distinctComponents = distinctComponents;
        this.aggregateComponents = aggregateComponents;
        this.flows = flows;
        this.odiAccessStrategy = odiAccessStrategy;
    }

    /**
     * Add a targetcomponent of type DataStoreComponent of
     * ReusableMappingComponent
     *
     * @param mapComponent map component
     */
    public void addTargetComponents(final IMapComponent mapComponent) {
        targetComponents = mapComponent;
    }

    /**
     * Add an expression component.
     *
     * @param expressionComponent expression component
     */
    public void addTargetExpressions(final ExpressionComponent expressionComponent) {
        targetExpressions = expressionComponent;
    }

    /**
     * Add a join component; a join component is used to join two source
     * component.
     *
     * @param sequence sequence number
     * @param joinComponent join component
     */
    public void addJoiner(final int sequence, final JoinComponent joinComponent) {
        joiners.add(joinComponent);
    }

    /**
     * Add a lookup component; a lookup component is used to left join ( lookup
     * ) a component, to a source or filter or join component
     *
     * @param lookup
     */
    public void addLookup(final LookupComponent lookup) {
        lookups.add(lookup);
    }

    /**
     * Add a setcomponent used for instance in UNION, UNION ALL, MINUS or
     * INTERSECT operations.
     *
     * @param setComponent set component
     */
    public void addSetComponent(final SetComponent setComponent) {
        setComponents = setComponent;
    }

    /**
     * Add a filter component a filter is added to a map with as sequence the
     * key that is of type string, and refers to a source component by naming
     * conventions.
     *
     * @param sequence sequence number
     * @param filterComponent filter component
     */
    public void addFilterComponent(final String sequence, final FilterComponent filterComponent) {
        filterComponents.put(sequence, filterComponent);
    }

    /**
     * Add a distinct component; at a maximum one distinct component is used per
     * mapping.
     *
     * @param distinctComponent distinct component
     */
    public void addDistinctComponents(final DistinctComponent distinctComponent) {
        distinctComponents = distinctComponent;
    }

    /**
     * Add a aggregate component; at a maximum one aggregate component is used
     * per mapping.
     *
     * @param aggregateComponent aggregate component
     */
    public void addAggregate(final AggregateComponent aggregateComponent) {
        aggregateComponents.add(aggregateComponent);
    }

    /**
     * Get a filtercomponent by name convention.
     *
     * @param filterKeyForSource filter for source
     * @return MapComponent odi map component
     */
    public IMapComponent getFilterComponentsByKey(String filterKeyForSource) {
        return filterComponents.get(filterKeyForSource);
    }

    // getters below
    public List<ExpressionComponent> getTargetExpressions() {
        List<ExpressionComponent> list = new ArrayList<>();
        list.add(targetExpressions);
        return Collections.unmodifiableList(list);
    }

    public List<JoinComponent> getJoiners(int datasetNumber) {
        assert (datasetNumber > 0) : "DatasetNumber starts with 1.";
        List<JoinComponent> joinComponentsPerDataSet = joiners.stream().filter(
                j -> odiAccessStrategy.getDataSetNumberFromComponentName(j) ==
                        datasetNumber).collect(Collectors.toList());
        return Collections.unmodifiableList(joinComponentsPerDataSet);
    }

    public List<LookupComponent> getLookups(int datasetNumber) {
        assert (datasetNumber > 0) : "DatasetNumber starts with 1.";
        List<LookupComponent> lookupsPerDataSet = lookups.stream().filter(
                l -> odiAccessStrategy.getDataSetNumberFromComponentName(l) ==
                        datasetNumber).collect(Collectors.toList());
        return Collections.unmodifiableList(lookupsPerDataSet);
    }

    public List<SetComponent> getSetComponents() {
        List<SetComponent> list = new ArrayList<>();
        if (setComponents != null) {
            list.add(setComponents);
        }
        return Collections.unmodifiableList(list);
    }

    public List<FilterComponent> getFilterComponents(int datasetNumber) {
        assert (datasetNumber > 0) : "DatasetNumber starts with 1.";
        List<FilterComponent> filterComponentsPerDataSet =
                filterComponents.values().stream().filter(
                        f -> odiAccessStrategy.getDataSetNumberFromComponentName(f) ==
                                datasetNumber).collect(Collectors.toList());
        return Collections.unmodifiableList(filterComponentsPerDataSet);
    }

    public List<DistinctComponent> getDistinctComponents() {
        List<DistinctComponent> list = new ArrayList<>();
        if (distinctComponents != null) {
            list.add(distinctComponents);
        }
        return Collections.unmodifiableList(list);
    }

    public List<AggregateComponent> getAggregateComponents(int datasetNumber) {
        assert (datasetNumber > 0) : "DatasetNumber starts with 1.";
        List<AggregateComponent> list = aggregateComponents.stream().filter(
                agg -> odiAccessStrategy.getDataSetNumberFromComponentName(agg) ==
                        datasetNumber).collect(Collectors.toList());
        return list;
    }

    public List<IMapComponent> getTargetComponents() {
        List<IMapComponent> list = new ArrayList<>();
        if (targetComponents != null) {
            list.add(targetComponents);
        }
        return Collections.unmodifiableList(list);
    }


    public List<IMapComponent> getFlows(Source source) {
        List<IMapComponent> list = new ArrayList<>();
        if (flows.get(source) != null) {
            list.addAll(flows.get(source));
        }
        return Collections.unmodifiableList(list);
    }

    public Map<Source, List<IMapComponent>> getFlows() {
        return flows;
    }

    public void addFlowItem(Source source, IMapComponent component) {
        assert (component instanceof PivotComponent || component instanceof UnpivotComponent || component instanceof SubqueryFilterComponent);
        List<IMapComponent> list = flows.get(source);
        if (list == null) {
            list = new ArrayList<>();
            flows.put(source, list);
        }
        list.add(component);
    }

    public List<FilterComponent> getAllFilterComponents() {
        return Collections.unmodifiableList(new ArrayList<>(this.filterComponents.values()));
    }

}
package one.jodi.base.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TableBase extends ModelNode {

    @Override
    SchemaBase getParent();

    String getPhysicalDataServerName();

    String getDataBaseServiceName();

    int getDataBaseServicePort();

    String getSchemaName();

    String getDescription();

    /**
     * contains the additional meta data in the table comment that exist before
     * the meta data separator, e.g. '---'.
     *
     * @return businessName name used by end user
     */
    String getBusinessName();

    /**
     * contains the additional abbreviation in the table comment that exist before
     * the meta data separator and matches the abbreviation pattern, e.g. '((*))$'.
     *
     * @return abbreviatedBusinessName abbreviation of the name used by end user
     */
    String getAbbreviatedBuisnessName();

    Map<String, ? extends ColumnBase> getColumns();

    List<? extends ColumnBase> getOrderedColumns();

    List<? extends ColumnBase> getOrderedColumns(boolean reversed);

    List<? extends KeyBase> getKeys();

    KeyBase getPrimaryKey();

    KeyBase getAlternateKey();

    void remove(FkRelationshipBase fk);

    List<? extends FkRelationshipBase> getFks();

    List<? extends FkRelationshipBase> getFks(List<ColumnBase> columns);

    List<? extends FkRelationshipBase> getFks(ColumnBase namingColumn);

    List<? extends FkRelationshipBase> getFks(TableBase target);

    List<? extends ColumnBase> getFkColumns();

    Set<? extends FkRelationshipBase> getIncomingFks();

    Set<? extends FkRelationshipBase> getIncomingFks(TableBase from);

    boolean nameEndsWith(List<String> postfixes);

    List<? extends ColumnBase> columnNameEndingWith(List<String> postfixes);

    List<? extends ColumnBase> columnNameContains(List<String> postfixes);

    HierarchyBranchBase createBranch(String name, boolean explicitlyDefined);

    void addBranch(HierarchyBranchBase branch);

}

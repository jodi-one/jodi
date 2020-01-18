package one.jodi.core.service.impl;

import one.jodi.etl.service.table.TableDefaultBehaviors;

import java.io.Serializable;
import java.util.Comparator;

public class TableSortComparer implements
        Comparator<TableDefaultBehaviors>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(TableDefaultBehaviors o1, TableDefaultBehaviors o2) {
        String string1 = o1.getModel() + "." + o1.getTableName() + "." + o1.getOlapType() + "." + o2.getDefaultAlias();
        String string2 = o2.getModel() + "." + o2.getTableName() + "." + o2.getOlapType() + "." + o2.getDefaultAlias();
        return string1.compareTo(string2);
    }

}

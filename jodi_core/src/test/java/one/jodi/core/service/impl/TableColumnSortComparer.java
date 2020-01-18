package one.jodi.core.service.impl;

import one.jodi.etl.service.table.ColumnDefaultBehaviors;

import java.io.Serializable;
import java.util.Comparator;

public class TableColumnSortComparer implements
        Comparator<ColumnDefaultBehaviors>, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(ColumnDefaultBehaviors o1, ColumnDefaultBehaviors o2) {
        String s1 = o1.getColumnName() + "." + o1.getScdType();
        String s2 = o2.getColumnName() + "." + o2.getScdType();
        return s2.compareTo(s1);
    }

}

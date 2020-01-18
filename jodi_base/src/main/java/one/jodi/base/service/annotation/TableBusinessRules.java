package one.jodi.base.service.annotation;

import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataStoreDescriptor;

public interface TableBusinessRules {

    String getDescription(DataStoreDescriptor table);

    String getDescription(ColumnMetaData column);

    String getExtendedMetadata(DataStoreDescriptor table);

    String getExtendedMetadata(ColumnMetaData column);

    String getAbbreviatedMetadata(DataStoreDescriptor table);

    String getAbbreviatedMetadata(ColumnMetaData column);

    String getMetadata(DataStoreDescriptor table);

    String getMetadata(ColumnMetaData column);

}

package one.jodi.base.model;

import one.jodi.base.model.types.DataModel;
import one.jodi.base.model.types.DataStore;
import one.jodi.base.model.types.DataStoreColumn;
import one.jodi.base.model.types.DataStoreForeignReference;
import one.jodi.base.model.types.DataStoreKey;
import one.jodi.base.model.types.DataStoreType;
import one.jodi.base.model.types.SCDType;
import one.jodi.base.model.types.impl.DataModelImpl;
import one.jodi.base.model.types.impl.DataStoreForeignReferenceImpl;
import one.jodi.base.service.metadata.ColumnMetaData;
import one.jodi.base.service.metadata.DataModelDescriptor;
import one.jodi.base.service.metadata.DataStoreDescriptor;
import one.jodi.base.service.metadata.ForeignReference;
import one.jodi.base.service.metadata.Key;
import one.jodi.base.service.metadata.SlowlyChangingDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDatastoreHelper {

   public static final String PHYSICAL_SERVER_NAME = "localhost";
   public static final String DB_SERVER = "My Data Server";
   private static final String DB_SERVER_TECH = "Oracle";

   public static DataStore createMockDataStore(final String tableName, final String modelCode) {
      return createMockDataStore(tableName, modelCode, false);
   }

   public static DataStore createMockDataStore(final String tableName, final String modelCode,
                                               final boolean isIgnoredbyHeuristics) {
      return createMockDataStore(tableName, modelCode, isIgnoredbyHeuristics, new String[]{});
   }

   private static DataStoreType getDataStoreType(final String tableName) {
      final DataStoreType dsType;
      if (tableName.toUpperCase()
                   .endsWith("_F")) {
         dsType = DataStoreType.FACT;
      } else if (tableName.toUpperCase()
                          .endsWith("SCD2_D")) {
         dsType = DataStoreType.SLOWLY_CHANGING_DIMENSION;
      } else if (tableName.toUpperCase()
                          .endsWith("_D")) {
         dsType = DataStoreType.DIMENSION;
      } else {
         dsType = DataStoreType.UNKNOWN;
      }
      return dsType;
   }

   public static DataStore createMockDataStore(final String tableName, final String modelCode, final String[] columns) {

      Map<String, Object> emptyFlexfieldMap = Collections.emptyMap();
      final DataModel dataModel =
              new DataModelImpl(modelCode, DB_SERVER, PHYSICAL_SERVER_NAME, DB_SERVER_TECH, "SOMESCHEMA",
                                emptyFlexfieldMap, "UNKNOWN", false, "ORCL", 1521);

      final DataStoreType dsType = getDataStoreType(tableName);

      return new DataStore() {
         @Override
         public String getDataStoreName() {
            return tableName;
         }

         @Override
         public Map<String, DataStoreColumn> getColumns() {
            return createDataStoreColumns(this, columns);
         }

         @Override
         public List<DataStoreKey> getDataStoreKeys() {
            return Collections.emptyList();
         }

         @Override
         public DataStoreKey getPrimaryKey() {
            return null;
         }

         @Override
         public DataStoreKey getAlternateKey() {
            return null;
         }

         @Override
         public boolean isTemporary() {
            return false;
         }

         @Override
         public DataStoreType getDataStoreType() {
            return dsType;
         }

         @Override
         public Map<String, Object> getDataStoreFlexfields() {
            return Collections.emptyMap();
         }

         @Override
         public List<DataStoreForeignReference> getDataStoreForeignReference() {
            return null;
         }

         @Override
         public String getDescription() {
            return "Table Description";
         }

         @Override
         public DataModel getDataModel() {
            return dataModel;
         }

      };
   }

   private static List<DataStoreForeignReference> createFks(final DataStore foreignDataStore,
                                                            final Map<String, DataStore> referenced) {
      final List<DataStoreForeignReference> dsFks;
      if ((referenced == null) || (referenced.isEmpty())) {
         dsFks = Collections.emptyList();
      } else {
         dsFks = new ArrayList<>();
         for (Map.Entry<String, DataStore> entry : referenced.entrySet()) {
            DataStoreForeignReference fk = createMockFK(foreignDataStore, entry.getKey(), entry.getValue());
            dsFks.add(fk);
         }
      }
      return dsFks;
   }


   public static DataStoreForeignReference createMockFK(final DataStore foreign, final String fkColumn,
                                                        final DataStore primary) {

      List<ForeignReference> fkRef = createFKRel(foreign.getDataStoreName() + "_FK", primary.getDataStoreName(),
                                                 primary.getDataModel()
                                                        .getModelCode(), new String[]{fkColumn}, false);

      return new DataStoreForeignReferenceImpl(fkRef.get(0), foreign, primary);

   }

   public static DataStore createMockDataStore(final String tableName, final String modelCode, final String[] columns,
                                               final int keyCol, final Map<String, DataStore> referenced) {
      assert (keyCol <= columns.length);
      final String[] keyColumns = Arrays.copyOf(columns, keyCol);

      DataStoreKey primary = null;
      final DataStoreType dsType = getDataStoreType(tableName);

      final List<DataStoreKey> keys = new ArrayList<>();
      if ((keyColumns != null) && (keyColumns.length > 0) && keyColumns[0].length() > 0) {

         final DataStoreKey.KeyType type;
         if (keyColumns[0].toLowerCase()
                          .equals("row_wid")) {
            type = DataStoreKey.KeyType.PRIMARY;
         } else if (keyColumns[0].toLowerCase()
                                 .startsWith("p_")) {
            type = DataStoreKey.KeyType.PRIMARY;
         } else if (keyColumns[0].toLowerCase()
                                 .startsWith("a_")) {
            type = DataStoreKey.KeyType.ALTERNATE;
         } else {
            type = DataStoreKey.KeyType.INDEX;
         }

         keys.add(new DataStoreKey() {
            @Override
            public String getName() {
               return keyColumns[0].substring(0, 1)
                                   .toUpperCase() + "_key";
            }

            @Override
            public KeyType getType() {
               return type;
            }

            @Override
            public List<String> getColumns() {
               return Arrays.asList(keyColumns);
            }

            @Override
            public boolean existsInDatabase() {
               return true;
            }

            @Override
            public boolean isEnabledInDatabase() {
               return true;
            }
         });

         if (type == DataStoreKey.KeyType.PRIMARY) {
            primary = keys.get(0);
         }
      }

      // add alternative key defined through columns that end on "_U1"
      final List<String> altKeyColumns = new ArrayList<>();
      for (String column : columns) {
         if (column.toUpperCase()
                   .endsWith("_U1")) {
            altKeyColumns.add(column);
         }
      }
      if (!altKeyColumns.isEmpty()) {
         keys.add(new DataStoreKey() {
            @Override
            public String getName() {
               return tableName.toUpperCase() + "_U1";
            }

            @Override
            public KeyType getType() {
               return DataStoreKey.KeyType.ALTERNATE;
            }

            @Override
            public List<String> getColumns() {
               return altKeyColumns;
            }

            @Override
            public boolean existsInDatabase() {
               return true;
            }

            @Override
            public boolean isEnabledInDatabase() {
               return true;
            }
         });
      }

      final DataStoreKey primaryKey = primary;
      Map<String, Object> emptyFlexfieldMap = Collections.emptyMap();

      final DataModel dataModel =
              new DataModelImpl(modelCode, DB_SERVER, PHYSICAL_SERVER_NAME, DB_SERVER_TECH, modelCode,
                                emptyFlexfieldMap, "UNKNOWN", false, "ORCL", 1521);

      return new DataStore() {
         @Override
         public String getDataStoreName() {
            return tableName;
         }

         @Override
         public Map<String, DataStoreColumn> getColumns() {
            return createDataStoreColumns(this, columns);
         }

         @Override
         public List<DataStoreKey> getDataStoreKeys() {
            return keys;
         }

         @Override
         public DataStoreKey getPrimaryKey() {
            return primaryKey;
         }

         @Override
         public DataStoreKey getAlternateKey() {
            return null;
         }

         @Override
         public boolean isTemporary() {
            return false;
         }

         @Override
         public DataStoreType getDataStoreType() {
            return dsType;
         }

         @Override
         public Map<String, Object> getDataStoreFlexfields() {
            return Collections.emptyMap();
         }

         @Override
         public List<DataStoreForeignReference> getDataStoreForeignReference() {
            // create FK relationship
            List<DataStoreForeignReference> dsFks = createFks(this, referenced);
            return dsFks;
         }

         @Override
         public String getDescription() {
            return "Table Description";
         }

         @Override
         public DataModel getDataModel() {
            return dataModel;
         }
      };
   }

   public static DataStore createMockDataStore(final String tableName, final String modelCode,
                                               final boolean isIgnoredbyHeuristics, final String[] keyColumns) {

      DataStoreKey primary = null;
      final List<DataStoreKey> keys = new ArrayList<>();
      if ((keyColumns != null) && (keyColumns.length > 0) && keyColumns[0].length() > 0) {

         final DataStoreKey.KeyType type;
         if (keyColumns[0].toLowerCase()
                          .startsWith("p")) {
            type = DataStoreKey.KeyType.PRIMARY;
         } else if (keyColumns[0].toLowerCase()
                                 .startsWith("a")) {
            type = DataStoreKey.KeyType.ALTERNATE;
         } else if (keyColumns[0].toLowerCase()
                                 .startsWith("u")) {
            type = DataStoreKey.KeyType.ALTERNATE;
         } else {
            type = DataStoreKey.KeyType.INDEX;
         }

         keys.add(new DataStoreKey() {
            @Override
            public String getName() {

               return keyColumns[0].substring(0, 1)
                                   .toUpperCase() + ((keyColumns[0].toLowerCase()
                                                                   .startsWith("u")) ? "_U1" : "_key");
            }

            @Override
            public KeyType getType() {
               return type;
            }

            @Override
            public List<String> getColumns() {
               return Arrays.asList(keyColumns);
            }

            @Override
            public boolean existsInDatabase() {
               return true;
            }

            @Override
            public boolean isEnabledInDatabase() {
               return true;
            }
         });

         if (type == DataStoreKey.KeyType.PRIMARY) {
            primary = keys.get(0);
         }
      }
      final DataStoreKey primaryKey = primary;

      Map<String, Object> emptyFlexfieldMap = Collections.emptyMap();
      final DataModel dataModel =
              new DataModelImpl(modelCode, DB_SERVER, PHYSICAL_SERVER_NAME, DB_SERVER_TECH, "SOMESCHEMA",
                                emptyFlexfieldMap, "UNKNOWN", isIgnoredbyHeuristics, "ORCL", 1521);

      return new DataStore() {
         @Override
         public String getDataStoreName() {
            return tableName;
         }

         @Override
         public Map<String, DataStoreColumn> getColumns() {
            return Collections.emptyMap();
         }

         @Override
         public List<DataStoreKey> getDataStoreKeys() {
            return keys;
         }

         @Override
         public DataStoreKey getPrimaryKey() {
            return primaryKey;
         }

         @Override
         public DataStoreKey getAlternateKey() {
            return null;
         }

         @Override
         public boolean isTemporary() {
            return false;
         }

         @Override
         public DataStoreType getDataStoreType() {
            return DataStoreType.UNKNOWN;
         }

         @Override
         public Map<String, Object> getDataStoreFlexfields() {
            return Collections.emptyMap();
         }

         @Override
         public List<DataStoreForeignReference> getDataStoreForeignReference() {
            return null;
         }

         @Override
         public String getDescription() {
            return "Table Description";
         }

         @Override
         public DataModel getDataModel() {
            return dataModel;
         }
      };
   }

   public static Map<String, DataStoreColumn> createDataStoreColumns(final DataStore parent,
                                                                     final String[] columnNames) {

      Map<String, DataStoreColumn> columns = new HashMap<>();
      int position = 1;
      for (final String columnName : columnNames) {
         final int pos = position++;
         columns.put(columnName, new DataStoreColumn() {
            @Override
            public DataStore getParent() {
               return parent;
            }

            @Override
            public String getName() {
               return columnName;
            }

            @Override
            public int getLength() {
               return 10;
            }

            @Override
            public int getScale() {
               return 1;
            }

            @Override
            public String getColumnDataType() {
               return "VARCHAR";
            }

            @Override
            public SCDType getColumnSCDType() {
               return SCDType.ADD_ROW_ON_CHANGE;
            }

            @Override
            public boolean hasNotNullConstraint() {
               return false;
            }

            @Override
            public String getDescription() {
               return "Column Description";
            }

            @Override
            public int getPosition() {
               return pos;
            }
         });
      }
      return columns;
   }

   private static List<Key> createKey(final String[] keyColumns) {
      final List<Key> keys = new ArrayList<>();
      if ((keyColumns != null) && (keyColumns.length > 0) && keyColumns[0].length() > 0) {

         final Key.KeyType type;
         if (keyColumns[0].toLowerCase()
                          .startsWith("p")) {
            type = Key.KeyType.PRIMARY;
         } else if (keyColumns[0].toLowerCase()
                                 .startsWith("a")) {
            type = Key.KeyType.ALTERNATE;
         } else {
            type = Key.KeyType.INDEX;
         }

         keys.add(new Key() {
            @Override
            public String getName() {
               return keyColumns[0].substring(0, 1)
                                   .toUpperCase() + "_key";
            }

            @Override
            public KeyType getType() {
               return type;
            }

            @Override
            public List<String> getColumns() {
               return Arrays.asList(keyColumns);
            }

            @Override
            public boolean existsInDatabase() {
               return true;
            }

            @Override
            public boolean isEnabledInDatabase() {
               return true;
            }

            @Override
            public String getDataStoreName() {
               return "TEST";
            }

            @Override
            public void setDataStoreName(String datastoreName) {
               // TODO Auto-generated method stub

            }
         });
      }
      return keys;
   }

   public static List<ForeignReference> createFKRel(final String relationshipKeyName,
                                                    final String primaryKeyDataStoreName,
                                                    final String primaryKeyModelCode, final String[] keyColumns,
                                                    final boolean postfix) {

      final List<ForeignReference> fkRefs = new ArrayList<>();

      final List<ForeignReference.RefColumns> refColumns = new ArrayList<>();
      for (int i = 0; i < keyColumns.length; i++) {
         final String column = keyColumns[i];
         final String ending = (i == 0) ? "" : "_" + i;
         refColumns.add(new ForeignReference.RefColumns() {
            @Override
            public String getForeignKeyColumnName() {
               return column;
            }

            @Override
            public String getPrimaryKeyColumnName() {
               return "P_PK" + ending;
            }
         });
      }

      fkRefs.add(new ForeignReference() {
         @Override
         public String getName() {
            return relationshipKeyName;
         }

         @Override
         public String getPrimaryKeyDataStoreName() {
            return primaryKeyDataStoreName;
         }

         @Override
         public String getPrimaryKeyDataStoreModelCode() {
            return primaryKeyModelCode;
         }

         @Override
         public List<RefColumns> getReferenceColumns() {
            return refColumns;
         }

         @Override
         public boolean isEnabledInDatabase() {
            return true;
         }
      });

      return fkRefs;

   }

   public static Map<String, DataStoreDescriptor> createMockDSDescriptor(final String dataStoreName,
                                                                         final String modelCode,
                                                                         final boolean isTemporaryTable) {
      return createMockDSDescriptor(dataStoreName, modelCode, isTemporaryTable, new String[]{}, new String[]{}, null,
                                    null, new String[]{});
   }

   public static Map<String, DataStoreDescriptor> createMockDSDescriptor(final String dataStoreName,
                                                                         final String modelCode,
                                                                         final boolean isTemporaryTable,
                                                                         final String[] columns) {
      return createMockDSDescriptor(dataStoreName, modelCode, isTemporaryTable, columns, new String[]{}, null, null,
                                    new String[]{});
   }

   public static Map<String, DataStoreDescriptor> createMockDSDescriptor(final String dataStoreName,
                                                                         final String modelCode,
                                                                         final boolean isTemporaryTable,
                                                                         final String[] columns,
                                                                         final String[] keyColumns) {
      return createMockDSDescriptor(dataStoreName, modelCode, isTemporaryTable, columns, keyColumns, null, null,
                                    new String[]{});
   }

   public static Map<String, DataStoreDescriptor> createMockDSDescriptor(final String dataStoreName,
                                                                         final String modelCode,
                                                                         final boolean isTemporaryTable,
                                                                         final String[] columns,
                                                                         final String[] keyColumns,
                                                                         final String primaryKeyDataStoreName,
                                                                         final String primaryKeyModelCode,
                                                                         final String[] refKeyColumns) {

      final String relationshipKeyName = dataStoreName + "_" + primaryKeyDataStoreName + "_FK";

      Map<String, DataStoreDescriptor> dsDescMap = new HashMap<>();
      dsDescMap.put(dataStoreName, new DataStoreDescriptor() {
         @Override
         public String getDataStoreName() {
            return dataStoreName;
         }

         @Override
         public boolean isTemporary() {
            return isTemporaryTable;
         }

         @Override
         public Map<String, Object> getDataStoreFlexfields() {
            return Collections.emptyMap();
         }

         @Override
         public Collection<ColumnMetaData> getColumnMetaData() {
            return createColumnMetaData(columns).values();
         }

         @Override
         public DataModelDescriptor getDataModelDescriptor() {
            return createMockDataModelDescriptor(modelCode);
         }

         @Override
         public List<Key> getKeys() {
            return createKey(keyColumns);
         }

         @Override
         public List<ForeignReference> getFKRelationships() {
            return createFKRel(relationshipKeyName, primaryKeyDataStoreName, primaryKeyModelCode, refKeyColumns, true);
         }

         @Override
         public String getDescription() {
            return "DataStoreDescription";
         }
      });
      return dsDescMap;
   }

   public static DataModelDescriptor createMockDataModelDescriptor(final String modelCode) {

      return new DataModelDescriptor() {
         @Override
         public String getModelCode() {
            return modelCode;
         }

         @Override
         public Map<String, Object> getModelFlexfields() {
            return Collections.emptyMap();
         }

         @Override
         public String getPhysicalDataServerName() {
            return PHYSICAL_SERVER_NAME;
         }

         @Override
         public String getDataServerName() {
            return DB_SERVER;
         }

         @Override
         public String getDataServerTechnology() {
            return DB_SERVER_TECH;
         }

         @Override
         public String getSchemaName() {
            return "SOMESCHEMA";
         }

         @Override
         public String getDataBaseServiceName() {
            return "ORCL";
         }

         @Override
         public int getDataBaseServicePort() {
            return 1521;
         }
      };
   }

   public static Map<String, ColumnMetaData> createColumnMetaData(String[] names) {
      Map<String, ColumnMetaData> columns = new HashMap<>();

      int position = 1;
      for (final String name : names) {
         final int pos = position++;
         columns.put(name, new ColumnMetaData() {
            @Override
            public String getColumnDataType() {
               return name + "_DataType";
            }

            @Override
            public int getLength() {
               return 10;
            }

            @Override
            public int getScale() {
               return 1;
            }

            @Override
            public SlowlyChangingDataType getColumnSCDType() {
               return SlowlyChangingDataType.ADD_ROW_ON_CHANGE;
            }

            @Override
            public boolean hasNotNullConstraint() {
               int count;
               try {
                  count = Integer.parseInt(name.substring(name.length() - 1));
               } catch (RuntimeException e) {
                  //last character is not a number - use default value
                  count = 1;
               }
               return (count % 2) == 0 ? true : false;
            }

            @Override
            public Map<String, Object> getFlexFieldValues() {
               return Collections.emptyMap();
            }

            @Override
            public String getName() {
               return name;
            }

            @Override
            public String getDescription() {
               return "Column description";
            }

            @Override
            public int getPosition() {
               return pos;
            }

            @Override
            public String getDataStoreName() {
               return "Emp";
            }
         });
      }
      return columns;
   }

}

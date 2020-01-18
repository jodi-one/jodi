package one.jodi.base.model.types;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseDataTypeService {

    private Map<String, String> dbToBiTypeMap;

    public BaseDataTypeService() {
        super();
    }

    protected void instantiate(final Map<String, String> dbToBiTypeMap) {
        assert (dbToBiTypeMap != null);
        // capitalize keys of map and values of set
        this.dbToBiTypeMap = dbToBiTypeMap.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().toUpperCase(),
                        Map.Entry::getValue));
    }

    public String getMappedType(final String databaseType) {
        return this.dbToBiTypeMap.get(databaseType.toUpperCase());
    }

    ;

}

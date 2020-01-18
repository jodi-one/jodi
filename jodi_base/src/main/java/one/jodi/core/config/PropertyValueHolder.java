package one.jodi.core.config;

import java.util.List;
import java.util.Map;

public interface PropertyValueHolder {
    abstract String getString();

    abstract String getKey();

    abstract List<String> getList();

    abstract Map<String, String> getMap();
}

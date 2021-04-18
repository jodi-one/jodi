package one.jodi.core.config;

import java.util.List;
import java.util.Map;

public interface PropertyValueHolder {
    String getString();

    String getKey();

    List<String> getList();

    Map<String, String> getMap();

    String getListAsString();
}

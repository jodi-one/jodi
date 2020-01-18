package one.jodi.tools.dependency;

import java.util.ArrayList;
import java.util.List;

public class MappingHolder {
    private final MappingType mappingType;
    private final String mapping;
    private final ArrayList<String> sources;
    private final String target;


    public MappingHolder(String mapping, List<String> sources, String target, MappingType mappingType) {
        this.mapping = mapping;
        this.sources = new ArrayList<String>(sources);
        this.target = target;
        this.mappingType = mappingType;
    }

    public ArrayList<String> getSources() {
        return sources;
    }

    public String getTarget() {
        return target;
    }

    public String getMapping() {
        return mapping;
    }

    public MappingType getType() {
        return mappingType;
    }
}

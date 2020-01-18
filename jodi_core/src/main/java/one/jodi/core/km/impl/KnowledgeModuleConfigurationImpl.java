package one.jodi.core.km.impl;

import one.jodi.core.extensions.types.KnowledgeModuleConfiguration;
import one.jodi.etl.km.KnowledgeModuleType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/*
 * Holder of information associated with the selection of a Knowldge Module along with associated properties.
 *
 */
public class KnowledgeModuleConfigurationImpl implements KnowledgeModuleConfiguration {

    KnowledgeModuleType type;
    String name;
    Map<String, Object> options = new HashMap<>();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public KnowledgeModuleType getType() {
        return type;
    }

    public void setType(KnowledgeModuleType type) {
        this.type = type;
    }

    @Override
    public Set<String> getOptionKeys() {
        return options.keySet();
    }

    @Override
    public Object getOptionValue(String option) {
        return options.get(option);
    }

    public Object putOption(String option, Object value) {
        return options.put(option, value);
    }

}
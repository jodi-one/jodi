package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.KmType;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of KmType interface.
 *
 */
public class KmTypeImpl implements KmType {

    String name;

    Map<String, String> options;


    public KmTypeImpl(String name) {
        this.name = name;
    }

    public KmTypeImpl() {
        name = "";
        options = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void addOption(String option, String value) {
        options.put(option, value);
    }

    public void clearOptions() {
        options.clear();
    }

}

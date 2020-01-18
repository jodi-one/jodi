package one.jodi.etl.internalmodel.impl;

import one.jodi.etl.internalmodel.OutputAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

public class OutputAttributeImpl implements OutputAttribute {

    String name;

    LinkedHashMap<String, String> expressions = new LinkedHashMap<>();

    public OutputAttributeImpl() {

    }

    public OutputAttributeImpl(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, String> getExpressions() {
        return expressions;
    }

    @Override
    public boolean hasQualifiedExpressions() {
        if (expressions.size() == 0)
            return false;
        else if (expressions.size() == 1) {
            return !expressions.keySet().contains(null);
        } else {
            return true;
        }
    }

}

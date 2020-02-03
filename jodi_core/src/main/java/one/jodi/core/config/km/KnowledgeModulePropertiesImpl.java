package one.jodi.core.config.km;

import java.util.List;
import java.util.Map;

/**
 * This class represents a the implementation particular rule as defined in the Jodi properties file(s).
 * <p>
 * A rule is composed of criteria used for matching along with KM name and options to apply if
 * the rule fires.  However, if the rule is global, only options are applied and the name(s) is
 * used as a criterion.
 * <p>
 * The KnowledgeModuleProperties interface is a bean interface with accessors that mirror
 * the configuration file.  Jodi attempts to create lists and maps out of fields that are accordingly
 * defined.
 * <p>
 * KM rules have the following format:
 * km.<Rule ID>.<name> = <value>
 * <p>
 * Critical note: the km.<groupname field is used as a reference for the input model XML
 */
public class KnowledgeModulePropertiesImpl implements KnowledgeModuleProperties {

    private String id;
    private Integer order;
    private boolean global;
    private List<String> name;
    private Map<String, Object> options;
    private String trg_technology;
    private String src_technology;
    private boolean isDefault;
    private Integer trg_temporary;
    private String trg_regex;
    private List<String> trg_layer;
    private List<String> trg_datastoretype;
    private String src_regex;
    private List<String> src_layer;
    private List<String> src_datastoretype;

    @Override
    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Integer getOrder() {
        return order;
    }


    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public boolean isGlobal() {
        return global;
    }


    public void setGlobal(boolean global) {
        this.global = global;
    }

    @Override
    public List<String> getName() {
        return name;
    }


    public void setName(List<String> name) {
        this.name = name;
    }

    @Override
    public Map<String, Object> getOptions() {
        return options;
    }


    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    @Override
    public String getTrg_technology() {
        return trg_technology;
    }


    public void setTrg_technology(String trg_technology) {
        this.trg_technology = trg_technology;
    }

    @Override
    public String getSrc_technology() {
        return src_technology;
    }


    public void setSrc_technology(String src_technology) {
        this.src_technology = src_technology;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }


    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public Integer getTrg_temporary() {
        return trg_temporary;
    }


    public void setTrg_temporary(Integer trg_temporary) {
        this.trg_temporary = trg_temporary;
    }

    @Override
    public String getTrg_regex() {
        return trg_regex;
    }


    public void setTrg_regex(String trg_regex) {
        this.trg_regex = trg_regex;
    }

    @Override
    public List<String> getTrg_layer() {
        return trg_layer;
    }


    public void setTrg_layer(List<String> trg_layer) {
        this.trg_layer = trg_layer;
    }

    @Override
    public List<String> getTrg_tabletype() {
        return trg_datastoretype;
    }

    public void setTrg_tabletype(List<String> trg_datastoretype) {
        this.trg_datastoretype = trg_datastoretype;
    }

    @Override
    public String getSrc_regex() {
        return src_regex;
    }


    public void setSrc_regex(String src_regex) {
        this.src_regex = src_regex;
    }

    @Override
    public List<String> getSrc_layer() {
        return src_layer;
    }


    public void setSrc_layer(List<String> src_layer) {
        this.src_layer = src_layer;
    }

    @Override
    public List<String> getSrc_tabletype() {
        return src_datastoretype;
    }

    public void setSrc_tabletype(List<String> src_datastoretype) {
        this.src_datastoretype = src_datastoretype;
    }


    @Override
    public int compareOrderTo(KnowledgeModuleProperties other) {
        return order - other.getOrder();
    }

}

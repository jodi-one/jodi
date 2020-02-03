package one.jodi.core.config.km;

import one.jodi.base.model.types.DataStoreType;

import java.util.List;
import java.util.Map;

/**
 * This class represents a particular rule as defined in the Jodi properties file(s).
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

public interface KnowledgeModuleProperties {

    /**
     * Get the ID of the rule, defined here to be the group in the configuration based KM approach
     *
     * @return id/group
     */
    public String getId();

    /**
     * Get the order of the rule relative to the rule chain.  Within a chain the order should be unique.
     * Order is used both to determine KM name (when not explicitly defined) and to apply and over-ride
     * options.  Higher order rules will over-ride lower order rules apply the same option.
     *
     * @return order
     */
    public Integer getOrder();

    /**
     * Determines if the rule is global, e.g. isnt part of the KM selection process but participates
     * in the setting of KM options.
     *
     * @return true if the rule is global
     */
    public boolean isGlobal();

    /**
     * When not global, determines the KM name to be set if the rule fires.
     * When global the name is used as criteria to determine if a rule should fire
     *
     * @return name(s)
     */
    public List<String> getName();

    /**
     * List of options to apply to KM when rule fires.
     *
     * @return options
     */
    public Map<String, Object> getOptions();

    /**
     * The trg_technology field is used as a criteria to determine if the rule will fire when compared against input.
     *
     * @return target technology
     */
    public String getTrg_technology();

    /**
     * The source_technology field is used as a criteria to determine if the rule will fire when compared against input.
     *
     * @return source technology
     */
    public String getSrc_technology();

    /**
     * Rule parameter to evaluate which criteria to use to determine rule firing.  When default is true,
     * only target_technology (and in the case of LKM) source_technology is used to determine if a rule
     * should fire.
     *
     * @return rule should be evaluated as default
     */
    public boolean isDefault();

    /**
     * Criteria used for determining if the rule should fire given a table's temporary nature.
     * If trg_temporary is -1 the rule only applies to non-temporary tables
     * If trg_tempoprary is 0 the rule applies to any tables (default)
     * If trg_temporary is 1, the rule applies to only temporary tables.
     *
     * @return trg_temporary
     */
    public Integer getTrg_temporary();

    /**
     * Regular expression used is criteria to determine of the table name matches.
     *
     * @return trg_regex
     */
    public String getTrg_regex();

    /**
     * Criteria used to determine if the rule should be used when the target table is
     * of given type.  Values must match
     * {@link  DataStoreType}
     *
     * @return Criteria used to determine if the rule should be used when the target table is of given type.
     */
    public List<String> getTrg_tabletype();

    /**
     * Criteria object.  When specified, the rule may only apply when the target is
     * contained in the trg_layer list.
     *
     * @return trg_layer
     */
    public List<String> getTrg_layer();

    /**
     * Regular expression used is criteria to determine of the table name matches.
     *
     * @return trg-regex
     */
    public String getSrc_regex();

    /**
     * Criteria object.  When specified, the rule may only apply when the target is
     * contained in the trg_layer list.
     *
     * @return trg_layer
     */
    public List<String> getSrc_layer();

    /**
     * Criteria used to determine if the rule should be used on when the source table is
     * of given type. Values must match
     * {@link  DataStoreType}
     *
     * @return Criteria used to determine if the rule should be used on when the source table is of given type
     */
    public List<String> getSrc_tabletype();


    public int compareOrderTo(KnowledgeModuleProperties other);
}

package one.jodi.core.config.modelproperties;

import java.util.List;

public class ModelPropertiesImpl implements ModelProperties {

    private String modelID;
    private String code;
    private boolean isDefault;
    private boolean ignoredByHeuristics;
    private int order;
    private String layer;
    private List<String> prefix;
    private List<String> postfix;
    private boolean journalized;
    private List<String> jkmoptions;
    private String jkm; // Journalizing Knowledge Module Name

    private List<String> subscribers;

    /* (non-Javadoc)
     * @see one.jodi.core.datastore.impl.ModelProperties2#getModelID()
     */
    @Override
    public String getModelID() {
        return modelID;
    }

    public void setModelID(final String modelID) {
        this.modelID = modelID;
    }

    /* (non-Javadoc)
     * @see one.jodi.core.datastore.impl.ModelProperties2#getName()
     */
    @Override
    public String getCode() {
        return code;
    }

    public void setCode(final String name) {
        this.code = name;
    }

    /* (non-Javadoc)
     * @see one.jodi.core.datastore.impl.ModelProperties2#isDefault()
     */
    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public boolean isIgnoredByHeuristics() {
        return ignoredByHeuristics;
    }

    public void setIgnoredbyheuristics(final boolean ignoredByHeuristics) {
        this.ignoredByHeuristics = ignoredByHeuristics;
    }

    /* (non-Javadoc)
     * @see one.jodi.core.datastore.impl.ModelProperties2#getOrder()
     */
    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    /* (non-Javadoc)
     * @see one.jodi.core.datastore.impl.ModelProperties2#getLayer()
     */
    @Override
    public String getLayer() {
        return layer;
    }

    public void setLayer(final String layer) {
        this.layer = layer;
    }

    /* (non-Javadoc)
     * @see one.jodi.core.datastore.impl.ModelProperties2#getPrefix()
     */
    @Override
    public List<String> getPrefix() {
        return prefix;
    }

    public void setPrefix(final List<String> prefix) {
        this.prefix = prefix;
    }

    /* (non-Javadoc)
     * @see one.jodi.core.datastore.impl.ModelProperties2#getPostfix()
     */
    @Override
    public List<String> getPostfix() {
        return postfix;
    }

    public void setPostfix(final List<String> postfix) {
        this.postfix = postfix;
    }

    @Override
    public int compareOrderTo(final ModelProperties otherModelProperties) {
        return order - otherModelProperties.getOrder();
    }

    public boolean isJournalized() {
        return journalized;
    }

    public void setJournalized(final boolean journalized) {
        this.journalized = journalized;
    }

    @Override
    public List<String> getJkmoptions() {
        return jkmoptions;
    }

    public void setJkmoptions(final List<String> jkmOptions) {
        this.jkmoptions = jkmOptions;
    }

    @Override
    public List<String> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<String> subscribers) {
        this.subscribers = subscribers;
    }

    @Override
    public String getJkm() {
        return jkm;
    }

    public void setJkm(String jkm) {
        this.jkm = jkm;
    }

}
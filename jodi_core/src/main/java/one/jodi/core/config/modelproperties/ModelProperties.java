package one.jodi.core.config.modelproperties;

import java.util.List;

public interface ModelProperties {

    List<String> getJkmoptions();

    String getModelID();

    String getCode();

    boolean isDefault();

    @Deprecated
    boolean isIgnoredByHeuristics();

    int getOrder();

    String getLayer();

    List<String> getPrefix();

    List<String> getPostfix();

    int compareOrderTo(ModelProperties otherModelProperties);

    boolean isJournalized();

    void setJournalized(boolean journalized);

    List<String> getSubscribers();

    String getJkm();
}
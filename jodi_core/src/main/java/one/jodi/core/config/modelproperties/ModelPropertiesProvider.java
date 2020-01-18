package one.jodi.core.config.modelproperties;

import java.util.List;

public interface ModelPropertiesProvider {

    /**
     * @return models in ascending order
     */
    List<ModelProperties> getConfiguredModels();


    /**
     * @param layers name of a layers referring to one or multiple models
     * @return models in ascending order that are part of layer with name of
     * input parameter; will return an empty list if the layer is not
     * identified in the configuration
     */
    List<ModelProperties> getConfiguredModels(List<String> layers);

}

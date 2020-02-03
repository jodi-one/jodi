package one.jodi.base.model.types;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Defines the solution layer to which a model belongs
 */
public enum ModelSolutionLayerType implements ModelSolutionLayer {
    UNKNOWN, SOURCE, EDW_SDS, EDW_SIS, EDW, STAR_SDS, STAR_SIS, STAR;

    private static ConcurrentMap<String, ModelSolutionLayer> layers =
            new ConcurrentHashMap<>();

    /**
     * Gets the ModelSolutionLayer constant associated with the specified name.
     * If the name parameter is null ModelSolutionLayerType.UNKNOWN is returned.
     *
     * @param layerName
     * @return the ModelSolutionLayer constant associated with the specified
     * name
     */
    public static ModelSolutionLayer modelSolutionLayerFor(
            final String layerName) {
        ModelSolutionLayer result = null;
        if (layerName != null) {
            if (layers.isEmpty()) {
                for (ModelSolutionLayerType layer : values()) {
                    String name = layer.name();
                    layers.putIfAbsent(name, layer);
                }
                layers.putIfAbsent("DM", STAR);
            }
            final String uppercaseName = layerName.toUpperCase();
            result = layers.get(uppercaseName);

            if (result == null) {
                result = new ModelSolutionLayer() {
                    public String getSolutionLayerName() {
                        return layerName;
                    }

                    public String toString() {
                        return getSolutionLayerName();
                    }
                };

                layers.putIfAbsent(uppercaseName, result);
            }
        } else {
            result = UNKNOWN;
        }
        return result;
    }

    public String getSolutionLayerName() {
        return name().toLowerCase();
    }
}

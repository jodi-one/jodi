package one.jodi.base.model.types;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Defines the solution layer to which a model belongs
 */
public enum ModelSolutionLayerType implements ModelSolutionLayer {
    UNKNOWN, SOURCE, EDW_SDS, EDW_SIS, EDW, STAR_SDS, STAR_SIS, STAR;

    private static final ConcurrentMap<String, ModelSolutionLayer> LAYERS = new ConcurrentHashMap<>();

    /**
     * Gets the ModelSolutionLayer constant associated with the specified name.
     * If the name parameter is null ModelSolutionLayerType.UNKNOWN is returned.
     *
     * @param layerName
     * @return the ModelSolutionLayer constant associated with the specified
     * name
     */
    public static ModelSolutionLayer modelSolutionLayerFor(final String layerName) {
        ModelSolutionLayer result;
        if (layerName != null) {
            if (LAYERS.isEmpty()) {
                for (ModelSolutionLayerType layer : values()) {
                    String name = layer.name();
                    LAYERS.putIfAbsent(name, layer);
                }
                LAYERS.putIfAbsent("DM", STAR);
            }
            final String uppercaseName = layerName.toUpperCase();
            result = LAYERS.get(uppercaseName);

            if (result == null) {
                result = new ModelSolutionLayer() {
                    @Override
                    public String getSolutionLayerName() {
                        return layerName;
                    }

                    @Override
                    public String toString() {
                        return getSolutionLayerName();
                    }
                };

                LAYERS.putIfAbsent(uppercaseName, result);
            }
        } else {
            result = UNKNOWN;
        }
        return result;
    }

    @Override
    public String getSolutionLayerName() {
        return name().toLowerCase();
    }
}

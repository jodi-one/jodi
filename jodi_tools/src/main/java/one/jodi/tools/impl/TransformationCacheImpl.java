package one.jodi.tools.impl;

import com.google.inject.Inject;
import one.jodi.core.config.JodiProperties;
import one.jodi.core.model.Transformation;
import one.jodi.tools.TransformationCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TransformationCacheImpl implements TransformationCache {

    public static final int INITIAL_PACKAGE_SEQUENCE = 1000000;
    final static String INITIAL_PACKAGE_SEQUENCE_PROPERTY = "tools.initial_package_sequence";
    final static String PACKAGE_SEQUENCE_STEP_PROPERTY = "tools.package_sequence_step";
    private final JodiProperties properties;
    private final Logger logger = LogManager.getLogger(TransformationCacheImpl.class);
    private int packageSequence = INITIAL_PACKAGE_SEQUENCE;
    private int packageSequenceStep = 100;
    private LinkedHashMap<Transformation, Integer> map = new LinkedHashMap<Transformation, Integer>();


    @Inject
    public TransformationCacheImpl(JodiProperties properties) {
        this.properties = properties;
        packageSequence = getIntegerProperty(INITIAL_PACKAGE_SEQUENCE_PROPERTY, INITIAL_PACKAGE_SEQUENCE);
        packageSequenceStep = getIntegerProperty(PACKAGE_SEQUENCE_STEP_PROPERTY, packageSequenceStep);
    }

    @Override
    public void registerTransformation(Transformation transformation) {
        if (!map.containsKey(transformation)) {

            map.put(transformation, packageSequence);
            packageSequence += packageSequenceStep;
        }

    }

    @Override
    public void clear() {

        packageSequence = getIntegerProperty(INITIAL_PACKAGE_SEQUENCE_PROPERTY, INITIAL_PACKAGE_SEQUENCE);

        map.clear();
    }


    @Override
    public List<Transformation> getTransformations() {
        return new ArrayList<Transformation>(map.keySet());
    }

    @Override
    public int getPackageSequence(Transformation transformation) {
        return map.get(transformation);
    }

    private int getIntegerProperty(String propertyName, int defaultValue) {
        int value = defaultValue;
        if (properties.getPropertyKeys().contains(propertyName)) {
            try {
                value = Integer.parseInt(properties.getProperty(propertyName));
            } catch (NumberFormatException nfe) {
                logger.error("Jodi Property '" + propertyName + "' is not parseable as integer, defaulting to " + defaultValue);

            }
        }

        return value;
    }


}

package one.jodi.etl.internalmodel;

import java.util.Map;

/**
 * KMType is used to represent a decision regarding a KM and its options.
 */
public interface KmType {
    /**
     * Fetch the name of the KM used.
     *
     * @return KM Name
     */
    String getName();

    /**
     * Fetch a list of key/value options which will be applied to the KM.
     * <p>
     * This call will always return a non-null map.
     *
     * @return KM options.
     */
    Map<String, String> getOptions();

}

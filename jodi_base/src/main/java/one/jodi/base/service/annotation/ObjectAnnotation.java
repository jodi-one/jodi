package one.jodi.base.service.annotation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Infrastructure for validation of annotation
 *
 */
abstract public class ObjectAnnotation {

    private final String name;

    protected ObjectAnnotation(final String name) {
        this.name = name;
    }

    final static protected Map<String, Class<?>> createKeyTypeMap(
            final String[] bKeys,
            final String[] sKeys,
            final String[] iKeys,
            final String[] aKeys) {
        Map<String, Class<?>> allKeys = new HashMap<>();
        // Boolean Annotation Keys
        for (String s : bKeys) {
            allKeys.put(s.toLowerCase(), Boolean.class);
        }
        // String Annotation Keys
        for (String s : sKeys) {
            allKeys.put(s.toLowerCase(), String.class);
        }
        // String Annotation Keys
        for (String s : iKeys) {
            allKeys.put(s.toLowerCase(), Integer.class);
        }
        // Array of String Annotation Keys
        for (String s : aKeys) {
            allKeys.put(s.toLowerCase(), List.class);
        }
        return allKeys;
    }

    public String getName() {
        return name;
    }

    protected abstract Map<String, Class<?>> getKeyTypeMap();

    //
    // Infrastructure to filter out valid annotations and report incorrect
    // annotations.
    //

    private boolean keysAllLowerCase(final Set<String> keys) {
        return !keys.stream()
                .anyMatch(key -> !key.equals(key.toLowerCase()));
    }

    private boolean hasUnexpectedType(final Entry<String, Object> annotation,
                                      final Map<String, Class<?>> keyType) {
        // clazz may be null because annotation may not exist or
        // may not be overridden
        Class<?> clazz = keyType.get(annotation.getKey().toLowerCase());
        // returns true if not-null value is not of expected type
        return clazz != null && !clazz.isInstance(annotation.getValue());
    }

    private Set<String> findUnexpectedType(final Map<String, Object> annotations,
                                           final Map<String, Class<?>> keyTypes) {
        // find all keys that have an associated value with an unexpected type
        return annotations.entrySet()
                .stream()
                .filter(annotation -> annotation.getValue() != null)
                .filter(annotation -> hasUnexpectedType(annotation, keyTypes))
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    // hook for framework to validate annotations
    public abstract boolean isValid();

    protected abstract void reportUnexpectedType(final String key,
                                                 final Class<?> keyTypes);

    protected abstract void reportUndefinedAnnotation(final String key);


    /**
     * Validates annotations and filters out invalid or undefined annotations
     *
     * @param annotations validations as a String-Object map
     * @param keyTypes    Name-Type pair of supported annotation keys
     * @return valid annotations that with lower case keys
     */
    protected Map<String, Object> getValidAnnotations(
            final Map<String, Object> annotations,
            final Map<String, Class<?>> keyTypes) {
        // ensure that calling code uses lower case keys
        assert (keysAllLowerCase(keyTypes.keySet())) :
                "Pass only lower case names for annotation keys into this method.";

        Set<String> wrongTypes = findUnexpectedType(annotations, keyTypes);
        // report all incorrectly typed annotation values
        wrongTypes.forEach(key -> reportUnexpectedType(
                key, keyTypes.get(key.toLowerCase())));

        Set<String> undefinedKeys =
                annotations.keySet()
                        .stream()
                        .filter(annotation -> annotation != null &&
                                !keyTypes.containsKey(annotation.toLowerCase()))
                        .collect(Collectors.toSet());

        // report all unknown annotation keys
        undefinedKeys.forEach(this::reportUndefinedAnnotation);

        // collect all defined and correctly typed annotations
        return annotations.entrySet()
                .stream()
                .filter(a -> a.getValue() != null &&
                        !wrongTypes.contains(a.getKey()) &&
                        !undefinedKeys.contains(a.getKey()))
                .collect(Collectors.toMap(key -> key.getKey().toLowerCase(),
                        Map.Entry::getValue));
    }

}

package one.jodi.base.codecheck;

import com.google.inject.Inject;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.*;

public class ClassScan {

    public static final Set<Class<?>> getClasses(final String packageNameWithDots,
                                                 final Predicate<Class<?>> packageFilter) {
        List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setScanners(new SubTypesScanner(false),
                                new ResourcesScanner())
                        .setUrls(ClasspathHelper.forClassLoader(
                                classLoadersList.toArray(new ClassLoader[0])))
                        .filterInputsBy(new FilterBuilder()
                                .include(FilterBuilder.prefix(packageNameWithDots))
                        ));

        Set<Class<?>> classes =
                reflections.getSubTypesOf(Object.class).stream()
                        .filter(c -> packageFilter.test(c))
                        .collect(Collectors.toSet());

        return classes;
    }

    public static Map<Class<?>, Set<Field>> getAllNonFinalStaticFields(
            final String packageNameWithDots,
            final Predicate<Class<?>> packageFilter,
            final Predicate<Class<?>> classFilter) {

        Set<Class<?>> classes = getClasses(packageNameWithDots, packageFilter);
        @SuppressWarnings("unchecked")
        Map<Class<?>, Set<Field>> nonStaticNonFinalFields =
                classes.stream()
                        // find classes with a constructor that is annotated with '@Inject'
                        .filter(classFilter)
                        .flatMap(c -> getAllFields(c).stream())
                        // find fields that are not static and not final
                        .filter(f -> (f.getModifiers() & Modifier.FINAL) == 0 &&
                                (f.getModifiers() & Modifier.STATIC) == 0)
                        .collect(Collectors.groupingBy(f -> f.getDeclaringClass(),
                                HashMap::new,
                                Collectors.mapping(f -> f,
                                        Collectors.toSet())));

        return nonStaticNonFinalFields;
    }

    @SuppressWarnings("unchecked")
    private static String hasInjection(final Class<?> clazz) {
        return !getConstructors(clazz, withAnnotation(Inject.class)).isEmpty()
                ? "@Inject " : "";
    }

    public static void printMap(final Map<Class<?>, Set<Field>> map) {
        map.entrySet()
                .stream()
                .map(e -> hasInjection(e.getKey()) + e.getKey().getName() +
                        " (" + e.getValue().size() + "): " +
                        e.getValue().stream()
                                .map(f -> f.getName())
                                .collect(Collectors.joining(", ")))
                .sorted()
                .forEach(System.out::println);
    }

}

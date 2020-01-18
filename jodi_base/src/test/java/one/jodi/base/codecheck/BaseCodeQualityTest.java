package one.jodi.base.codecheck;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static one.jodi.base.codecheck.ClassScan.getAllNonFinalStaticFields;
import static one.jodi.base.codecheck.ClassScan.printMap;

//import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class BaseCodeQualityTest {

    @Test
    public void test() {
        Predicate<Class<?>> classFilter = c -> !c.isAnnotationPresent(RunWith.class);
        Predicate<Class<?>> packageFilter =
                c -> !c.getName().endsWith("JsTestAnnotationImpl") &&
                        !c.getName().endsWith("OdbETLProviderTest") &&
                        !c.getName().endsWith("ErrorWarningMessageJodiTrackerImpl") &&
                        !c.getName().endsWith("Mock_ClassWithError") &&
                        !c.getName().endsWith("ListAppender$Builder");

        Map<Class<?>, Set<Field>> fields =
                getAllNonFinalStaticFields("one.jodi.base",
                        packageFilter, classFilter);
        printMap(fields);
    }

}

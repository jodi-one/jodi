package one.jodi.base.service.annotation;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({KeyParserTest.class,
        JsAnnotationImplTest.class, ObjectAnnotationTest.class,
        AnnotationServiceDefaultImplBulkTest.class,
        TableAnnotationsTest.class,
        ColumnAnnotationsTest.class,
        VariableAnnotationsTest.class})
public class TestAll {
}

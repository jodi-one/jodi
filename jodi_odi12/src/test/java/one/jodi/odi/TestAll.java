package one.jodi.odi;

import one.jodi.odi.constraints.ConstraintValidationServiceImplTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        one.jodi.odi.common.TestAll.class,
        one.jodi.odi.etl.TestAll.class,
        ConstraintValidationServiceImplTest.class
})
public class TestAll {
}

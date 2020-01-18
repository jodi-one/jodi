package one.jodi.core.validation.etl;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ETLValidationResultTest.class,
        ETLValidatorImplTest.class
})
public class TestAll {
    public static void main(String[] args) {
        JUnitCore.runClasses(new Class[]{TestAll.class});
    }
}

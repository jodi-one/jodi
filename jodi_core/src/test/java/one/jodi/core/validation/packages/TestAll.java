package one.jodi.core.validation.packages;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        PackageValidatorImplTest.class,
})
public class TestAll {
    public static void main(String[] args) {
        JUnitCore.runClasses(new Class[]{TestAll.class});
    }
}

package one.jodi.qa.test;

import one.jodi.base.config.PasswordConfigImpl;

public class SampleHelper {

    public static String getFunctionalTestDir() {
        return "SampleC";
    }

    public static String getOdiPass() {
        return new PasswordConfigImpl().getOdiUserPassword();
    }
}

package one.jodi.error;

import org.junit.Test;

/**
 * This class just prints out links that either the;
 * Jodi Eclipse Plugin
 * or
 * Output Link Filter (IntelliJ) (https://plugins.jetbrains.com/plugin/7183?pr=idea)
 * Will parse into clickable links.
 */
public class TestClickableMessages {

    public static void main(String[] args) {
        System.out.println("test (./build.gradle:15)");
        System.out.println(
                "This is an error; (x)");
        System.out.println("http://oracle.com");
    }

    @Test
    public void testIntelliJ() {
        System.out.println("test (./build.gradle:16)");
        System.out.println(
                "This is an error; (y)");
        System.out.println("http://oracle.com");
    }
}

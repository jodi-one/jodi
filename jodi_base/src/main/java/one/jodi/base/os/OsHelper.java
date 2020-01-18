package one.jodi.base.os;

public class OsHelper {
    private static String OS;

    static {
        String value = System.getProperty("os.name");
        OS = (value != null) ? value.toLowerCase() : "";
    }

    public static boolean isWindows() {
        return (OS.contains("win"));
    }

    public static boolean isMac() {
        return (OS.contains("mac"));
    }

    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS
                .indexOf("aix") > 0);
    }

    public static boolean isSolaris() {
        return (OS.contains("sunos"));
    }
}

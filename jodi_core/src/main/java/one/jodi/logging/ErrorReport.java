package one.jodi.logging;

public class ErrorReport {

    private static StringBuffer errorReport = new StringBuffer();
    private static String errorReportnewLine = System.getProperty("line.separator");
    private static int errorReportCount = 0;

    static public void reset() {
        errorReport = new StringBuffer();
        errorReportCount = 0;
    }

    static public StringBuffer getErrorReport() {
        return errorReport;
    }

    static public void addErrorLine(int packageSequence, String line) {
        errorReportCount++;
        errorReport.append("[" + errorReportCount + "]: packageSequence:[" + packageSequence + "]:" + line + errorReportnewLine);
    }
}

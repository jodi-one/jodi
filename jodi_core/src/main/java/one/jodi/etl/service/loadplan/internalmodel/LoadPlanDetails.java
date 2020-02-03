package one.jodi.etl.service.loadplan.internalmodel;

import java.math.BigInteger;

/**
 * Internal model of a LoadPlan details like the name and folder.
 */
public class LoadPlanDetails {
    private String loadPlanName;
    private String folderName;
    private Integer keeplogHistory;
    private LogsessionType logsessionType;
    private LogsessionstepType logsessionstepType;
    private Integer sessiontaskloglevel;
    private String keywords;
    private Boolean limitconccurentexecutions;
    private ViolatebehaviourType violatebehaviourType;
    private BigInteger waitpollinginterval;
    private String description;
    private Number numberOfConcurrentexecutions;

    public LoadPlanDetails(final String pLoadPlanName, final String pFolderName, final Integer keeplogHistory, final LogsessionType logsessionType,
                           final LogsessionstepType logsessionstepType, final Integer sessiontaskloglevel, final String keywords, final Boolean limitconccurentexecutions,
                           final Number numberOfConcurrentexecutions, final ViolatebehaviourType violatebehaviourType, final BigInteger waitpollinginterval, final String description) {
        this.loadPlanName = pLoadPlanName;
        this.folderName = pFolderName;
        this.keeplogHistory = keeplogHistory;
        this.logsessionType = logsessionType;
        this.logsessionstepType = logsessionstepType;
        this.sessiontaskloglevel = sessiontaskloglevel;
        this.keywords = keywords;
        this.limitconccurentexecutions = limitconccurentexecutions;
        this.violatebehaviourType = violatebehaviourType;
        this.waitpollinginterval = waitpollinginterval;
        this.description = description;
        this.numberOfConcurrentexecutions = numberOfConcurrentexecutions;
    }

    public String getLoadPlanName() {
        return loadPlanName;
    }

    public void setLoadPlanName(String loadPlanName) {
        this.loadPlanName = loadPlanName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Integer getKeeplogHistory() {
        // this resolves a bug in the ui in odi 12.2.1.2.0
        return keeplogHistory == null || keeplogHistory == 0 ? 7 : keeplogHistory;
    }

    public void setKeeplogHistory(Integer keeplogHistory) {
        this.keeplogHistory = keeplogHistory;
    }

    public LogsessionType getLogsessionType() {
        return logsessionType;
    }

    public void setLogsessionType(LogsessionType logsessionType) {
        this.logsessionType = logsessionType;
    }

    public LogsessionstepType getLogsessionstepType() {
        return logsessionstepType;
    }

    public void setLogsessionstepType(LogsessionstepType logsessionstepType) {
        this.logsessionstepType = logsessionstepType;
    }

    public Integer getSessiontaskloglevel() {
        // this resolves a bug in the ui in odi 12.2.1.2.0
        return sessiontaskloglevel == null || sessiontaskloglevel == 0 ? 5 : sessiontaskloglevel;
    }

    public void setSessiontaskloglevel(Integer sessiontaskloglevel) {
        this.sessiontaskloglevel = sessiontaskloglevel;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public ViolatebehaviourType getViolatebehaviourType() {
        return violatebehaviourType;
    }

    public void setViolatebehaviourType(ViolatebehaviourType violatebehaviourType) {
        this.violatebehaviourType = violatebehaviourType;
    }

    public BigInteger getWaitpollinginterval() {
        return waitpollinginterval;
    }

    public void setWaitpollinginterval(BigInteger waitpollinginterval) {
        this.waitpollinginterval = waitpollinginterval;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isLimitconcurrentexecutions() {
        return limitconccurentexecutions;
    }

    public Number getNumberOfConcurrentexecutions() {
        return numberOfConcurrentexecutions;
    }

    public void setNumberOfConcurrentexecutions(Number numberOfConcurrentexecutions) {
        this.numberOfConcurrentexecutions = numberOfConcurrentexecutions;
    }

    public void setLimitconccurentexecutions(Boolean limitconccurentexecutions) {
        this.limitconccurentexecutions = limitconccurentexecutions;
    }

    public enum LogsessionType {
        ALWAYS, NEVER, ERRORS
    }

    public enum LogsessionstepType {
        BYSCENARIOSETTINGS, NEVER, ERRORS
    }

    public enum ViolatebehaviourType {
        NONE, RAISE_EXECUTION_ERROR, WAIT_TO_EXECUTE
    }

}

package uk.nhs.nhsx.analyticssubmission.model;

public class AnalyticsMetrics {

    public Long cumulativeDownloadBytes;
    public Long cumulativeUploadBytes;
    public Long cumulativeCellularDownloadBytes;
    public Long cumulativeCellularUploadBytes;
    public Long cumulativeWifiDownloadBytes;
    public Long cumulativeWifiUploadBytes;
    public int checkedIn;
    public int canceledCheckIn;
    public int receivedVoidTestResult;
    public int isIsolatingBackgroundTick;
    public int hasHadRiskyContactBackgroundTick;
    public int receivedPositiveTestResult;
    public int receivedNegativeTestResult;
    public int hasSelfDiagnosedPositiveBackgroundTick;
    public int completedQuestionnaireAndStartedIsolation;
    public int encounterDetectionPausedBackgroundTick;
    public int completedQuestionnaireButDidNotStartIsolation;
    public int totalBackgroundTasks;
    public int runningNormallyBackgroundTick;
    public int completedOnboarding;
    public Integer receivedVoidTestResultEnteredManually;
    public Integer receivedPositiveTestResultEnteredManually;
    public Integer receivedNegativeTestResultEnteredManually;
    public Integer receivedVoidTestResultViaPolling;
    public Integer receivedPositiveTestResultViaPolling;
    public Integer receivedNegativeTestResultViaPolling;
    public Integer hasSelfDiagnosedBackgroundTick;
    public Integer hasTestedPositiveBackgroundTick;
    public Integer isIsolatingForSelfDiagnosedBackgroundTick;
    public Integer isIsolatingForTestedPositiveBackgroundTick;
    public Integer isIsolatingForHadRiskyContactBackgroundTick;
    public Integer receivedRiskyContactNotification;
    public Integer startedIsolation;
    public Integer receivedPositiveTestResultWhenIsolatingDueToRiskyContact;


    public AnalyticsMetrics() { }
}

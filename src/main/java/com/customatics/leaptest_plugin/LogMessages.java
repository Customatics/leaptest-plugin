package com.customatics.leaptest_plugin;

/**
 * Created by User on 02.05.2017.
 */
public class LogMessages {

    private static LogMessages LOG = null;
    private LogMessages(){}

    public final String ERROR_CODE_MESSAGE = "Code: %1$s Status: %2$s!";
    public final String SCHEDULE_RUN_SUCCESS = "Schedule: %1$s | Schedule Id: %2$s | Launched: SUCCESSFULLY";
    public final String SCHEDULE_RUN_FAILURE = "Failed to run %1$s[%2$s]! Check it at your Leaptest server or connection to your server and try again!";
    public final String SCHEDULE_STATE_FAILURE = "Tried to get %1$s[%2$s] state! Check connection to your server and try again!";
    public final String REPORT_FILE_NOT_FOUND = "Couldn't find report file! Wrong path! Press \"help\" button nearby \"report\" textbox! ";
    public final String REPORT_FILE_CREATION_FAILURE = "Failed to create a report file!";
    public final String SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT = "Tried to get schedule title or id! Check connection to your server and try again!";
    public final String CASE_INFORMATION = "Case: %1$s | Status: %2$s | Elapsed: %3$s";
    public final String CASE_STACKTRACE_FORMAT = "%1$s - %2$s";
    public final String SCHEDULE_HAS_NO_CASES_XML = "Schedule [%1$s] has no cases! JSON: &#xA; %2$s";
    public final String SCHEDULE_HAS_NO_CASES = "Schedule[%1$s] has no cases! JSON:\n %2$s";
    public final String GET_ALL_AVAILABLE_SCHEDULES_URI = "%1$s/api/v1/runSchedules";
    public final String GET_LEAPTEST_VERSION_AND_API_URI = "%1$s/api/v1/misc/version";
    public final String RUN_SCHEDULE_URI = "%1$s/%2$s/runNow";
    public final String GET_SCHEDULE_STATE_URI = "%1$s/state/%2$s";
    public final String INVALID_SCHEDULES = "INVALID SCHEDULES";
    public final String NO_SCHEDULES_OR_WRONG_URL_ERROR = "No Schedules or wrong url! Check connection to your server or schedules and try again!";
    public final String PLUGIN_SUCCESSFUL_FINISH = "Leaptest for Jenkins  plugin  successfully finished!";
    public final String PLUGIN_ERROR_FINISH = "Leaptest for Jenkins plugin finished with errors!";
    public final String PLUGIN_NAME = "Leaptest Integration";
    public final String JENKINS_WORKSPACE_VARIABLE = "WORKSPACE";
    public final String NO_SUCH_SCHEDULE = "No such schedule!";

    public static LogMessages getInstance()
    {
        if(LOG == null) LOG = new LogMessages();
        return LOG;
    }
}

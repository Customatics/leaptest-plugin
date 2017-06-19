package com.customatics.leaptest_plugin;

/**
 * Created by User on 02.05.2017.
 */
public final class Messages {

    public static final String ERROR_CODE_MESSAGE = "Code: %1$s Status: %2$s!";
    public static final String SCHEDULE_RUN_SUCCESS = "Schedule %1$s[%2$s] Launched Successfully!";
    public static final String SCHEDULE_RUN_FAILURE = "Failed to run %1$s[%2$s]! Check it at your Leaptest server or connection to your server and try again!";
    public static final String SCHEDULE_STATE_FAILURE = "Tried to get %1$s[%2$s] state! Check connection to your server and try again!";
    public static final String REPORT_FILE_NOT_FOUND = "Couldn't find report file! Wrong path! Press \"help\" button nearby \"report\" textbox! ";
    public static final String REPORT_FILE_CREATION_FAILURE = "Failed to create a report file!";
    public static final String SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT = "Tried to get schedule title or id! Check connection to your server and try again!";
    public static final String CASE_CONSOLE_LOG_SEPARATOR = "----------------------------------------------------------------------------------------";
    public static final String SCHEDULE_CONSOLE_LOG_SEPARATOR = "//////////////////////////////////////////////////////////////////////////////////////";
    public static final String CASE_INFORMATION = "Case: %1$s | Status: %2$s | Elapsed: %3$s";
    public static final String CASE_STACKTRACE_FORMAT = "%1$s - %2$s";
    public static final String SCHEDULE_HAS_NO_CASES_XML = "Schedule [%1$s] has no cases! JSON: &#xA; %2$s";
    public static final String SCHEDULE_HAS_NO_CASES = "Schedule[%1$s] has no cases! JSON:\n %2$s";
    public static final String GET_ALL_AVAILABLE_SCHEDULES_URI = "%1$s/api/v1/runSchedules";
    public static final String GET_LEAPTEST_VERSION_AND_API_URI = "%1$s/api/v1/misc/version";
    public static final String RUN_SCHEDULE_URI = "%1$s/api/v1/runSchedules/%2$s/runNow";
    public static final String GET_SCHEDULE_STATE_URI = "%1$s/api/v1/runSchedules/state/%2$s";
    public static final String INVALID_SCHEDULES = "INVALID SCHEDULES";
    public static final String NO_SCHEDULES_OR_WRONG_URL_ERROR = "No Schedules or wrong url! Check connection to your server or schedules and try again!";
    public static final String PLUGIN_SUCCESSFUL_FINISH = "Leaptest for Jenkins  plugin  successfully finished!";
    public static final String PLUGIN_ERROR_FINISH = "Leaptest for Jenkins plugin finished with errors!";
    public static final String PLUGIN_NAME = "Leaptest Integration";
    public static final String JENKINS_WORKSPACE_VARIABLE = "WORKSPACE";
    public static final String NO_SUCH_SCHEDULE = "No such schedule!";
    public static final String SCHEDULE_DETECTED = "Schedule %1$s[%2$s] successfully detected!";
    public static final String SCHEDULE_FORMAT = "%1$s[%2$s]";


}

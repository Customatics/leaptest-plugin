package com.customatics.leaptest_plugin;

/**
 * Created by User on 02.05.2017.
 */
public final class Messages {

    public static final String SCHEDULE_FORMAT = "%1$s[%2$s]";
    public static final String SCHEDULE_DETECTED = "Schedule %1$s[%2$s] successfully detected!";
    public static final String SCHEDULE_RUN_SUCCESS = "Schedule %1$s[%2$s] Launched Successfully!";

    public static final String SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT = "Tried to get schedule title or id!";
    public static final String SCHEDULE_RUN_FAILURE = "Failed to run %1$s[%2$s]!";
    public static final String SCHEDULE_STATE_FAILURE = "Tried to get %1$s[%2$s] state!";
    public static final String NO_SUCH_SCHEDULE = "No such schedule! This may occur if try to run schedule that controller does not have. It can be deleted. Or you simply have forgotten to select schedules after changing controller address;";
    public static final String NO_SUCH_SCHEDULE_WAS_FOUND = "Could not find %1$s[%2$s] schedule! It was likely deleted!";
    public static final String SCHEDULE_HAS_NO_CASES = "Schedule %1$s[%2$s] has no cases! Add them in your Leaptest studio and try again!";
    public static final String SCHEDULE_IS_RUNNING_NOW = "Schedule %1$s[%2$s] is already running or queued now! Try to run it again when it's finished or you can try stop it, then run it!";


    public static final String REPORT_FILE_NOT_FOUND = "Couldn't find report file! Wrong path! Press \"help\" button nearby \"report\" textbox! ";
    public static final String REPORT_FILE_CREATION_FAILURE = "Failed to create a report file!";

    public static final String CASE_CONSOLE_LOG_SEPARATOR = "----------------------------------------------------------------------------------------";
    public static final String SCHEDULE_CONSOLE_LOG_SEPARATOR = "//////////////////////////////////////////////////////////////////////////////////////";

    public static final String CASE_INFORMATION = "Case: %1$s | Status: %2$s | Elapsed: %3$s";
    public static final String CASE_STACKTRACE_FORMAT = "%1$s - %2$s";

    public static final String GET_ALL_AVAILABLE_SCHEDULES_URI = "%1$s/api/v1/runSchedules";
    public static final String GET_LEAPTEST_VERSION_AND_API_URI = "%1$s/api/v1/misc/version";
    public static final String RUN_SCHEDULE_URI = "%1$s/api/v1/runSchedules/%2$s/runNow";
    public static final String STOP_SCHEDULE_URI = "%1$s/api/v1/runSchedules/%2$s/stop";
    public static final String GET_SCHEDULE_STATE_URI = "%1$s/api/v1/runSchedules/state/%2$s";

    public static final String INVALID_SCHEDULES = "INVALID SCHEDULES";
    public static final String PLUGIN_NAME = "Leaptest Integration";
    public static final String JENKINS_WORKSPACE_VARIABLE = "WORKSPACE";

    public static final String NO_SCHEDULES = "No Schedules to run! All schedules you've selected could be deleted. Or you simply have forgotten to select schedules after changing controller address;";

    public static final String PLUGIN_SUCCESSFUL_FINISH = "Leaptest for Jenkins  plugin  successfully finished!";
    public static final String PLUGIN_ERROR_FINISH = "Leaptest for Jenkins plugin finished with errors!";

    public static final String CONTROLLER_RESPONDED_WITH_ERRORS = "Controller responded with errors! Please check controller logs and try again! If does not help, try to restart controller.";
    public static final String PLEASE_CONTACT_SUPPORT = "If nothing helps, please contact support https://leaptest.com/support and provide the next information:\n1.Plugin Logs\n2.Leaptest and plugin version\n3.Controller logs from the moment you've run the plugin.\n4.Assets without videos if possible.\nYou can find them {Path to Leaptest}/LEAPTEST/Assets\nThank you";

    public static final String ERROR_CODE_MESSAGE = "Code: %1$s Status: %2$s!";
    public static final String COULD_NOT_CONNECT_TO = "Could not connect to %1$s! Check it and try again! ";
    public static final String COULD_NOT_CONNECT_TO_BUT_WAIT = "Could not connect to %1$s! Check connection! The plugin is waiting for connection reestablishment! ";
    public static final String CONNECTION_LOST = "Connection to controller is lost: %1$s! Check connection! The plugin is waiting for connection reestablishment!";
    public static final String INTERRUPTED_EXCEPTION = "Interrupted exception: %1$s!";
    public static final String EXECUTION_EXCEPTION = "Execution exception: %1$s!";
    public static final String IO_EXCEPTION = "I/O exception: %1$s!";
    public static final String EXCEPTION = "Exception: %1$s!";
    public static final String CACHE_TIMEOUT_EXCEPTION = "Cache time out exception has occurred! Don't worry! This schedule %1$s[%2$s] will be run again later!";

    public static final String LICENSE_EXPIRED = "Your Leaptest license has expired. Please contact support https://leaptest.com/support";

    public static final String SCHEDULE_IS_STILL_RUNNING = "Schedule %1$s[%2$s] is still running!";

    public static final String STOPPING_SCHEDULE = "Stopping schedule %1$s[%2$s]!";
    public static final String STOP_SUCCESS = "Schedule %1$s[%2$s] stopped successfully!";
    public static final String STOP_FAIL = "Failed to stop schedule %1$s[%2$s]!";

}

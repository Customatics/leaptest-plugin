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
    public static final String NO_SUCH_SCHEDULE = "No such schedule! This may occur if try to run schedule that controller does not have. It can be deleted. Or you simply have forgotten to select schedules after changing controller address;";
    public static final String NO_SUCH_SCHEDULE_WAS_FOUND = "Could not find %1$s[%2$s] schedule! It was likely deleted!";
    public static final String NO_SUCH_RUN_WAS_FOUND = "Could not find run %1$s of %2$s schedule!";
    public static final String NO_SUCH_RUN_ITEM_WAS_FOUND = "Could not find run item %1$s of %2$s schedule!";
    public static final String NO_SUCH_RUN = "No such run %1$s !";


    public static final String REPORT_FILE_NOT_FOUND = "Couldn't find report file! Wrong path! Press \"help\" button nearby \"report\" textbox! ";
    public static final String REPORT_FILE_CREATION_FAILURE = "Failed to create a report file!";

    public static final String CASE_CONSOLE_LOG_SEPARATOR = "----------------------------------------------------------------------------------------";
    public static final String SCHEDULE_CONSOLE_LOG_SEPARATOR = "//////////////////////////////////////////////////////////////////////////////////////";

    public static final String CASE_INFORMATION = "RunItem: %1$s | Status: %2$s | Elapsed: %3$s";
    public static final String CASE_STACKTRACE_FORMAT = "%1$s - %2$s";

    public static final String GET_ALL_AVAILABLE_SCHEDULES_URI = "%1$s/api/v3/schedules";
    public static final String RUN_SCHEDULE_URI = "%1$s/api/v3/schedules/%2$s/runNow";
    public static final String STOP_RUN_URI = "%1$s/api/v3/run/%2$s/stop";
    public static final String STOP_SCHEDULE_URI = "%1$s/api/v3/schedules/%2$s/stop";
    public static final String GET_RUN_STATUS_URI = "%1$s/api/v3/run/%2$s/status";
    public static final String GET_RUN_ITEMS_IDS_URI = "%1$s/api/v3/run/%2$s/runItemIds";
    public static final String GET_RUN_ITEM_URI = "%1$s/api/v3/runItems/%2$s";

    public static final String INVALID_SCHEDULES = "INVALID SCHEDULES";
    public static final String PLUGIN_NAME = "Leapwork Integration";
    public static final String JENKINS_WORKSPACE_VARIABLE = "WORKSPACE";

    public static final String NO_SCHEDULES = "No Schedules to run! All schedules you've selected could be deleted. Or you simply have forgotten to select schedules after changing controller address;";

    public static final String PLUGIN_SUCCESSFUL_FINISH = "Leapwork for Jenkins  plugin  successfully finished!";
    public static final String PLUGIN_ERROR_FINISH = "Leapwork for Jenkins plugin finished with errors!";

    public static final String CONTROLLER_RESPONDED_WITH_ERRORS = "Controller responded with errors! Please check controller logs and try again! If does not help, try to restart controller.";
    public static final String PLEASE_CONTACT_SUPPORT = "If nothing helps, please contact support https://leapwork.com/support and provide the next information:\n1.Plugin Logs\n2.Leapwork and plugin version\n3.Controller logs from the moment you've run the plugin.\n4.Assets without videos if possible.\nYou can find them {Path to Leapwork}/LEAPTEST/Assets\nThank you";

    public static final String ERROR_CODE_MESSAGE = "Code: %1$s Status: %2$s!";
    public static final String COULD_NOT_CONNECT_TO = "Could not connect to %1$s! Check it and try again! ";
    public static final String COULD_NOT_CONNECT_TO_BUT_WAIT = "Could not connect to %1$s! Check connection! The plugin is waiting for connection reestablishment! ";
    public static final String CONNECTION_LOST = "Connection to controller is lost: %1$s! The plugin is waiting for connection reestablishment!";
    public static final String INTERRUPTED_EXCEPTION = "Interrupted exception: %1$s!";
    public static final String EXECUTION_EXCEPTION = "Execution exception: %1$s!";
    public static final String IO_EXCEPTION = "I/O exception: %1$s!";
    public static final String EXCEPTION = "Exception: %1$s!";
    public static final String CACHE_TIMEOUT_EXCEPTION = "Cache time out exception has occurred! This schedule will be run later";

    public static final String LICENSE_EXPIRED = "Your Leapwork license has expired. Please contact support https://leapwork.com/support";

    public static final String SCHEDULE_IS_STILL_RUNNING = "Schedule %1$s[%2$s] is still running!";

    public static final String STOPPING_SCHEDULE = "Stopping schedule %1$s[%2$s]!";

    public static final String STOPPING_RUN = "Stopping schedule %1$s run %2$s!";

    public static final String STOP_SCHEDULE_SUCCESS = "Schedule %1$s[%2$s] stopped successfully!";

    public static final String STOP_SCHEDULE_FAIL = "Failed to stop schedule %1$s[%2$s]!";

    public static final String STOP_RUN_SUCCESS = "Schedule %1$s run %2$s stopped successfully!";

    public static final String STOP_RUN_FAIL = "Failed to stop schedule %1$s run %2$s!";

    public static final String INVALID_ACCESS_KEY = "Invalid or empty access key!";

    public static final String DATABASE_NOT_RESPONDING = "Data base is not responding!";

    public static final String INVALID_VARIABLE_KEY_NAME = "Variable name is invalid or variable with such name is already in request!";

    public static final String NO_DISK_SPACE = "No enough disk space to start schedule!";

    public static final String PORT_NUMBER_IS_INVALID = "Port number is invalid, setting to default %1$d";

    public static final String TIME_DELAY_NUMBER_IS_INVALID = "Time delay number is invalid, setting to default %1$s";

    public static final String IS_POOL_MODE_FLAG_IS_INVALID = "Invalid value of pool mode flag, setting to default false";

    public static final String SCHEDULE_DISABLED = "Schedule %1$s[%2$s] is disabled!";


}

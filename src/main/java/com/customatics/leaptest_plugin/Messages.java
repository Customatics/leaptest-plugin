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
    public static final String RUN_SCHEDULE_URI = "%1$s/api/v3/schedules/%2$s/runNow%3$s";
    public static final String STOP_RUN_URI = "%1$s/api/v3/run/%2$s/stop";
    public static final String STOP_SCHEDULE_URI = "%1$s/api/v3/schedules/%2$s/stop";
    public static final String GET_RUN_STATUS_URI = "%1$s/api/v3/run/%2$s/status";
    public static final String GET_RUN_ITEMS_IDS_URI = "%1$s/api/v3/run/%2$s/runItemIds";
    public static final String GET_RUN_ITEM_URI = "%1$s/api/v3/runItems/%2$s";
    public static final String GET_RUN_ITEM_KEYFRAMES_URI = "%1$s/api/v3/runItems/%2$s/keyframes";

    public static final String INVALID_SCHEDULES = "INVALID SCHEDULES";
    public static final String PLUGIN_NAME = "Leapwork Integration";
    public static final String JENKINS_WORKSPACE_VARIABLE = "WORKSPACE";

    public static final String NO_SCHEDULES = "No Schedules to run! All schedules you've selected could be deleted. Or you simply have forgotten to select schedules after changing controller address;";

    public static final String PLUGIN_SUCCESSFUL_FINISH = "Leapwork for Jenkins  plugin  successfully finished!";
    public static final String PLUGIN_ERROR_FINISH = "Leapwork for Jenkins plugin finished with errors!";

    public static final String CONTROLLER_RESPONDED_WITH_ERRORS = "Controller responded with errors! Please check controller logs and try again! If does not help, try to restart controller.";
    public static final String PLEASE_CONTACT_SUPPORT = "If nothing helps, please contact support https://leapwork.com/chat and provide the next information:\n1.Plugin Logs\n2.Leapwork and plugin version\n3.Controller logs from the moment you've run the plugin.\n4.Assets without videos if possible.\nYou can find them {Path to Leapwork}/LEAPWORK/Assets\nThank you";

    public static final String ERROR_CODE_MESSAGE = "Code: %1$s Status: %2$s!";
    public static final String COULD_NOT_CONNECT_TO = "Could not connect to %1$s! Check it and try again! ";
    public static final String COULD_NOT_CONNECT_TO_BUT_WAIT = "Could not connect to %1$s! Check connection! The plugin is waiting for connection reestablishment! ";
    public static final String CONNECTION_LOST = "Connection to controller is lost: %1$s! The plugin is waiting for connection reestablishment!";
    public static final String INTERRUPTED_EXCEPTION = "Interrupted exception: %1$s!";
    public static final String EXECUTION_EXCEPTION = "Execution exception: %1$s!";
    public static final String IO_EXCEPTION = "I/O exception: %1$s!";

    public static final String LICENSE_EXPIRED = "Your Leapwork license has expired. Please contact support https://leapwork.com/support";

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

    public static final String TIME_DELAY_NUMBER_IS_INVALID = "Time delay number is invalid: %1$s, setting to default %2$s";
    public static final String FULL_REPORT_FILE_PATH = "Creating report file by path: %1$s";

    public static final String SCHEDULE_DISABLED = "Schedule %1$s[%2$s] is disabled!";

    public static final String INVALID_SCHEDULE_VARIABLE = "Failed to parse variable: %1$s";
    public static final String SCHEDULE_VARIABLE_KEY_DUPLICATE = "Key value pair with the same key already exists: %1$s. This pair will be ignored";
    public static final String SCHEDULE_VARIABLE_REQUEST_PART = "Schedule variables request part: %1$s";

    public static final String INPUT_VALUES_MESSAGE = "LeapWork Plugin input parameters:";
    public static final String INPUT_HOSTNAME_VALUE = "LEAPWORK controller hostname: %1$s";
    public static final String INPUT_PORT_VALUE = "LEAPWORK controller port: %1$s";
    public static final String INPUT_ACCESS_KEY_VALUE = "LEAPWORK controller Access Key: %1$s";
    public static final String INPUT_REPORT_VALUE = "JUnit report file name: %1$s";
    public static final String INPUT_WORKSPACE_VALUE = "Workspace: %1$s";
    public static final String INPUT_SCHEDULE_NAMES_VALUE = "Schedule names: %1$s";
    public static final String INPUT_SCHEDULE_IDS_VALUE = "Schedule ids: %1$s";
    public static final String INPUT_DELAY_VALUE = "Delay between status checks: %1$s";
    public static final String INPUT_DONE_VALUE = "Done Status As: %1$s";
    public static final String INPUT_WRITE_PASSED = "Write keyframes of passed flows: %1$b";
    public static final String INPUT_VARIABLES = "Passed schedule variables: %1$s";

    public static final String SCHEDULE_TITLE = "Schedule: %1$s[%2$s]";
    public static final String CASES_PASSED = "Passed testcases: %1$d";
    public static final String CASES_FAILED = "Failed testcases: %1$d";
    public static final String CASES_ERRORED = "Error testcases: %1$d";

    public static final String TOTAL_SEPARATOR = "|---------------------------------------------------------------";
    public static final String TOTAL_CASES_PASSED = "| Total passed testcases: %1$d";
    public static final String TOTAL_CASES_FAILED = "| Total failed testcases: %1$d";
    public static final String TOTAL_CASES_ERROR = "| Total error testcases: %1$d";

    public static final String FAILED_TO_PARSE_RESPONSE_KEYFRAME_JSON_ARRAY = "Failed to parse response keyframe json array";
    public static final String ERROR_NOTIFICATION = "[ERROR] There were detected case(s) with status 'Failed', 'Error', 'Inconclusive', 'Timeout' or 'Cancelled'. Please check the report or console output for details. Set the build status to FAILURE as the results of the cases are not deterministic..";

}

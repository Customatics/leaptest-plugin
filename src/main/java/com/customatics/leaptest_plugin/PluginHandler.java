package com.customatics.leaptest_plugin;

import com.customatics.leaptest_plugin.model.LeapworkRun;
import com.customatics.leaptest_plugin.model.RunCollection;
import com.customatics.leaptest_plugin.model.RunItem;
import com.customatics.leaptest_plugin.model.InvalidSchedule;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import hudson.model.TaskListener;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;


public final class PluginHandler {

    private static PluginHandler pluginHandler = null;

    private PluginHandler(){}

    public static PluginHandler getInstance()
    {
        if( pluginHandler == null ) pluginHandler = new PluginHandler();

        return pluginHandler;
    }

    public String getJunitReportFilePath(String jenkinsWorkspacePath, String reportFileName)
    {
        if(reportFileName.isEmpty() || "".equals(reportFileName))
        {
            reportFileName = "report.xml";
        }

        if(!reportFileName.contains(".xml"))
        {
            reportFileName +=".xml";
        }
        return String.format("%1$s/%2$s",jenkinsWorkspacePath,reportFileName);
    }
    public ArrayList<String> getRawScheduleList(String rawScheduleIds, String rawScheduleTitles)
    {
        ArrayList<String> rawScheduleList = new ArrayList<>();

        String[] schidsArray = rawScheduleIds.split("\n|, |,");
        String[] testsArray = rawScheduleTitles.split("\n|, |,");

        rawScheduleList.addAll(Arrays.asList(schidsArray));
        rawScheduleList.addAll(Arrays.asList(testsArray));
        rawScheduleList.removeIf(sch->sch.trim().length() == 0);

        return rawScheduleList;
    }

    public int getTimeDelay(String rawTimeDelay, TaskListener listener)
    {
        int defaultTimeDelay = 5;
        try
        {
            if(!rawTimeDelay.isEmpty() || !"".equals(rawTimeDelay))
                return Integer.parseInt(rawTimeDelay);
            else
            {
                listener.getLogger().println(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID,defaultTimeDelay));
                return defaultTimeDelay;
            }
        }
        catch (Exception e)
        {
            listener.getLogger().println(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID,defaultTimeDelay));
            return defaultTimeDelay;
        }
    }
    public String getControllerApiHttpAdderess(String hostname, String rawPort, TaskListener listener)
    {
        StringBuilder stringBuilder = new StringBuilder();
        int port = getPortNumber(rawPort,listener);
        stringBuilder.append("http://").append(hostname).append(":").append(port);
        return stringBuilder.toString();
    }
    private int getPortNumber(String rawPortStr,TaskListener listener)
    {
        int defaultPortNumber = 9001;
        try
        {
            if(!rawPortStr.isEmpty() || !"".equals(rawPortStr))
                return Integer.parseInt(rawPortStr);
            else
            {
                listener.getLogger().println(String.format(Messages.PORT_NUMBER_IS_INVALID,defaultPortNumber));
                return defaultPortNumber;
            }
        }
        catch (Exception e)
        {
            listener.getLogger().println(String.format(Messages.PORT_NUMBER_IS_INVALID,defaultPortNumber));
            return defaultPortNumber;
        }
    }


    public HashMap<UUID, String> getSchedulesIdTitleHashMap(
            AsyncHttpClient client,
            String accessKey,
            String controllerApiHttpAddress,
            ArrayList<String> rawScheduleList,
            TaskListener listener,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws  Exception {

        HashMap<UUID, String> schedulesIdTitleHashMap = new HashMap<>();

        String scheduleListUri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, controllerApiHttpAddress);

        try {

            try
            {
                Response response = client.prepareGet(scheduleListUri).setHeader("AccessKey",accessKey).execute().get();

                switch (response.getStatusCode()) {
                    case 200:
                        JsonParser parser = new JsonParser();
                        JsonArray jsonScheduleList = parser.parse(response.getResponseBody()).getAsJsonArray();

                        for (String rawSchedule : rawScheduleList) {
                            boolean successfullyMapped = false;
                            for (JsonElement jsonScheduleElement : jsonScheduleList) {
                                JsonObject jsonSchedule = jsonScheduleElement.getAsJsonObject();

                                UUID Id = Utils.defaultUuidIfNull(jsonSchedule.get("Id"), UUID.randomUUID());
                                String Title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");
                                boolean isEnabled = Utils.defaultBooleanIfNull(jsonSchedule.get("IsEnabled"), false);

                                if (Id.toString().contentEquals(rawSchedule))
                                {
                                    if (!schedulesIdTitleHashMap.containsValue(Title))
                                    {
                                        if(isEnabled)
                                        {
                                            schedulesIdTitleHashMap.put(Id, Title);
                                            System.out.println(String.format(Messages.SCHEDULE_DETECTED, Title, rawSchedule));
                                        }
                                        else
                                        {
                                            invalidSchedules.add(new InvalidSchedule(rawSchedule, String.format(Messages.SCHEDULE_DISABLED,Title,Id)));
                                        }
                                    }

                                    successfullyMapped = true;
                                }

                                if (Title.contentEquals(rawSchedule))
                                {
                                    if (!schedulesIdTitleHashMap.containsKey(Id))
                                    {
                                        if(isEnabled)
                                        {
                                            schedulesIdTitleHashMap.put(Id, rawSchedule);
                                            System.out.println(String.format(Messages.SCHEDULE_DETECTED,rawSchedule, Id));
                                        }
                                        else
                                        {
                                            invalidSchedules.add(new InvalidSchedule(rawSchedule, String.format(Messages.SCHEDULE_DISABLED,Title,Id)));
                                        }
                                    }

                                    successfullyMapped = true;
                                }
                            }

                            if (!successfullyMapped)
                                invalidSchedules.add(new InvalidSchedule(rawSchedule, Messages.NO_SUCH_SCHEDULE));
                        }
                        break;

                    case 401:
                        String errorMessage401 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage401 += String.format("\n%1$s", Messages.INVALID_ACCESS_KEY);
                        throw new Exception(errorMessage401);

                    case 500:
                        String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                        throw new Exception(errorMessage500);

                    default:
                        String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        throw new Exception(errorMessage);

                }
            }
            catch (ConnectException | UnknownHostException e )
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
                throw new Exception(connectionErrorMessage);
            }
            catch (InterruptedException e)
            {
                String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
                throw new Exception(interruptedExceptionMessage);
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof  UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getCause().getMessage());
                    throw new Exception(connectionErrorMessage);
                }
                else
                {
                    String executionExceptionMessage = String.format(Messages.EXECUTION_EXCEPTION, e.getMessage());
                    throw new Exception(executionExceptionMessage);
                }
            }
            catch (IOException e)
            {
                String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
                throw new Exception(ioExceptionMessage);
            }
        }
        catch (Exception e)
        {
            listener.error(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
            throw e;
        }

        return schedulesIdTitleHashMap;
    }

    public UUID runSchedule(
            AsyncHttpClient client,
            String controllerApiHttpAddress,
            String accessKey,
            UUID scheduleId,
            String scheduleTitle,
            TaskListener listener,
            LeapworkRun run
    ) throws Exception {

        String uri = String.format(Messages.RUN_SCHEDULE_URI, controllerApiHttpAddress, scheduleId.toString());

        try {

            try
            {
                Response response = client.preparePut(uri).setHeader("AccessKey",accessKey).setBody("").execute().get();

                switch (response.getStatusCode()) {
                    case 200:
                        String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, scheduleTitle, scheduleId);
                        listener.getLogger().println(Messages.SCHEDULE_CONSOLE_LOG_SEPARATOR);
                        listener.getLogger().println(successMessage);
                        JsonParser parser = new JsonParser();
                        JsonObject jsonRunObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                        JsonElement jsonRunId = jsonRunObject.get("RunId");
                        String runIdStr = Utils.defaultStringIfNull(jsonRunId);
                        UUID runId = UUID.fromString(runIdStr);
                        return runId;

                    case 400:
                        String errorMessage400 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage400 += String.format("\n%1$s", Messages.INVALID_VARIABLE_KEY_NAME);
                        throw new Exception(errorMessage400);

                    case 401:
                        String errorMessage401 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage401 += String.format("\n%1$s", Messages.INVALID_ACCESS_KEY);
                        throw new Exception(errorMessage401);

                    case 404:
                        String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage404);

                    case 446:
                        String errorMessage446 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage446 += String.format("\n%1$s", Messages.NO_DISK_SPACE);
                        throw new Exception(errorMessage446);

                    case 455:
                        String errorMessage455 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage455 += String.format("\n%1$s", Messages.DATABASE_NOT_RESPONDING);
                        throw new Exception(errorMessage455);

                    case 500:
                        String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                        throw new Exception(errorMessage500);

                    default:
                        String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        throw new Exception(errorMessage);
                }
            }
            catch (ConnectException | UnknownHostException e)
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getMessage());
                listener.error(connectionErrorMessage);
                return null;
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getCause().getMessage());
                    listener.error(connectionErrorMessage);
                    return null;
                }
                else
                {
                    String executionExceptionMessage = String.format(Messages.EXECUTION_EXCEPTION, e.getMessage());
                    throw new Exception(executionExceptionMessage);
                }
            }
            catch (IOException e)
            {
                String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
                throw new Exception(ioExceptionMessage);
            }
            catch (Exception e)
            {
                throw e;
            }
        }
        catch (InterruptedException e)
        {
            throw new Exception(e.getMessage());
        }
        catch (Exception e)
        {
            String errorMessage = String.format(Messages.SCHEDULE_RUN_FAILURE, scheduleTitle, scheduleId);
            listener.error(errorMessage);
            listener.error(e.getMessage());
            listener.error(Messages.PLEASE_CONTACT_SUPPORT);
            run.setError(String.format("%1$s\n%2$s", errorMessage, e.getMessage()));
            run.incErrors();
            return null;
        }

    }

    public boolean stopRun(String controllerApiHttpAddress, UUID runId, String scheduleTitle,String accessKey ,final TaskListener listener)
    {
        boolean isSuccessfullyStopped = false;

        listener.error(String.format(Messages.STOPPING_RUN,scheduleTitle,runId));
        String uri = String.format(Messages.STOP_RUN_URI, controllerApiHttpAddress, runId.toString());
        try(AsyncHttpClient client = new AsyncHttpClient())
        {

            Response response = client.preparePut(uri).setBody("").setHeader("AccessKey",accessKey).execute().get();
            client.close();

            switch (response.getStatusCode())
            {
                case 200:
                    JsonParser parser = new JsonParser();
                    JsonObject jsonStopRunObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                    JsonElement jsonStopSuccessfull = jsonStopRunObject.get("OperationCompleted");
                    isSuccessfullyStopped = Utils.defaultBooleanIfNull(jsonStopSuccessfull,false);
                    if(isSuccessfullyStopped)
                        System.err.println(String.format(Messages.STOP_RUN_SUCCESS,scheduleTitle,runId.toString()));
                    else
                        System.err.println(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));
                    break;

                case 401:
                    String errorMessage401 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage401 += String.format("\n%1$s", Messages.INVALID_ACCESS_KEY);
                    throw new Exception(errorMessage401);

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_RUN_WAS_FOUND,  runId,scheduleTitle));
                    throw new Exception(errorMessage404);

                case 446:
                    String errorMessage446 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage446 += String.format("\n%1$s", Messages.NO_DISK_SPACE);
                    throw new Exception(errorMessage446);

                case 455:
                    String errorMessage455 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage455 += String.format("\n%1$s", Messages.DATABASE_NOT_RESPONDING);
                    throw new Exception(errorMessage455);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);
                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);

            }
        } catch (Exception e)
        {
            listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));
            listener.error(e.getMessage());
        }
        finally
        {
            return isSuccessfullyStopped;
        }

    }

    public void createJUnitReport(String JUnitReportFilePath,final TaskListener listener, RunCollection buildResult) throws Exception {
        try
        {
            File reportFile = new File(JUnitReportFilePath);
            if(!reportFile.exists()) reportFile.createNewFile();

            try(StringWriter writer = new StringWriter())
            {
                JAXBContext context = JAXBContext.newInstance(RunCollection.class);

                Marshaller m = context.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                //m.setProperty(Marshaller.JAXB_ENCODING,"Unicode");
                m.marshal(buildResult, writer);

                try(StringWriter formattedWriter  =  new StringWriter())
                {
                    formattedWriter.append(writer.getBuffer().toString().replace("&amp;#xA;","&#xA;"));
                    try (PrintStream out = new PrintStream(new FileOutputStream(reportFile.getAbsolutePath()))) {
                        out.print(formattedWriter);
                        out.close();
                    }
                }

            }
        }
        catch (FileNotFoundException e) {
            listener.error(Messages.REPORT_FILE_NOT_FOUND);
            throw new Exception(e);
        } catch (IOException e) {
            listener.error(Messages.REPORT_FILE_CREATION_FAILURE);
            throw new Exception(e);
        } catch (JAXBException e) {
            listener.error(Messages.REPORT_FILE_CREATION_FAILURE);
            throw new Exception(e);
        }
    }

    public String getRunStatus(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {

        String uri = String.format(Messages.GET_RUN_STATUS_URI, controllerApiHttpAddress, runId.toString());

        try
        {
            Response response = client.prepareGet(uri).setHeader("AccessKey",accessKey).execute().get();

            switch (response.getStatusCode())
            {
                case 200:
                    JsonParser parser = new JsonParser();
                    JsonObject runStatusObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                    JsonElement jsonRunStatus = runStatusObject.get("Status");
                    String runStatus = Utils.defaultStringIfNull(jsonRunStatus, "Queued");
                    return runStatus;

                case 401:
                    String errorMessage401 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage401 += String.format("\n%1$s", Messages.INVALID_ACCESS_KEY);
                    throw new Exception(errorMessage401);

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_RUN, runId));
                    throw new Exception(errorMessage404);

                case 455:
                    String errorMessage455 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage455 += String.format("\n%1$s", Messages.DATABASE_NOT_RESPONDING);
                    throw new Exception(errorMessage455);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);

            }
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    public List<UUID> getRunRunItems(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {
        String uri = String.format(Messages.GET_RUN_ITEMS_IDS_URI, controllerApiHttpAddress, runId.toString());

        try
        {
            Response response = client.prepareGet(uri).setHeader("AccessKey",accessKey).execute().get();

            switch (response.getStatusCode())
            {
                case 200:

                    JsonParser parser = new JsonParser();
                    JsonObject jsonRunItemsObject = parser.parse(response.getResponseBody()).getAsJsonObject();
                    JsonElement jsonRunItemsElement = jsonRunItemsObject.get("RunItemIds");

                    List<UUID> runItems = new ArrayList<>();

                    if(jsonRunItemsElement != null)
                    {
                        JsonArray jsonRunItems = jsonRunItemsElement.getAsJsonArray();
                        for(int i = 0; i < jsonRunItems.size(); i++)
                        {
                            UUID runItemId = UUID.fromString(jsonRunItems.get(i).getAsString());
                            runItems.add(runItemId);
                        }
                    }

                    return runItems;

                case 401:
                    String errorMessage401 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage401 += String.format("\n%1$s", Messages.INVALID_ACCESS_KEY);
                    throw new Exception(errorMessage401);

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_RUN, runId));
                    throw new Exception(errorMessage404);

                case 446:
                    String errorMessage446 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage446 += String.format("\n%1$s", Messages.NO_DISK_SPACE);
                    throw new Exception(errorMessage446);

                case 455:
                    String errorMessage455 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage455 += String.format("\n%1$s", Messages.DATABASE_NOT_RESPONDING);
                    throw new Exception(errorMessage455);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);

            }
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    public RunItem getRunItem(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey,  UUID runItemId, String scheduleTitle, final TaskListener listener) throws Exception {

        String uri = String.format(Messages.GET_RUN_ITEM_URI, controllerApiHttpAddress, runItemId.toString());

        try {
            Response response = client.prepareGet(uri).setHeader("AccessKey", accessKey).execute().get();

            switch (response.getStatusCode()) {
                case 200:

                    JsonParser parser = new JsonParser();
                    JsonObject jsonRunItem = parser.parse(response.getResponseBody()).getAsJsonObject();
                    parser = null;

                    //FlowInfo
                    JsonElement jsonFlowInfo = jsonRunItem.get("FlowInfo");
                    JsonObject flowInfo = jsonFlowInfo.getAsJsonObject();
                    JsonElement jsonFlowId = flowInfo.get("FlowId");
                    UUID flowId = Utils.defaultUuidIfNull(jsonFlowId, UUID.randomUUID());
                    JsonElement jsonFlowTitle = flowInfo.get("FlowTitle");
                    String flowTitle = Utils.defaultStringIfNull(jsonFlowTitle);
                    JsonElement jsonFlowStatus = flowInfo.get("Status");
                    String flowStatus = Utils.defaultStringIfNull(jsonFlowStatus, "NoStatus");

                    //EnvironmentInfo
                    JsonElement jsonEnvironmentInfo = jsonRunItem.get("EnvironmentInfo");
                    JsonObject environmentInfo = jsonEnvironmentInfo.getAsJsonObject();
                    JsonElement jsonEnvironmentId = environmentInfo.get("EnvironmentId");
                    UUID environmentId = Utils.defaultUuidIfNull(jsonEnvironmentId, UUID.randomUUID());
                    JsonElement jsonEnvironmentTitle = environmentInfo.get("EnvironmentTitle");
                    String environmentTitle = Utils.defaultStringIfNull(jsonEnvironmentTitle);
                    JsonElement jsonEnvironmentConnectionType = environmentInfo.get("ConnectionType");
                    String environmentConnectionType = Utils.defaultStringIfNull(jsonEnvironmentConnectionType, "Not defined");

                    JsonElement jsonRunId = jsonRunItem.get("AutomationRunId");
                    UUID runId = Utils.defaultUuidIfNull(jsonRunId, UUID.randomUUID());

                    String elapsed = defaultElapsedIfNull(jsonRunItem.get("Elapsed"));
                    double milliseconds = Utils.defaultDoubleIfNull(jsonRunItem.get("ElapsedSeconds"), 0);

                    RunItem runItem;

                    if(!flowStatus.contentEquals("Initializing") &&
                            !flowStatus.contentEquals("Connecting") &&
                            !flowStatus.contentEquals("Connected") &&
                            !flowStatus.contentEquals("Running") &&
                            !flowStatus.contentEquals("NoStatus"))
                    {
                        JsonArray jsonKeyframes = jsonRunItem.getAsJsonObject().get("Keyframes").getAsJsonArray();

                        synchronized (listener) {
                            listener.getLogger().println(Messages.CASE_CONSOLE_LOG_SEPARATOR);
                            StringBuilder fullKeyframes = new StringBuilder("");
                            listener.getLogger().println(String.format(Messages.CASE_INFORMATION, flowTitle, flowStatus, milliseconds));

                            for (JsonElement jsonKeyFrame : jsonKeyframes) {
                                String level = Utils.defaultStringIfNull(jsonKeyFrame.getAsJsonObject().get("Level"), "Trace");
                                if (!level.contentEquals("") && !level.contentEquals("Trace")) {
                                    String keyFrameTimeStamp = jsonKeyFrame.getAsJsonObject().get("Timestamp").getAsJsonObject().get("Value").getAsString();
                                    String keyFrameLogMessage = jsonKeyFrame.getAsJsonObject().get("LogMessage").getAsString();
                                    String keyFrame = String.format(Messages.CASE_STACKTRACE_FORMAT, keyFrameTimeStamp, keyFrameLogMessage);
                                    listener.getLogger().println(keyFrame);
                                    fullKeyframes.append(keyFrame);
                                    fullKeyframes.append("&#xA;");
                                }
                            }

                            fullKeyframes.append("Environment: ").append(environmentTitle).append("&#xA;");
                            listener.getLogger().println("Environment: " + environmentTitle);
                            fullKeyframes.append("Schedule: ").append(scheduleTitle);
                            listener.getLogger().println("Schedule: " + scheduleTitle);

                            runItem = new RunItem(flowTitle, flowStatus, milliseconds, fullKeyframes.toString(), scheduleTitle);
                        }
                    }
                    else
                        runItem = new RunItem(flowTitle, flowStatus, milliseconds, scheduleTitle);

                    return runItem;


                case 401:
                    String errorMessage401 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage401 += String.format("\n%1$s", Messages.INVALID_ACCESS_KEY);
                    throw new Exception(errorMessage401);

                case 404:
                    String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_RUN_ITEM_WAS_FOUND, runItemId, scheduleTitle));
                    throw new Exception(errorMessage404);

                case 446:
                    String errorMessage446 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage446 += String.format("\n%1$s", Messages.NO_DISK_SPACE);
                    throw new Exception(errorMessage446);

                case 455:
                    String errorMessage455 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage455 += String.format("\n%1$s", Messages.DATABASE_NOT_RESPONDING);
                    throw new Exception(errorMessage455);

                case 500:
                    String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    throw new Exception(errorMessage500);

                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);
            }
        } catch (Exception e) {

            throw e;
        }
    }

    private String defaultElapsedIfNull(JsonElement rawElapsed)
    {
        if(rawElapsed != null)
            return rawElapsed.getAsString();
        else
            return "00:00:00.0000000";

    }
}

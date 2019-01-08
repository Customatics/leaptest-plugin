package com.customatics.leaptest_plugin;

import com.customatics.leaptest_plugin.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
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
                listener.getLogger().println(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID,rawTimeDelay,defaultTimeDelay));
                return defaultTimeDelay;
            }
        }
        catch (Exception e)
        {
            listener.getLogger().println(String.format(Messages.TIME_DELAY_NUMBER_IS_INVALID,rawTimeDelay,defaultTimeDelay));
            return defaultTimeDelay;
        }
    }

    public boolean isDoneStatusAsSuccess(String doneStatusAs)
    {
        return doneStatusAs.contentEquals("Success");
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
    public String getWorkSpaceSafe(FilePath workspace, EnvVars env)
    {
        try {
            return workspace.toURI().getPath();
        }
        catch (Exception e)
        {
            return env.get(Messages.JENKINS_WORKSPACE_VARIABLE);
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

                            if(jsonSchedule.get("Type").getAsString().contentEquals("TemporaryScheduleInfo")) continue;

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
                                        listener.getLogger().println(String.format(Messages.SCHEDULE_DETECTED, Title, rawSchedule));
                                    }
                                    else
                                    {
                                        invalidSchedules.add(new InvalidSchedule(rawSchedule, String.format(Messages.SCHEDULE_DISABLED,Title,Id)));
                                        listener.getLogger().println(String.format(Messages.SCHEDULE_DISABLED,Title, Id));

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
                                        listener.getLogger().println(String.format(Messages.SCHEDULE_DETECTED,rawSchedule, Id));
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
                    StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                    OnFailedToGetScheduleTitleIdMap(null,errorMessage401.toString(),listener);

                case 500:
                    StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    OnFailedToGetScheduleTitleIdMap(null,errorMessage500.toString(),listener);

                default:
                    StringBuilder errorMessage = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    OnFailedToGetScheduleTitleIdMap(null,errorMessage.toString(),listener);

            }
        }
        catch (ConnectException | UnknownHostException e )
        {
            String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getMessage());
            OnFailedToGetScheduleTitleIdMap(e,connectionErrorMessage,listener);
        }
        catch (InterruptedException e)
        {
            String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
            OnFailedToGetScheduleTitleIdMap(e,interruptedExceptionMessage,listener);
        }
        catch (ExecutionException e)
        {
            if(e.getCause() instanceof ConnectException || e.getCause() instanceof  UnknownHostException)
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO, e.getCause().getMessage());
                OnFailedToGetScheduleTitleIdMap(e,connectionErrorMessage,listener);
            }
            else
            {
                String executionExceptionMessage = String.format(Messages.EXECUTION_EXCEPTION, e.getMessage());
                OnFailedToGetScheduleTitleIdMap(e,executionExceptionMessage,listener);
            }
        }
        catch (IOException e)
        {
            String ioExceptionMessage = String.format(Messages.IO_EXCEPTION, e.getMessage());
            OnFailedToGetScheduleTitleIdMap(e,ioExceptionMessage,listener);
        }

        return schedulesIdTitleHashMap;
    }

    private static HashMap<UUID,String> OnFailedToGetScheduleTitleIdMap(Exception e, String errorMessage, TaskListener listener) throws Exception {
        listener.error(Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT);
        if(errorMessage != null && errorMessage.isEmpty() == false)
            listener.error(errorMessage);
        else
            errorMessage = Messages.SCHEDULE_TITLE_OR_ID_ARE_NOT_GOT;
        if(e == null)
            e = new Exception(errorMessage);
        throw e;
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
                    StringBuilder errorMessage400 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage400,Messages.INVALID_VARIABLE_KEY_NAME);
                    return OnScheduleRunFailure(errorMessage400,run,scheduleId,listener);

                case 401:
                    StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                    return OnScheduleRunFailure(errorMessage401,run,scheduleId,listener);

                case 404:
                    StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage404,String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
                    return OnScheduleRunFailure(errorMessage404,run,scheduleId,listener);

                case 446:
                    StringBuilder errorMessage446 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage446,Messages.NO_DISK_SPACE);
                    return OnScheduleRunFailure(errorMessage446,run,scheduleId,listener);

                case 455:
                    StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                    return OnScheduleRunFailure(errorMessage455,run,scheduleId,listener);

                case 500:
                    StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    return OnScheduleRunFailure(errorMessage500,run,scheduleId,listener);

                default:
                    StringBuilder errorMessage = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    return OnScheduleRunFailure(errorMessage,run,scheduleId,listener);
            }
        }
        catch (ConnectException | UnknownHostException e)
        {
            OnScheduleRunConnectionFailure(e,listener);
        }
        catch (ExecutionException e)
        {
            if(e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException)
            {
                OnScheduleRunConnectionFailure(e,listener);
            }
            else
                throw e;
        }
        return null;
    }

    private static UUID OnScheduleRunFailure(StringBuilder errorMessage,LeapworkRun failedRun,UUID scheduleId, TaskListener listener)
    {
        listener.error(String.format(Messages.SCHEDULE_RUN_FAILURE, failedRun.getScheduleTitle(), scheduleId.toString()));
        listener.error(errorMessage.toString());
        failedRun.setError(errorMessage.toString());
        failedRun.incErrors();
        return null;
    }

    private static UUID OnScheduleRunConnectionFailure(Exception e, TaskListener listener)
    {
        listener.error(String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getCause().getMessage()));
        return null;
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
                        listener.error(String.format(Messages.STOP_RUN_SUCCESS,scheduleTitle,runId.toString()));
                    else
                        listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));
                    break;

                case 401:
                    listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    listener.error(Messages.INVALID_ACCESS_KEY);
                    listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 404:
                    listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    listener.error(String.format(Messages.NO_SUCH_RUN_WAS_FOUND,  runId,scheduleTitle));
                    listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 446:
                    listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    listener.error(Messages.NO_DISK_SPACE);
                    listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 455:
                    listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    listener.error(Messages.DATABASE_NOT_RESPONDING);
                    listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

                case 500:
                    listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    listener.error(Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                    listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));
                default:
                    listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                    listener.error(String.format(Messages.STOP_RUN_FAIL,scheduleTitle,runId.toString()));

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

    public void createJUnitReport(FilePath workspace, String JUnitReportFile, final TaskListener listener, RunCollection buildResult) throws Exception {
        try
        {
            FilePath reportFile;
            if(workspace.isRemote())
            {
                VirtualChannel channel = workspace.getChannel();
                reportFile = new FilePath(channel, Paths.get(workspace.toURI().getPath(), JUnitReportFile).toString());
                listener.getLogger().println(String.format(Messages.FULL_REPORT_FILE_PATH,reportFile.toURI().getPath()));
            }
            else
            {
                File file = new File(workspace.toURI().getPath(),JUnitReportFile);
                listener.getLogger().println(String.format(Messages.FULL_REPORT_FILE_PATH,file.getCanonicalPath()));
                if(!file.exists()) file.createNewFile();
                reportFile = new FilePath(file);
            }

            try(StringWriter writer = new StringWriter())
            {
                JAXBContext context = JAXBContext.newInstance(RunCollection.class);

                Marshaller m = context.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                m.marshal(buildResult, writer);

                try(StringWriter formattedWriter  =  new StringWriter())
                {
                    formattedWriter.append(writer.getBuffer().toString().replace("&amp;#xA;","&#xA;"));
                    reportFile.write(formattedWriter.toString(),"UTF-8");
                }

            }
        }
        catch (FileNotFoundException e) {
            listener.error(Messages.REPORT_FILE_NOT_FOUND);
            listener.error(e.getMessage());
            throw new Exception(e);
        } catch (IOException e) {
            listener.error(Messages.REPORT_FILE_CREATION_FAILURE);
            listener.error(e.getMessage());
            throw new Exception(e);
        } catch (JAXBException e) {
            listener.error(Messages.REPORT_FILE_CREATION_FAILURE);
            listener.error(e.getMessage());
            throw new Exception(e);
        }
    }

    public String getRunStatus(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {

        String uri = String.format(Messages.GET_RUN_STATUS_URI, controllerApiHttpAddress, runId.toString());

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
                StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage401, Messages.INVALID_ACCESS_KEY);
                throw new Exception(errorMessage401.toString());

            case 404:
                StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage404,String.format(Messages.NO_SUCH_RUN, runId));
                throw new Exception(errorMessage404.toString());

            case 455:
                StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                throw new Exception(errorMessage455.toString());

            case 500:
                StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                throw new Exception(errorMessage500.toString());

            default:
                String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                throw new Exception(errorMessage);
        }
    }

    public List<UUID> getRunRunItems(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runId) throws Exception {
        String uri = String.format(Messages.GET_RUN_ITEMS_IDS_URI, controllerApiHttpAddress, runId.toString());

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
                StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                throw new Exception(errorMessage401.toString());

            case 404:
                StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage404,String.format(Messages.NO_SUCH_RUN, runId));
                throw new Exception(errorMessage404.toString());

            case 446:
                StringBuilder errorMessage446 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage446,Messages.NO_DISK_SPACE);
                throw new Exception(errorMessage446.toString());

            case 455:
                StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                throw new Exception(errorMessage455.toString());

            case 500:
                StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                throw new Exception(errorMessage500.toString());

            default:
                String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                throw new Exception(errorMessage);

        }
    }

    public RunItem getRunItem(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey,  UUID runItemId, String scheduleTitle,boolean doneStatusAsSuccess, boolean writePassedKeyframes,  final TaskListener listener) throws Exception {

        String uri = String.format(Messages.GET_RUN_ITEM_URI, controllerApiHttpAddress, runItemId.toString());

        Response response = client.prepareGet(uri).setHeader("AccessKey", accessKey).execute().get();

        switch (response.getStatusCode()) {
            case 200:

                JsonParser parser = new JsonParser();
                JsonObject jsonRunItem = parser.parse(response.getResponseBody()).getAsJsonObject();

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

                RunItem runItem = new RunItem(flowTitle, flowStatus, milliseconds, scheduleTitle);

                if(flowStatus.contentEquals("Initializing") ||
                        flowStatus.contentEquals("Connecting")   ||
                        flowStatus.contentEquals("Connected")    ||
                        flowStatus.contentEquals("Running")      ||
                        flowStatus.contentEquals("NoStatus")     ||
                        (flowStatus.contentEquals("Passed") && !writePassedKeyframes) ||
                        (flowStatus.contentEquals("Done") && doneStatusAsSuccess && !writePassedKeyframes))
                {
                    return runItem;
                }
                else
                {
                    Failure keyframes = getRunItemKeyFrames(client,controllerApiHttpAddress,accessKey,runItemId,runItem,scheduleTitle,environmentTitle,listener);
                    runItem.failure = keyframes;
                    return runItem;
                }

            case 401:
                StringBuilder errorMessage401 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage401,Messages.INVALID_ACCESS_KEY);
                throw new Exception(errorMessage401.toString());

            case 404:
                StringBuilder errorMessage404 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage404,String.format(Messages.NO_SUCH_RUN_ITEM_WAS_FOUND, runItemId, scheduleTitle));
                throw new Exception(errorMessage404.toString());

            case 446:
                StringBuilder errorMessage446 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage446,Messages.NO_DISK_SPACE);
                throw new Exception(errorMessage446.toString());

            case 455:
                StringBuilder errorMessage455 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage455,Messages.DATABASE_NOT_RESPONDING);
                throw new Exception(errorMessage455.toString());

            case 500:
                StringBuilder errorMessage500 = new StringBuilder(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                appendLine(errorMessage500,Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                throw new Exception(errorMessage500.toString());

            default:
                String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                throw new Exception(errorMessage);
        }
    }

    public Failure getRunItemKeyFrames(AsyncHttpClient client, String controllerApiHttpAddress, String accessKey, UUID runItemId, RunItem runItem, String scheduleTitle, String environmentTitle, final TaskListener listener) throws Exception {

        String uri = String.format(Messages.GET_RUN_ITEM_KEYFRAMES_URI, controllerApiHttpAddress, runItemId.toString());

        Response response = client.prepareGet(uri).setHeader("AccessKey", accessKey).execute().get();

        switch (response.getStatusCode()) {
            case 200:

                JsonArray jsonKeyframes = TryParseKeyframeJson(response.getResponseBody(),listener);

                if(jsonKeyframes != null)
                {
                    listener.getLogger().println(Messages.CASE_CONSOLE_LOG_SEPARATOR);
                    listener.getLogger().println(String.format(Messages.CASE_INFORMATION, runItem.getCaseName(), runItem.getCaseStatus(), runItem.getElapsedTime()));
                    StringBuilder fullKeyframes = new StringBuilder("");

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

                    return new Failure(fullKeyframes.toString());
                }
                else
                {
                    listener.getLogger().println(Messages.FAILED_TO_PARSE_RESPONSE_KEYFRAME_JSON_ARRAY);
                    return new Failure(Messages.FAILED_TO_PARSE_RESPONSE_KEYFRAME_JSON_ARRAY);
                }

            case 401:
                listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                listener.error(Messages.INVALID_ACCESS_KEY);
                break;
            case 404:
                listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                listener.error(String.format(Messages.NO_SUCH_RUN_ITEM_WAS_FOUND, runItemId, scheduleTitle));
                break;

            case 446:
                listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                listener.error(Messages.NO_DISK_SPACE);
                break;

            case 455:
                listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                listener.error(Messages.DATABASE_NOT_RESPONDING);
                break;

            case 500:
                listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                listener.error(Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                break;

            default:
                listener.error(String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText()));
                break;
        }
        return null;
    }

    private static JsonArray TryParseKeyframeJson(String response,TaskListener listener)
    {
        JsonParser parser = new JsonParser();
        try {
            JsonArray jsonKeyframes = parser.parse(response).getAsJsonArray();
            return jsonKeyframes;
        }
        catch (Exception e)
        {
            listener.error(e.getMessage());
            return null;
        }
    }

    private String defaultElapsedIfNull(JsonElement rawElapsed)
    {
        if(rawElapsed != null)
            return rawElapsed.getAsString();
        else
            return "00:00:00.0000000";

    }

    private void appendLine(StringBuilder stringBuilder, String line)
    {
        if(stringBuilder != null)
        {
            stringBuilder.append(System.getProperty("line.separator"));
            stringBuilder.append(line);
        }
    }
}

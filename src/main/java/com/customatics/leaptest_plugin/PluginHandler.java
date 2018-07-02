package com.customatics.leaptest_plugin;

import com.customatics.leaptest_plugin.model.Case;
import com.customatics.leaptest_plugin.model.InvalidSchedule;
import com.customatics.leaptest_plugin.model.Schedule;
import com.customatics.leaptest_plugin.model.ScheduleCollection;
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
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

        return rawScheduleList;
    }

    public int getTimeDelay(String rawTimeDelay)
    {
        int defaultTimeDelay = 3;
        if(!rawTimeDelay.isEmpty() || !"".equals(rawTimeDelay))
            return Integer.parseInt(rawTimeDelay);
        else
            return defaultTimeDelay;
    }


    public HashMap<String, String> getSchedulesIdTitleHashMap(
            String leaptestAddress,
            ArrayList<String> rawScheduleList,
            final TaskListener listener,
            ScheduleCollection buildResult,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws  Exception {

        HashMap<String, String> schedulesIdTitleHashMap = new HashMap<>();

        String scheduleListUri = String.format(Messages.GET_ALL_AVAILABLE_SCHEDULES_URI, leaptestAddress);

        try {

            try(AsyncHttpClient client = new AsyncHttpClient())
            {
                Response response = client.prepareGet(scheduleListUri).execute().get();
                client.close();

                switch (response.getStatusCode()) {
                    case 200:
                        JsonParser parser = new JsonParser();
                        JsonArray jsonScheduleList = parser.parse(response.getResponseBody()).getAsJsonArray();

                        for (String rawSchedule : rawScheduleList) {
                            boolean successfullyMapped = false;
                            for (JsonElement jsonScheduleElement : jsonScheduleList) {
                                JsonObject jsonSchedule = jsonScheduleElement.getAsJsonObject();

                                String Id = Utils.defaultStringIfNull(jsonSchedule.get("Id"), "null Id");
                                String Title = Utils.defaultStringIfNull(jsonSchedule.get("Title"), "null Title");

                                if (Id.contentEquals(rawSchedule)) {
                                    if (!schedulesIdTitleHashMap.containsValue(Title)) {
                                        schedulesIdTitleHashMap.put(rawSchedule, Title);
                                        buildResult.Schedules.add(new Schedule(rawSchedule, Title));
                                        listener.getLogger().println(String.format(Messages.SCHEDULE_DETECTED, Title, rawSchedule));
                                    }
                                    successfullyMapped = true;
                                }

                                if (Title.contentEquals(rawSchedule)) {
                                    if (!schedulesIdTitleHashMap.containsKey(Id)) {
                                        schedulesIdTitleHashMap.put(Id, rawSchedule);
                                        buildResult.Schedules.add(new Schedule(Id, rawSchedule));
                                        listener.getLogger().println(String.format(Messages.SCHEDULE_DETECTED,rawSchedule, Id));
                                    }
                                    successfullyMapped = true;
                                }
                            }

                            if (!successfullyMapped)
                                invalidSchedules.add(new InvalidSchedule(rawSchedule, Messages.NO_SUCH_SCHEDULE));
                        }
                        break;

                    case 445:
                        String errorMessage445 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage445 += String.format("\n%1$s", Messages.LICENSE_EXPIRED);
                        throw new Exception(errorMessage445);

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

    public  RUN_RESULT runSchedule(
            String leaptestAddress,
            String scheduleId,
            String scheduleTitle,
            int currentScheduleIndex,
            final TaskListener listener,
            ScheduleCollection buildResult,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws Exception {

        RUN_RESULT isSuccessfullyRun = RUN_RESULT.RUN_FAIL;

        String uri = String.format(Messages.RUN_SCHEDULE_URI, leaptestAddress, scheduleId);

        try {

            try(AsyncHttpClient client = new AsyncHttpClient())
            {
                Response response = client.preparePut(uri).setBody("").execute().get();
                client.close();


                switch (response.getStatusCode()) {
                    case 204:
                        isSuccessfullyRun = RUN_RESULT.RUN_SUCCESS;
                        String successMessage = String.format(Messages.SCHEDULE_RUN_SUCCESS, scheduleTitle, scheduleId);
                        buildResult.Schedules.get(currentScheduleIndex).setId(currentScheduleIndex);
                        listener.getLogger().println(Messages.SCHEDULE_CONSOLE_LOG_SEPARATOR);
                        listener.getLogger().println(successMessage);
                        break;

                    case 404:
                        String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage404);

                    case 444:
                        String errorMessage444 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage444 += String.format("\n%1$s", String.format(Messages.SCHEDULE_HAS_NO_CASES, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage444);

                    case 445:
                        String errorMessage445 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage445 += String.format("\n%1$s", Messages.LICENSE_EXPIRED);
                        throw new InterruptedException(errorMessage445);

                    case 448:
                        String errorMessage448 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage448 += String.format("\n%1$s", String.format(Messages.CACHE_TIMEOUT_EXCEPTION, scheduleTitle, scheduleId));
                        isSuccessfullyRun = RUN_RESULT.RUN_REPEAT;
                        listener.error(errorMessage448);
                        break;

                    case 500:
                        String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage500 += String.format("\n%1$s", String.format(Messages.SCHEDULE_IS_RUNNING_NOW, scheduleTitle, scheduleId));
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
                return RUN_RESULT.RUN_REPEAT;
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getCause().getMessage());
                    listener.error(connectionErrorMessage);
                    return RUN_RESULT.RUN_REPEAT;
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
            buildResult.Schedules.get(currentScheduleIndex).setError(String.format("%1$s\n%2$s", errorMessage, e.getMessage()));
            buildResult.Schedules.get(currentScheduleIndex).incErrors();
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT, scheduleTitle, scheduleId), buildResult.Schedules.get(currentScheduleIndex).getError()));
            return RUN_RESULT.RUN_FAIL;
        }

        return isSuccessfullyRun;
    }

    public boolean stopSchedule(String leaptestAddress, String scheduleId, String scheduleTitle, final TaskListener listener)
    {
        boolean isSuccessfullyStopped = false;

        listener.error(String.format(Messages.STOPPING_SCHEDULE,scheduleTitle,scheduleId));
        String uri = String.format(Messages.STOP_SCHEDULE_URI, leaptestAddress, scheduleId);
        try(AsyncHttpClient client = new AsyncHttpClient())
        {

            Response response = client.preparePut(uri).setBody("").execute().get();
            client.close();


            switch (response.getStatusCode())
            {
                case 204:
                    listener.error(String.format(Messages.STOP_SUCCESS,scheduleTitle,scheduleId));
                    isSuccessfullyStopped = true;
                    break;
                default:
                    String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                    throw new Exception(errorMessage);

            }
        } catch (Exception e)
        {
            listener.error(String.format(Messages.STOP_FAIL,scheduleTitle,scheduleId));
            listener.error(e.getMessage());
        }
        finally
        {
            return isSuccessfullyStopped;
        }

    }

    public boolean getScheduleState(
            String leaptestAddress,
            String scheduleId,
            String scheduleTitle,
            int currentScheduleIndex,
            final TaskListener listener,
            String doneStatusValue,
            ScheduleCollection buildResult,
            ArrayList<InvalidSchedule> invalidSchedules
    ) throws InterruptedException {
        boolean isScheduleStillRunning = true;

        String uri = String.format(Messages.GET_SCHEDULE_STATE_URI, leaptestAddress, scheduleId);

        try {

            try(AsyncHttpClient client = new AsyncHttpClient())
            {

                Response response = client.prepareGet(uri).execute().get();
                client.close();

                switch (response.getStatusCode()) {
                    case 200:

                        JsonParser parser = new JsonParser();
                        JsonObject jsonState = parser.parse(response.getResponseBody()).getAsJsonObject();
                        parser = null;

                        String ScheduleId = jsonState.get("ScheduleId").getAsString();

                        if (isScheduleStillRunning(jsonState))
                            isScheduleStillRunning = true;
                        else {
                            isScheduleStillRunning = false;

                            /////////Schedule Info
                            JsonElement jsonLastRun = jsonState.get("LastRun");

                            JsonObject lastRun = jsonLastRun.getAsJsonObject();

                            String ScheduleTitle = lastRun.get("ScheduleTitle").getAsString();

                            buildResult.Schedules.get(currentScheduleIndex).setTime(parseExecutionTimeToSeconds(lastRun.get("ExecutionTotalTime")));

                            int passedCount = caseStatusCount("PassedCount", lastRun);
                            int failedCount = caseStatusCount("FailedCount", lastRun);
                            int doneCount = caseStatusCount("DoneCount", lastRun);

                            if (doneStatusValue.contentEquals("Failed"))
                                failedCount += doneCount;
                            else
                                passedCount += doneCount;


                            ///////////AutomationRunItemsInfo
                            JsonArray jsonAutomationRunItems = lastRun.get("AutomationRunItems").getAsJsonArray();

                            ArrayList<String> automationRunId = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                automationRunId.add(jsonAutomationRunItem.getAsJsonObject().get("AutomationRunId").getAsString());
                            ArrayList<String> statuses = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                statuses.add(jsonAutomationRunItem.getAsJsonObject().get("Status").getAsString());
                            ArrayList<String> elapsed = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                elapsed.add(defaultElapsedIfNull(jsonAutomationRunItem.getAsJsonObject().get("Elapsed")));
                            ArrayList<String> environments = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems)
                                environments.add(jsonAutomationRunItem.getAsJsonObject().get("Environment").getAsJsonObject().get("Title").getAsString());

                            ArrayList<String> caseTitles = new ArrayList<String>();
                            for (JsonElement jsonAutomationRunItem : jsonAutomationRunItems) {
                                String caseTitle = Utils.defaultStringIfNull(jsonAutomationRunItem.getAsJsonObject().get("Case").getAsJsonObject().get("Title"), "Null case Title");
                                if (caseTitle.contentEquals("Null case Title"))
                                    caseTitles.add(caseTitles.get(caseTitles.size() - 1));
                                else
                                    caseTitles.add(caseTitle);
                            }


                            for (int i = 0; i < jsonAutomationRunItems.size(); i++) {

                                //double seconds = jsonArray.getJSONObject(i).getDouble("TotalSeconds");
                                double seconds = parseExecutionTimeToSeconds(elapsed.get(i));

                                listener.getLogger().println(Messages.CASE_CONSOLE_LOG_SEPARATOR);

                                if (statuses.get(i).contentEquals("Failed") || (statuses.get(i).contentEquals("Done") && doneStatusValue.contentEquals("Failed")) || statuses.get(i).contentEquals("Error") || statuses.get(i).contentEquals("Cancelled")) {
                                    if (statuses.get(i).contentEquals("Error") || statuses.get(i).contentEquals("Cancelled"))
                                        failedCount++;

                                    JsonArray jsonKeyframes = jsonAutomationRunItems.get(i).getAsJsonObject().get("Keyframes").getAsJsonArray();

                                    //KeyframeInfo
                                    ArrayList<String> keyFrameTimeStamps = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameTimeStamps.add(jsonKeyFrame.getAsJsonObject().get("Timestamp").getAsString());
                                    ArrayList<String> keyFrameLogMessages = new ArrayList<String>();
                                    for (JsonElement jsonKeyFrame : jsonKeyframes)
                                        keyFrameLogMessages.add(jsonKeyFrame.getAsJsonObject().get("LogMessage").getAsString());


                                    listener.getLogger().println(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));

                                    StringBuilder fullKeyframes = new StringBuilder();
                                    int currentKeyFrameIndex = 0;

                                    for (JsonElement jsonKeyFrame : jsonKeyframes) {
                                        String level = Utils.defaultStringIfNull(jsonKeyFrame.getAsJsonObject().get("Level"), "");
                                        if (!level.contentEquals("") && !level.contentEquals("Trace")) {
                                            String keyFrame = String.format(Messages.CASE_STACKTRACE_FORMAT, keyFrameTimeStamps.get(currentKeyFrameIndex), keyFrameLogMessages.get(currentKeyFrameIndex));
                                            listener.getLogger().println(keyFrame);
                                            fullKeyframes.append(keyFrame);
                                            fullKeyframes.append("&#xA;");//fullKeyframes += '\n';
                                        }

                                        currentKeyFrameIndex++;
                                    }

                                    fullKeyframes.append("Environment: ").append(environments.get(i));
                                    listener.getLogger().println("Environment: " + environments.get(i));
                                    buildResult.Schedules.get(currentScheduleIndex).Cases.add(new Case(caseTitles.get(i), statuses.get(i), seconds, fullKeyframes.toString(), ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                                } else {
                                    listener.getLogger().println(String.format(Messages.CASE_INFORMATION, caseTitles.get(i), statuses.get(i), elapsed.get(i)));
                                    buildResult.Schedules.get(currentScheduleIndex).Cases.add(new Case(caseTitles.get(i), statuses.get(i), seconds, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                                }
                            }

                            buildResult.Schedules.get(currentScheduleIndex).setPassed(passedCount);
                            buildResult.Schedules.get(currentScheduleIndex).setFailed(failedCount);

                            if (buildResult.Schedules.get(currentScheduleIndex).getFailed() > 0)
                                buildResult.Schedules.get(currentScheduleIndex).setStatus("Failed");
                            else
                                buildResult.Schedules.get(currentScheduleIndex).setStatus("Passed");
                        }
                        break;

                    case 404:
                        String errorMessage404 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage404 += String.format("\n%1$s", String.format(Messages.NO_SUCH_SCHEDULE_WAS_FOUND, scheduleTitle, scheduleId));
                        throw new Exception(errorMessage404);

                    case 445:
                        String errorMessage445 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage445 += String.format("\n%1$s", Messages.LICENSE_EXPIRED);
                        throw new InterruptedException(errorMessage445);

                    case 448:
                        String errorMessage448 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage448 += String.format("\n%1$s", String.format(Messages.CACHE_TIMEOUT_EXCEPTION, scheduleTitle, scheduleId));
                        isScheduleStillRunning = true;
                        listener.error(errorMessage448);
                        break;

                    case 500:
                        String errorMessage500 = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        errorMessage500 += String.format("\n%1$s", Messages.CONTROLLER_RESPONDED_WITH_ERRORS);
                        throw new Exception(errorMessage500);

                    default:
                        String errorMessage = String.format(Messages.ERROR_CODE_MESSAGE, response.getStatusCode(), response.getStatusText());
                        buildResult.Schedules.get(currentScheduleIndex).setError(errorMessage);
                        throw new Exception(errorMessage);
                }
            }
            catch (NoRouteToHostException e)
            {
                String connectionLostErrorMessage = String.format(Messages.CONNECTION_LOST, e.getCause().getMessage());
                listener.error(connectionLostErrorMessage);
                return true;
            }
            catch (ConnectException | UnknownHostException e)
            {
                String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getMessage());
                listener.error(connectionErrorMessage);
                return true;
            }
            catch (ExecutionException e)
            {
                if(e.getCause() instanceof ConnectException || e.getCause() instanceof  UnknownHostException)
                {
                    String connectionErrorMessage = String.format(Messages.COULD_NOT_CONNECT_TO_BUT_WAIT, e.getCause().getMessage());
                    listener.error(connectionErrorMessage);
                    return true;
                }
                else if(e.getCause() instanceof NoRouteToHostException)
                {
                    String connectionLostErrorMessage = String.format(Messages.CONNECTION_LOST, e.getCause().getMessage());
                    listener.error(connectionLostErrorMessage);
                    return true;
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
          throw new InterruptedException(e.getMessage());
        }
        catch (Exception e)
        {
            String errorMessage = String.format(Messages.SCHEDULE_STATE_FAILURE, scheduleTitle, scheduleId);
            listener.error(errorMessage);
            listener.error(e.getMessage());
            listener.error(Messages.PLEASE_CONTACT_SUPPORT);
            buildResult.Schedules.get(currentScheduleIndex).setError(String.format("%1$s\n%2$s", errorMessage, e.getMessage()));
            buildResult.Schedules.get(currentScheduleIndex).incErrors();
            invalidSchedules.add(new InvalidSchedule(String.format(Messages.SCHEDULE_FORMAT, scheduleTitle, scheduleId), buildResult.Schedules.get(currentScheduleIndex).getError()));
            return false;
        }

        return isScheduleStillRunning;
    }

    public void createJUnitReport(String JUnitReportFilePath, final TaskListener listener, ScheduleCollection buildResult) throws Exception {
        try
        {
            File reportFile = new File(JUnitReportFilePath);
            if(!reportFile.exists()) reportFile.createNewFile();

            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(ScheduleCollection.class);

            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(buildResult, writer);

            StringWriter formattedWriter  =  new StringWriter();
            formattedWriter.append(writer.getBuffer().toString().replace("&amp;#xA;","&#xA;"));

            writer = null;

            try (PrintStream out = new PrintStream(new FileOutputStream(reportFile.getAbsolutePath()))) {
                out.print(formattedWriter);
                out.close();
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

    private boolean isScheduleStillRunning(JsonObject jsonState)
    {
        String status = Utils.defaultStringIfNull(jsonState.get("Status"), "Finished");

        return status.contentEquals("Running") || status.contentEquals("Queued");

    }

    private double parseExecutionTimeToSeconds(String rawExecutionTime)
    {
        String ExecutionTotalTime[] = rawExecutionTime.split(":|\\.");

        return  Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 +  //hours
                Double.parseDouble(ExecutionTotalTime[1]) * 60 +        //minutes
                Double.parseDouble(ExecutionTotalTime[2]) +             //seconds
                Double.parseDouble("0." + ExecutionTotalTime[3]);     //milliseconds
    }

    private double parseExecutionTimeToSeconds(JsonElement rawExecutionTime)
    {
        if(rawExecutionTime != null) {
            String ExecutionTotalTime[] = rawExecutionTime.getAsString().split(":|\\.");

            return Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 +  //hours
                    Double.parseDouble(ExecutionTotalTime[1]) * 60 +        //minutes
                    Double.parseDouble(ExecutionTotalTime[2]) +             //seconds
                    Double.parseDouble("0." + ExecutionTotalTime[3]);     //milliseconds
        }
        else
            return 0;
    }

    private int caseStatusCount(String statusName, JsonObject lastRun)
    {
        Integer temp =  Utils.defaultIntIfNull(lastRun.get(statusName), 0);
        return temp.intValue();
    }

    private String defaultElapsedIfNull(JsonElement rawElapsed)
    {
        if(rawElapsed != null)
            return rawElapsed.getAsString();
        else
            return "00:00:00.0000000";

    }
}

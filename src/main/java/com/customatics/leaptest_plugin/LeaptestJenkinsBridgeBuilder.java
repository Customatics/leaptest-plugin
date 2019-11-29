package com.customatics.leaptest_plugin;

import com.customatics.leaptest_plugin.model.LeapworkRun;
import com.customatics.leaptest_plugin.model.RunItem;
import com.customatics.leaptest_plugin.model.InvalidSchedule;
import com.customatics.leaptest_plugin.model.RunCollection;
import com.ning.http.client.AsyncHttpClient;
import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LeaptestJenkinsBridgeBuilder extends Builder  implements SimpleBuildStep {

    private final String leapworkHostname;
    private final String leapworkPort;
    private final String leapworkAccessKey;
    private String leapworkDelay;
    private String leapworkDoneStatusAs;
    private String leapworkReport;
    private final String leapworkSchIds;
    private final String leapworkSchNames;
    private boolean leapworkWritePassedFlowKeyFrames;
    private String leapworkScheduleVariables;

    private static PluginHandler pluginHandler = PluginHandler.getInstance();

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LeaptestJenkinsBridgeBuilder(String leapworkHostname,String leapworkPort, String leapworkAccessKey, String leapworkDelay, String leapworkDoneStatusAs, String leapworkReport, String leapworkSchNames, String leapworkSchIds/*, boolean leapworkWritePassedFlowKeyFrames */)
    {

        this.leapworkHostname = leapworkHostname;
        this.leapworkPort = leapworkPort;
        this.leapworkAccessKey = leapworkAccessKey;
        this.leapworkDelay = leapworkDelay;
        this.leapworkDoneStatusAs = leapworkDoneStatusAs;
        this.leapworkReport = leapworkReport;
        this.leapworkSchIds = leapworkSchIds;
        this.leapworkSchNames = leapworkSchNames;
        //this.leapworkWritePassedFlowKeyFrames = leapworkWritePassedFlowKeyFrames;
    }

    @DataBoundSetter
    public void setLeapworkReport(String report) { this.leapworkReport = report; }

    @DataBoundSetter
    public void setLeapworkDoneStatusAs(String doneStatusAs) {  this.leapworkDoneStatusAs = doneStatusAs;}

    @DataBoundSetter
    public void setLeapworkDelay(String leapworkDelay) {  this.leapworkDelay = leapworkDelay;}

    @DataBoundSetter
    public void setLeapworkWritePassedFlowKeyFrames(boolean leapworkWritePassedFlowKeyFrames) {  this.leapworkWritePassedFlowKeyFrames = leapworkWritePassedFlowKeyFrames;}

    @DataBoundSetter
    public void setLeapworkScheduleVariables(String leapworkScheduleVariables) {  this.leapworkScheduleVariables = leapworkScheduleVariables;}

    public String getLeapworkHostname()     { return leapworkHostname;}
    public String getLeapworkPort()         { return leapworkPort;}
    public String getLeapworkAccessKey()    { return leapworkAccessKey;}
    public String getLeapworkDelay()        { return leapworkDelay;}
    public String getLeapworkSchNames()     { return leapworkSchNames;}
    public String getLeapworkSchIds()       { return leapworkSchIds;}
    public String getLeapworkDoneStatusAs() { return leapworkDoneStatusAs;}
    public String getLeapworkReport()       { return leapworkReport;}
    public boolean isLeapworkWritePassedFlowKeyFrames() {return  leapworkWritePassedFlowKeyFrames;}
    public String getLeapworkScheduleVariables() {return leapworkScheduleVariables;}

    //@Override
    public void perform(final Run<?,?> build, FilePath workspace, Launcher launcher, final TaskListener listener) throws IOException, InterruptedException {

        EnvVars env = build.getEnvironment(listener);
        ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();

        printPluginInputs(listener, env.get(Messages.JENKINS_WORKSPACE_VARIABLE));

        this.leapworkReport = pluginHandler.getReportFileName(this.getLeapworkReport(),DescriptorImpl.DEFAULT_REPORT_NAME);

        ArrayList<String> rawScheduleList = pluginHandler.getRawScheduleList(leapworkSchIds, leapworkSchNames);
        String controllerApiHttpAddress = pluginHandler.getControllerApiHttpAdderess(leapworkHostname, leapworkPort, listener);

        int timeDelay = pluginHandler.getTimeDelay(leapworkDelay, listener);
        boolean isDoneStatusAsSuccess = pluginHandler.isDoneStatusAsSuccess(leapworkDoneStatusAs);
        boolean writePassedKeyframes = isLeapworkWritePassedFlowKeyFrames();

        String scheduleVariablesRequestPart = pluginHandler.getScheduleVariablesRequestPart(getLeapworkScheduleVariables(),listener);

        try( AsyncHttpClient mainClient = new AsyncHttpClient())
        {

            //Get schedule titles (or/and ids in case of pipeline)
            LinkedHashMap<UUID, String> schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(mainClient, leapworkAccessKey, controllerApiHttpAddress,rawScheduleList, listener,invalidSchedules);
            rawScheduleList.clear();//don't need that anymore

            if(schedulesIdTitleHashMap.isEmpty())
            {
                throw new Exception(Messages.NO_SCHEDULES);
            }

            List<UUID> schIdsList = new ArrayList<>(schedulesIdTitleHashMap.keySet());
            LinkedHashMap<UUID, LeapworkRun> resultsMap = new LinkedHashMap<>();

            ListIterator<UUID> iter = schIdsList.listIterator();
            while( iter.hasNext())
            {

                UUID schId = iter.next();
                String schTitle = schedulesIdTitleHashMap.get(schId);
                LeapworkRun run  = new LeapworkRun(schId.toString(),schTitle);

                UUID runId = pluginHandler.runSchedule(mainClient,controllerApiHttpAddress, leapworkAccessKey, schId, schTitle, listener,  run,scheduleVariablesRequestPart);
                if(runId != null)
                {
                    resultsMap.put(runId,run);
                    CollectScheduleRunResults(controllerApiHttpAddress,leapworkAccessKey,runId,schTitle,timeDelay,isDoneStatusAsSuccess,writePassedKeyframes,run,listener);
                }
                else
                    resultsMap.put(UUID.randomUUID(),run);

                iter.remove();

            }

            schIdsList.clear();
            schedulesIdTitleHashMap.clear();
            RunCollection buildResult = new RunCollection();

            if (invalidSchedules.size() > 0)
            {
                listener.getLogger().println(Messages.INVALID_SCHEDULES);

                for (InvalidSchedule invalidSchedule : invalidSchedules)
                {
                    listener.getLogger().println(String.format("%1$s: %2$s",invalidSchedule.getName(),invalidSchedule.getStackTrace()));
                    LeapworkRun notFoundSchedule = new LeapworkRun(invalidSchedule.getName());
                    RunItem invalidRunItem = new RunItem("Error","Error",0,invalidSchedule.getStackTrace(),invalidSchedule.getName());
                    notFoundSchedule.runItems.add(invalidRunItem);
                    notFoundSchedule.setError(invalidSchedule.getStackTrace());
                    buildResult.leapworkRuns.add(notFoundSchedule);
                }

            }

            List<LeapworkRun> resultRuns = new ArrayList<>(resultsMap.values());
            listener.getLogger().println(Messages.TOTAL_SEPARATOR);

            for (LeapworkRun run : resultRuns)
            {
                buildResult.leapworkRuns.add(run);

                buildResult.addFailedTests(run.getFailed());
                buildResult.addPassedTests(run.getPassed());
                buildResult.addErrors(run.getErrors());
                run.setTotal(run.getPassed() + run.getFailed());
                buildResult.addTotalTime(run.getTime());
                listener.getLogger().println(String.format(Messages.SCHEDULE_TITLE,run.getScheduleTitle(),run.getScheduleId()));
                listener.getLogger().println(String.format(Messages.CASES_PASSED,run.getPassed()));
                listener.getLogger().println(String.format(Messages.CASES_FAILED,run.getFailed()));
                listener.getLogger().println(String.format(Messages.CASES_ERRORED,run.getErrors()));
            }
            buildResult.setTotalTests(buildResult.getFailedTests() + buildResult.getPassedTests());

            listener.getLogger().println(Messages.TOTAL_SEPARATOR);
            listener.getLogger().println(String.format(Messages.TOTAL_CASES_PASSED,buildResult.getPassedTests()));
            listener.getLogger().println(String.format(Messages.TOTAL_CASES_FAILED,buildResult.getFailedTests()));
            listener.getLogger().println(String.format(Messages.TOTAL_CASES_ERROR,buildResult.getErrors()));

            pluginHandler.createJUnitReport(workspace,leapworkReport,listener,buildResult);

            if (buildResult.getErrors() > 0 ) {
                listener.getLogger().println("[ERROR] There were detected case(s) with status 'Error', 'Inconclusive', 'Timeout' or 'Cancelled'. Please check the report or console output for details. Set the build status to FAILURE as the results of the cases are not deterministic..");
                build.setResult(Result.FAILURE);
                listener.getLogger().println("");
            }
            if ( buildResult.getFailedTests() > 0 ) {
                if ( "Success".equals(this.leapworkDoneStatusAs) ){
                    listener.getLogger().println("There were test cases that had failures/issues, but the plugin has been configured to return: 'Success' in this case");
                    build.setResult(Result.SUCCESS);
                } else if ( "Unstable".equals(this.leapworkDoneStatusAs) ) {
                    listener.getLogger().println("There were test cases that had failures/issues, but the plugin has been configured to return: 'Unstable' in this case");
                    build.setResult(Result.UNSTABLE);
                }
            } else {
                listener.getLogger().println("No issues detected");
            }
            listener.getLogger().println(Messages.PLUGIN_SUCCESSFUL_FINISH);

        }
        catch (AbortException | InterruptedException e)
        {
            listener.error("ABORTED");
            build.setResult(Result.ABORTED);
            listener.error(Messages.PLUGIN_ERROR_FINISH);
        }
        catch (Exception e)
        {
            listener.error(Messages.PLUGIN_ERROR_FINISH);
            listener.error(e.getMessage());
            listener.error(Messages.PLEASE_CONTACT_SUPPORT);
            listener.error("FAILURE");
            build.setResult(Result.FAILURE);
        }


        return;
    }


    private static void CollectScheduleRunResults(String controllerApiHttpAddress, String accessKey, UUID runId, String scheduleName, int timeDelay,boolean isDoneStatusAsSuccess,boolean writePassedKeyframes, LeapworkRun resultRun,  final TaskListener listener) throws AbortException, InterruptedException {
        List<UUID> runItemsId = new ArrayList<>();
        Object waiter = new Object();
        //get statuses
        try(AsyncHttpClient client = new AsyncHttpClient())
        {
            boolean isStillRunning = true;

            do
            {
                synchronized (waiter)
                {
                    waiter.wait(timeDelay * 1000);//Time delay
                }

                List<UUID> executedRunItems = pluginHandler.getRunRunItems(client,controllerApiHttpAddress,accessKey,runId);
                executedRunItems.removeAll(runItemsId); //left only new


                for(ListIterator<UUID> iter = executedRunItems.listIterator(); iter.hasNext();)
                {
                    UUID runItemId = iter.next();
                    RunItem runItem = pluginHandler.getRunItem(client,controllerApiHttpAddress,accessKey,runItemId, scheduleName,isDoneStatusAsSuccess,writePassedKeyframes,listener );

                    String status = runItem.getCaseStatus();


                    resultRun.addTime(runItem.getElapsedTime());
                    switch (status)
                    {
                        case "NoStatus":
                        case "Initializing":
                        case "Connecting":
                        case "Connected":
                        case "Running":
                            iter.remove();
                            break;
                        case "Passed":
                            resultRun.incPassed();
                            resultRun.runItems.add(runItem);
                            resultRun.incTotal();
                            break;
                        case "Failed":
                            resultRun.incFailed();
                            resultRun.runItems.add(runItem);
                            resultRun.incTotal();
                            break;
                        case "Error":
                        case "Inconclusive":
                        case "Timeout":
                        case "Cancelled":
                            resultRun.incErrors();
                            resultRun.runItems.add(runItem);
                            resultRun.incTotal();
                            break;
                        case"Done":
                            resultRun.runItems.add(runItem);
                            if(isDoneStatusAsSuccess)
                                resultRun.incPassed();
                            else
                                resultRun.incFailed();
                            resultRun.incTotal();
                            break;

                    }

                }

                runItemsId.addAll(executedRunItems);

                String runStatus = pluginHandler.getRunStatus(client,controllerApiHttpAddress,accessKey,runId);
                if(runStatus.contentEquals("Finished"))
                {
                    List<UUID> allExecutedRunItems = pluginHandler.getRunRunItems(client,controllerApiHttpAddress,accessKey,runId);
                    if(allExecutedRunItems.size() > 0 && allExecutedRunItems.size() <= runItemsId.size())
                        isStillRunning = false;
                }

                if(isStillRunning)
                    listener.getLogger().println(String.format("The schedule status is already '%1$s' - wait a minute...", runStatus));

            }
            while (isStillRunning);

        }
        catch (AbortException | InterruptedException e)
        {
            Lock lock = new ReentrantLock();
            lock.lock();
            try
            {
                String interruptedExceptionMessage = String.format(Messages.INTERRUPTED_EXCEPTION, e.getMessage());
                listener.error(interruptedExceptionMessage);
                RunItem invalidItem = new RunItem("Aborted run","Cancelled",0,e.getMessage(),scheduleName);
                pluginHandler.stopRun(controllerApiHttpAddress,runId,scheduleName,accessKey, listener);
                resultRun.incErrors();
                resultRun.runItems.add(invalidItem);
            }
            finally {
                lock.unlock();
                throw e;
            }
        }
        catch (Exception e)
        {
            listener.error(e.getMessage());
            RunItem invalidItem = new RunItem("Invalid run","Error",0,e.getMessage(),scheduleName);
            resultRun.incErrors();
            resultRun.runItems.add(invalidItem);
        }
    }

    private void printPluginInputs(final TaskListener listener, String workspace)
    {
        listener.getLogger().println(Messages.INPUT_VALUES_MESSAGE);
        listener.getLogger().println(Messages.CASE_CONSOLE_LOG_SEPARATOR);
        listener.getLogger().println(String.format(Messages.INPUT_HOSTNAME_VALUE,getLeapworkHostname()));
        listener.getLogger().println(String.format(Messages.INPUT_PORT_VALUE,getLeapworkPort()));
        //listener.getLogger().println(String.format(Messages.INPUT_ACCESS_KEY_VALUE,getLeapworkAccessKey()));
        listener.getLogger().println(String.format(Messages.INPUT_REPORT_VALUE,getLeapworkReport()));
        listener.getLogger().println(String.format(Messages.INPUT_WORKSPACE_VALUE,workspace));
        listener.getLogger().println(String.format(Messages.INPUT_SCHEDULE_NAMES_VALUE,getLeapworkSchNames()));
        listener.getLogger().println(String.format(Messages.INPUT_SCHEDULE_IDS_VALUE,getLeapworkSchIds()));
        listener.getLogger().println(String.format(Messages.INPUT_DELAY_VALUE,getLeapworkDelay()));
        listener.getLogger().println(String.format(Messages.INPUT_DONE_VALUE,getLeapworkDoneStatusAs()));
        listener.getLogger().println(String.format(Messages.INPUT_WRITE_PASSED,isLeapworkWritePassedFlowKeyFrames()));
        listener.getLogger().println(String.format(Messages.INPUT_VARIABLES,getLeapworkScheduleVariables()));

    }

    @Override
    public  DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() { load();}

        public ListBoxModel doFillLeapworkDoneStatusAsItems(@QueryParameter("leapworkDoneStatusAs") String selection) {
            return new ListBoxModel(
                    new ListBoxModel.Option("Success", "Success",selection.matches("Success")),
                    new ListBoxModel.Option("Unstable", "Unstable",selection.matches("Unstable")),
                    new ListBoxModel.Option("Failed", "Failed",selection.matches("Failed"))
            );
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        public static final String DEFAULT_DELAY = "5";
        public static final String DEFAULT_REPORT_NAME = "report.xml";
        public static final boolean DEFAULT_WRITE_PASSED_FLOW_KEYFRAMES = false;

        public FormValidation doCheckLeapworkDelay (@QueryParameter("leapworkDelay") String delay){
            int temp;
            try {
                temp = Integer.parseInt(delay);

                if ( temp < 1 ){
                    return FormValidation.error("Entered number must be higher than 0");
                }
            } catch (NumberFormatException ex){
                return FormValidation.error("Invalid number");
            }
            return FormValidation.ok();
        }
        public String getDefaultLeapworkDelay() { return DEFAULT_DELAY; }
        public String getDefaultLeapworkReport() { return DEFAULT_REPORT_NAME; }

        public boolean getDefaultLeapworkWritePassedFlowKeyFrames() {return DEFAULT_WRITE_PASSED_FLOW_KEYFRAMES;}

        public String getDisplayName() {
            return Messages.PLUGIN_NAME;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }

    }



}


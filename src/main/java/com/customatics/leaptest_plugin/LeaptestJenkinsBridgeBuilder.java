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
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
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
    private final String leapworkDelay;
    private final String leapworkDoneStatusAs;
    private final String leapworkReport;
    private final String leapworkSchIds;
    private final String leapworkSchNames;

    private static PluginHandler pluginHandler = PluginHandler.getInstance();

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LeaptestJenkinsBridgeBuilder(String leapworkHostname,String leapworkPort, String leapworkAccessKey, String leapworkDelay, String leapworkDoneStatusAs, String leapworkReport, String leapworkSchNames, String leapworkSchIds )
    {

        this.leapworkHostname = leapworkHostname;
        this.leapworkPort = leapworkPort;
        this.leapworkAccessKey = leapworkAccessKey;
        this.leapworkDelay = leapworkDelay;
        this.leapworkDoneStatusAs = leapworkDoneStatusAs;
        this.leapworkReport = leapworkReport;
        this.leapworkSchIds = leapworkSchIds;
        this.leapworkSchNames = leapworkSchNames;
    }

    public String getLeapworkHostname()     { return leapworkHostname;}
    public String getLeapworkPort()         { return leapworkPort;}
    public String getLeapworkAccessKey()    { return leapworkAccessKey;}
    public String getLeapworkDelay()        { return leapworkDelay;}
    public String getLeapworkSchNames()     { return leapworkSchNames;}
    public String getLeapworkSchIds()       { return leapworkSchIds;}
    public String getLeapworkDoneStatusAs() { return leapworkDoneStatusAs;}
    public String getLeapworkReport()       { return leapworkReport;}

    //@Override
    public void perform(final Run<?,?> build, FilePath workspace, Launcher launcher, final TaskListener listener) throws IOException, InterruptedException {

        EnvVars env = build.getEnvironment(listener);
        HashMap<UUID, String> schedulesIdTitleHashMap = null; // Id-Title
        ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
        ArrayList<String> rawScheduleList = null;

        String junitReportPath = pluginHandler.getJunitReportFilePath(env.get(Messages.JENKINS_WORKSPACE_VARIABLE), leapworkReport);
        listener.getLogger().println(junitReportPath);
        env = null;


        rawScheduleList = pluginHandler.getRawScheduleList(leapworkSchIds, leapworkSchNames);
        String controllerApiHttpAddress = pluginHandler.getControllerApiHttpAdderess(leapworkHostname, leapworkPort, listener);

        int timeDelay = pluginHandler.getTimeDelay(leapworkDelay, listener);

        try( AsyncHttpClient mainClient = new AsyncHttpClient())
        {

            //Get schedule titles (or/and ids in case of pipeline)
            schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(mainClient, leapworkAccessKey, controllerApiHttpAddress,rawScheduleList, listener,invalidSchedules);
            rawScheduleList.clear();//don't need that anymore

            if(schedulesIdTitleHashMap.isEmpty())
            {
                throw new Exception(Messages.NO_SCHEDULES);
            }

            List<UUID> schIdsList = new ArrayList<>(schedulesIdTitleHashMap.keySet());
            HashMap<UUID, LeapworkRun> resultsMap = new HashMap<>();

            ListIterator<UUID> iter = schIdsList.listIterator();
            while( iter.hasNext())
            {

                UUID schId = iter.next();
                String schTitle = schedulesIdTitleHashMap.get(schId);
                LeapworkRun run  = new LeapworkRun(schTitle);

                UUID runId = pluginHandler.runSchedule(mainClient,controllerApiHttpAddress, leapworkAccessKey, schId, schTitle, listener,  run);
                if(runId != null)
                {
                    resultsMap.put(runId,run);
                    CollectScheduleRunResults(controllerApiHttpAddress, leapworkAccessKey,runId,schTitle,timeDelay,leapworkDoneStatusAs,run, listener);
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
            for (LeapworkRun run : resultRuns)
            {
                buildResult.leapworkRuns.add(run);

                buildResult.addFailedTests(run.getFailed());
                buildResult.addPassedTests(run.getPassed());
                buildResult.addErrors(run.getErrors());
                run.setTotal(run.getPassed() + run.getFailed());
                buildResult.addTotalTime(run.getTime());
            }
            buildResult.setTotalTests(buildResult.getFailedTests() + buildResult.getPassedTests());

            pluginHandler.createJUnitReport(junitReportPath,listener,buildResult);

            if (buildResult.getErrors() > 0 || buildResult.getFailedTests() > 0 || invalidSchedules.size() > 0) {
                listener.getLogger().println("FAILURE");
                build.setResult(Result.FAILURE);
            }
            else {
                listener.getLogger().println("SUCCESS");
                build.setResult(Result.SUCCESS);
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


    private static void CollectScheduleRunResults(String controllerApiHttpAddress, String accessKey, UUID runId, String scheduleName, int timeDelay,String doneStatusAs, LeapworkRun resultRun,  final TaskListener listener) throws AbortException, InterruptedException {
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
                    RunItem runItem = pluginHandler.getRunItem(client,controllerApiHttpAddress,accessKey,runItemId, scheduleName,listener );

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
                            if(doneStatusAs.contentEquals("Success"))
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

    @Override
    public  DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() { load();}

        public ListBoxModel doFillSelectionStatus(@QueryParameter String status) {
            return new ListBoxModel(new ListBoxModel.Option("Success", "Success", status.matches("Success") ),
                    new ListBoxModel.Option("Success", "Success", status.matches("Success") ),
                    new ListBoxModel.Option("Failed", "Failed", status.matches("Failed") ));
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

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


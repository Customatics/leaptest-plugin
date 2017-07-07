package com.customatics.leaptest_plugin;

import com.customatics.leaptest_plugin.model.Case;
import com.customatics.leaptest_plugin.model.InvalidSchedule;
import com.customatics.leaptest_plugin.model.Schedule;
import com.customatics.leaptest_plugin.model.ScheduleCollection;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


public class LeaptestJenkinsBridgeBuilder extends Builder  implements SimpleBuildStep {


    private final String address;
    private final String delay;
    private final String doneStatusAs;
    private final String report;
    private final String schIds;
    private final String schNames;

    private static PluginHandler pluginHandler = PluginHandler.getInstance();

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LeaptestJenkinsBridgeBuilder( String address, String delay, String doneStatusAs, String report, String schNames, String schIds )
    {

        this.address = address;
        this.delay = delay;
        this.doneStatusAs = doneStatusAs;
        this.report = report;
        this.schIds = schIds;
        this.schNames = schNames;
    }


    public String getDelay()        { return delay;}
    public String getAddress()      { return address;}
    public String getSchNames()     { return schNames;}
    public String getSchIds()       { return schIds;}
    public String getDoneStatusAs() { return doneStatusAs;}
    public String getReport()       { return  report;}

    //@Override
    public void perform(final Run<?,?> build, FilePath workspace, Launcher launcher, final TaskListener listener) throws IOException, InterruptedException {

        EnvVars env = build.getEnvironment(listener);
        HashMap<String, String> schedulesIdTitleHashMap = null; // Id-Title
        ArrayList<InvalidSchedule> invalidSchedules = new ArrayList<>();
        ScheduleCollection buildResult = new ScheduleCollection();
        ArrayList<String> rawScheduleList = null;


        String junitReportPath = pluginHandler.getJunitReportFilePath(env.get(Messages.JENKINS_WORKSPACE_VARIABLE), getReport());
        listener.getLogger().println(junitReportPath);
        env = null;

        rawScheduleList = pluginHandler.getRawScheduleList(getSchIds(),getSchNames());

        int timeDelay = pluginHandler.getTimeDelay(getDelay());

        try
        {    //Get schedule titles (or/and ids in case of pipeline)
            schedulesIdTitleHashMap = pluginHandler.getSchedulesIdTitleHashMap(getAddress(),rawScheduleList,listener,buildResult,invalidSchedules);
            rawScheduleList = null;                                        //don't need that anymore


            int currentScheduleIndex = 0;
            for (HashMap.Entry<String,String> schedule : schedulesIdTitleHashMap.entrySet())
            {

                if (pluginHandler.runSchedule(getAddress(),schedule, currentScheduleIndex, listener, buildResult, invalidSchedules)) // if schedule was successfully run
                {
                    boolean isStillRunning = true;

                    do
                    {
                        Thread.sleep(timeDelay * 1000); //Time delay
                        isStillRunning = pluginHandler.getScheduleState(getAddress(),schedule,currentScheduleIndex, getDoneStatusAs(), listener,buildResult, invalidSchedules);
                    }
                    while (isStillRunning);
                }

                currentScheduleIndex++;
            }

            if (invalidSchedules.size() > 0)
            {
                listener.getLogger().println(Messages.INVALID_SCHEDULES);
                buildResult.Schedules.add(new Schedule(Messages.INVALID_SCHEDULES));

                for (InvalidSchedule invalidSchedule : invalidSchedules)
                {
                    listener.getLogger().println(invalidSchedule.getName());
                    buildResult.Schedules.get(buildResult.Schedules.size() - 1).Cases.add(new Case(invalidSchedule.getName(), "Failed", 0, invalidSchedule.getStackTrace(), "INVALID SCHEDULE"));
                }

            }

            for (Schedule schedule : buildResult.Schedules)
            {
                buildResult.addFailedTests(schedule.getFailed());
                buildResult.addPassedTests(schedule.getPassed());
                buildResult.addErrors(schedule.getErrors());
                schedule.setTotal(schedule.getPassed() + schedule.getFailed());
                buildResult.addTotalTime(schedule.getTime());
            }
            buildResult.setTotalTests(buildResult.getFailedTests() + buildResult.getPassedTests());

            pluginHandler.createJUnitReport(junitReportPath,listener,buildResult);

            if (buildResult.getErrors() > 0 || buildResult.getFailedTests() > 0 || invalidSchedules.size() > 0)
                build.setResult(Result.FAILURE);
            else
                build.setResult(Result.SUCCESS);

            listener.getLogger().println(Messages.PLUGIN_SUCCESSFUL_FINISH);
        }

        catch (IndexOutOfBoundsException e)
        {
            listener.error(Messages.NO_SCHEDULES_OR_WRONG_URL_ERROR);
            listener.error(e.getMessage());
        }
        catch (InterruptedException e) {
            listener.error(e.getMessage());
        }
        catch (Exception e)
        {
            listener.error(Messages.PLUGIN_ERROR_FINISH);
            listener.error(e.getMessage());
            listener.error(Messages.PLEASE_CONTACT_SUPPORT);
            build.setResult(Result.FAILURE);
        }

        return;
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


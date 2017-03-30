package com.customatics.leaptest_plugin;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
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
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class LeaptestJenkinsBridgeBuilder extends Builder implements SimpleBuildStep {

    private final String version;
    private final String address;
    private final String delay;
    private final String doneStatusAs;
    private final String report;
    private final String schIds;
    private final String schNames;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LeaptestJenkinsBridgeBuilder(String version, String address, String delay, String doneStatusAs, String report, String schNames, String schIds )
    {
        this.version = version;
        this.address = address;
        this.delay = delay;
        this.doneStatusAs = doneStatusAs;
        this.report = report;
        this.schIds = schIds;
        this.schNames = schNames;

    }


    public String getDelay() {
        return delay;
    }
    public String getAddress() {return address;}
    public String getSchNames(){return schNames;}
    public String getSchIds(){return schIds;}
    public String getVersion(){return version;}
    public String getDoneStatusAs(){return doneStatusAs;}
    public String getReport(){return  report;}

    private static String JsonToJenkins( String str, int current, final TaskListener listener, MutableBoolean isRunning,  String doneStatus, testsuites buildResult, HashMap<String, String> InValidSchedules)
    {

        String JenkinsMessage = "";


        org.json.JSONObject json = new org.json.JSONObject(str);

        String ScheduleId = json.getString("ScheduleId");


        if (json.optString("Status").equals("Running"))
        {
            isRunning.setValue(true);
        }
        else
        {
            isRunning.setValue(false);


            /////////Schedule Info
            org.json.JSONObject LastRun = json.optJSONObject("LastRun");

            if (LastRun != null)
            {
                String ScheduleTitle = json.optJSONObject("LastRun").getString("ScheduleTitle");

                String ExecutionTotalTime[] = json.getJSONObject("LastRun").getString("ExecutionTotalTime").split(":|\\.");
                buildResult.Schedules.get(current).Time(Double.parseDouble(ExecutionTotalTime[0]) * 60 * 60 + Double.parseDouble(ExecutionTotalTime[1]) * 60 + Double.parseDouble(ExecutionTotalTime[2]) + Double.parseDouble("0." + ExecutionTotalTime[3]));


                Integer temp;

                int PassedCount = 0;
                int FailedCount = 0;
                temp = json.getJSONObject("LastRun").optInt("FailedCount",0);
                if (!temp.equals(null)) {FailedCount = temp.intValue();}
                temp = json.getJSONObject("LastRun").optInt("PassedCount",0);
                if (!temp.equals(null)) {PassedCount = temp.intValue();}
                buildResult.Schedules.get(current).Passed(PassedCount);
                buildResult.Schedules.get(current).Failed(FailedCount);

                int DoneCount = 0;
                temp = json.getJSONObject("LastRun").optInt("DoneCount",0);
                if (temp > 0) {DoneCount = temp.intValue();}


                if (doneStatus.contains("Failed"))
                {
                    buildResult.Schedules.get(current).addFailed(DoneCount);
                }
                else
                {
                    buildResult.Schedules.get(current).addPassed(DoneCount);
                }

                ///////////AutomationRunItemInfo
                JSONArray jsonArray = json.getJSONObject("LastRun").getJSONArray("AutomationRunItems");

                ArrayList<String> AutomationRunId = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) AutomationRunId.add(jsonArray.getJSONObject(i).getString("AutomationRunId"));
                ArrayList<String> Status = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Status.add(jsonArray.getJSONObject(i).getString("Status"));
                ArrayList<String> Elapsed = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Elapsed.add(jsonArray.getJSONObject(i).getString("Elapsed"));


                ArrayList<String> CaseName = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++)
                {
                    String caseTitle = jsonArray.getJSONObject(i).getJSONObject("Case").optString("Title","null");
                    if(caseTitle.contains("null"))
                    {
                        CaseName.add(CaseName.get(CaseName.size() - 1));
                    }
                    else {
                        CaseName.add(caseTitle);
                    }
                }

                ArrayList<String> Environment = new ArrayList<String>();
                for (int i = 0;  i < jsonArray.length(); i++) Environment.add(jsonArray.getJSONObject(i).getJSONObject("Environment").getString("Title"));




                for (int i = 0; i < jsonArray.length(); i++)
                {

                    //double seconds = jsonArray.getJSONObject(i).getDouble("TotalSeconds");
                    String ElapsedTime[] = Elapsed.get(i).split(":|\\.");
                    double seconds  = Double.parseDouble(ElapsedTime[0]) * 60 * 60 + Double.parseDouble(ElapsedTime[1]) * 60 + Double.parseDouble(ElapsedTime[2]) + Double.parseDouble("0." + ElapsedTime[3]);
                    ElapsedTime = null;

                    if (Status.get(i).contains("Failed") || (Status.get(i).contains("Done") && doneStatus.contains("Failed")))
                    {
                        JSONArray keyframes = jsonArray.getJSONObject(i).getJSONArray("Keyframes");
                        //KeyframeInfo
                        ArrayList<String> KeyFrameTimeStamp = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameTimeStamp.add(keyframes.getJSONObject(j).getString("Timestamp"));
                        ArrayList<String> KeyFrameLogMessage = new ArrayList<String>();
                        for (int j = 0;  j < keyframes.length(); j++) KeyFrameLogMessage.add(keyframes.getJSONObject(j).getString("LogMessage"));

                        listener.getLogger().println(String.format("Case: %1$s | Status: %2$s | Elapsed: %3$s", CaseName.get(i), Status.get(i), Elapsed.get(i)));

                        String fullstacktrace = "";

                        for (int j = 0; j < keyframes.length(); j++)
                        {
                            String level =  ObjectUtils.firstNonNull(keyframes.getJSONObject(j).optString("Level"));
                            if (level.equals("") || level.contains("Trace")) { }
                            else
                            {
                                String stacktrace = String.format("%1$s - %2$s", KeyFrameTimeStamp.get(j),  KeyFrameLogMessage.get(j));
                                listener.getLogger().println(stacktrace);
                                fullstacktrace += stacktrace;
                                fullstacktrace += "&#xA;"; //fullstacktrace += '\n';
                            }
                        }
                        fullstacktrace += "Environment: " + Environment.get(i);
                        listener.getLogger().println("Environment: " + Environment.get(i));
                        buildResult.Schedules.get(current).Cases.add(new testcase(CaseName.get(i), Status.get(i), seconds, fullstacktrace, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                        keyframes = null;
                    }
                    else
                    {
                        listener.getLogger().println(String.format("Case: %1$s | Status: %2$s | Elapsed: %3$s", CaseName.get(i), Status.get(i), Elapsed.get(i)));
                        buildResult.Schedules.get(current).Cases.add(new testcase(CaseName.get(i), Status.get(i), seconds, ScheduleTitle/* + "[" + ScheduleId + "]"*/));
                    }
                }

                if (buildResult.Schedules.get(current).Failed() > 0)
                {
                    buildResult.Schedules.get(current).Status("Failed");
                }
                else
                {
                    buildResult.Schedules.get(current).Status("Passed");
                }
                jsonArray = null;
            }
            else
            {
                String errorMessage = String.format("Schedule [%1$s] has no cases! JSON: &#xA; %2$s", ScheduleId, str);
                buildResult.Schedules.get(current).Error(errorMessage);
                buildResult.Schedules.get(current).incErrors();
                listener.error(String.format("Schedule[%1$s] has no cases! JSON:\n %2$s",  ScheduleId,str));
                InValidSchedules.put(ScheduleId,errorMessage);
            }
        }

        return JenkinsMessage;
    }


    private static  void GetSchTitlesOrIds(String uri, ArrayList<String> scheduleInfo, final TaskListener listener, HashMap<String, String> schedules, testsuites buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {


            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();

            client = null;

            JSONArray jsonArray =  new JSONArray(response.getResponseBody()); response = null;



            for (int i = 0; i < scheduleInfo.size(); i++)
            {
                boolean success = false;
                for (int j = 0; j < jsonArray.length(); j++)
                {
                    if ( ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Id")).contentEquals(scheduleInfo.get(i)))
                    {
                        String title = ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Title"));


                        if (!schedules.containsValue(title))
                        {
                            schedules.put(scheduleInfo.get(i), title);
                            buildResult.Schedules.add(new testsuite(scheduleInfo.get(i), title));
                        }
                        success = true;
                    }
                    else if (ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Title")).contentEquals(scheduleInfo.get(i)))
                    {
                        String id = ObjectUtils.firstNonNull(jsonArray.getJSONObject(j).getString("Id"));

                        if (!schedules.containsKey(id))
                        {
                            schedules.put(id, scheduleInfo.get(i));
                            buildResult.Schedules.add(new testsuite(id, scheduleInfo.get(i)));
                        }
                        success = true;
                    }
                    else
                    {
                    }
                }

                if (!success)
                { InValidSchedules.put(scheduleInfo.get(i),"Tried to get schedule title or id! Check connection to your server and try again!");}
            }
            return ;
        }
        catch (InterruptedException e) {
            listener.error(e.getMessage());
        } catch (ExecutionException e) {
            listener.error(e.getMessage());
        } catch (IOException e) {
            listener.error(e.getMessage());
        }
        catch (Exception e)
        {
            listener.error(String.format("Tried to get schedule titles or id! Check connection to your server and try again!"));
        }
    }

    private static void RunSchedule(String uri, String schId, String schTitle, int current,final TaskListener listener, MutableBoolean successfullyLaunchedSchedule, testsuites buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.preparePut(uri).setBody("").execute().get();
            client = null;



            if (response.getStatusCode() != 204)          // 204 Response means correct schedule launching
            {


                String errormessage = String.format("Code: %1$s Status: %2$s!", response.getStatusCode(), response.getStatusText());
                listener.error(errormessage);
                buildResult.Schedules.get(current).Error(errormessage);
                throw new Exception();
            }
            else
            {

                successfullyLaunchedSchedule.setValue(true);
                String successmessage = String.format("Schedule: %1$s | Schedule Id: %2$s | Launched: SUCCESSFULLY", schTitle, schId);
                buildResult.Schedules.get(current).Id(current);
                listener.getLogger().println(successmessage);
            }

            return;
        }
         catch (InterruptedException e) {
            buildResult.Schedules.get(current).Error(e.getMessage());
            listener.error(e.getMessage());
        } catch (ExecutionException e) {
            buildResult.Schedules.get(current).Error(e.getMessage());
            listener.error(e.getMessage());
        } catch (IOException e) {
            buildResult.Schedules.get(current).Error(e.getMessage());
            listener.error(e.getMessage());
        }
        catch (Exception e){
            String errormessage = String.format("Failed to run %1$s[%2$s]! Check it at your Leaptest server or connection to your server and try again!",  schTitle, schId);
            listener.error(errormessage);
            buildResult.Schedules.get(current).Error(errormessage);
            buildResult.Schedules.get(current).incErrors();
            InValidSchedules.put(schId,buildResult.Schedules.get(current).Error());
            successfullyLaunchedSchedule.setValue(false);
        }

    }

    private static void GetScheduleState(String uri, String schId, String schTitle, int current, final TaskListener listener, MutableBoolean isRunning,  String doneStatus, testsuites buildResult, HashMap<String, String> InValidSchedules)
    {
        try
        {
            AsyncHttpClient client = new AsyncHttpClient();
            Response response = client.prepareGet(uri).execute().get();
            client = null;


            if(response.getStatusCode() != 200)
            {
                String errormessage = String.format("Code: %1$s Status: %2$s!", response.getStatusCode(), response.getStatusText());
                listener.error(errormessage);
                buildResult.Schedules.get(current).Error(errormessage);
                throw new Exception();
            }
            else
            {


                    JsonToJenkins( response.getResponseBody(), current, listener, isRunning,  doneStatus, buildResult, InValidSchedules);

            }
        }
        catch (InterruptedException e) {
            listener.error(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        } catch (ExecutionException e) {
            listener.error(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        } catch (IOException e) {
            listener.error(e.getMessage());
            buildResult.Schedules.get(current).Error(e.getMessage());
        }catch (Exception e)
        {
            String errorMessage = String.format("Tried to get %1$s[%2$s] state! Check connection to your server and try again!", schTitle, schId);
            buildResult.Schedules.get(current).Error(errorMessage);
            buildResult.Schedules.get(current).incErrors();
            listener.error(errorMessage);
            InValidSchedules.put(schId,buildResult.Schedules.get(current).Error());
        }
    }

    private static void CreateJunitReport(String reportPath, final TaskListener listener, testsuites buildResult)
    {
        try
        {

            File reportFile = new File(reportPath);
            if(!reportFile.exists()) reportFile.createNewFile();


            StringWriter writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(testsuites.class);

            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(buildResult, writer);

            StringWriter formattedWriter  =  new StringWriter();
            formattedWriter.append(writer.getBuffer().toString().replace("&amp;","&"));

            writer = null;

            try (PrintStream out = new PrintStream(new FileOutputStream(reportFile.getAbsolutePath()))) {
                out.print(formattedWriter);
                out.close();
            }



        }
        catch (FileNotFoundException e) {
            listener.error("Couldn't find report file! Wrong path! Press \"help\" button nearby \"report\" textbox! ");
            listener.error(e.getMessage());
        } catch (IOException e) {
            listener.error(e.getMessage());
        } catch (JAXBException e) {
            listener.error(e.getMessage());
        }

    }


    @Override
    public void perform(final Run<?,?> build, FilePath workspace, Launcher launcher, final TaskListener listener) throws IOException, InterruptedException {

        EnvVars env = build.getEnvironment(listener);
        HashMap<String, String> schedules = new HashMap<String, String>(); // Id-Title
        HashMap<String,String> InValidSchedules = new HashMap<>(); // Id-Stack trace
        MutableBoolean isRunning = new MutableBoolean(false);
        MutableBoolean successfullyLaunchedSchedule =  new MutableBoolean(false);
        testsuites buildResult = new testsuites();
        ArrayList<String> scheduleInfo = new ArrayList<String>();

        String report = getReport();

        if(report.isEmpty())
        {
            report = "report.xml";
        }

        if(!report.contains(".xml"))
        {
            report+=".xml";
        }

        String junitReportPath =  new File( System.getenv("JENKINS_HOME")+"\\workspace\\" +workspace.getName() + "\\" + report).getPath();

        String[] schidsArray = schIds.split("\n|, |,");//was "\n"
        String[] testsArray = schNames.split("\n|, |,");//was "\n"


        for(int i = 0; i < schidsArray.length; i++)
        {
            scheduleInfo.add(schidsArray[i]);
        }
        for(int i = 0; i < testsArray.length; i++)
        {
            scheduleInfo.add(testsArray[i]);
        }
        schidsArray = null;
        testsArray = null;


        String uri = getAddress() + "/api/v1/runSchedules";
        int timeDelay = 1;
        if(ObjectUtils.firstNonNull(delay) != null)
        {timeDelay = Integer.parseInt(delay);}

        try
        {
            String APIuri = getAddress() + "/api/v1/misc/version";


            GetSchTitlesOrIds(uri, scheduleInfo, listener,schedules, buildResult, InValidSchedules); //Get schedule titles (or/and ids in case of pipeline)
            scheduleInfo = null;                                        //don't need that anymore
            int index = 0;

            for (HashMap.Entry<String,String> schedule : schedules.entrySet())
            {

                String runUri = uri + "/" + schedule.getKey() + "/runNow";
                String stateUri = uri + "/state/" + schedule.getKey();



                RunSchedule(runUri, schedule.getKey(), schedule.getValue(), index, listener,successfullyLaunchedSchedule, buildResult, InValidSchedules); // Run schedule. In case of unsuccessfull run throws exception

                if (successfullyLaunchedSchedule.getValue()) // if schedule was successfully run
                {
                    do
                    {

                        Thread.sleep(timeDelay * 1000); //Time delay
                        GetScheduleState(stateUri, schedule.getKey(), schedule.getValue(), index, listener, isRunning,  getDoneStatusAs(), buildResult, InValidSchedules); //Get schedule state info

                    }
                    while (isRunning.getValue());
                }

                index++;
            }

            if (InValidSchedules.size() > 0)
            {
                listener.getLogger().println("INVALID SCHEDULES:");
                for (String invalidsch : InValidSchedules.keySet())
                {
                    listener.getLogger().println(invalidsch);
                }

                buildResult.Schedules.add(new testsuite("INVALID SCHEDULES"));

                ArrayList<String> invSch = new ArrayList<>(InValidSchedules.keySet());
                ArrayList<String> invSchStackTrace = new ArrayList<>(InValidSchedules.values());

               for(int i = 0; i < InValidSchedules.size();i++)
                {
                    buildResult.Schedules.get(buildResult.Schedules.size() - 1).Cases.add(new testcase(invSch.get(i), "Failed", 0, invSchStackTrace.get(i), "INVALID SCHEDULE"));
                    //buildResult.Schedules.get(buildResult.Schedules.size()- 1).incErrors();
                }

                invSch = null;
                invSchStackTrace = null;
            }



            for (testsuite sch : buildResult.Schedules)
            {
                buildResult.addFailedTests(sch.Failed());
                buildResult.addPassedTests(sch.Passed());
                buildResult.addErrors(sch.Errors());
                sch.Total(sch.Passed() + sch.Failed());
                buildResult.addTotalTime(sch.Time());
            }
            buildResult.TotalTests(buildResult.FailedTests() + buildResult.PassedTests());

            CreateJunitReport(junitReportPath, listener, buildResult);

            if (buildResult.Errors() > 0 || buildResult.FailedTests() > 0)
            {
                build.setResult(Result.FAILURE);
            }
            else
            {
                build.setResult(Result.SUCCESS);
            }
            listener.getLogger().println("Leaptest for Jenkins  plugin  successfully finished!");
        }

        catch (IndexOutOfBoundsException e)
        {
            listener.error("No Schedules or wrong url! Check connection to your server or schedules and try again!");
            listener.error(e.getMessage());
        }
        catch (InterruptedException e) {
            listener.error(e.getMessage());
        }
        catch (Exception e)
        {
            listener.error("Leaptest for Jenkins plugin finished with errors!");
            build.setResult(Result.FAILURE);
        }


        return;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public  DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }




    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {



        public DescriptorImpl() { load();}


        public ListBoxModel doFillSelectionVersion(@QueryParameter String version) {
            return new ListBoxModel(new ListBoxModel.Option("1.1.0", "1.1.0", version.matches("1.1.0") ),
                    new ListBoxModel.Option("1.1.0", "1.1.0", version.matches("1.1.0") ));

        }
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
            return "Leaptest Integration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }


    }

}


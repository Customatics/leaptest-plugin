# leaptest-plugin
This is Leaptest plugin for Jenkins (version 1.635 or later)

# More Details
Leaptest is a mighty automation testing system and now it can be used for running [smoke, functional, acceptance] tests, generating reports and a lot more in Jenkins. You can easily configure integration directly in Jenkins enjoying UI friendly configuration page with easy connection and test suites selection. 

# Features:
 - Setup and test Leaptest connection in few clicks
 - Run automated tests in your Jenkins build tasks
 - Automatically receive test results
 - Build status based tests results
 - Generate a xml report file in JUnit format
 - Write tests trace to build output log
 - Smart UI
 
# Installing
- Use maven 
- Command: mvn package 
- Or simply install hpi-file from the "target" folder: Manage Jenkins -> Manage Plugins -> Advanced -> Upload Plugin -> Choose that hpi-file -> Press Upload

# Instruction
1. Add Build "Leaptest for Jenkins" to your job.
2. Enter your Leaptest server address something like "http://win10-agent20:9000" or "http://localhost:9000".
3. Enter JUnit report file name. This file will be created at your job's working directory. If there is an xml file with the same name, it will be overwritten. By default it is "report.xml".
4. Enter time delay in seconds. When schedule is run, plugin will wait this time before trying to get schedule state. If schedule is still running, plugin will wait this time again. By default this value is 1 second.
5. Select how plugin should set "Done" status value: to Success or Failed.
6. Press button "Select Schedules" to get a list of all available schedules grouped by projects. Select schedules you want to run.
7. If your workspace folder path is not default (JENKINS_HOME\workspace),enter your full path here like {Your path to workspace folder}\workspace. Otherwise DO NOT enter anything!
8. Add Post-Build "Publish JUnit test result report" to your job. Enter JUnit report file name. It MUST be the same you've entered before!
9. Run your job and get results. Enjoy!

# Screenshots
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-1.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-2.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-3.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-4.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-5.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-6.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-7.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-8.png)
![ScreenShot](http://customatics.com/wp-content/uploads/2017/03/jenkins-9.png)


# Pipeline
This is an example script for pipeline:
 ```
node
{
    stage "Leaptest-Jenkins"

    step([$class: 'LeaptestJenkinsBridgeBuilder',
    version:"1.1.0",
    address:"http://win10-agent2:9000",
    delay:"5",
    doneStatusAs:"Success", //"Failed"
    report:"report.xml",
    schIds:"",//"9c3fa950-d1e8-4e12-bf17-ebc945defad5\ndb5c3a25-8eec-434c-8526-c1b2ef9c56f2",   // splitters: "\n" "," ", " 
    schNames:"Problem schedule, Open Applications"
    ]);
   step([$class: 'JUnitResultArchiver',
   testResults: 'report.xml']);
   
    if(currentBuild.result!="FAILURE")
    {
    echo "RESULT: ${currentBuild.result}  SUCCESS INFO"
    // do something else  
    }
    else
    {   stage("FAIL Stage")
        echo "RESULT: ${currentBuild.result}  FAIL INFO"
         // do something else
    }  
}
```


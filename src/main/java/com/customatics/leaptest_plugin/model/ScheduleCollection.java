package com.customatics.leaptest_plugin.model;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;



@XmlRootElement(name = "testsuites")
@XmlAccessorType(XmlAccessType.NONE)
public final class ScheduleCollection
{

    public int totalTests = 0;

    private int passedTests = 0;

    private int failedTests = 0;

    private int errors = 0;

    private int disabled = 0;

    private double totalTime = 0;

    @XmlElement(name = "testsuite")
    public ArrayList<Schedule> Schedules;


    public ScheduleCollection()
    {
        Schedules = new ArrayList<Schedule>();

        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        errors = 0;
        disabled = 0;
        totalTime = 0;
    }

    @XmlAttribute(name = "total")
    public int    getTotalTests()               { return  totalTests; }
    public void   setTotalTests(int total)      { totalTests = total; }

    @XmlAttribute(name = "tests")
    public int    getPassedTests()              { return passedTests; }
    public void   setPassedTests(int tests)     { passedTests = tests; }
    public void   addPassedTests (int tests)    { passedTests += tests; }

    @XmlAttribute(name = "failures")
    public int    getFailedTests()              { return failedTests; }
    public void   setFailedTests(int failures)  { failedTests = failures; }
    public void   addFailedTests (int failures) { failedTests += failures; }

    @XmlAttribute(name = "errors")
    public int    getErrors()                   { return  errors; }
    public void   setErrors(int errors)         { this.errors = errors; }
    public void   addErrors (int errors)        { this.errors += errors; }

    @XmlAttribute(name = "disabled")
    public int    getDisabled()                 { return disabled; }
    public void   setDisabled(int disabled)     { this.disabled = disabled; }

    @XmlAttribute(name = "time")
    public double getTotalTime()                { return totalTime; }
    public void   setTotalTime(double time)     { totalTime = time; }
    public void   addTotalTime (double time)    { totalTime += time; }


}




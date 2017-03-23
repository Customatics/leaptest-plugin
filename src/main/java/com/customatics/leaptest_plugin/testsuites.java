package com.customatics.leaptest_plugin;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;


@XmlRootElement(name = "testsuites")
@XmlAccessorType(XmlAccessType.NONE)
public class testsuites
{
    @XmlAttribute(name = "total")
    public int totalTests = 0;
    @XmlAttribute(name = "tests")
    private int passedTests = 0;
    @XmlAttribute(name = "failures")
    private int failedTests = 0;
    @XmlAttribute(name = "errors")
    private int errors = 0;
    @XmlAttribute(name = "disabled")
    private int disabled = 0;
    @XmlAttribute(name = "time")
    private double totalTime = 0;
    @XmlElement(name = "testsuite")
    public ArrayList<testsuite> Schedules;


    public testsuites()
    {
        Schedules = new ArrayList<testsuite>();

        totalTests = 0;
        passedTests = 0;
        failedTests = 0;
        errors = 0;
        disabled = 0;
        totalTime = 0;
    }


    public void TotalTests (int total) {totalTests = total;}
    public int  TotalTests(){return  totalTests;}

    public void PassedTests (int tests){passedTests = tests;}
    public int  PassedTests () {return passedTests;}
    public void  addPassedTests (int tests) {passedTests += tests;}

    public void FailedTests (int failures){failedTests = failures; }
    public int  FailedTests (){return failedTests;}
    public void addFailedTests (int failures){failedTests += failures; }

    public void Errors (int errors){ this.errors = errors; }
    public int Errors (){ return  errors; }
    public void addErrors (int errors){ this.errors += errors; }

    public void Disabled (int disabled){ this.disabled = disabled; }
    public int Disabled  () { return disabled; }

    public void TotalTime (double time){ totalTime = time; }
    public double TotalTime () { return totalTime; }
    public void addTotalTime (double time){ totalTime += time; }


}




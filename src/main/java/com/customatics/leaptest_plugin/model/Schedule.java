package com.customatics.leaptest_plugin.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name = "testsuite")
public final class Schedule
{
    private String scheduleTitle;

    private String scheduleId;

    private String status;

    private int total;

    private int passed ;

    private int failed ;

    private int errors ;

    private String error;

    private double time ;

    private int id;

    @XmlElement(name = "testcase")
    public ArrayList<Case> Cases;


    public Schedule() { Cases = new ArrayList<Case>(); }
    public Schedule(String title)
    {
        Cases = new ArrayList<Case>();
        scheduleTitle = title;
        failed = 0;
        passed = 0;
        errors = 0;
        time = 0;
    }
    public Schedule(String schid, String title)
    {
        Cases = new ArrayList<Case>();
        scheduleId = schid;
        scheduleTitle = title;
        failed = 0;
        passed = 0;
        errors = 0;
    }

    @XmlAttribute(name = "name")
    public String getScheduleTitle()            { return scheduleTitle;}
    public void   setScheduleTitle(String name) { scheduleTitle = name; }

    @XmlAttribute(name = "schId")
    public String getScheduleId()               { return  scheduleId;}
    public void   setScheduleId(String schId)   { scheduleId = schId; }

    @XmlAttribute(name = "status")
    public String getStatus()                   { return status; }
    public void   setStatus(String status)      { this.status = status; }

    @XmlAttribute(name = "tests")
    public int    getTotal()                    { return total; }
    public void   setTotal(int tests)           { total = tests; }

    @XmlAttribute(name = "passed")
    public int    getPassed()                   { return passed; }
    public void   setPassed(int passed)         { this.passed = passed; }
    public void   incPassed()                   { passed++; }
    public void   addPassed(int passed)         { this.passed += passed; }

    @XmlAttribute(name = "failures")
    public int    getFailed()                   { return failed; }
    public void   setFailed(int failures)       { failed = failures; }
    public void   incFailed()                   { failed++; }
    public void   addFailed(int failures)       { this.failed += failures; }

    @XmlAttribute(name = "errors")
    public int    getErrors()                   { return errors; }
    public void   setErrors(int errors)         { this.errors = errors; }
    public void   incErrors()                   { errors++; }

    @XmlAttribute(name = "error")
    public String getError()                    { return error; }
    public void   setError(String error)
    {
        if(this.error == null)
            this.error = error;
        else
            this.error += ("\n" + error);
    }

    @XmlAttribute(name = "time")
    public double getTime()                     { return time; }
    public void   setTime(double time)          { this.time = time; }

    @XmlAttribute(name = "id")
    public int    getId()                       { return id; }
    public void   setId(int id)                 { this.id = id; }

}

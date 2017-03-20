package com.customatics.leaptest;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name = "testsuite")
public class testsuite
{

    @XmlElement(name = "testcase")
    public ArrayList<testcase> Cases;
    @XmlAttribute(name = "name")
    private String scheduleTitle;
    @XmlAttribute(name = "schId")
    private String scheduleId;
    @XmlAttribute(name = "status")
    private String status;
    @XmlAttribute(name = "error")
    private String error;
    @XmlAttribute(name = "failures")
    private int failed ;
    @XmlAttribute(name = "passed")
    private int passed ;
    @XmlAttribute(name = "errors")
    private int errors ;
    @XmlAttribute(name = "time")
    private double time ;
    @XmlAttribute(name = "id")
    private int id;
    @XmlAttribute(name = "tests")
    private int total;



    public testsuite() { Cases = new ArrayList<testcase>(); }
    public testsuite(String title)
    {
        Cases = new ArrayList<testcase>();
        scheduleTitle = title;
        failed = 0;
        passed = 0;
        errors = 0;
        time = 0;
    }
    public testsuite(String schid, String title)
    {
        Cases = new ArrayList<testcase>();
        scheduleId = schid;
        scheduleTitle = title;
        failed = 0;
        passed = 0;
        errors = 0;
    }


    public void ScheduleTitle (String name) { scheduleTitle = name; }
    public String ScheduleTitle () { return scheduleTitle;}


    public void ScheduleId (String schId){ scheduleId = schId; }
    public String ScheduleId () { return  scheduleId;}


    public void Id (int id){ this.id = id; }
    public int Id () { return id;}


    public void Passed (int passed){ this.passed = passed; }
    public int  Passed (){ return passed; }
    public void incPassed(){ passed++;}
    public void addPassed(int passed){ this.passed += passed;}


    public void Failed (int failures){ failed = failures; }
    public int Failed () { return failed; }
    public void incFailed(){ failed++;}
    public void addFailed(int failures){ this.failed += failures;}


    public void Total (int tests){ total = tests; }
    public int Total  () { return total; }


    public void Errors (int errors){ this.errors = errors; }
    public int Errors (){return errors; }
    public void incErrors(){ errors++;}


    public void Status (String status){this.status = status; }
    public String Status() { return status; }


    public void Error (String error)
    { if(this.error == null)
        this.error = error;
    else
        this.error += ("\n" + error);

    }
    public String Error() { return error; }



    public void Time (double time){ this.time = time; }
    public double Time (){ return time; }


}

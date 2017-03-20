package com.customatics.leaptest;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "testcase")
public class testcase
{
    @XmlAttribute(name = "name")
    private String caseName;
    @XmlAttribute(name = "status")
    private String caseStatus;
    @XmlAttribute(name = "time")
    private double elapsedTime ;
    @XmlAttribute(name = "classname")
    private String classname;
    @XmlElement
    public failure failure;
    public testcase() { }

    public testcase(String caseTitle, String caseStatus, double elapsed, String schedule)
    {
        caseName = caseTitle;
        this.caseStatus = caseStatus;
        elapsedTime = elapsed;
        classname = schedule;
        failure = null;
    }

    public testcase(String caseTitle, String caseStatus, double elapsed, String stacktrace, String schedule)
    {
        caseName = caseTitle;
        this.caseStatus = caseStatus;
        elapsedTime = elapsed;
        failure = new failure(stacktrace);
        classname = schedule;
    }


    public void CaseName(String name) { caseName = name; }
    public String CaseName () { return  caseName; }


    public void CaseStatus (String status){ caseStatus = status; }
    public String CaseStatus (){ return  caseStatus;}


    public void ElapsedTime (double time){ elapsedTime = time; }
    public double ElapsedTime() { return elapsedTime; }


    public void Classname (String classname){ this.classname = classname; }
    public String Classname () { return classname; }



}

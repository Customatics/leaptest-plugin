package com.customatics.leaptest_plugin.model;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "failure")
public final class Failure
{

    private String message;

    private String type;

    public Failure() { }

    public Failure(String stacktrace)
    {
        message = stacktrace;
        type = " ";
    }

    @XmlAttribute(name = "message")
    public String getMessage()              { return message; }
    public void   setMessage(String message){ this.message = message; }

    @XmlAttribute(name = "type")
    public String getType()                 { return type; }
    public void setType(String type)        { this.type = type;}


}

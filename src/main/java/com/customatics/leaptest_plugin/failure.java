package com.customatics.leaptest_plugin;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "failure")
public class failure
{
    @XmlAttribute(name = "message")
    private String message;
    @XmlAttribute(name = "type")
    private String type;

    public failure() { }
    public failure(String stacktrace)
    {
        message = stacktrace;
        type = " ";
    }


    public void Message (String message){ this.message = message; }
    public String Message() { return message; }


    public void Type (String type){ this.type = type;}
    public String Type (){return type; }

}

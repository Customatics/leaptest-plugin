package com.customatics.leaptest;

public class ErrorCodeException extends Exception{

    public ErrorCodeException(Integer code, String status) {
        super("Code:" + code.toString() + " Status:" + status + " !");
    }
}

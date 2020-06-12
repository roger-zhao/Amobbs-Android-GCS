package com.evenbus;

/**
 * Created by Linjieqiang on 2016/1/29.
 */
public class LogMessageEvent {

    private int logLevel;
    private String message;

    public LogMessageEvent(int logLevel, String message) {
        this.logLevel = logLevel;
        this.message = message;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public String getMessage() {
        return message;
    }
}

package com.sruteesh.imgSync.logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sruteesh.imgSync.constants.Constants;

public class LoggerImpl implements Logger {
    private final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

    private String logContent;

    public String getLogContent() {
        return logContent;
    }

    public void setLogContent(String logContent) {
        this.logContent = logContent;
    }



    private String loggerID;
    
    
    public LoggerImpl() {
        loggerID = UUID.randomUUID().toString();
        logContent = "";
    }



    @Override
    public void log(LogType logType, String message, String moduleName) {
        logContent += System.currentTimeMillis() + " - " + this.loggerID;
        String type; 
        switch (logType){
            case INFO:
                type = "INFO";
                break;
            case WARNING:
                type = "WARNING";
                break;
            case ERROR:
                type = "ERROR";
                break;
            case DEBUG:
                type = "DEBUG";
                break;
            default:
                type = "DEBUG";
        }
        logContent += " - [" + type + "] - (" + moduleName + "): " + message + System.lineSeparator();
    }

    @Override
    public void finish() throws IOException{
        streamLogToS3();
    }
    
    private void streamLogToS3() throws IOException{
        byte[] logBytes = this.logContent.getBytes();
        InputStream logStream = new ByteArrayInputStream(logBytes);
        s3.putObject(new PutObjectRequest(Constants.S3_LOGGER_BUCKET, LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "_" + loggerID, logStream, null));
        logStream.close();
    }
}

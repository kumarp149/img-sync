package com.sruteesh.imgSync.logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
        if (Integer.valueOf(System.getenv("BYPASS_LOGS")) == 0){
            System.out.println(System.currentTimeMillis() + " - " + this.loggerID + " - [" + type + "] - (" + moduleName + "): " + message + System.lineSeparator());
        }
    }

    @Override
    public void finish() throws IOException{
        System.out.println("FINISHING LOGS");
        streamLogToS3();
    }

    @Override
    public void logTrace(Exception e, String MODULE){
        StringWriter stringWriter = new StringWriter();
        PrintWriter stackWriter = new PrintWriter(stringWriter);
        e.printStackTrace(stackWriter);
        stackWriter.close();
        log(LogType.ERROR, stringWriter.toString(), MODULE);
    }
    
    private void streamLogToS3() throws IOException{
        byte[] logBytes = this.logContent.getBytes();
        InputStream logStream = new ByteArrayInputStream(logBytes);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(this.logContent.length());
        s3.putObject(new PutObjectRequest(Constants.S3_LOGGER_BUCKET, Constants.S3_UPLOAD_PREFIX + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) + "_" + loggerID, logStream, metadata));
        logStream.close();
    }
}
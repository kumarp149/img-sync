package com.sruteesh.imgSync.s3api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
import com.sruteesh.imgSync.constants.Constants;
import com.sruteesh.imgSync.googledriveapi.entities.GDriveEntity;
import com.sruteesh.imgSync.logger.LogType;
import com.sruteesh.imgSync.logger.Logger;

public class S3WriteFromLinkImpl implements S3WriteFromLink {
    
    public void uploadToS3(GDriveEntity entity, String token, String filePath, Logger logger, AmazonS3 s3Client) throws IOException{
        final String MODULE = "S3WriteFromLink.uploadToS3";
        if (Instant.now().getEpochSecond() - Constants.TIMESTAMP > 840){
            logger.log(LogType.DEBUG, "ABOUT TO REACH THE EXECUTION TIME LIMIT", MODULE);
            return;
        }
        logger.log(LogType.DEBUG, "UPLOADING FILE: [" + Constants.FILE_INDEX + "]", MODULE);
        String objectHash = getObjectHash(filePath, entity.getName(), logger,s3Client);
        logger.log(LogType.DEBUG, "S3 OBJECT HASH: [" + objectHash + "] AND HASH IN GDRIVE: [" + entity.getSha256Checksum() + "]", MODULE);
        if ((objectHash == null) || (!objectHash.equals(entity.getSha256Checksum()))){
            int maxRetries = 5;
            long retryCount = 0;
            URL fileDownloadURL = new URL(Constants.DRIVE_API_URL + "/" + entity.getId() + "?" + "alt=media");
            HttpURLConnection connection = (HttpURLConnection) fileDownloadURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization","Bearer " + token);
            while(retryCount < maxRetries){
                try{
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK){
                        InputStream fileStream = connection.getInputStream();

                        ObjectMetadata metadata = new ObjectMetadata();
                        metadata.setContentLength(entity.getSize());

                        PutObjectRequest putObjectRequest = new PutObjectRequest(Constants.S3_BUCKET_NAME, filePath + "/" + entity.getName(), fileStream, metadata);
                        putObjectRequest.getRequestClientOptions().setReadLimit(getNumericFactorForStream(entity.getSize()) * 1024 * 1024);
                        s3Client.putObject(putObjectRequest);

                        logger.log(LogType.DEBUG, "OBJECT " + filePath + "/" + entity.getName() + " SUCCESSFULLY UPLOADED TO S3", MODULE);
                        
                        Tag objectHashTag = new Tag("hash", entity.getSha256Checksum());
                        List<Tag> objectTags = new ArrayList<>();
                        objectTags.add(objectHashTag);

                        ObjectTagging objectTagging = new ObjectTagging(objectTags);
                        SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(Constants.S3_BUCKET_NAME,filePath + "/" + entity.getName(),objectTagging);
                        s3Client.setObjectTagging(setObjectTaggingRequest);
                        logger.log(LogType.DEBUG, "TAG " + objectHashTag.toString() + " SUCCESSFULLY SET FOR THE OBJECT " + filePath + "/" + entity.getName(), MODULE);

                        fileStream.close();
                        Constants.FILE_INDEX += 1;
                    }
                    connection.disconnect();
                } catch(Exception e){
                    retryCount += 1;
                    logger.log(LogType.ERROR, "UPLOADING " + filePath + " FAILED DURING THE ATTEMPT: " + retryCount, MODULE);
                    try {
                        TimeUnit.MILLISECONDS.sleep(150+20*retryCount*(long)Math.pow(2, retryCount));
                    } catch (InterruptedException e1) {
                        logger.logTrace(e1, MODULE);
                        return;
                    }
                }
            } 
        } else{
            logger.log(LogType.DEBUG, "S3 OBJECT HASH MATCHED WITH THAT OF DRIVE HASH", MODULE);
            Constants.FILE_INDEX += 1;
        }
    }


    private String getObjectHash(String objectPrefix, String objectName, Logger logger,AmazonS3 s3Client){
        final String MODULE = "S3WriteFromLink.getObjectHash";
        try {
            GetObjectTaggingRequest objectTaggingRequest = new GetObjectTaggingRequest(Constants.S3_BUCKET_NAME, objectPrefix + "/" + objectName);
            GetObjectTaggingResult taggingResult = s3Client.getObjectTagging(objectTaggingRequest);
            List<Tag> objectTags = taggingResult.getTagSet();
            for (Tag tag : objectTags){
                if (tag.getKey().equals("hash")){
                    return tag.getValue();
                }
            }
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404){
                logger.log(LogType.DEBUG, "OBJECT " + objectPrefix + "/" + objectName + " IS NOT FOUND IN THE BUCKET " + Constants.S3_BUCKET_NAME, MODULE);
                return null;
            } else{
                logger.logTrace(e, MODULE);
            }
        } catch (SdkClientException e){
            logger.logTrace(e, MODULE);
        } catch (Exception e){
            logger.logTrace(e, MODULE);
        }
        return null;
    }
    private static int getNumericFactorForStream(long size){
        int megaBytes = (int) size/(1024*1024);
        return (megaBytes%4 == 0) ? megaBytes/4  : (megaBytes/4)+1;
    }
}

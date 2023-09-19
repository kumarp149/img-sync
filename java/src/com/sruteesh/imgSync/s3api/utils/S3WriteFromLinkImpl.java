package com.sruteesh.imgSync.s3api.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
import com.sruteesh.imgSync.constants.Constants;
import com.sruteesh.imgSync.googledriveapi.entities.GDriveEntity;
import com.sruteesh.imgSync.logger.LogType;
import com.sruteesh.imgSync.logger.Logger;

public class S3WriteFromLinkImpl implements S3WriteFromLink {
    private final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
    
    public void uploadToS3(GDriveEntity entity, String token, String filePath, Logger logger) throws IOException{
        final String MODULE = "S3WriteFromLink.uploadToS3";
        String objectHash = getObjectHash(filePath, entity.getName(), logger);
        logger.log(LogType.DEBUG, "S3 OBJECT HASH: [" + objectHash + "] AND HASH IN GDRIVE: [" + entity.getSha256Checksum() + "]", MODULE);
        if (objectHash.equals(entity.getSha256Checksum())){
            logger.log(LogType.DEBUG, "S3 OBJECT HASH MATCHED WITH THAT OF DRIVE HASH", MODULE);
            return;
        } else{
            InputStream fileStream = getFileStream(entity.getWebContentLink(), token, logger);
            streamToS3(fileStream, entity, filePath, logger);
        }
    }
    private InputStream getFileStream(String fileContentURL,String token, Logger logger) throws IOException{
        final String MODULE = "S3WriteFromLink.getFileStream";
        URL fileDownloadURL = new URL(fileContentURL);
        HttpURLConnection connection = (HttpURLConnection) fileDownloadURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization",token);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK){
            logger.log(LogType.DEBUG, "SUCCESSFULLY FETCHED FILE CONTENT FROM GDRIVE", MODULE);
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while ((reader.readLine()) != null) {}
            reader.close();
            connection.disconnect();
            return inputStream;
        }
        return null;
    }
    private void streamToS3(InputStream stream,GDriveEntity entity, String filePath, Logger logger) throws IOException{
        final String MODULE = "S3WriteFromLinkImpl.streamToS3";
        s3.putObject(new PutObjectRequest(Constants.S3_BUCKET_NAME, filePath + "/" + entity.getName(), stream, null));
        logger.log(LogType.DEBUG, "OBJECT " + filePath + "/" + entity.getName() + " SUCCESSFULLY UPLOADED TO S3", MODULE);
        Tag objectHashTag = new Tag("hash", entity.getSha256Checksum());
        List<Tag> objectTags = new ArrayList<>(null);
        objectTags.add(objectHashTag);
        ObjectTagging objectTagging = new ObjectTagging(objectTags);
        SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(Constants.S3_BUCKET_NAME,filePath + "/" + entity.getName(),objectTagging);
        s3.setObjectTagging(setObjectTaggingRequest);
        logger.log(LogType.DEBUG, "TAG " + objectHashTag.toString() + " SUCCESSFULLY SET FOR THE OBJECT " + filePath + "/" + entity.getName(), MODULE);
    }

    private String getObjectHash(String objectPrefix, String objectName, Logger logger){
        final String MODULE = "S3WriteFromLink.getObjectHash";
        try {
            s3.getObject(Constants.S3_BUCKET_NAME, objectPrefix + "/" + objectName);
            GetObjectTaggingRequest objectTaggingRequest = new GetObjectTaggingRequest(Constants.S3_BUCKET_NAME, objectPrefix + "/" + objectName);
            GetObjectTaggingResult taggingResult = s3.getObjectTagging(objectTaggingRequest);
            List<Tag> objectTags = taggingResult.getTagSet();
            for (Tag tag : objectTags){
                if (tag.getKey().equals("hash")){
                    return tag.getValue();
                }
            }
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404){
                logger.log(LogType.DEBUG, "OBJECT " + objectPrefix + "/" + objectName + "IS NOT FOUND IN THE BUCKET " + Constants.S3_BUCKET_NAME, MODULE);
                return null;
            } else{
                logger.log(LogType.ERROR, e.getMessage(), MODULE);
                throw e;
            }
        } catch (SdkClientException e){
            logger.log(LogType.ERROR, e.getMessage(), MODULE);
            throw e;
        } catch (Exception e){
            logger.log(LogType.ERROR, e.getMessage(), MODULE);
            throw e;
        }
        return null;
    }
}

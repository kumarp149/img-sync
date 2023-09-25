package com.sruteesh.imgSync.s3api.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
    
    public void uploadToS3(GDriveEntity entity, String token, String filePath, Logger logger) throws IOException{
        final String MODULE = "S3WriteFromLink.uploadToS3";
        Constants.FILE_INDEX += 1;
        logger.log(LogType.DEBUG, "UPLOADING FILE: [" + Constants.FILE_INDEX + "]", MODULE);
        String objectHash = getObjectHash(filePath, entity.getName(), logger);
        logger.log(LogType.DEBUG, "S3 OBJECT HASH: [" + objectHash + "] AND HASH IN GDRIVE: [" + entity.getSha256Checksum() + "]", MODULE);
        if ((objectHash == null) || (!objectHash.equals(entity.getSha256Checksum()))){
            URL fileDownloadURL = new URL(Constants.DRIVE_API_URL + "/" + entity.getId() + "?" + "alt=media");
            logger.log(LogType.DEBUG, "FETCH STREAM FROM URL: [" + fileDownloadURL.toString() + "]", MODULE);
            HttpURLConnection connection = (HttpURLConnection) fileDownloadURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization","Bearer " + token);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK){
                InputStream fileStream = connection.getInputStream();
                streamToS3(fileStream, entity, filePath, logger);
                fileStream.close();
            }
        } else{
            logger.log(LogType.DEBUG, "S3 OBJECT HASH MATCHED WITH THAT OF DRIVE HASH", MODULE);
        }
    }
    
    private void streamToS3(InputStream stream,GDriveEntity entity, String filePath, Logger logger) throws IOException{
        final String MODULE = "S3WriteFromLinkImpl.streamToS3";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(entity.getSize());
        s3.putObject(new PutObjectRequest(Constants.S3_BUCKET_NAME, filePath + "/" + entity.getName(), stream, metadata));
        logger.log(LogType.DEBUG, "OBJECT " + filePath + "/" + entity.getName() + " SUCCESSFULLY UPLOADED TO S3", MODULE);
        Tag objectHashTag = new Tag("hash", entity.getSha256Checksum());
        List<Tag> objectTags = new ArrayList<>();
        objectTags.add(objectHashTag);
        ObjectTagging objectTagging = new ObjectTagging(objectTags);
        SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(Constants.S3_BUCKET_NAME,filePath + "/" + entity.getName(),objectTagging);
        s3.setObjectTagging(setObjectTaggingRequest);
        logger.log(LogType.DEBUG, "TAG " + objectHashTag.toString() + " SUCCESSFULLY SET FOR THE OBJECT " + filePath + "/" + entity.getName(), MODULE);
    }

    private String getObjectHash(String objectPrefix, String objectName, Logger logger){
        final String MODULE = "S3WriteFromLink.getObjectHash";
        try {
            //s3.getObject(Constants.S3_BUCKET_NAME, objectPrefix + "/" + objectName);
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
}

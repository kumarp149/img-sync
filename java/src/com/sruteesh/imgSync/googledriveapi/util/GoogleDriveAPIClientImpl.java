package com.sruteesh.imgSync.googledriveapi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sruteesh.imgSync.constants.Constants;
import com.sruteesh.imgSync.googledriveapi.entities.GDriveEntity;
import com.sruteesh.imgSync.logger.LogType;
import com.sruteesh.imgSync.logger.Logger;
import com.sruteesh.imgSync.s3api.utils.S3WriteFromLink;
import com.sruteesh.imgSync.s3api.utils.S3WriteFromLinkImpl;


public class GoogleDriveAPIClientImpl implements GoogleDriveAPIClient {
    private S3WriteFromLink s3Writer = new S3WriteFromLinkImpl();    
    /*
     * Initiates the sync from a google drive folder to an AWS S3 bucket
     * folderId is the id of the folder in google drive
     * authToken is OAuth 2.0 token needed for Drive API calls
     * parentPath along with static prefix is the path at which the file will be uploaded in AWS S3
     */

    @Override
    public void initiateSync(String folderId,String authToken, String parentPath, Logger logger,AmazonS3 s3Client, boolean flag) throws IOException, InterruptedException, ExecutionException{
        final String MODULE = "GoogleDriveAPIClient.initiateSync";
        logger.log(LogType.DEBUG, "PUSHING NEW FUTURES TO QUEUE", MODULE);

        logger.log(LogType.DEBUG, "CALLING GDRIVE API CALL FOR FOLDER: [" + folderId + "]", MODULE);
        URL contentFetchEndpoint = new URL(Constants.DRIVE_API_URL + "?" + "q='" + folderId + "'+in+parents&fields=files(id,name,parents,sha256Checksum,mimeType,webContentLink,size)&pageSize=1000");
        logger.log(LogType.DEBUG, "GDRIVE API ENDPOINT URL TO FETCH FILE LIST: [" + contentFetchEndpoint.toString() + "]", MODULE);

        HttpURLConnection connection = (HttpURLConnection) contentFetchEndpoint.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent",Constants.API_CLIENT_NAME);
        connection.setRequestProperty("Accept","*/*");
        connection.setRequestProperty("Accept-Encoding","gzip, deflate, br");
        connection.setRequestProperty("Connection","keep-alive");
        connection.setRequestProperty("Authorization","Bearer " + authToken);
        
        int responseCode = connection.getResponseCode();
        logger.log(LogType.DEBUG,"GDRIVE API RESPONSE FOR FOLDER ID: [" + folderId + "] is [" + responseCode + "]" , MODULE);
        if (responseCode == HttpURLConnection.HTTP_OK){
            String contentEncoding = connection.getHeaderField("Content-Encoding");
            InputStream encodedResponseStream = connection.getInputStream();
            InputStream decodedInputStream = encodedResponseStream;
            if (contentEncoding != null && contentEncoding.equals("gzip")){
                decodedInputStream = new GZIPInputStream(encodedResponseStream);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(decodedInputStream,StandardCharsets.UTF_8));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            connection.disconnect();
            String cleanedResponse = response.toString().replaceAll("[\\p{Cc}&&[^\\t\\r\\n]]", "");
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(cleanedResponse);
            JsonNode filesNode = rootNode.get("files");
            List<GDriveEntity> contentInFolders = new ArrayList<>();
            for (JsonNode fileNode : filesNode) {
                GDriveEntity myFile = objectMapper.convertValue(fileNode, GDriveEntity.class);
                contentInFolders.add(myFile);
            }
            logger.log(LogType.DEBUG, "API RESPONSE OF FOLDER " + folderId + " HAS " + contentInFolders.size() + " ELEMENTS", MODULE);
            for (GDriveEntity entity : contentInFolders){
                if (entity.getMimeType().equals("application/vnd.google-apps.folder")){
                    logger.log(LogType.DEBUG, entity.getId() + " IS A FOLDER", MODULE);
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            initiateSync(entity.getId(), authToken, parentPath + "/" + entity.getName(), logger,s3Client,false);
                        } catch (IOException | InterruptedException | ExecutionException e) {
                            logger.log(LogType.ERROR, "ERROR WHILE SYNCING THE FOLDER: [" + entity.getId() + "]", MODULE);
                            logger.logTrace(e, MODULE);
                        }
                        return null;
                    });
                    Constants.SYNCFUTURES.add(future);
                } else{
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            s3Writer.uploadToS3(entity, authToken, parentPath, logger,s3Client);    
                        } catch (Exception e) {
                            logger.log(LogType.ERROR, "ERROR UPLOADING FILE :[" + entity.getId() + "] TO S3", MODULE);
                            logger.logTrace(e, MODULE);
                        }
                        return null;
                    });
                    if (Constants.FUTURES.size() >= Integer.valueOf(System.getenv("MAX_THREADS"))){
                        CompletableFuture<Void> allFutures = CompletableFuture.allOf(Constants.FUTURES.toArray(new CompletableFuture[0]));
                        logger.log(LogType.DEBUG, "WAITING FOR ALL EXISTING FUTURES TO BE OVER", MODULE);
                        allFutures.get();
                        Constants.FUTURES.clear();
                    }
                    Constants.FUTURES.add(future);
                }
                if (flag){
                    waitForCompletion(logger);
                }
            }
        }
    }

    private void waitForCompletion(Logger logger){
        final String MODULE = "GoogleDriveAPIClient.waitForCompletion";
        while(!Constants.SYNCFUTURES.isEmpty()){
            try {
                Constants.SYNCFUTURES.peek().get();
                Constants.SYNCFUTURES.remove();
            } catch (InterruptedException | ExecutionException e) {
                logger.logTrace(e, MODULE);
            }
        }
    }
}
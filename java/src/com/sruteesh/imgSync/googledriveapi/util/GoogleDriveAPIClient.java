package com.sruteesh.imgSync.googledriveapi.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.concurrent.ExecutionException;

import com.amazonaws.services.s3.AmazonS3;
import com.sruteesh.imgSync.logger.Logger;


public interface GoogleDriveAPIClient {
    public void initiateSync(String folderId, String authToken, String parentPath, Logger logger,AmazonS3 s3Client,boolean flag) throws MalformedURLException, ProtocolException, IOException, InterruptedException, ExecutionException;
}

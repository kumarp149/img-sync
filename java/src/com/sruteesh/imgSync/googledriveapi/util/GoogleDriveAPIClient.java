package com.sruteesh.imgSync.googledriveapi.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.concurrent.ExecutionException;

import com.sruteesh.imgSync.logger.Logger;


public interface GoogleDriveAPIClient {
    public void initiateSync(String folderId, String authToken, String parentPath, Logger logger) throws MalformedURLException, ProtocolException, IOException, InterruptedException, ExecutionException;
}

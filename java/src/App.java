import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.sruteesh.imgSync.constants.Constants;
import com.sruteesh.imgSync.googledriveapi.util.GoogleDriveAPIClient;
import com.sruteesh.imgSync.googledriveapi.util.GoogleDriveAPIClientImpl;
import com.sruteesh.imgSync.logger.LogType;
import com.sruteesh.imgSync.logger.Logger;
import com.sruteesh.imgSync.logger.LoggerImpl;

public class App implements RequestStreamHandler {

    private static String generateOAuthToken(Logger logger) throws IOException, MalformedURLException {
        final String MODULE = "App.generateOAuthToken";
        URL OAuthGenUrl = new URL(Constants.OAUTH_TOKEN_URL);
        HttpURLConnection connection = (HttpURLConnection) OAuthGenUrl.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            return response.toString();
        } else {
            logger.log(LogType.ERROR, "ERROR GENERATING TOKEN", MODULE);
            logger.log(LogType.DEBUG, "RESPONSE FROM API: " + response, MODULE);
            connection.disconnect();
            return null;
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws MalformedURLException, IOException {
        Constants.FOLDER_TO_SYNC = System.getenv("FOLDER_TO_SYNC");
        Constants.S3_UPLOAD_PREFIX = System.getenv("S3_UPLOAD_PREFIX");
        Constants.OAUTH_TOKEN_URL = System.getenv("OAUTH_TOKEN_URL");

        Constants.FUTURES = new ArrayList<>();
        Constants.SYNCFUTURES = new LinkedList<>();

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxConnections(5000);
        clientConfiguration.setRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
        PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, 5, true));

        AmazonS3 s3Client = AmazonS3Client.builder().withClientConfiguration(clientConfiguration).build();

        final String MODULE = "App.handleRequest";
        Logger logger = new LoggerImpl();
        logger.log(LogType.INFO, "STARTED SYNCING THE FOLDER " + Constants.FOLDER_TO_SYNC + " FROM GDRIVE TO S3", MODULE);
        String authToken = generateOAuthToken(logger);
        if (authToken == null) {
            logger.log(LogType.ERROR,"ERROR GENERATING TOKEN",MODULE);
            logger.finish();
            return;
        } else {
            GoogleDriveAPIClient driveClient = new GoogleDriveAPIClientImpl();
            try {
                driveClient.initiateSync(Constants.FOLDER_TO_SYNC, authToken, Constants.S3_UPLOAD_PREFIX, logger,s3Client,true);
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(Constants.FUTURES.toArray(new CompletableFuture[0]));
                logger.log(LogType.DEBUG, "WAITING FOR ALL FUTURES TO BE OVER", MODULE);
                allFutures.get();
                Constants.FUTURES.clear();
                logger.finish();
            } catch (InterruptedException | ExecutionException e) {
                logger.logTrace(e, MODULE);
            }
        }
    }
}
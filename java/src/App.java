import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
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
        final String MODULE = "App.handleRequest";
        Logger logger = new LoggerImpl();
        String authToken = generateOAuthToken(logger);
        if (authToken == null) {
            logger.log(LogType.DEBUG,"ERROR GENERATING TOKEN",MODULE);
            return;
        } else {
            GoogleDriveAPIClient driveClient = new GoogleDriveAPIClientImpl();
            try {
                driveClient.initiateSync(Constants.FOLDER_TO_SYNC, authToken, Constants.S3_UPLOAD_PREFIX, logger);
            } catch (InterruptedException | ExecutionException e) {
                logger.log(LogType.ERROR, e.getMessage(), MODULE);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.finish();
            } catch (IOException e) {
                System.out.println("ERROR WRITING LOG TO S3");
                System.out.println(e.getMessage());
            }
        }));
    }

}
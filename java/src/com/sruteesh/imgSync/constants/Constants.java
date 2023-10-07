package com.sruteesh.imgSync.constants;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Constants {
    public static final String DRIVE_API_URL = "https://www.googleapis.com/drive/v3/files";
    public static final String API_CLIENT_NAME = "IMG-SYNC";
    public static final String DRIVE_SCOPE_URL = "https://www.googleapis.com/auth/drive.readonly";
    public static final String S3_BUCKET_NAME = "sruteesh-gdrive-sync";
    public static final String S3_LOGGER_BUCKET = "sruteesh-gdrive-sync-logs";
    public static String S3_UPLOAD_PREFIX;
    public static String FOLDER_TO_SYNC;
    public static String OAUTH_TOKEN_URL;
    public static Integer FILE_INDEX = -1;
    public static List<CompletableFuture<Void>> FUTURES;
}

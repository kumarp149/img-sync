package com.sruteesh.imgSync.s3api.utils;

import java.io.IOException;

import com.sruteesh.imgSync.googledriveapi.entities.GDriveEntity;
import com.sruteesh.imgSync.logger.Logger;

public interface S3WriteFromLink {
    public void uploadToS3(GDriveEntity entity,String token,String filePath, Logger logger) throws IOException;    
}

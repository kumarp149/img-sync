package com.sruteesh.imgSync.logger;

import java.io.IOException;

public interface Logger {
    public void log(LogType logType, String message, String moduleName);
    public void finish() throws IOException;
}

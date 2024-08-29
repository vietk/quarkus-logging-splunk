package io.quarkiverse.logging.splunk;

import java.util.List;

import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.HttpEventCollectorMiddleware;

public class LogCounter extends HttpEventCollectorMiddleware.HttpSenderMiddleware {

    public static long logSent = 0;

    @Override
    public void postEvents(List<HttpEventCollectorEventInfo> events,
            HttpEventCollectorMiddleware.IHttpSender sender,
            HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        logSent += events.size();
        sender.postEvents(events, callback);
    }
}
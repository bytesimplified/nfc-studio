package com.nfcstudio.app.automation;
import java.net.URL;
import java.io.*;
final class WebhookClient {
    static void post(String value)throws Exception {
        javax.net.ssl.HttpsURLConnection c=(javax.net.ssl.HttpsURLConnection)new URL(value).openConnection();
        try {
            c.setInstanceFollowRedirects(false);
            c.setRequestMethod("POST");
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setDoOutput(true);
            c.setFixedLengthStreamingMode(0);
            try(OutputStream out=c.getOutputStream()) {
            }
            int status=c.getResponseCode();
            if(status<200||status>=300)throw new IOException("HTTP "+status);
        } finally {
            c.disconnect();
        }
    }
}

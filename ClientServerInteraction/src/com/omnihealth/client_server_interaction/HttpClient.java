package com.omnihealth.client_server_interaction;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import javax.net.SocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: yuriy
 * Date: 30.03.13
 * Time: 23:37
 * To change this template use File | Settings | File Templates.
 */
public class HttpClient {

    private static DefaultHttpClient mClient;

    static {
        mClient = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(mClient.getParams(), 5000);
    }

    public static JSONObject post(String url, String content) throws Exception {
        SocketFactory.getDefault().createSocket();
        HttpPost post = new HttpPost(url);
        StringEntity se = new StringEntity(content);
        se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,
                "application/x-www-form-urlencoded"));
        post.setEntity(se);
        HttpResponse response = mClient.execute(post);        /* Checking response */

        if (response != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));
            String json = "";
            while (true) {
                String append = reader.readLine();
                if (append == null) {
                    break;
                }
                json += append;
            }
            return new JSONObject(json);
        }
        return null;
    }
}

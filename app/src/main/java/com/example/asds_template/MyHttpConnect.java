package com.example.asds_template;

import android.net.Uri;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TingYao on 7/7/2016.
 */
public class MyHttpConnect {
    private String link;
    private URL url;

    public MyHttpConnect(String addr){
        link=addr;
        try{
            url = new URL(link);
        } catch(MalformedURLException e){
            Log.e("connect", e.toString());
        }

    }

    public URLConnection PostToServer(String params) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(params);
        writer.flush();
        writer.close();
        os.close();

        conn.connect();
        return conn;
    }

    public String SetParams(HashMap<String, String> keyValuePairs){
        Uri.Builder builder = new Uri.Builder();
        for(Map.Entry<String, String> entry : keyValuePairs.entrySet())
            builder.appendQueryParameter(entry.getKey(),entry.getValue());
        return builder.build().getEncodedQuery();
    }
}

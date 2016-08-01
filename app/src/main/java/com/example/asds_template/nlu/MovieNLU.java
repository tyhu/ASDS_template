package com.example.asds_template.nlu;

import android.util.Log;

import com.example.asds_template.util.MyHttpConnect;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TingYao on 7/19/2016.
 */
public class MovieNLU {

    MyHttpConnect conn;

    public MovieNLU (String url, int port){
        conn = new MyHttpConnect("http://"+url+":"+String.valueOf(port));
    }

    public void understand(String sent){
        HashMap<String, String> keyValuePairs = new HashMap<String,String>();
        keyValuePairs.put("utt", sent);
        String params = conn.SetParams(keyValuePairs);
        String rstr="";
        try{
            HttpURLConnection response = (HttpURLConnection)conn.PostToServer(params);
            System.out.println("nlu connection success!");
            int responseCode = response.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(response.getInputStream()));
                while ((line=br.readLine()) != null) {
                    rstr+=line;
                }

            }
        } catch (IOException e){
            Log.e("NLU", "connection error");
        }

        System.out.println("nlu output: "+rstr);

    }

}

package com.example.asds_template.asr;

import android.media.AudioRecord;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by harry on 11/10/15.
 */


public class BingRecognizer {
    private String client_id = "dmeexdia";
    private String client_secret = "wNUXY7NvpIw1ugB4zVcUPhVQS6Lv9MFNPWa6qWIkIFY=";
    private String link = "https://speech.platform.bing.com/recognize/?";
    private URL url;
    private byte[] wav_header;
    private final AudioRecord recorder;
    private final int sampleRate=16000;
    private int bufferSize;

    //static{
    //   System.loadLibrary("opensmileTest");
    //}

    public BingRecognizer(String client_id,String client_secret){
        this.client_id = client_id;
        this.client_secret = client_secret;
        try{
            url = new URL(link);
        } catch(MalformedURLException e){
            Log.e("bingVoice", e.toString());
        }
        wav_header = new byte[44];
        File sampleWav=new File("/sdcard/history.raw.wav");
        try(FileInputStream in = new FileInputStream(sampleWav)){
            in.read(wav_header);
        }catch(Exception ex){
            Log.e("VS", "exception: " + ex.getMessage());
        }
        bufferSize = Math.round((float)sampleRate * 0.8F);
        recorder = new AudioRecord(6,sampleRate, 16, 2, bufferSize * 2);

    }

    /*
    super recognition because we want to process audio simultaneously
     */
    public String BingSuperRecognition() throws IOException {
        URL asr_url = new URL(link+BingASRParams());
        HttpURLConnection conn = (HttpURLConnection) asr_url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type","audio/wav; codec=\"audio/pcm\"; samplerate=16000");
        conn.setRequestProperty("Accept","application/json;text/xml");
        conn.setRequestProperty("ProtocolVersion","HTTP/1.1");
        conn.setRequestProperty("Authorization", "Bearer " + GetAuthToken());
        conn.setRequestProperty("Host", "speech.platform.bing.com");
        conn.connect();
        //String params = BingASRParams();
        //System.out.println(params);

        /*
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
        writer.write(params);
        writer.flush();
        writer.close();*/

        DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
        //outputStream.writeBytes(params);

        //File historyRaw=new File("/sdcard/opensmile.wav");
        byte[] buffer = new byte[bufferSize];
        int byteread;
        short tmpshort;
        int silenceCount=0, totalCount=0;
        short[] shortbuf;
        float energy;

        outputStream.write(wav_header);
        recorder.startRecording();
        System.out.println("start recording!");
        while(silenceCount<7 || totalCount>300) {
            recorder.read(buffer, 0, buffer.length);
            shortbuf=byte2short(buffer, bufferSize);
            energy = bufferEnergy(shortbuf);

            //opensmile execution

            if(energy>1E9)
                silenceCount=0;
            else
                silenceCount+=1;
            outputStream.write(buffer);
            System.out.println("energy: " + energy);
            totalCount+=1;
        }



        System.out.println("after sending");
        outputStream.flush();
        outputStream.close();
        System.out.println("after close");
        recorder.release();

        String jsonstr="";
        String output = "[Noise]";
        try{
            int responseCode = conn.getResponseCode();
            System.out.println("code: "+responseCode);
            if(responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    if(!line.startsWith("<"))
                        jsonstr += line;
                }
            }
            else{
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
            System.out.println(jsonstr);
            JSONObject jobj = new JSONObject(jsonstr);
            if(jobj.getJSONObject("header").has("lexical"))
                output = (String)jobj.getJSONObject("header").get("lexical");
        }catch (IOException e){
            Log.e("Main","connection error");
        }catch (JSONException je){ Log.e("Bing",je.getMessage());}
        return output;

    }

    private float bufferEnergy(short[] buf){
        float eng = 0;
        for(int i=0;i<buf.length;i++)
            eng+=buf[i]*buf[i];
        return eng;
    }


    public String BingASRParams(){
        HashMap<String, String> keyValuePairs = new HashMap<String,String>();
        keyValuePairs.put("scenarios", "smd");
        keyValuePairs.put("appid", "D4D52672-91D7-4C74-8AD8-42B1D98141A5");
        keyValuePairs.put("locale", "en-US");
        keyValuePairs.put("maxnbest", "5");
        keyValuePairs.put("version", "3.0");
        keyValuePairs.put("device.os", "wp7");
        keyValuePairs.put("format", "json");
        keyValuePairs.put("instanceid", "565D69FF-E928-4B7E-87DA-9A750B96D9E3");
        keyValuePairs.put("requestid", UUID.randomUUID().toString());
        return SetParams(keyValuePairs);
    }

    public String PostToAuthServer(HttpURLConnection conn){
        String responseStr = "";
        try{
            HttpURLConnection response = conn;
            System.out.println("connection success!");
            int responseCode = response.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(response.getInputStream()));
                while ((line=br.readLine()) != null) {
                    responseStr+=line;
                }

            }
        }catch (IOException e){
            Log.e("Main","connection error");
        }
        return responseStr;
    }

    public String GetAuthToken()  {
        String auth_str="";
        String auth_token="";
        String authlink = "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13";
        HashMap<String, String> keyValuePairs = new HashMap<String,String>();
        keyValuePairs.put("grant_type", "client_credentials");
        keyValuePairs.put("client_id", client_id);
        keyValuePairs.put("client_secret", client_secret);
        keyValuePairs.put("scope", "https://speech.platform.bing.com");
        String params = SetParams(keyValuePairs);

        try {
            URL auth_url = new URL(authlink);
            HttpURLConnection conn = (HttpURLConnection) auth_url.openConnection();
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
            auth_str = PostToAuthServer(conn);
            JSONObject jobj = new JSONObject(auth_str);
            //System.out.println(jobj.get("access_token"));
            auth_token = (String) jobj.get("access_token");
        } catch(MalformedURLException me){}
        catch(IOException e){}
        catch(JSONException je){}
        return auth_token;

    }

    public String SetParams(HashMap<String, String> keyValuePairs){
        Uri.Builder builder = new Uri.Builder();
        for(Map.Entry<String, String> entry : keyValuePairs.entrySet())
            builder.appendQueryParameter(entry.getKey(),entry.getValue());
        return builder.build().getEncodedQuery();
    }

    public byte[] byteSwape(byte[] buf){
        int len = buf.length;
        byte[] swaped=new byte[len];
        for (int i=0;i<len/2;i++){
            swaped[2*i] = buf[2*i+1];
            swaped[2*i+1] = buf[2*i];
        }
        return swaped;
    }

    public short[] byte2short(byte[] buf, int bufsize){
        short[] audioSeg=new short[bufsize/2];
        for (int i = 0; i <bufsize/2 ; i++) {
            audioSeg[i]=buf[i*2];
            //audioSeg[i] = (short) ((buf[2*i] << 8) | buf[2*i+1]);
            audioSeg[i] = (short) ((buf[2*i+1] << 8) | buf[2*i]);
        }
        return audioSeg;
    }

    public byte[] short2byte(short[] buf,int bufsize){
        byte[] bytebuf=new byte[bufsize*2];
        for(int i=0;i<bufsize;i++){
            bytebuf[2*i+1]= (byte)(buf[i] & 0xff);
            bytebuf[2*i]= (byte)((buf[i] >> 8) & 0xff);
        }
        return bytebuf;
    }

}

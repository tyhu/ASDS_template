package com.example.asds_template.asr;

import android.media.AudioRecord;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.FsgModel;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;

/**
 * This recognizer can run pocketsphinx and Bing simultaneously
 * Created by TingYao on 3/3/2016.
 */
public class MultipleRecognizer {
    protected static final String TAG = "myASR";
    private final Decoder decoder;
    private final int sampleRate;
    private static final float BUFFER_SIZE_SECONDS = 0.4F;
    private int bufferSize;
    private final AudioRecord recorder;
    private Thread recognizerThread;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Collection<RecognitionListener> listeners = new HashSet();

    private File historyRaw = new File("/sdcard/history.raw");
    private Queue<short[]> signalQE = new ConcurrentLinkedQueue<short[]>();
    private int queueSize = 100;
    Queue<float[]> MFCCQueue = new ConcurrentLinkedQueue<float[]>();

    private String client_id = "dmeexdia";
    private String client_secret = "wNUXY7NvpIw1ugB4zVcUPhVQS6Lv9MFNPWa6qWIkIFY=";
    private String link = "https://speech.platform.bing.com/recognize/?";
    private URL url;
    private byte[] wav_header;
    //private final int sampleRate=16000;
;
    Calendar calendar;
    SimpleDateFormat df1;

    protected MultipleRecognizer(Config config, String client_id,String client_secret) throws IOException {
        this.decoder = new Decoder(config);
        this.sampleRate = (int)this.decoder.getConfig().getFloat("-samprate");
        this.bufferSize = Math.round((float) this.sampleRate * 0.4F);
        this.recorder = new AudioRecord(6, this.sampleRate, 16, 2, this.bufferSize * 2);
        if(this.recorder.getState() == 0) {
            this.recorder.release();
            throw new IOException("Failed to initialize recorder. Microphone might be already in use.");
        }

        //for recording
        calendar = Calendar.getInstance();
        df1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a");

        //For Bing ASR
        this.client_id = client_id;
        this.client_secret = client_secret;
        try{
            url = new URL(link);
        } catch(MalformedURLException e){
            Log.e("bingVoice", e.toString());
        }
        wav_header = new byte[44];
        //File sampleWav=new File("/sdcard/history.raw.wav");
        File sampleWav=new File("/sdcard/tmp.wav");

        try(FileInputStream in = new FileInputStream(sampleWav)){
            in.read(wav_header);
        }catch(Exception ex){
            Log.e("VS", "exception: " + ex.getMessage());
        }
    }


    public void addListener(RecognitionListener listener) {
        Collection var2 = this.listeners;
        synchronized(this.listeners) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(RecognitionListener listener) {
        Collection var2 = this.listeners;
        synchronized(this.listeners) {
            this.listeners.remove(listener);
        }
    }

    public boolean startListening(String searchName) {
        if(null != this.recognizerThread) {
            return false;
        } else {
            Log.i(TAG, String.format("Start recognition \"%s\"", new Object[]{searchName}));
            this.decoder.setSearch(searchName);
            this.recognizerThread = new RecognizerThread(-1);
            this.recognizerThread.start();
            return true;
        }
    }

    public boolean startSuperListening(String searchName, int timeout) {
        if(null != this.recognizerThread) {
            return false;
        } else {
            Log.i(TAG, String.format("Start recognition \"%s\"", new Object[]{searchName}));
            decoder.setSearch(searchName);
            recognizerThread = new MultipleRecognizer.MultiRecognizerThread(timeout);
            //detectionThread = new DetectionThread();
            recognizerThread.start();
            //detectionThread.start();
            return true;
        }
    }

    public boolean startListening(String searchName, int timeout) {
        if(null != this.recognizerThread) {
            return false;
        } else {
            Log.i(TAG, String.format("Start recognition \"%s\"", new Object[]{searchName}));
            this.decoder.setSearch(searchName);
            this.recognizerThread = new RecognizerThread(timeout);
            this.recognizerThread.start();
            return true;
        }
    }


    private boolean stopRecognizerThread() {
        if(null == this.recognizerThread) {
            return false;
        } else {
            try {
                this.recognizerThread.interrupt();
                this.recognizerThread.join();
            } catch (InterruptedException var2) {
                Thread.currentThread().interrupt();
            }

            this.recognizerThread = null;
            return true;
        }
    }

    public boolean stop() {
        boolean result = this.stopRecognizerThread();
        if(result) {
            Log.i(TAG, "Stop recognition");
            Hypothesis hypothesis = this.decoder.hyp();
            this.mainHandler.post(new ResultEvent(hypothesis, true));
        }
        return result;
    }

    public boolean cancel() {
        boolean result = this.stopRecognizerThread();
        if(result) {
            Log.i(TAG, "Cancel recognition");
        }

        return result;
    }

    public Decoder getDecoder() {
        return this.decoder;
    }

    public void shutdown() {
        this.recorder.release();
    }

    public String getSearchName() {
        return this.decoder.getSearch();
    }

    public void addFsgSearch(String searchName, FsgModel fsgModel) {
        this.decoder.setFsg(searchName, fsgModel);
    }

    public void addGrammarSearch(String name, File file) {
        Log.i(TAG, String.format("Load JSGF %s", new Object[]{file}));
        this.decoder.setJsgfFile(name, file.getPath());
    }

    public void addNgramSearch(String name, File file) {
        Log.i(TAG, String.format("Load N-gram model %s", new Object[]{file}));
        this.decoder.setLmFile(name, file.getPath());
    }

    public void addKeyphraseSearch(String name, String phrase) {
        this.decoder.setKeyphrase(name, phrase);
    }

    public void addKeywordSearch(String name, File file) {
        this.decoder.setKws(name, file.getPath());
    }

    public void addAllphoneSearch(String name, File file) {
        this.decoder.setAllphoneFile(name, file.getPath());
    }

    private class TimeoutEvent extends RecognitionEvent {
        private TimeoutEvent() {
            super();
        }

        protected void execute(RecognitionListener listener) {
            listener.onTimeout();
        }
    }

    private class OnErrorEvent extends RecognitionEvent {
        private final Exception exception;

        OnErrorEvent(Exception exception) {
            super();
            this.exception = exception;
        }

        protected void execute(RecognitionListener listener) {
            listener.onError(this.exception);
        }
    }

    private class ResultEvent extends RecognitionEvent {
        protected final Hypothesis hypothesis;
        private final boolean finalResult;
        private final int interruptType;

        ResultEvent(Hypothesis hypothesis, boolean finalResult) {
            super();
            this.hypothesis = hypothesis;
            this.finalResult = finalResult;
            this.interruptType = 0;
        }

        ResultEvent(Hypothesis hypothesis, boolean finalResult, int interruptType){
            super();
            this.hypothesis = hypothesis;
            this.finalResult = finalResult;
            this.interruptType = interruptType;
        }

        protected void execute(RecognitionListener listener) {
            if(this.finalResult) {
                //this.hypothesis.setBestScore(1);
                listener.onResult(this.hypothesis);
            } else if(interruptType!=0){
                //hypothesis.setBestScore(-1);
                listener.onResult(this.hypothesis);
            }
            else {
                listener.onPartialResult(this.hypothesis);
            }

        }
    }

    private class InSpeechChangeEvent extends RecognitionEvent {
        private final boolean state;

        InSpeechChangeEvent(boolean state) {
            super();
            this.state = state;
        }

        protected void execute(RecognitionListener listener) {
            if(this.state) {
                listener.onBeginningOfSpeech();
            } else {
                listener.onEndOfSpeech();
            }

        }
    }

    private abstract class RecognitionEvent implements Runnable {
        private RecognitionEvent() {
        }

        public void run() {
            RecognitionListener[] emptyArray = new RecognitionListener[0];
            RecognitionListener[] var2 = (RecognitionListener[])listeners.toArray(emptyArray);
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                RecognitionListener listener = var2[var4];
                this.execute(listener);
            }

        }

        protected abstract void execute(RecognitionListener var1);
    }

    private final class RecognizerThread extends Thread {
        private int remainingSamples;
        private int timeoutSamples;
        private static final int NO_TIMEOUT = -1;

        public RecognizerThread(int timeout) {
            if(timeout != -1) {
                this.timeoutSamples = timeout * sampleRate / 1000;
            } else {
                this.timeoutSamples = -1;
            }

            this.remainingSamples = this.timeoutSamples;
        }

        public RecognizerThread() {
            //this();
        }

        public void run() {

            //recording .wav file
            calendar = Calendar.getInstance();
            FileOutputStream out = null;
            String audiofn = df1.format(calendar.getTime());
            audiofn = audiofn.replace(" ","_")+".raw";

            try {
                out = new FileOutputStream("/sdcard/yahoo_test/"+audiofn);
                System.out.println("wav file name: "+audiofn);

                //***********end turn detection preparation
                //***********but it's for distraction detection...
                float energy;
                float maxEnergy = 0;
                float minEnergy = (float)1E12;
                int postCount = 0;
                int postThres = 6;
                int preCount = 0;
                int preThres = 10;
                boolean endTurn = false;
                boolean noTurn = false;

                recorder.startRecording();
                if(recorder.getRecordingState() == 1) {
                    recorder.stop();
                    IOException buffer1 = new IOException("Failed to start recording. Microphone might be already in use.");
                    mainHandler.post(new OnErrorEvent(buffer1));
                } else {
                    Log.d(MyRecognizer.TAG, "Starting decoding");
                    decoder.startUtt();
                    short[] buffer = new short[bufferSize];
                    byte[] bytebuf;
                    boolean inSpeech = decoder.getInSpeech();
                    recorder.read(buffer, 0, buffer.length);

                    while(!interrupted() && (this.timeoutSamples == -1 || this.remainingSamples > 0) && !endTurn) {
                        int nread = recorder.read(buffer, 0, buffer.length);
                        if(-1 == nread) {
                            throw new RuntimeException("error reading audio buffer");
                        }

                        if(nread > 0) {
                            bytebuf = short2byte(buffer, buffer.length);
                            decoder.processRaw(buffer, (long)nread, false, false);
                            if(decoder.getInSpeech() != inSpeech) {
                                inSpeech = decoder.getInSpeech();
                                mainHandler.post(new InSpeechChangeEvent(inSpeech));
                            }
                            out.write(bytebuf);

                            //remove the inSpeech detection
                            //if(inSpeech) {
                            //    this.remainingSamples = this.timeoutSamples;
                            //}

                            Hypothesis hypothesis = decoder.hyp();
                            mainHandler.post(new ResultEvent(hypothesis, false));
                        }


                        //**********end turn detection
                        //***********but it's for distraction detection...
                        energy = bufferEnergy(buffer);
                        if(energy>maxEnergy) maxEnergy = energy;
                        if(energy<minEnergy) minEnergy = energy;

                        if(energy<maxEnergy*0.01) postCount+=1;
                        else postCount=0;
                        if(maxEnergy<minEnergy*10) preCount+=1;

                        if(postCount>postThres) endTurn = true;
                        if(preCount>preThres) noTurn = true;

                        System.out.println("energy: "+energy);
                        System.out.println("postCount: "+postCount);
                        //*****end of end turn detection
                        //***********but it's for distraction detection...


                        if(this.timeoutSamples != -1) {
                            this.remainingSamples -= nread;
                        }
                    }
                    recorder.stop();
                    decoder.endUtt();
                    if(endTurn) {
                        System.out.println("you are distracted!!!!");
                        Hypothesis hypothesis = new Hypothesis("distracted!",0,0);
                        mainHandler.post(new ResultEvent(hypothesis, false));
                    }
                    mainHandler.removeCallbacksAndMessages((Object)null);
                    if(this.timeoutSamples != -1 && this.remainingSamples <= 0) {
                        mainHandler.post(new TimeoutEvent());
                    }

                }
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Thread for multiple recognizers
     */
    private final class MultiRecognizerThread extends Thread {
        private int remainingSamples;
        private int timeoutSamples;
        private static final int NO_TIMEOUT = -1;

        public MultiRecognizerThread(int timeout) {
            if(timeout != -1) {
                this.timeoutSamples = timeout * sampleRate / 1000;
            } else {
                this.timeoutSamples = -1;
            }

            this.remainingSamples = this.timeoutSamples;
        }

        public MultiRecognizerThread() {
            //this();
        }

        public void run() {
            try{

                //connect to bing
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

                DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
                outputStream.write(wav_header);
                byte[] bytesbuf;

                //***********end turn detection preparation
                float energy;
                float maxEnergy = 0;
                float minEnergy = (float)1E12;
                int postCount = 0;
                int postThres = 4;
                int preCount = 0;
                int preThres = 10;
                boolean endTurn = false;
                boolean noTurn = false;

                recorder.startRecording();
                if(recorder.getRecordingState() == 1) {
                    recorder.stop();
                    IOException buffer1 = new IOException("Failed to start recording. Microphone might be already in use.");
                    mainHandler.post(new OnErrorEvent(buffer1));
                } else {
                    Log.d(MyRecognizer.TAG, "Starting decoding");
                    decoder.startUtt();
                    short[] buffer = new short[bufferSize];
                    boolean inSpeech = decoder.getInSpeech();
                    recorder.read(buffer, 0, buffer.length);
                    bytesbuf = short2byte(buffer,bufferSize);
                    outputStream.write(bytesbuf);
                    System.out.println("time out: "+timeoutSamples);
                    while(!interrupted() && (this.timeoutSamples == -1 || this.remainingSamples > 0) && !noTurn && !endTurn) {
                        int nread = recorder.read(buffer, 0, buffer.length);
                        decoder.processRaw(buffer, (long)nread, false, false);
                        bytesbuf = short2byte(buffer,bufferSize);
                        outputStream.write(bytesbuf);
                        if(-1 == nread) {
                            throw new RuntimeException("error reading audio buffer");
                        }

                        if(nread > 0) {
                            decoder.processRaw(buffer, (long)nread, false, false);
                            if(decoder.getInSpeech() != inSpeech) {
                                inSpeech = decoder.getInSpeech();
                                mainHandler.post(new InSpeechChangeEvent(inSpeech));
                            }

                            //remove the inSpeech detection
                            if(inSpeech) {
                                this.remainingSamples = this.timeoutSamples;
                                System.out.println("within speech!!");
                            }

                            Hypothesis hypothesis = decoder.hyp();
                            mainHandler.post(new ResultEvent(hypothesis, false));
                        }

                        //**********end turn detection
                        energy = bufferEnergy(buffer);
                        if(energy>maxEnergy) maxEnergy = energy;
                        if(energy<minEnergy) minEnergy = energy;

                        if(energy<maxEnergy*0.01) postCount+=1;
                        else postCount=0;
                        if(maxEnergy<minEnergy*10) preCount+=1;

                        if(postCount>postThres) endTurn = true;
                        if(preCount>preThres) noTurn = true;

                        System.out.println("energy: "+energy);
                        System.out.println("postCount: "+postCount);
                        //*****end of end turn detection

                        if(this.timeoutSamples != -1) {
                            this.remainingSamples -= nread;
                        }
                    }
                    //MFCCQueue.clear();
                    recorder.stop();
                    decoder.endUtt();
                    mainHandler.removeCallbacksAndMessages((Object)null);
                    if(this.timeoutSamples != -1 && this.remainingSamples <= 0) {
                        mainHandler.post(new TimeoutEvent());
                    }

                    // receiving output from bing server
                    String jsonstr="";
                    String output = "[Noise]";
                    try {
                        int responseCode = conn.getResponseCode();
                        System.out.println("code: " + responseCode);
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            String line;
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            while ((line = br.readLine()) != null) {
                                if (!line.startsWith("<"))
                                    jsonstr += line;
                            }
                        } else {
                            String line;
                            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                            while ((line = br.readLine()) != null) {
                                System.out.println(line);
                            }
                        }
                        System.out.println(jsonstr);
                        JSONObject jobj = new JSONObject(jsonstr);
                        if (jobj.getJSONObject("header").has("lexical") && endTurn){
                            output = (String) jobj.getJSONObject("header").get("lexical");
                            System.out.println(output);
                            Hypothesis hypothesis = new Hypothesis("[bing] "+output,1,1);
                            mainHandler.post(new ResultEvent(hypothesis, false));
                        }
                        else{
                            mainHandler.post(new TimeoutEvent());
                        }


                    }catch (IOException e){
                        Log.e("Main","connection error");
                    }catch (JSONException je){ Log.e("Bing",je.getMessage());}

                }
            } catch (IOException e){
                Log.e("MultiRecognizerThread","connection error");
            }
        }

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
            bytebuf[2*i]= (byte)(buf[i] & 0xff);
            bytebuf[2*i+1]= (byte)((buf[i] >> 8) & 0xff);
        }
        return bytebuf;
    }


    //Bing ASR part
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


}

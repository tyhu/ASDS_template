package com.example.asds_template;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Feature;
import edu.cmu.pocketsphinx.FrontEnd;
import edu.cmu.pocketsphinx.FsgModel;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.transform.Result;

/**
 * Created by harry on 11/5/15.
 */
public class MyRecognizer {
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
    int mfccQueueSize = 10;
    float[] MFCCs = new float[mfccQueueSize*39];
    float[] mfcc_buf;
    private float[] classifier_coef;
    private float bias;
    boolean distraction = false;

    static{
        System.loadLibrary("opensmileTest");
    }
    public native float[] opensmilefunc(Object[] m);
    private boolean detectStatus;
    private boolean filler_flag;
    private Thread detectionThread;

    protected MyRecognizer(Config config) throws IOException {
        this.decoder = new Decoder(config);
        this.sampleRate = (int)this.decoder.getConfig().getFloat("-samprate");
        this.bufferSize = Math.round((float)this.sampleRate * 0.4F);
        this.recorder = new AudioRecord(6, this.sampleRate, 16, 2, this.bufferSize * 2);
        if(this.recorder.getState() == 0) {
            this.recorder.release();
            throw new IOException("Failed to initialize recorder. Microphone might be already in use.");
        }
        detectStatus = false;
        classifier_coef = new float[5];
        classifier_coef[0] = (float)-0.16523084;
        classifier_coef[1] = (float)0.02930656;
        classifier_coef[2] = (float)0.04600631;
        classifier_coef[3] = (float)-0.09614151;
        classifier_coef[4] = (float)0.01635;
        bias = (float) -1.45;
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
            this.recognizerThread = new MyRecognizer.RecognizerThread(-1);
            this.recognizerThread.start();
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

    public boolean startSuperListening(String searchName, int timeout) {
        if(null != this.recognizerThread) {
            return false;
        } else {
            Log.i(TAG, String.format("Start recognition \"%s\"", new Object[]{searchName}));
            decoder.setSearch(searchName);
            recognizerThread = new SuperRecognizerThread(timeout);
            //detectionThread = new DetectionThread();
            recognizerThread.start();
            //detectionThread.start();
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
        detectStatus = false;
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

                while(!interrupted() && (this.timeoutSamples == -1 || this.remainingSamples > 0)) {
                    int nread = recorder.read(buffer, 0, buffer.length);
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
                        //if(inSpeech) {
                        //    this.remainingSamples = this.timeoutSamples;
                        //}

                        Hypothesis hypothesis = decoder.hyp();
                        mainHandler.post(new ResultEvent(hypothesis, false));
                    }

                    if(this.timeoutSamples != -1) {
                        this.remainingSamples -= nread;
                    }
                }
                detectStatus=false;
                MFCCQueue.clear();
                recorder.stop();
                decoder.endUtt();
                mainHandler.removeCallbacksAndMessages((Object)null);
                if(this.timeoutSamples != -1 && this.remainingSamples <= 0) {
                    mainHandler.post(new TimeoutEvent());
                }

            }
        }
    }

    private final class SuperRecognizerThread extends Thread {
        private int remainingSamples;
        private int timeoutSamples;
        private static final int NO_TIMEOUT = -1;

        //speech properties
        Queue<float[]> energyQueue = new ConcurrentLinkedQueue<float[]>();

        int silenceCount;
        int speechCount;
        int smallChunkNum;
        float silenceThres = (float)1E9;
        float[] smile_output;


        public SuperRecognizerThread(int timeout) {
            if(timeout != -1) {
                this.timeoutSamples = timeout * sampleRate / 1000;
            } else {
                this.timeoutSamples = -1;
            }

            this.remainingSamples = this.timeoutSamples;
            silenceCount=0;
            speechCount=0;
            smallChunkNum = 0;
        }

        //speech utilities
        private float bufferEnergy(short[] buf){
            float eng = 0;
            for(int i=0;i<buf.length;i++)
                eng+=buf[i]*buf[i];
            return eng;
        }

        public void run() {
            distraction = false;
            try(FileOutputStream fop = new FileOutputStream(historyRaw)){
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

                    energyQueue.clear();
                    float energy;
                    byte[] bytebuf;

                    while(!distraction && !interrupted() && (this.timeoutSamples == -1 || this.remainingSamples > 0)) {
                        int nread = recorder.read(buffer, 0, buffer.length);
                        if(-1 == nread) {
                            throw new RuntimeException("error reading audio buffer");
                        }
                        energy = bufferEnergy(buffer);
                        //System.out.println("instant energy: "+energy);

                        if(nread > 0) {
                            bytebuf = short2byte(buffer, buffer.length);
                            signalQE.add(buffer);
                            if (signalQE.size()>queueSize){
                                //System.out.println("remove something");
                                signalQE.remove();
                            }
                            fop.write(bytebuf);

                            //whatever audio processing here
                            if(energy<silenceThres){
                                if(silenceCount==0) {
                                    System.out.println("speech count: " + speechCount);
                                    //accumulate speech segment, whose chunk size make us believe that it is a filler
                                    //if(speechCount>2 && speechCount<6)
                                    if(speechCount>1 && speechCount<8)
                                        smallChunkNum+=1;
                                    else
                                        smallChunkNum=0;
                                }
                                silenceCount+=1;
                                speechCount=0;

                            }
                            else {
                                silenceCount = 0;
                                speechCount+=1;
                            }
                            //System.out.println("silence count: " + silenceCount);
                            //count small speech chunks



                            decoder.processRaw(buffer, (long) nread, false, false);
                            //FrontEnd fe = decoder.getFe();
                            //fe.


                            if(decoder.getInSpeech() != inSpeech) {
                                inSpeech = decoder.getInSpeech();
                                mainHandler.post(new InSpeechChangeEvent(inSpeech));
                            }

                            if(inSpeech) {
                                this.remainingSamples = this.timeoutSamples;
                            }

                            Hypothesis hypothesis = decoder.hyp();
                            if(silenceCount>7 || smallChunkNum>2) {
                            //if(silenceCount>10 || filler_flag) {
                                if(hypothesis==null)
                                    hypothesis = new Hypothesis("distracted!",0,0);
                                distraction = true;
                                detectStatus = false;
                                mainHandler.post(new ResultEvent(hypothesis, false));
                            }
                                mainHandler.post(new ResultEvent(hypothesis, false));
                        }

                        if(this.timeoutSamples != -1) {
                            this.remainingSamples -= nread;
                        }
                    }
                    detectStatus=false;
                    recorder.stop();
                    decoder.endUtt();
                    mainHandler.removeCallbacksAndMessages((Object)null);
                    if(this.timeoutSamples != -1 && this.remainingSamples <= 0) {
                        mainHandler.post(new TimeoutEvent());
                    }

                }
                fop.flush();
                fop.close();
            } catch(Exception ex){
                Log.e("VS", "exception: " + ex.getMessage());
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

    private final class DetectionThread extends Thread {
        Object[] filler_buf;
        int filler_count;
        public void run() {
            filler_buf = new Object[4];
            float[] smile_output;
            detectStatus = true;
            filler_flag = false;
            filler_count=0;
            while(detectStatus==true){
                if (signalQE.size()>=6){
                    filler_buf[0] = signalQE.poll();
                    //signalQE.poll();
                    filler_buf[1] = signalQE.poll();
                    //signalQE.poll();
                    filler_buf[2] = signalQE.poll();
                    filler_buf[3] = signalQE.poll();
                    smile_output = opensmilefunc(filler_buf);
                    //System.out.println("smileoutput length: " + smile_output.length);

                    MFCCQueue.add(smile_output);
                    if(MFCCQueue.size()>mfccQueueSize){
                        MFCCQueue.remove();

                        Iterator it = MFCCQueue.iterator();
                        int count=0;
                        while(it.hasNext()){
                            mfcc_buf = (float[])it.next();
                            for(int i=0;i<39;i++)
                                MFCCs[count*39+i]=mfcc_buf[i];
                            count+=1;
                        }

                        //System.out.println("fillerbuff length: " + filler_buf.length);
                        //System.out.println("smileoutput length: " + smile_output.length);
                        System.out.println("queue length: "+signalQE.size());
                        if(FillerDetection()==1)
                            filler_count+=1;
                        else
                            filler_count=0;
                    }
                    System.out.println("filler count: "+filler_count);
                    //if(filler_count>3)
                    //   filler_flag=true;
                }


            }
        }
    }

    public int FillerDetection(){
        float filler_output;
        float[] sorted;
        float[] feature = new float[5];
        sorted = MyFunctional.mysort(MFCCs, 39, MFCCs.length / 39, 0);
        feature[0] = MyFunctional.quartile1(sorted);
        feature[1] = MyFunctional.quartile3(sorted)-feature[0];
        feature[2] = MyFunctional.percentile1(sorted);
        sorted = MyFunctional.mysort(MFCCs, 39, MFCCs.length / 39, 2);
        feature[3] = MyFunctional.quartile1(sorted);
        feature[4] = MyFunctional.LinearRegErrorA(MFCCs, 39, MFCCs.length / 39, 0);

        filler_output = MyFunctional.dot(classifier_coef,feature)+bias;
        System.out.println("filler output:"+filler_output);

        if(filler_output<2.0)
            return 1;
        else
            return 0;
    }
}

package com.example.tingyao.emailapp;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;


/**
 * Created by tingyao on 10/20/15.
 */
public class AndroidCommandListener {
    private SpeechRecognizer sr;
    Context context;
    private String TAG = "androidASR";
    private static final String START_KEY = "in mind agent";
    private static final String TERMINATE_WORD = "terminate";
    private Handler commandHandler;

    public AndroidCommandListener(Context con, Handler commandHandler){
        context = con;
        this.commandHandler = commandHandler;
        sr = SpeechRecognizer.createSpeechRecognizer(context);
        sr.setRecognitionListener(new listener());
    }

    public void Search(String type, int search_duration){
        sr.stopListening();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        sr.startListening(intent);
    }

    public void StopSearch(){ sr.stopListening(); }

    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "onReadyForSpeech");
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
            Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error)
        {
            Log.d(TAG, "error " + error);
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                Log.d(TAG, "result " + data.get(i));
                //str += data.get(i);
                str = (String) data.get(i);
                Message msg = new Message();
                if(str.equals("check inbox")){
                    msg.arg1 = 1;
                    msg.obj = str;
                    commandHandler.sendMessage(msg);
                    break;
                }
                if(str.equals("read first email")){
                    msg.arg1 = 1;
                    msg.obj = str;
                    commandHandler.sendMessage(msg);
                    break;
                }
            }

            //mText.setText("results: "+String.valueOf(data.size()));
        }
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }
    }
}

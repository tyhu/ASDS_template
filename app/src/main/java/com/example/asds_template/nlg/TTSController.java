package com.example.asds_template.nlg;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.media.AudioManager;

/**
 * Created by tingyao on 4/17/15.
 */
public class TTSController {

    final boolean alwaysUseFlite = true;

    TextToSpeech ttobj;
    Context appContect;
    //Handler mCallWhenDone;
    Integer lastMessageQueued = 0;

    //control volume
    int amStreamMusicMaxVol;
    AudioManager am;

    public TTSController(Context appContext)
    {
        //mCallWhenDone = callWhenDone;
        this.appContect = appContext;

        TextToSpeech.OnInitListener listener = new TextToSpeech.OnInitListener()
        {
            @Override
            public void onInit(int status)
            {
                if (status != TextToSpeech.ERROR)
                {
                    ttobj.setLanguage(Locale.US);
                    // ttobj.setPitch(3f);
                    // ttobj.setSpeechRate(0.1f);

                    ttobj.setOnUtteranceProgressListener(new UtteranceProgressListener()
                    {

                        @Override
                        public void onDone(String utteranceId)
                        {
                            Log.d("TTSController", "OnDone called");
                            if (utteranceId.equals(lastMessageQueued.toString()))
                            {
                                Message msg = new Message();
                                msg.arg1 = 1;
                                //mCallWhenDone.sendMessage(msg);
                            }
                        }

                        @Override
                        public void onError(String utteranceId)
                        {
                        }

                        @Override
                        public void onStart(String utteranceId)
                        {
                        }
                    });
                }
            }
        };

        if (alwaysUseFlite)
            ttobj = new TextToSpeech(appContext, listener, "edu.cmu.cs.speech.tts.flite");
        else
            ttobj = new TextToSpeech(appContext, listener);

        //tingyao
        //adjust speaking rate

        am = (AudioManager) appContect.getSystemService(Context.AUDIO_SERVICE);
        amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
        am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol-5, 0);
        //am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol-10, 0);
        ttobj.setSpeechRate((float) 0.90);
    }

    @SuppressWarnings("deprecation")
    public void speakThis(String message)
    {

        Integer currentMessageId = ++lastMessageQueued;
        // Toast.makeText(appContect, message, Toast.LENGTH_SHORT).show();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                currentMessageId.toString());
        ttobj.speak(message, TextToSpeech.QUEUE_ADD, map);
    }

    public boolean Speaking(){
        return ttobj.isSpeaking();
    }
    public void playPause() { ttobj.playSilence(300,1,null); }
    public void volumeUp(){ am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol, 0); }
    public void volumeBack(){ am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol-10, 0); }
}

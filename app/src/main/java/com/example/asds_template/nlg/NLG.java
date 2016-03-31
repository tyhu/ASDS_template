package com.example.asds_template.nlg;

/**
 * Created by TingYao on 3/3/2016.
 */

import android.content.Context;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;

public class NLG{
    TTSController tts;
    public NLG(Context context, Handler handler){
        tts = new TTSController(context, handler);
    }
    public void speakRaw(String msg){ tts.speakThis(msg); }

    public void InformEmail(String sender,String content){
        String output = sender+" said, "+content;
        tts.speakThis(output);
    }

    public void InformUnread(int num){
        if(num==-1)
            tts.speakThis("Connection Error, please check your internet");
        else if(num==0)
            tts.speakThis("You have no unread email");
        else if(num<6){
            String numStr = String.valueOf(num);
            tts.speakThis("You have "+numStr+", unread email");
        }
        else
            tts.speakThis("You have more than 5 unread email");
    }

}

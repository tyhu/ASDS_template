package com.example.asds_template.nlg;

/**
 * Created by TingYao on 3/3/2016.
 */

import android.content.Context;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;

import java.util.HashMap;

public class NLG{
    TTSController tts;
    public NLG(Context context, Handler handler){
        tts = new TTSController(context, handler);
    }
    public void speakRaw(String msg){ tts.speakThis(msg); }

    public void inputMap(HashMap<String,String> hashmap){
        String action = hashmap.get("action");
        if(action=="request_profile")
            speakRaw("Ok, tell me what you like");
        else if(action=="recommand_movies"){
            String movie = hashmap.get("movie");
            speakRaw("I think you might like "+movie);
        }
    }

}

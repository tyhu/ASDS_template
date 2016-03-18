package com.example.asds_template.nlg;

/**
 * Created by TingYao on 3/3/2016.
 */

import android.content.Context;

public class NLG {
    TTSController tts;
    public NLG(Context context){
        tts = new TTSController(context);
    }
    public void speakRaw(String msg){ tts.speakThis(msg); }
}

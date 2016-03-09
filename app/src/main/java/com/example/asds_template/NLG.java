package com.example.asds_template;

/**
 * Created by TingYao on 3/3/2016.
 */

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NLG {
    TTSController tts;
    public NLG(Context context){
        tts = new TTSController(context);
    }
    public void speakRaw(String msg){ tts.speakThis(msg); }
}

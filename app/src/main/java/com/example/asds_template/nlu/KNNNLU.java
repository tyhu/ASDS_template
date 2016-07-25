package com.example.asds_template.nlu;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by TingYao on 7/19/2016.
 */
public class KNNNLU {

    //public float sim(NgramFeat nf1, NgramFeat nf2){
    //
    //}

    public class NgramFeat{


        ArrayList<String> unigramList;
        ArrayList<String> bigramList;
        ArrayList<String> trigramList;
    }

    public class NLUFrame{
        String intent;
        Map<String, String> attributes;
    }
}

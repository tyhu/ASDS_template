package com.example.asds_template.nlu;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TingYao on 3/18/2016.
 */
public class NLU {
    List<String> dialogIntent;
    public NLU(List<String> intentLst){
        dialogIntent = intentLst;
    }
    public NLUState understanding(String asrInput){
        NLUState nluState = new NLUState();
        String [] tokens = asrInput.split(" ");
        nluState.intentIdx = 2;
        return nluState;
    }

    public class NLUState{
        int order;
        String tag;
        String query;
        int intentIdx;

        public NLUState(){
            order = 0;
            tag = "";
            query = "";
            intentIdx = 0;
        }

        public int getIntent(){ return intentIdx; }
        public String getTag(){ return tag; }
        public String getQuery(){ return query; }
    }
}

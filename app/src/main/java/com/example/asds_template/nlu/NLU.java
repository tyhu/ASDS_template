package com.example.asds_template.nlu;

import java.util.ArrayList;
import java.util.Arrays;
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
        List<String> tokens = Arrays.asList(asrInput.split(" "));
        nluState.intentIdx = 2;
        System.out.println(tokens);
        if(tokens.contains("search")){
            nluState.intentIdx = 5;
            nluState.query = extractQuery(asrInput);
        }
        else if(tokens.contains("email")){
            nluState.intentIdx = 0;
            nluState.order = 0;
        }
        else if(tokens.contains("repeat")){
            nluState.intentIdx = 3;
        }

        return nluState;
    }
    public String extractQuery(String asrinput){
        String q = "";
        List<String> tokens = Arrays.asList(asrinput.split(" "));
        int idx = tokens.indexOf("about");
        if(idx<0)
            idx = tokens.indexOf("from");
        if(idx<tokens.size()-1) q = tokens.get(idx+1);
        System.out.println("query extracted: "+q);
        return q;
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
        public int getOrder() { return order; }
    }
}

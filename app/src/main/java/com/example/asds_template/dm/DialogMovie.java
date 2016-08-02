package com.example.asds_template.dm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by TingYao on 8/2/2016.
 */
/*
 * Dialogue manager for movie demo
 * actions: recommendation (movies, entity), request preference ()
 * states: user profile (entites), recommendation time
 *
 */
public class DialogMovie {
    ArrayList<String> entities;
    int rcount;
    public DialogMovie(){
        startOver();
    }

    public HashMap takePolicy(JSONObject nluOut) throws JSONException {
        JSONArray inputEntities = nluOut.getJSONArray("entities");
        System.out.println("I am taking action");
        for(int i=0;i<inputEntities.length();i++){

            //movie backend here

            String entity =((JSONArray) inputEntities.get(i)).getString(1);
            if (!entities.contains(entity)){
                System.out.println("entity!!! "+entity);
                entities.add(entity);
            }
        }

        //==================to nlg=================
        HashMap<String, String> hashMap = new HashMap<String,String>();
        String action = "";
        String movie = "Mission Impossible";
        if (entities.size()==0)
            action = "request_profile";
        else{
            action = "recommand_movies";
            hashMap.put("movie",movie);
            rcount+=1;
        }

        hashMap.put("action",action);
        return hashMap;
    }

    public void startOver(){
        rcount = 0;
        entities = new ArrayList<String>();
        entities.clear();
    }
}

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

    public String inputMap(HashMap<String,String> hashmap){
        String action = hashmap.get("action");
        String rawout = "";
        if(action=="request_profile"){
            rawout = "Ok, tell me what you like";
        }
        else if(action=="recommand_movies"){
            String moviesStr = hashmap.get("movie_str");
            String entity = hashmap.get("reason_entity");
            String type = hashmap.get("reason_type");
            String movie_num = hashmap.get("movie_num");


            rawout+=explainStr(entity,type,movie_num);
            rawout+=" "+moviesStr;

        }
        else if (action=="infer+recommand_movie"){
            String moviesStr = hashmap.get("movie_str");
            String entity = hashmap.get("reason_entity");
            String type = hashmap.get("reason_type");
            String movie_num = hashmap.get("movie_num");
            String sourceMovieStr = hashmap.get("source_movies");
            String sourceNum = hashmap.get("source_num");

            rawout+=inferredExplain(entity, type, sourceNum, sourceMovieStr);
            rawout+=furtherRecommend(moviesStr,movie_num);
        }
        else if (action=="end"){
            rawout = "That's good. enjoy!";
        }
        speakRaw(rawout);
        return rawout;
    }
    private String explainStr(String entity,String type, String movie_num){
        String out = "";
        String entity2 = postProcess(entity);
        if (type.equals("genre"))
            if (movie_num.equals("1"))
                out+="Here is a "+entity2+" movie you might like, ";
            else
                out+="Here are "+movie_num+" "+entity2+" movies you might like, ";
        else if (type.equals("actor"))
            if (movie_num.equals("1"))
                out+="Here is a movie with "+entity2+" that you might like, ";
            else
                out+="Here are "+movie_num+" movies with "+entity2+" that you might like, ";
        else if (type.equals("director"))
            if (movie_num.equals("1"))
                out+="Here is a movie directed by "+entity2+", ";
            else
                out+="Here are "+movie_num+" movies directed by "+entity2+", ";
        return out;
    }

    private String inferredExplain(String entity,String type, String movie_num, String movieStr){
        String out = "";
        System.out.println("movie_num: "+movie_num);
        if (type.equals("genre"))
            if (movie_num.equals("1"))
                out+=movieStr+" is a good "+entity+" movie, ";
            else
                out+=movieStr+" are good "+entity+" movies, ";
        else if (type.equals("actor"))
            if (movie_num.equals("1"))
                out+=movieStr+" is a good movie with "+entity+", ";
            else
                out+=movieStr+" are good movies with "+entity+", ";
        else if (type.equals("director"))
            if (movie_num.equals("1"))
                out+=movieStr+" is directed by "+entity+" , ";
            else
                out+=movieStr+" are directed by "+entity+" , ";
        return out;
    }
    private String furtherRecommend(String moviesStr,String movie_num){
        String out = "";
        if (movie_num.equals("1"))
            out+="Here is another one, ";
        else
            out+="Here are another "+movie_num+", ";
        out+=moviesStr;
        return out;
    }

    private String postProcess(String entity){
        if (entity.equals("sci-fi")){
            return "science-fiction";
        }
        else if(entity.contains(", the")){
            return entity.split(" ")[0];
        }
        else
            return entity;
    }
}

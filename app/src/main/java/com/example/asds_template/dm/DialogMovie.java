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

    //user profile
    ArrayList<String> user_entities;
    ArrayList<String> user_movies;

    //current input
    ArrayList<String> curr_entities;
    ArrayList<String> curr_types;
    ArrayList<String> curr_movies;

    //system proposed
    ArrayList<String> systemMovies;

    int rcount;
    public DialogMovie(){
        rcount = 0;
        user_entities = new ArrayList<String>();
        user_entities.clear();
        user_movies = new ArrayList<String>();
        user_movies.clear();
        systemMovies = new ArrayList<String>();
        systemMovies.clear();
        curr_entities = new ArrayList<String>();
        curr_entities.clear();
        curr_movies = new ArrayList<String>();
        curr_movies.clear();
        curr_types = new ArrayList<String>();
        curr_types.clear();
    }

    public HashMap takePolicy(JSONObject nluOut) throws JSONException {
        String intent = nluOut.getString("label");
        JSONArray inputEntities = nluOut.getJSONArray("entities");
        System.out.println("I am taking action");

        curr_entities.clear();
        curr_movies.clear();
        curr_types.clear();
        for(int i=0;i<inputEntities.length();i++){

            //movie backend here
            String type = ((JSONArray) inputEntities.get(i)).getString(0);
            String entity =((JSONArray) inputEntities.get(i)).getString(1);
            if (!curr_entities.contains(entity)){
                System.out.println("entity!!! "+entity);
                if (type.equals("movie"))
                    curr_movies.add(entity);
                else{
                    curr_entities.add(entity);
                    curr_types.add(type);
                }

            }
        }

        //==================to nlg=================
        HashMap<String, String> hashMap = new HashMap<String,String>();
        String action = "";

        if(intent.equals("request_recommend"))
            action = "request_profile";
        else if(intent.equals("inform_preference")){
            if (curr_movies.size()==0){
                //FROM BACKEND: retrieve movies from current entities
                systemMovies = fakeBackendRetrieveMovie(curr_entities);
                action = "recommand_movies";
                hashMap.put("reason_entity",curr_entities.get(0));
                hashMap.put("reason_type",curr_types.get(0));

            }
            else{
                //FROM BACKEND: infer the entity and retrieve movies from inferred entities
                ArrayList<String> inferredEntities = fakeInferEntities(curr_movies);
                System.out.println("infer results: "+curr_movies+inferredEntities);
                systemMovies = fakeBackendRetrieveMovie(inferredEntities);
                action = "infer+recommand_movie";
                hashMap.put("source_movies",movieList2Str(curr_movies));
                hashMap.put("source_num",String.valueOf(curr_movies));
                hashMap.put("reason_entity", inferredEntities.get(0));
                hashMap.put("reason_type","director");
            }
            hashMap.put("movie_num",String.valueOf(systemMovies.size()));
            hashMap.put("movie_str",movieList2Str(systemMovies));
        }
        else if (intent.equals("decide")){
            action = "end";
        }


        hashMap.put("action", action);
        return hashMap;
    }

    public void startOver(){
        rcount = 0;
        user_entities.clear();
        user_movies.clear();
        systemMovies.clear();
    }

    private ArrayList<String> fakeBackendRetrieveMovie(List<String> entities){
        ArrayList<String> movieList = new ArrayList<String>();
        if(entities.get(0).equals("sci-fi")){
            movieList.add("star wars");
            movieList.add("jurrasic park");
        }
        else if (entities.get(0).equals("steven spielberg")){
            movieList.add("e.t. the extra-terrestrial");
        }
        else if (entities.get(0).equals("george lucas")){
            movieList.add("t-h-x eleven thirty eight");
        }
        return movieList;
    }

    private ArrayList<String> fakeInferEntities(List<String> movies){
        ArrayList<String> entitesList = new ArrayList<String>();
        if (movies.contains("jurassic park"))
            entitesList.add("steven spielberg");
        else if (movies.contains("star wars"))
            entitesList.add("george lucas");
        return entitesList;
    }

    //concatenate movie strings
    private String movieList2Str(List<String> movieList){
        String movieStr = movieList.get(0);
        if (movieList.size()==1)
            return movieStr;
        else{
            for(int i=1;i<movieList.size()-1;i++)
                movieStr+=", "+movieList.get(i);
            movieStr+=" and "+movieList.get(movieList.size()-1);
            return movieStr;
        }
    }
}

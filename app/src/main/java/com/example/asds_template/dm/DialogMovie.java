package com.example.asds_template.dm;

import android.content.Context;
import android.util.Log;

import com.example.asds_template.util.MyHttpConnect;
import com.example.asds_template.util.SocketCaller;
import com.google.api.client.json.Json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.cmu.pocketsphinx.Assets;

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
    HashMap<String,String> movieIdxMap;

    //backend
    MyHttpConnect conn;
    Context context;

    public DialogMovie(Context context){
        this.context = context;
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
        conn = new MyHttpConnect("http://sijo.ml.cmu.edu:8080/imdb/query.jsp");
        movieIdxMap = new HashMap<String,String>();
        readMovieDict();



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
            int sent = ((JSONArray) inputEntities.get(i)).getInt(4);
            if (!curr_entities.contains(entity)){
                if (type.equals("movie")) {
                    if(sent==1){
                        curr_movies.add(entity);
                        user_movies.add(entity);
                    }
                }
                else{
                    curr_entities.add(entity);
                    curr_types.add(type);
                    user_entities.add(entity);
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

                //fake
                //systemMovies = fakeBackendRetrieveMovie(curr_entities);
                //real
                systemMovies = backendRetrieveMovie(curr_entities,2);

                action = "recommand_movies";
                hashMap.put("reason_entity",curr_entities.get(0));
                hashMap.put("reason_type",curr_types.get(0));

            }
            else{
                //FROM BACKEND: infer the entity and retrieve movies from inferred entities
                //fake
                //ArrayList<String> inferredEntities = fakeInferEntities(curr_movies);
                //real
                ArrayList<String> inferredEntities = inferEntityFromSingleMovie(curr_movies.get(0));

                System.out.println("infer results: " + curr_movies + inferredEntities);
                user_entities.addAll(inferredEntities);
                //fake
                //systemMovies = fakeBackendRetrieveMovie(inferredEntities);

                //real
                systemMovies = backendRetrieveMovie(inferredEntities,3);
                //remove the first
                systemMovies.remove(0);
                systemMovies.remove(0);

                action = "infer+recommand_movie";
                hashMap.put("source_movies",movieList2Str(curr_movies));
                hashMap.put("source_num",String.valueOf(curr_movies.size()));
                hashMap.put("reason_entity", inferredEntities.get(0));
                hashMap.put("reason_type","actor");
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
        if (entities.get(0).equals("sci-fi")){
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


    public String buildJSONStr(ArrayList<String> inputEntities, int [] inputMovies, int explain_num, int result_num){
        JSONObject jobj = new JSONObject();
        JSONObject profile = new JSONObject();
        JSONObject environment = new JSONObject();
        JSONArray posEntities = new JSONArray(inputEntities);
        try {
            JSONArray posMovies = new JSONArray(inputMovies);
            profile.put("likedMovies",posMovies);
            profile.put("likedEntities",posEntities);
            environment.put("maxExplanations",explain_num);
            jobj.put("function","rexplain");
            jobj.put("profile",profile);
            jobj.put("environment",environment);
            jobj.put("maxResults",result_num);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jobj.toString();
    }

    public String httpCall2Backend(String jsonstr){
        HashMap<String, String> keyValuePairs = new HashMap<String,String>();
        keyValuePairs.put("device_id","random");
        keyValuePairs.put("query",jsonstr);
        String params = conn.SetParams(keyValuePairs);
        String rstr="";
        try{
            HttpURLConnection response = (HttpURLConnection)conn.PostToServer(params);
            System.out.println("movie backend connection success!");
            int responseCode = response.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(response.getInputStream()));
                while ((line=br.readLine()) != null) {
                    rstr+=line;
                }

            }

        } catch (IOException e){
            Log.e("Movie Backend", "connection error");
        }
        return rstr;
    }

    private ArrayList<String> inferEntityFromSingleMovie(String movieStr) throws JSONException{

        int movieIdx = Integer.parseInt(movieIdxMap.get(movieStr));
        System.out.println("input: " + movieIdx);
        ArrayList<String> dummy = new ArrayList<String>();
        int[] targetList = new int[1];
        //temporarily, select the first one,
        targetList[0] = movieIdx;
        //json build up
        String jsonstr = buildJSONStr(dummy,targetList,2,1);
        String rstr = httpCall2Backend(jsonstr);
        System.out.println("json string: "+rstr);
        JSONObject rJsonObj = new JSONObject(rstr);

        //extract entityList
        ArrayList<String> entityList = new ArrayList<String>();
        JSONArray recommendArray = rJsonObj.getJSONArray("rexplanations");
        JSONArray entityArray = ((JSONObject) recommendArray.get(0)).getJSONArray("explanations");
        entityList.add(entityTransferFunction(entityArray.getString(1)));

        return entityList;
    }

    private ArrayList<String> backendRetrieveMovie(ArrayList<String> entities,int movie_num) throws JSONException {
        int[] dummy = new int[0];
        //json build up
        String jsonstr = buildJSONStr(entities, dummy, 1,movie_num);
        //http call
        String rstr = httpCall2Backend(jsonstr);
        JSONObject rJsonObj = new JSONObject(rstr);

        //extract movie list
        ArrayList<String> movieList = new ArrayList<String>();
        JSONArray recommendArray = rJsonObj.getJSONArray("rexplanations");
        for(int i=0;i<recommendArray.length();i++){
            String mstr = recommendArray.getJSONObject(i).getString("recommendation");
            System.out.println("movie: "+mstr);

            String[] parts = mstr.split(" ");
            mstr = parts[0];
            for(int j=1;j<parts.length-1;j++)
                mstr+=" "+parts[j];
            movieList.add(mstr.toLowerCase());
        }


        return movieList;
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

    /**
     * for demo, need to be changed
     * @return
     */
    //public String typeHardCodeFunction(){}

    public String entityTransferFunction(String entity){
        if (entity.contains("sdouglasmcferran"))
            return "douglas mcferran";
        else
            return "null";
    }

    public void readMovieDict(){
        int idx = 1;
        try{
            Assets assets = new Assets(context);
            File movieFile = new File(assets.syncAssets(),"movie.txt");
            FileInputStream fstream = new FileInputStream(movieFile);

            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;
            while ((strLine = br.readLine()) != null)   {
                // Print the content on the console
                movieIdxMap.put(strLine.toLowerCase(),String.valueOf(idx));
                idx+=1;
            }
            br.close();
        } catch (IOException e){
            Log.e("read movie", e.getMessage());
        }
        System.out.println("size: "+movieIdxMap.size());

    }

    public ArrayList<String> getUserProfileEntity(){
        return user_entities;
    }

    public ArrayList<String> getUserProfileMovies(){
        ArrayList<String> allMovies = new ArrayList<String>();
        allMovies.addAll(user_movies);
        allMovies.addAll(systemMovies);
        return allMovies;
    }
}

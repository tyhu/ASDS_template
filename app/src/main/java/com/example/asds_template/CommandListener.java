package com.example.asds_template;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import java.io.File;
import java.io.IOException;

/**
 * Created by harry on 1/12/16.
 */
public class CommandListener implements RecognitionListener  {
    private MyRecognizer recognizer;
    Context context;
    private static final String CMD_START = "cmd_start";
    private static final String CMD_FINAL = "cmd_final";
    private static final String CMD_TYPE1 = "cmd1";
    private static final String CMD_CONTACT = "contact";
    private static final String CMD_CONTI = "cmd_continue";
    private static final String CMD_REPLY_ONLY = "cmd_reply_only";
    private static final String START_KEY = "in mind agent";
    private static final String REPLY_EMAIL = "reply email";
    private static final String TERMINATE_WORD = "terminate";
    private Handler commandHandler;

    public CommandListener(Context con, Handler commandHandler){
        context = con;
        this.commandHandler = commandHandler;

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(context);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    Log.e("cmd listen", e.getMessage());
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    System.out.println("exception for command listener");
                } else {
                    System.out.println("after initialization");
                    //switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }
    public void finalize() throws Throwable {
        super.finalize();
        recognizer.cancel();
        recognizer.shutdown();
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        //recognizer = defaultSetup()
        recognizer = MyRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                        // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setRawLogDir(assetsDir)

                        // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-20f)
                        //.setKeywordThreshold(1e-15f)

                        // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        recognizer.addListener(this);

        //recognizer.addKeyphraseSearch("cmd1", "reply email");
        recognizer.addKeyphraseSearch(CMD_START, "in mind agent");
        recognizer.addKeyphraseSearch(CMD_REPLY_ONLY, REPLY_EMAIL);
        recognizer.addKeyphraseSearch(CMD_FINAL, TERMINATE_WORD);
        File cmd1Grammar = new File(assetsDir, "cmd1.gram");
        File contactGrammar = new File(assetsDir, "contact2.gram");
        File continueGrammar = new File(assetsDir, "continue.gram");
        recognizer.addGrammarSearch(CMD_TYPE1, cmd1Grammar);
        recognizer.addGrammarSearch(CMD_CONTACT, contactGrammar);
        recognizer.addGrammarSearch(CMD_CONTI, continueGrammar);
        //TODO
        //I should define all the searching type here
        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */
        /*
        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

        // Create language model search
        File languageModel = new File(assetsDir, "weather.dmp");
        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

        // Phonetic search
        File phoneticModel = new File(assetsDir, "en-phone.dmp");
        recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
        */
    }

    /**
     * this function should be called by the activity requiring voice command
     * @param type
     * @param search_duration "in ms"
     */
    public void Search(String type, int search_duration){
        recognizer.stop();
        if(search_duration<0)
            recognizer.startListening(type);
        else
            recognizer.startListening(type, search_duration);
    }



    public void SuperSearch(String type, int search_duration){
        recognizer.stop();
        recognizer.startSuperListening(type, search_duration);
    }

    public void StopSearch(){
        recognizer.stop();
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
        //notify that no result to show
    }

    @Override
    public void onResult(Hypothesis hypothesis){
        System.out.println("we have result");
        //if(hypothesis.getBestScore()==-1){
        //    Message msg = new Message();
        //    msg.arg1 = 6;
        //    commandHandler.sendMessage(msg);
        //}

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if(hypothesis != null){
            String cmd = hypothesis.getHypstr();
            System.out.println(cmd);
            if (cmd.equals(START_KEY)){
                Message msg = new Message();
                msg.arg1 = 0;
                commandHandler.sendMessage(msg);
            }
            if (cmd.equals("reply email") || cmd.equals("check in box") || cmd.equals("read the email from") || cmd.equals("summarize them")){
                Message msg = new Message();
                msg.arg1 = 1;
                msg.obj = cmd;

                commandHandler.sendMessage(msg);
                recognizer.stop();
            }
            if (cmd.equals("play history")){
                Message msg = new Message();
                msg.arg1 = 5;
                msg.obj = cmd;
                recognizer.stop();
                commandHandler.sendMessage(msg);
            }
            if (cmd.equals("distracted!")){
                Message msg = new Message();
                msg.arg1 = 6;
                commandHandler.sendMessage(msg);
                recognizer.stop();
                commandHandler.sendMessage(msg);
            }
            if (cmd.equals(TERMINATE_WORD)){
                Message msg = new Message();
                msg.arg1 = 3;
                commandHandler.sendMessage(msg);
            }
            if (recognizer.getSearchName().equals(CMD_CONTACT)){
                Message msg = new Message();
                msg.arg1 = 4;
                msg.obj = cmd;
                commandHandler.sendMessage(msg);
            }
            if (cmd.equals("read the email about scheduling")){
                Message msg = new Message();
                msg.arg1 = 7;
                commandHandler.sendMessage(msg);
            }
            if (cmd.equals("read next email")){
                Message msg = new Message();
                msg.arg1 = 9;
                commandHandler.sendMessage(msg);
            }
            if (cmd.equals("continue")||cmd.equals("yes")){
                Message msg = new Message();
                msg.arg1 = 8;
                commandHandler.sendMessage(msg);
            }
        }

        //System.out.println("partial result!");
    }

    @Override
    public void onTimeout() {
        //notify that no result to show
        System.out.println("we have time out");
        Message msg = new Message();
        msg.arg1 = 2;
        commandHandler.sendMessage(msg);
    }

    @Override
    public void onError(Exception error) {

    }
}

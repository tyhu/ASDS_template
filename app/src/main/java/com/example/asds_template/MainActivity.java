package com.example.asds_template;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.asds_template.asr.CommandListener;
import com.example.asds_template.dm.DialogOne;
import com.example.asds_template.nlg.NLG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import com.example.asds_template.config.Constants;
import com.example.asds_template.nlu.NLU;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    public CommandListener commandListener;
    //public BingRecognizer bingRecognizer;
    public Handler commandHandler;
    Context context;

    TextView textView;
    TextView instructText;
    Button stopButton;
    Button voiceCMD;
    Button asrButton;
    Button denyButton;

    GmailManager gm;
    NLU nlu;
    NLG nlg;
    DialogOne dm;

    //for experiment
    int nexttag = 0;
    int nameIdx = -1;
    ArrayList<String> templates;
    ArrayList<String> namelist;
    Calendar calendar;
    SimpleDateFormat df1;
    String trans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        voiceCMD = (Button) findViewById(R.id.voiceCmd);
        stopButton = (Button) findViewById(R.id.stop_button);
        asrButton = (Button) findViewById(R.id.bingASRButton);
        denyButton = (Button) findViewById(R.id.deny_button);
        denyButton.setVisibility(View.GONE);

        textView = (TextView) findViewById(R.id.textView);
        instructText = (TextView) findViewById(R.id.instructText);

        setTitle("InMind Agent Template (for experiment)");
        //allow main thread execute network operation
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //hard code each template
        templates = new ArrayList<String>();
        templates.add("");
        templates.add("The speaker is ");
        templates.add("When will be the talk by ");
        templates.add("The talk is given by ");

        //name list:
        namelist = readNameList();
        nameIdx = 0;
        instructText.setText(namelist.get(nameIdx));

        commandHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.arg1==Constants.ASR_OUTPUT){
                    //================== pipeline =====================
                    String outStr = (String) msg.obj;
                    textView.setText(outStr);
                    try {
                        FileWriter  f = new FileWriter("/sdcard/log.txt",true);
                        f.write(trans+"\n");
                        f.write(outStr+"\n");
                        f.flush();
                        f.close();
                    } catch (IOException e){
                        System.out.println("fail to open log file");
                    }
                    trans = getTrans(nameIdx);
                    instructText.setText("try next: " + trans);

                    //commandListener.StopSearch();
                    //commandListener.Search("cmd_start", -1);
                    //nlg.speakRaw("you have no unread email");

                }

                return false;
            }
        });
        context = getApplicationContext();
        //bingRecognizer = new BingRecognizer("dmeexdia","wNUXY7NvpIw1ugB4zVcUPhVQS6Lv9MFNPWa6qWIkIFY=");
        commandListener = new CommandListener(context, commandHandler);
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        gm = new GmailManager(this.context,settings,this);
        List<String> dialogIntent = new ArrayList<String>();
        dialogIntent.add("read");
        dialogIntent.add("summarize");
        dialogIntent.add("check");
        dialogIntent.add("repeat");
        dialogIntent.add("spell");
        dialogIntent.add("search");
        nlu = new NLU(dialogIntent);
        nlg = new NLG(context,commandHandler);
        dm = new DialogOne(gm,nlg,dialogIntent);

        denyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //commandListener.SuperSearch("KW1", 20000);
                textView.setText("Listening...");
                denyButton.setVisibility(View.GONE);
            }
        });

        voiceCMD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //commandListener.Search("cmd_start",-1);
                //textView.setText("IN MIND AGENT");
                if(nameIdx<namelist.size()){
                    commandListener.StopSearch();
                    commandListener.SuperSearch("KW1", 20000, String.valueOf(nameIdx)+".raw");
                    try {
                        FileWriter  f = new FileWriter("/sdcard/log.txt",true);
                        f.write("q: "+namelist.get(nameIdx)+"\n");
                    } catch (IOException e){
                        System.out.println("fail to open log file");
                    }
                    nameIdx+=1;
                    textView.setText("Listening...");

                } else{
                    instructText.setText("The end");
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commandListener.StopSearch();
                textView.setText("STOP");
            }
        });

        asrButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commandListener.Search("cmd_start", 20000);
                textView.setText("IN MIND AGENT");
                //commandListener.StopSearch();
                //try{
                //String asrOutput = bingRecognizer.BingSuperRecognition();
                //textView.setText("output from bingASR: \n"+asrOutput);
                //}catch(IOException e){
                //    Log.e("ASDS", e.getMessage());}
            }
        });
        asrButton.setEnabled(false);
        calendar = Calendar.getInstance();
        df1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a");
        //gm.updateUnReadLstFromGmail();
    }

    public void startGMail(View view) {
        //Intent intent = new Intent(this, GmailActivity.class);
        //startActivity(intent);
        //gm.updateLabelLstFromGmail();
        //if()
        nameIdx = 0;
        trans = getTrans(nameIdx);
        instructText.setText(trans);

        //PlayBack();
    }

    public String getTrans(int idx){
        Random r = new Random();
        int ri = r.nextInt(templates.size());
        return templates.get(ri)+" "+namelist.get(idx);
    }

    public void PlayBack(){
        AudioTrack trackplay=new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT,1280, AudioTrack.MODE_STREAM);
        //trackplay.setStereoVolume((float) volume,(float) volume);
        byte[] buffer = new byte[1280];
        int byteread;
        short tmpshort;
        File historyRaw=new File("/sdcard/yahoo_test/1.raw");
        //connectAudioToServer();

        try(FileInputStream in = new FileInputStream(historyRaw)){
            trackplay.play();
            while((byteread = in.read(buffer))!=-1 ){

                //amplification
                /*for(int i = 0;i<640;i++){
                    tmpshort = (short) ((buffer[2*i] << 8) | buffer[2*i+1]);
                    //tmpshort *= 2;
                    buffer[2*i]=(byte) (tmpshort >>> 8);
                    buffer[2*i+1]=(byte) (tmpshort >>> 0);
                }*/

                trackplay.write(buffer,0,1280);
            }
            trackplay.release();
        }
        catch (Exception ex){}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    gm.isGooglePlayServicesAvailable();
                }
                break;
            case Constants.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        System.out.println("catch the name");
                        gm.setSelectedAccountName(accountName);
                        /*
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();*/

                    }
                } else if (resultCode == RESULT_CANCELED) {
                    System.out.println("Account unspecified.");
                }
                break;
            case Constants.REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    gm.chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    public ArrayList<String> readNameList(){
        File namefile = new File("/sdcard/unseen_speaker.txt");
        ArrayList<String> names = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(namefile));
            String line;

            while ((line = br.readLine()) != null) {
                names.add(line);
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            System.out.println("can not open unseen_speaker.txt");
        }
        return names;
    }
}

package com.example.asds_template;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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

    String instruction = "You can speak the following commands:\n" +
            "1. [NEXT EMAIL]: Read next email in the inbox \n" +
            "2. [REPEAT THAT]: Repeat the current email\n" +
            "3. [REPLY EMAIL]: Reply the current email\n" +
            "4. [TERMINATE]: terminate your reply and send the email";
    String instruction2 = "Were you distracted? If yes, press start to continue. If not press \"I'm NOT DISTRACTED!\" and continue";

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
    int emailIdx = -1;
    ArrayList<String> emails;
    Calendar calendar;
    SimpleDateFormat df1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        voiceCMD = (Button) findViewById(R.id.voiceCmd);
        stopButton = (Button) findViewById(R.id.stop_button);
        asrButton = (Button) findViewById(R.id.bingASRButton);
        denyButton = (Button) findViewById(R.id.deny_button);
        denyButton.setVisibility(View.GONE);
        //gmailButton = (Button) findViewById(R.id.gmail_button);
        textView = (TextView) findViewById(R.id.textView);
        instructText = (TextView) findViewById(R.id.instructText);
        instructText.setText(instruction);
        setTitle("InMind Agent Template (for experiment)");
        //allow main thread execute network operation
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //hard code each email
        emails = new ArrayList<String>();
        emails.add("mom said, how are you today?");
        emails.add("Steve said, We're going to Spice Island for your birthday.  Does tonight at 7 work for you?");
        emails.add("Tony said, Happy Birthday, Tell me 5 things that you would like, and I will surprise you with one at the party");
        emails.add("Mom said, What did you have for dinner last night?");
        emails.add("Justin said, Hey, I'm having friends over to watch some movies tonight on Netflix, can you suggest three?");
        emails.add("From your professor, I have not received your assignment, why?");

        commandHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.arg1==Constants.KEYWD_DETECTED){
                    //start keyword
                    //commandListener.Search("cmd1",7000);
                    nlg.speakRaw("Yes");
                    //commandListener.SuperSearch("cmd1", 4000);
                    textView.setText("Listening...");

                }
                else if (msg.arg1==Constants.ASR_TIME_OUT){
                    textView.setText("STOP");
                    //commandListener.StopSearch();
                    //gm.updateUnReadLstFromGmail();
                    //textView.setText("IN MIND AGENT");
                    //commandListener.Search("cmd_start", 20000);
                }
                else if (msg.arg1==-1){

                    //String asrOutput = (String)msg.obj;
                    NLU.NLUState nluState = nlu.understanding((String)msg.obj);
                    dm.inputNLUState(nluState);

                    commandListener.StopSearch();
                    commandListener.Search("cmd_start", -1);
                    //nlg.speakRaw("you have no unread email");
                    //DM
                    //nlg
                }
                else if (msg.arg1==Constants.ASR_OUTPUT){
                    //================== pipeline =====================
                    textView.setText((String) msg.obj);
                    NLU.NLUState nluState = nlu.understanding((String)msg.obj);
                    dm.inputNLUState(nluState);

                    //commandListener.StopSearch();
                    //commandListener.Search("cmd_start", -1);
                    //nlg.speakRaw("you have no unread email");

                }
                else if (msg.arg1==Constants.TTS_COMPLETE){
                    textView.setText("Listening...");
                    if (nexttag==1){
                        commandListener.Search("cmd_final", -1);
                    }
                    else if (nexttag==0){
                        commandListener.SuperSearch("KW1", 20000);
                    }
                    else if (nexttag==2){
                        textView.setText("STOP");
                        denyButton.setVisibility(View.VISIBLE);
                        instructText.setText(instruction2);
                    }
                    //commandListener.SuperSearch("KW1", 5000);
                    //textView.setText("Listening...");
                    /*
                    System.out.println("tts: "+(String)msg.obj);
                    if(((String)msg.obj).equals("Yes"))
                        commandListener.SuperSearch("cmd1", 7000);
                    else{
                        commandListener.StopSearch();
                        commandListener.Search("cmd_start", -1);
                    }
                    */
                }
                else if (msg.arg1==Constants.ASR_NEXT_EMAIL){
                    emailIdx+=1;
                    nexttag = 0;
                    if (emailIdx>emails.size()-1){
                        nlg.speakRaw("there is no more email");
                    }
                    else{
                        nlg.speakRaw(emails.get(emailIdx));
                        //commandListener.SuperSearch("KW1", 20000);
                    }
                }
                else if (msg.arg1==Constants.ASR_REPLY_EMAIL){
                    nexttag = 1;
                    nlg.speakRaw("say terminate when you finish, you can start to speak now");
                    textView.setText("Replying...");
                }
                else if (msg.arg1== Constants.ASR_TERMINATE){
                    nexttag = 0;
                    nlg.speakRaw("your email has been sent");
                    //commandListener.Search("KW1", 20000);
                }
                else if (msg.arg1== Constants.ASR_DISTRACTION){
                    nexttag = 2;
                    nlg.speakRaw("You are distracted, system shutting down");
                    textView.setText("STOP");
                    //commandListener.Search("KW1", 20000);
                }
                else if (msg.arg1== Constants.ASR_REPEAT){
                    nexttag = 0;
                    if (emailIdx>emails.size()-1){
                        nlg.speakRaw("there is no more email");
                    }
                    else{
                        nlg.speakRaw(emails.get(emailIdx));
                        //commandListener.Search("KW1", 20000);
                    }
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
                commandListener.SuperSearch("KW1", 20000);
                textView.setText("Listening...");
                instructText.setText(instruction);
                denyButton.setVisibility(View.GONE);
            }
        });

        voiceCMD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //commandListener.Search("cmd_start",-1);
                //textView.setText("IN MIND AGENT");
                commandListener.SuperSearch("KW1", 20000);
                textView.setText("Listening...");
                instructText.setText(instruction);
                //commandListener.SuperSearch("cmd1", 4000);
                //textView.setText("Listening...");
                //commandListener.Search("cmd_start", -1);
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
        emailIdx = -1;
        FileWriter f;
        try {
            calendar = Calendar.getInstance();
            String audiofn = df1.format(calendar.getTime());
            f = new FileWriter("/sdcard/log.txt",true);
            f.write(audiofn+"\n");
            f.flush();
            f.close();
        } catch(IOException e){
            System.out.println("fail to open log file");
        }
        //gm.updateUnReadLstFromGmail();
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

}

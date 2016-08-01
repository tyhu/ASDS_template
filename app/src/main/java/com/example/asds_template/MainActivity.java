package com.example.asds_template;

import android.accounts.AccountManager;
import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.example.asds_template.asr.CommandListener;
import com.example.asds_template.dm.DialogTwo;
import com.example.asds_template.nlg.NLG;

import java.util.ArrayList;
import java.util.List;
import com.example.asds_template.config.Constants;
import com.example.asds_template.nlu.MovieNLU;
import com.example.asds_template.nlu.NLU;
import com.example.asds_template.util.GmailManager;
import com.example.asds_template.util.IMAPManager;
import com.example.asds_template.util.ImapLoginActivity;

public class MainActivity extends AppCompatActivity {
//public class MainActivity extends AppCompatActivity {

    public CommandListener commandListener;
    //public BingRecognizer bingRecognizer;
    public Handler commandHandler;
    Context context;

    TextView textView;
    Button stopButton;
    Button voiceCMD;
    Button asrButton;
    ImageView yahooImage;


    GmailManager gm;
    IMAPManager imap;
    NLU nlu;
    NLG nlg;
    DialogTwo dm;
    boolean asrTest;

    SharedPreferences  inmindSharedPreferences ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        voiceCMD = (Button) findViewById(R.id.voiceCmd);
        stopButton = (Button) findViewById(R.id.stop_button);
        asrButton = (Button) findViewById(R.id.bingASRButton);
        //gmailButton = (Button) findViewById(R.id.gmail_button);
        textView = (TextView) findViewById(R.id.textView);
        yahooImage = (ImageView) findViewById(R.id.imageView);
        yahooImage.setImageResource(R.mipmap.ymail);
        setTitle("InMind Agent Template");
        //allow main thread execute network operation
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        asrTest = false;
        commandHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.arg1==Constants.KEYWD_DETECTED){
                    //====start keyword
                    //commandListener.Search("cmd1",7000);
                    nlg.speakRaw("Yes");
                    //commandListener.SuperSearch("cmd1", 4000);
                    textView.setText("Listening...");

                }
                else if (msg.arg1==Constants.ASR_TIME_OUT){
                    commandListener.StopSearch();
                    //gm.updateUnReadLstFromGmail();

                    textView.setText("IN MIND AGENT");
                    commandListener.Search("cmd_start", 20000);
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
                    if (asrTest){
                        textView.setText("ASR test result: "+msg.obj);
                        asrTest = false;
                        return false;
                    }
                    //================== pipeline =====================
                    textView.setText((String) msg.obj);
                    NLU.NLUState nluState = nlu.understanding((String)msg.obj);
                    dm.inputNLUState(nluState);

                    //commandListener.StopSearch();
                    //commandListener.Search("cmd_start", -1);
                    //nlg.speakRaw("you have no unread email");

                }
                else if (msg.arg1==Constants.TTS_COMPLETE){
                    commandListener.SuperSearch("KW1", 5000);
                    textView.setText("Listening...");
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
                return false;
            }
        });
        context = getApplicationContext();
        //bingRecognizer = new BingRecognizer("dmeexdia","wNUXY7NvpIw1ugB4zVcUPhVQS6Lv9MFNPWa6qWIkIFY=");
        commandListener = new CommandListener(context, commandHandler);
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        //gm = new GmailManager(this.context,settings,this);
        List<String> dialogIntent = new ArrayList<String>();
        dialogIntent.add("read");
        dialogIntent.add("summarize");
        dialogIntent.add("check");
        dialogIntent.add("repeat");
        dialogIntent.add("spell");
        dialogIntent.add("search");
        nlu = new NLU(dialogIntent);
        nlg = new NLG(context,commandHandler);

        voiceCMD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //commandListener.Search("cmd_start",-1);
                asrTest = false;
                textView.setText("IN MIND AGENT");
                commandListener.Search("cmd_start", 4000);
                //commandListener.SuperSearch("cmd1", 4000);
                //textView.setText("Listening...");
                //commandListener.Search("cmd_start", -1);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commandListener.StopSearch();
                textView.setText("STOP");
                imap.closeInbox();
            }
        });

        asrButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commandListener.SuperSearch("KW1", 5000);
                textView.setText("Listening...");
                asrTest = true;

                //MovieNLU mnlu = new MovieNLU("tts.speech.cs.cmu.edu",9001);
                //mnlu.understand("tell me something");
            }
        });

        //=====imap log in
        inmindSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Activity.MODE_PRIVATE);

        //startActivity(imapLogin);
        if(!inmindSharedPreferences.contains(Constants.LOGINED_FLAG)) {
            Intent imapLogin = new Intent().setClass(this.context,ImapLoginActivity.class);
            startActivityForResult(imapLogin,Constants.RECEIVE_IMAP_LOGIN);
        }
        else
            System.out.println("Hey! login info existed!!!!");
        initialImap();
        //=====end of imap log in

        dm = new DialogTwo(imap,nlg,dialogIntent);
    }

    public void initialImap(){
        String user = inmindSharedPreferences.getString(Constants.USERNAME_FLAG,"");
        String pwd = inmindSharedPreferences.getString(Constants.PWD_FLAG,"");
        String host = inmindSharedPreferences.getString(Constants.HOST_FLAG,"");
        System.out.println("user: "+user);
        System.out.println("pwd: "+pwd);
        System.out.println("host: "+host);
        imap = new IMAPManager(user,pwd,host);
    }

    public void startGMail(View view){
        //Intent intent = new Intent(this, GmailActivity.class);
        //startActivity(intent);
        //gm.updateLabelLstFromGmail();
        //if()
        //gm.updateUnReadLstFromGmail();
        //IMAPManager imap = new IMAPManager();
        imap.markAllRead();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
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
            case Constants.RECEIVE_IMAP_LOGIN:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    SharedPreferences.Editor editor = inmindSharedPreferences.edit();
                    editor.putBoolean(Constants.LOGINED_FLAG, true);
                    editor.putString(Constants.USERNAME_FLAG, data.getStringExtra(Constants.USERNAME_FLAG));
                    editor.putString(Constants.PWD_FLAG,data.getStringExtra(Constants.PWD_FLAG));
                    editor.putString(Constants.HOST_FLAG,data.getStringExtra(Constants.HOST_FLAG));
                    editor.apply();

                    //initialize
                    initialImap();
                    dm.setImap(imap);
                }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}

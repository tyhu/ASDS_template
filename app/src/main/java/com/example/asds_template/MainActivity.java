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

import java.util.ArrayList;
import java.util.List;
import com.example.asds_template.config.Constants;
import com.example.asds_template.nlu.NLU;

public class MainActivity extends AppCompatActivity {

    public CommandListener commandListener;
    //public BingRecognizer bingRecognizer;
    public Handler commandHandler;
    Context context;

    TextView textView;
    Button stopButton;
    Button voiceCMD;
    Button asrButton;

    GmailManager gm;
    NLU nlu;
    NLG nlg;
    DialogOne dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        voiceCMD = (Button) findViewById(R.id.voiceCmd);
        stopButton = (Button) findViewById(R.id.stop_button);
        asrButton = (Button) findViewById(R.id.bingASRButton);
        //gmailButton = (Button) findViewById(R.id.gmail_button);
        textView = (TextView) findViewById(R.id.textView);

        //allow main thread execute network operation
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        commandHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.arg1==0){
                    //start keyword
                    //commandListener.Search("cmd1",7000);
                    nlg.speakRaw("Yes");
                    commandListener.SuperSearch("cmd1", 4000);
                    textView.setText("Listening...");

                }
                else if (msg.arg1==1){
                    //================== pipeline =====================
                    //String asrOutput = (String)msg.obj;
                    NLU.NLUState nluState = nlu.understanding((String)msg.obj);
                    dm.inputNLUState(nluState);

                    commandListener.StopSearch();
                    commandListener.Search("cmd_start", -1);
                    //nlg.speakRaw("you have no unread email");
                    //DM
                    //nlg
                }
                else if (msg.arg1==10){
                    //commandListener.Search("cmd1",7000);
                    textView.setText((String) msg.obj);
                    NLU.NLUState nluState = nlu.understanding((String)msg.obj);
                    dm.inputNLUState(nluState);
                    commandListener.StopSearch();
                    commandListener.Search("cmd_start", -1);
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
        nlu = new NLU(dialogIntent);
        nlg = new NLG(context);
        dm = new DialogOne(gm,nlg,dialogIntent);


        voiceCMD.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                //commandListener.Search("cmd_start",-1);
                commandListener.Search("cmd_start", 4000);
                //commandListener.SuperSearch("cmd1", 4000);
                textView.setText("Listening...");
                //commandListener.Search("cmd_start", -1);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commandListener.StopSearch();
            }
        });

        asrButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                commandListener.Search("cmd_start", -1);
                textView.setText("Listening...");
                //commandListener.StopSearch();
                //try{
                //String asrOutput = bingRecognizer.BingSuperRecognition();
                //textView.setText("output from bingASR: \n"+asrOutput);
                //}catch(IOException e){
                //    Log.e("ASDS", e.getMessage());}
            }
        });
        gm.updateUnReadLstFromGmail();
    }

    public void startGMail(View view){
        //Intent intent = new Intent(this, GmailActivity.class);
        //startActivity(intent);
        List<String> labels = new ArrayList<String>();
        //gm.updateLabelLstFromGmail();
        gm.updateUnReadLstFromGmail();
        System.out.println(labels);
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
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}

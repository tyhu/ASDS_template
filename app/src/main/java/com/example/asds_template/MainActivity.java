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
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.asds_template.asr.CommandListener;
import com.example.asds_template.dm.DialogOne;
import com.example.asds_template.dm.DialogTwo;
import com.example.asds_template.nlg.NLG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.example.asds_template.config.Constants;
import com.example.asds_template.nlu.NLU;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {
//public class MainActivity extends AppCompatActivity {

    public CommandListener commandListener;
    //public BingRecognizer bingRecognizer;
    public Handler commandHandler;
    Context context;

    TextView textView;
    Button stopButton;
    Button voiceCMD;
    Button asrButton;

    GmailManager gm;
    IMAPManager imap;
    NLU nlu;
    NLG nlg;
    DialogTwo dm;

    SharedPreferences  inmindSharedPreferences ;

    //video part
    private CameraBridgeViewBase   mOpenCvCameraView;
    private static final String    TAG                 = "OCVSample::Activity";
    /*
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;
    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem               mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;*/

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");


                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        voiceCMD = (Button) findViewById(R.id.voiceCmd);
        stopButton = (Button) findViewById(R.id.stop_button);
        asrButton = (Button) findViewById(R.id.bingASRButton);
        //gmailButton = (Button) findViewById(R.id.gmail_button);
        textView = (TextView) findViewById(R.id.textView);
        setTitle("InMind Agent Template");
        //allow main thread execute network operation
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.videoView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

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

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }
}

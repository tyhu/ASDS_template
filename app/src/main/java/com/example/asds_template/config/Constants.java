package com.example.asds_template.config;

/**
 * Created by TingYao on 3/17/2016.
 */
public class Constants {

    //==dialogue message for handler
    public static final int KEYWD_DETECTED = 0;
    public static final int ASR_TIME_OUT = 1;
    public static final int ASR_OUTPUT = 2;
    public static final int TTS_COMPLETE = 3;

    //==Activity result
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int RECEIVE_IMAP_LOGIN = 1003;

    //==flag for share preference
    public static final String PREFERENCE_NAME = "yahoo_inmind_preference";
    public static final String LOGINED_FLAG = "logined";
    public static final String USERNAME_FLAG = "username";
    public static final String HOST_FLAG = "host";
    public static final String PWD_FLAG = "pwd";
}

package com.example.asds_template.util;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.example.asds_template.dm.DialogOne;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.asds_template.config.Constants;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by TingYao on 3/17/2016.
 */
public class GmailManager {
    GoogleAccountCredential mCredential;
    //static final int REQUEST_ACCOUNT_PICKER = 1000;
    //static final int REQUEST_AUTHORIZATION = 1001;
    //static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "inminddistraction";
    //private static final String[] SCOPES = { GmailScopes.GMAIL_LABELS };
    private static final String[] SCOPES = { GmailScopes.GMAIL_MODIFY };
    private SharedPreferences settings;
    Context context;
    Activity rootActivity;

    //****** Gmail content
    List<String> labels;
    List<Message> messages;
    String query = "";
    //List<String> snippets;

    public GmailManager(Context context, SharedPreferences settings, Activity rootActivity){
        this.settings = settings;
        this.context = context;
        this.rootActivity = rootActivity;
        mCredential = GoogleAccountCredential.usingOAuth2(
                context, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        messages = new ArrayList<Message>();
    }

    public int unreadNum(){ return messages.size(); }

    public void markAsRead(String msgId){
        MarkRequestTask setUnreadTask = new MarkRequestTask(msgId);
        setUnreadTask.execute();
    }

    public boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            System.out.println("please update the google play service");
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    public void chooseAccount() {
        rootActivity.startActivityForResult(
                mCredential.newChooseAccountIntent(), Constants.REQUEST_ACCOUNT_PICKER);
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                rootActivity,
                Constants.REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private Gmail getGmailService(){
        com.google.api.services.gmail.Gmail mService = null;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.gmail.Gmail.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Gmail API Android Quickstart")
                .build();
        return mService;
    }

    public int getUnReadNum(){
        return messages.size();
    }

    public Message getMsg(int order){
        Message msg = messages.get(order);
        return msg;
    }

    public int getOrder(String msgid){
        int order = -1;
        for(int i=0;i<messages.size();i++)
            if(messages.get(i).getId().equals(msgid)){
                order = i;
                break;
            }
        return order;
    }

    public String getSender(int order){
        String sender = "unknown ";
        String jstr = getMsg(order).getPayload().getHeaders().toString();
        try{
            JSONArray obj = new JSONArray(jstr);
            for(int i=0;i<obj.length();i++){
                System.out.println(obj.getJSONObject(i).getString("name"));
                if(obj.getJSONObject(i).getString("name").equals("From")) {
                    sender = obj.getJSONObject(i).getString("value");
                    sender = sender.split("<")[0];
                }
            }
        } catch (JSONException e) {
            System.out.println("json warning: "+e.toString());
        }
        return sender;
    }

    public void searchLstFromGmail(String q, DialogOne.gmailResponse gmr){
        Searchtask searchtask = new Searchtask(gmr,q);
        searchtask.execute();
    }

    public void updateUnReadLstFromGmail(){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if(mCredential.getSelectedAccountName() == null)
                    chooseAccount();
                com.google.api.services.gmail.Gmail mService = getGmailService();

                String user = "me";
                ArrayList<String> tmpLabs = new ArrayList<String>();
                //if(messages == null)

                tmpLabs.add("UNREAD");
                try {
                    ListMessagesResponse listResponse = mService.users().messages().list(user)
                            .setLabelIds(tmpLabs).execute();
                    //System.out.println(listResponse.size());
                    messages.clear();
                    for (Message msg : listResponse.getMessages()) {
                        //System.out.println(msg.getId());
                        //Message message = mService.users().messages().get(user, msg.getId()).setFormat("raw").execute();
                        Message message = mService.users().messages().get(user, msg.getId()).execute();
                        messages.add(message);
                    }
                } catch (Exception e){
                    System.out.println(e.toString());
                    requestException(e);
                }

                return null;
            }
        }.execute();
    }

    public void updateLabelLstFromGmail() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if(mCredential.getSelectedAccountName() == null)
                    chooseAccount();
                com.google.api.services.gmail.Gmail mService = getGmailService();

                String user = "me";
                if(labels==null)
                    labels = new ArrayList<String>();
                try {

                    ListLabelsResponse listResponse =
                            mService.users().labels().list(user).execute();
                    for (Label label : listResponse.getLabels()) {
                        labels.add(label.getName());
                        System.out.println(label.getName());
                    }

                } catch (Exception e){
                    System.out.println(e.toString());
                    requestException(e);
                }

                return null;
            }
        }.execute();
    }

    public void setSelectedAccountName(String accountName){
        mCredential.setSelectedAccountName(accountName);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME, accountName);
        editor.apply();
    }

    public void requestException(Exception mLastError){
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    rootActivity.startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            Constants.REQUEST_AUTHORIZATION);
                //chooseAccount();
            } else {
                System.out.println("The following error occurred:\n"+mLastError.getMessage());
                //mOutputText.setText("The following error occurred:\n"
                //        + mLastError.getMessage());
            }
        } else {
            System.out.println("Request cancelled.");
        }
    }

    private class Searchtask extends AsyncTask<Void, Void, Void> {
        private DialogOne.gmailResponse gmr;
        private String query;
        public Searchtask(DialogOne.gmailResponse gmr, String query){
            this.gmr = gmr;
            this.query = query;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(mCredential.getSelectedAccountName() == null)
                chooseAccount();
            com.google.api.services.gmail.Gmail mService = getGmailService();

            String user = "me";
            ArrayList<String> tmpLabs = new ArrayList<String>();
            //if(messages == null)

            tmpLabs.add("UNREAD");
            try {
                ListMessagesResponse listResponse = mService.users().messages().list(user)
                        .setLabelIds(tmpLabs).setQ(query).execute();
                System.out.println(listResponse.size()+" emails retrieved");
                messages.clear();
                for (Message msg : listResponse.getMessages()) {
                    System.out.println(msg.getId());
                    //Message message = mService.users().messages().get(user, msg.getId()).setFormat("raw").execute();
                    Message message = mService.users().messages().get(user, msg.getId()).execute();
                    messages.add(message);
                }
            } catch (Exception e){
                System.out.println(e.toString());
                requestException(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            gmr.informSearchResults(messages.size());
        }
    }

    private class MarkRequestTask extends AsyncTask<Void, Void, Void> {
        private String msgId;

        public MarkRequestTask(String msgId) {
            this.msgId = msgId;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Gmail service = getGmailService();
            List<String> labelsToRemove = new ArrayList<String>();
            labelsToRemove.add("UNREAD");
            ModifyMessageRequest mods = new ModifyMessageRequest().setRemoveLabelIds(labelsToRemove);
            try{
                service.users().messages().modify("me", msgId, mods).execute();
            } catch (IOException e) {
                System.out.println(e.toString());
            }
            messages.remove(getOrder(msgId));
            return null;
        }
    }
}

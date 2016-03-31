package com.example.asds_template.dm;

import com.example.asds_template.GmailManager;
import com.example.asds_template.nlg.NLG;
import com.example.asds_template.nlu.NLU;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.util.List;

/**
 * Created by TingYao on 3/18/2016.
 */
public class DialogOne {
    //======additional resource======
    GmailManager gm;
    NLG nlg;

    List<String> dialogIntent;
    dialogueState state;

    public DialogOne(GmailManager gm,NLG nlg, List<String> dialogIntent){
        this.gm = gm;
        this.nlg = nlg;
        this.dialogIntent = dialogIntent;
        state = new dialogueState();
    }

    public void inputNLUState(NLU.NLUState nluState){
        int intentIdx = nluState.getIntent();
        state.setCurrentIntent(dialogIntent.get(intentIdx));
        //update state
        //take action
        if(state.getCurrentIntent().equals("check")) {
            nlg.InformUnread(gm.getUnReadNum());
            state.setLastAction("check");
        }
        else if(state.getCurrentIntent().equals("read")){
            if(gm.unreadNum()==0){
                nlg.InformUnread(gm.getUnReadNum());
                state.setLastAction("check");
            }
            else{
                state.setFocusMsg(gm.getMsg(nluState.getOrder()));
                Message msg = state.getFocusMsg();
                gm.markAsRead(msg.getId());
                //System.out.println("snippet: "+msg.getSnippet());
                String snippet = msg.getSnippet();
                String sender = gm.getSender(nluState.getOrder());
                nlg.InformEmail(sender,snippet);
                state.setLastAction("read");
            }
        }
        else if(state.getCurrentIntent().equals("repeat")){
            System.out.println("last action: "+state.getLastAction());
            if(state.getLastAction().equals("read")){
                Message msg = state.getFocusMsg();
                nlg.speakRaw(msg.getSnippet());
            }
            else if(state.getLastAction().equals("check")){
                nlg.InformUnread(gm.getUnReadNum());
            }

        }
    }

    //===action set: read, check, summarize, repeat, spell
    public class dialogueState{
        String lastAction;
        String currentIntent;
        Message focusMsg;

        public dialogueState(){
            lastAction="none";
            currentIntent = "";
        }

        public String getLastAction() {return lastAction;}
        public Message getFocusMsg() { return focusMsg; }
        public String getCurrentIntent() {return currentIntent; }

        public void setLastAction(String lastAction) {
            this.lastAction = lastAction;
        }
        public void setFocusMsg(Message focusMsg) {
            this.focusMsg = focusMsg;
        }
        public void setCurrentIntent(String currentIntent) {
            this.currentIntent = currentIntent;
        }
    }
}

package com.example.asds_template.dm;

import com.example.asds_template.IMAPManager;
import com.example.asds_template.nlg.NLG;
import com.example.asds_template.nlu.NLU;

import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Multipart;

/**
 * Created by TingYao on 4/16/2016.
 */
public class DialogTwo {
    IMAPManager imap;
    NLG nlg;

    List<String> dialogIntent;
    dialogueState state;
    gmailResponse gmr;

    public DialogTwo(IMAPManager imap,NLG nlg, List<String> dialogIntent){
        this.imap = imap;
        this.nlg = nlg;
        this.dialogIntent = dialogIntent;
        state = new dialogueState();
        gmr = new gmailResponse();
    }

    public void inputNLUState(NLU.NLUState nluState){
        int intentIdx = nluState.getIntent();
        state.setCurrentIntent(dialogIntent.get(intentIdx));
        //update state
        //take action
        if(state.getCurrentIntent().equals("check")) {
            imap.checkInBox();
            nlg.InformUnread(imap.getUnReadNum());
            state.setLastAction("check");
        }
        else if(state.getCurrentIntent().equals("read")){
            if(imap.getUnReadNum()==0){
                nlg.InformUnread(imap.getUnReadNum());
                state.setLastAction("check");
            }
            else{
                state.setFocusMsg(imap.getMsg(nluState.getOrder()));
                Message msg = state.getFocusMsg();
                try {
                    Multipart mp = (Multipart) msg.getContent();
                    BodyPart bp = mp.getBodyPart(0);
                    String content = bp.getContent().toString();
                    String sender = msg.getFrom()[0].toString();
                    nlg.InformEmail(sender,content);
                    msg.setFlag(Flags.Flag.SEEN, true);
                    imap.removeMsgLocal(nluState.getOrder());
                } catch (Exception mex) {
                    mex.printStackTrace();
                }
                /*
                gm.markAsRead(msg.getId());
                //System.out.println("snippet: "+msg.getSnippet());
                String snippet = msg.getSnippet();
                String sender = gm.getSender(nluState.getOrder());
                nlg.InformEmail(sender,snippet);
                state.setLastAction("read");
                */
            }
        }
        else if(state.getCurrentIntent().equals("repeat")){
            System.out.println("last action: "+state.getLastAction());
            if(state.getLastAction().equals("read")){
                Message msg = state.getFocusMsg();
                try {
                    Multipart mp = (Multipart) msg.getContent();
                    BodyPart bp = mp.getBodyPart(0);
                    nlg.speakRaw(bp.getContent().toString());
                } catch (Exception mex) {
                    mex.printStackTrace();
                }
            } else if (state.getLastAction().equals("check")){
                nlg.InformUnread(imap.getUnReadNum());
            }

        } else if (state.getCurrentIntent().equals("search")){
            //imap.searchLstFromGmail(nluState.getQuery(),gmr);
            imap.searchContent(nluState.getQuery());
            nlg.InformFound(imap.getUnReadNum());
            state.setLastAction("search");
        }
    }

    public class gmailResponse {
        public void informSearchResults(int num){
            nlg.InformFound(num);
            state.setLastAction("search");
        }
    }

    //===action set: read, check, summarize, repeat, spell, search
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

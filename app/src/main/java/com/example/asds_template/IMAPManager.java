package com.example.asds_template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;
import javax.mail.search.SearchTerm;

/**
 * Created by TingYao on 4/11/2016.
 */
public class IMAPManager {
    private String host;
    private String user;
    private String passwd;

    List<Message> messages;
    Message current_msg;

    Properties props;
    Folder inbox;

    public IMAPManager (String user,String passwd,String host){
        this.host = host;
        this.user = user;
        this.passwd = passwd;
        System.out.println("Imap manager initialized!!");
        //host = "imap.srv.cs.cmu.edu";
        //user = "tingyaoh";
        //passwd = "for2scs3email";
        props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        messages = new ArrayList<Message>();
    }

    public void checkInBox(){
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(host, user, passwd);
            closeInbox();
            inbox = store.getFolder("INBOX");

            inbox.open(Folder.READ_WRITE);
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            messages = Arrays.asList(inbox.search(ft));
            System.out.println("num of msg: "+messages.size());

            /*
            Message msg = inbox.getMessage(inbox.getMessageCount());
            Address[] in = msg.getFrom();
            for (Address address : in) {
                System.out.println("FROM:" + address.toString());
            }
            Multipart mp = (Multipart) msg.getContent();
            BodyPart bp = mp.getBodyPart(0);
            System.out.println("SENT DATE:" + msg.getSentDate());
            System.out.println("SUBJECT:" + msg.getSubject());
            System.out.println("CONTENT:" + bp.getContent());*/
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }

    public String parseContent(Message msg){
        String out = "um, something i don't understand";
        try {
            String contentType = msg.getContentType().toLowerCase();
            if(contentType.contains("text/plain")|| contentType.contains("text/html"))
                out = msg.getContent().toString();
            else if(contentType.contains("multipart/alternative")){
                BodyPart bp = ((Multipart) msg.getContent()).getBodyPart(0);
                out = bp.getContent().toString();
            }

        } catch (Exception mex) {
            mex.printStackTrace();
        }
        return out;
    }

    public String parseSender(String raw){
        return raw.split("<")[0];
    }

    public void closeInbox(){
        try {
            if(inbox!=null&&inbox.isOpen())
                inbox.close(true);
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }

    public void markAllRead(){
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(host, user, passwd);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            messages = Arrays.asList(inbox.search(ft));
            System.out.println("num of msg: " + messages.size());
            Message [] msgArr = new Message[messages.size()];
            messages.toArray(msgArr);
            inbox.setFlags(msgArr, new Flags(Flags.Flag.SEEN), true);
            inbox.close(true);

            /*
            Message msg = inbox.getMessage(inbox.getMessageCount());
            Address[] in = msg.getFrom();
            for (Address address : in) {
                System.out.println("FROM:" + address.toString());
            }
            Multipart mp = (Multipart) msg.getContent();
            BodyPart bp = mp.getBodyPart(0);
            System.out.println("SENT DATE:" + msg.getSentDate());
            System.out.println("SUBJECT:" + msg.getSubject());
            System.out.println("CONTENT:" + bp.getContent());*/
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }


    public int getUnReadNum(){
        return messages.size();
    }

    public Message getMsg(int order){
        Message msg = messages.get(reverseOrder(order));
        return msg;
    }

    public int reverseOrder(int order){ return messages.size()-1-order; }

    public void printInfo(){
        System.out.println("IMAPManager: ");
        System.out.println("username: "+user);
        System.out.println("pwd: "+passwd);
        System.out.println("hostname: "+host);

    }

    public void searchContent(String query){
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(host, user, passwd);
            closeInbox();
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            ContentSearchTerm cst = new ContentSearchTerm(query);
            messages = Arrays.asList(inbox.search(cst));

        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }

    /**
     * Search term in email content
     */
    public class ContentSearchTerm extends SearchTerm {
        private String content;

        public ContentSearchTerm(String content) {
            this.content = content;
        }

        @Override
        public boolean match(Message message) {

            String messageContent = parseContent(message);
            if (messageContent.contains(content)) {
                return true;
            }
            /*
            try {

                String contentType = message.getContentType().toLowerCase();
                if (contentType.contains("text/plain")
                        || contentType.contains("text/html")) {
                    String messageContent = message.getContent().toString();
                    //if (messageContent.contains(content)&&!message.isSet(Flags.Flag.SEEN)) {
                    if (messageContent.contains(content)) {
                        return true;
                    }
                }
                else if(contentType.contains("multiparty")){

                }
            } catch (MessagingException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }*/
            return false;
        }

    }

    public class FromFieldSearchTerm extends SearchTerm {
        private String fromEmail;

        public FromFieldSearchTerm(String fromEmail) {
            this.fromEmail = fromEmail;
        }

        @Override
        public boolean match(Message message) {
            try {
                Address[] fromAddress = message.getFrom();
                if (fromAddress != null && fromAddress.length > 0) {
                    if (fromAddress[0].toString().contains(fromEmail)&&!message.isSet(Flags.Flag.SEEN)) {
                        return true;
                    }
                }
            } catch (MessagingException ex) {
                ex.printStackTrace();
            }

            return false;
        }

    }
}

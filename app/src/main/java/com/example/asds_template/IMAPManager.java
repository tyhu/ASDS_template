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

    private boolean auth;
    private boolean debuggable;

    public IMAPManager (String user,String passwd,String host){
        this.host = host;
        this.user = user;
        this.passwd = passwd;
        System.out.println("Imap manager initialized!!");
        //host = "imap.srv.cs.cmu.edu";
        //user = "tingyaoh";
        //passwd = "for2scs3email";
    }

    public void checkInBox(){
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(host, user, passwd);
            Folder inbox = store.getFolder("INBOX");
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

    public void removeMsgLocal(int order){
        System.out.println("remove MstLocal");
        messages.remove(reverseOrder(order));
        System.out.println("num of msg now: "+messages.size());
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
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(host, user, passwd);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            ContentSearchTerm cst = new ContentSearchTerm(query);
            messages.clear();
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
            try {
                String contentType = message.getContentType().toLowerCase();
                if (contentType.contains("text/plain")
                        || contentType.contains("text/html")) {
                    String messageContent = message.getContent().toString();
                    if (messageContent.contains(content)) {
                        return true;
                    }
                }
            } catch (MessagingException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
                    if (fromAddress[0].toString().contains(fromEmail)) {
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

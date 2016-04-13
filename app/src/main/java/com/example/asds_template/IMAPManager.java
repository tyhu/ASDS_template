package com.example.asds_template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;

/**
 * Created by TingYao on 4/11/2016.
 */
public class IMAPManager {
    private String host;
    private String port;
    private String user;
    private String passwd;

    List<Message> messages;
    Message current_msg;

    private boolean auth;
    private boolean debuggable;

    public IMAPManager (){
        host = "imap.srv.cs.cmu.edu";
        user = "tingyaoh";
        passwd = "for2scs3email";

    }

    public void checkInBox(){
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        try {
            Session session = Session.getInstance(props, null);
            Store store = session.getStore();
            store.connect(host, user, passwd);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            messages = Arrays.asList(inbox.getMessages());

            Message msg = inbox.getMessage(inbox.getMessageCount());
            Address[] in = msg.getFrom();
            for (Address address : in) {
                System.out.println("FROM:" + address.toString());
            }
            Multipart mp = (Multipart) msg.getContent();
            BodyPart bp = mp.getBodyPart(0);
            System.out.println("SENT DATE:" + msg.getSentDate());
            System.out.println("SUBJECT:" + msg.getSubject());
            System.out.println("CONTENT:" + bp.getContent());
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }
}

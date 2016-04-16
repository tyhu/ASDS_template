package com.example.asds_template;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.asds_template.config.Constants;

public class ImapLoginActivity extends AppCompatActivity {

    String address;
    String pwd;
    String host;

    Button confirmButton;
    Button refreshButton;

    EditText address_edit;
    EditText pwd_edit;
    EditText host_edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imap_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        confirmButton = (Button) findViewById(R.id.button_confirm);
        refreshButton = (Button) findViewById(R.id.button_refresh);
        address_edit = (EditText) findViewById(R.id.edit_email);
        pwd_edit = (EditText) findViewById(R.id.edit_pwd);
        host_edit = (EditText) findViewById(R.id.edit_host);

        host_edit.setText("imap.srv.cs.cmu.edu");

        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                address = address_edit.getText().toString();
                pwd = pwd_edit.getText().toString();
                host = host_edit.getText().toString();
                if(address.equals("")||pwd.equals("")||host.equals(""))
                    System.out.println("Info isn't completed");
                else{
                    Intent loginInfo=new Intent();
                    loginInfo.putExtra(Constants.USERNAME_FLAG,address);
                    loginInfo.putExtra(Constants.PWD_FLAG,pwd);
                    loginInfo.putExtra(Constants.HOST_FLAG,host);
                    setResult(RESULT_OK, loginInfo);
                    finish();
                }

            }
        });
        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });
    }



}

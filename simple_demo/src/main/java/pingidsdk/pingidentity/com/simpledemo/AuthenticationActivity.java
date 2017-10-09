package pingidsdk.pingidentity.com.simpledemo;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import pingidsdk.pingidentity.com.simpledemo.beans.AuthenticationData;
import pingidsdkclient.PingID;

import static pingidsdk.pingidentity.com.simpledemo.beans.AuthenticationData.TRANSACTION_TYPE_AUTHENTICATION;

//
// Class Name : AuthenticationActivity
// App name : Moderno
//
// This activity is used for MFA user authentication. It can be modified to prompt the user for a PIN-Code,
// fingerprint or any other way to verify the user's identity
//
// See LICENSE.txt for this sample’s licensing information and LICENSE_SDK.txt for the PingID SDK library licensing information.
// Created by Ping Identity on 3/23/17.
// Copyright © 2017 Ping Identity. All rights reserved.
//
public class AuthenticationActivity extends BaseActivity {

    public static final String TAG = AuthenticationActivity.class.getName();

    private static final String KEY_TIMEOUT = "timeout";
    public static final String KEY_CLIENT_CONTEXT = "client_context";
    private boolean isTransaction = false; //regular authentication or transaction approval
    CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        TextView textViewCaption1 = (TextView) findViewById(R.id.transactionApprovalCaption);
        TextView textViewCaption2 = (TextView) findViewById(R.id.textCaption2);
        TextView textViewCaption3 = (TextView) findViewById(R.id.textCaption3);
        TextView textViewCaption4 = (TextView) findViewById(R.id.textCaption4);

        AuthenticationData authenticationData = null;
        //if there are text extras for the captions - get them and display them
        if (getIntent()!=null && getIntent().getExtras()!=null && getIntent().getExtras().containsKey(KEY_CLIENT_CONTEXT) && getIntent().getExtras().getString(KEY_CLIENT_CONTEXT)!=null) {
            String clientContext = getIntent().getExtras().containsKey(KEY_CLIENT_CONTEXT) ? getIntent().getStringExtra(KEY_CLIENT_CONTEXT) : getString(R.string.auth_please_authenticate);

            //get the authentication data (which will be used later)
            authenticationData = new Gson().fromJson(clientContext, AuthenticationData.class);
            if (authenticationData.getTransactionType().equals(TRANSACTION_TYPE_AUTHENTICATION)){
                //regular authentication
                isTransaction=false;
                textViewCaption3.setText(getString(R.string.prompt_username));
                textViewCaption4.setText(authenticationData.getMsg());
                textViewCaption3.setVisibility(View.VISIBLE);
                textViewCaption4.setVisibility(View.VISIBLE);
            }else{
                //step up
                isTransaction=true;
                textViewCaption1.setText(getString(R.string.auth_transaction_authorization));
                textViewCaption4.setText(authenticationData.getMsg());
                textViewCaption2.setVisibility(View.VISIBLE);
                textViewCaption3.setVisibility(View.VISIBLE);
                textViewCaption4.setVisibility(View.VISIBLE);
            }

            //store the clientContext data in the global hashmap for future use
            if (PingIDSdkDemoApplication.globalData.containsKey(KEY_CLIENT_CONTEXT)){
                PingIDSdkDemoApplication.globalData.remove(KEY_CLIENT_CONTEXT);
            }
            PingIDSdkDemoApplication.globalData.put(KEY_CLIENT_CONTEXT, clientContext);
        }else{
            textViewCaption1.setText(getString(R.string.auth_please_authenticate));
        }

        //prepare the "approve" button
        final Button approveButton = (Button)findViewById(R.id.approveButton);
        approveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                approveButton.setEnabled(false);
                setAuthenticationStatus(PingID.PIDActionType.PIDActionTypeApprove);

            }
        });

        //prepare the "deny" button
        final Button denyButton = (Button)findViewById(R.id.denyButton);
        final AuthenticationData finalAuthenticationData = authenticationData;
        denyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                denyButton.setEnabled(false);
                setAuthenticationStatus(PingID.PIDActionType.PIDActionTypeDeny);

            }
        });

        //set up a countdown timer
        int timeout = 40000; //default value is 40 seconds
        if (getIntent()!=null && getIntent().getExtras()!=null && getIntent().getExtras().containsKey(KEY_TIMEOUT)) {
            timeout = getIntent().getIntExtra(KEY_TIMEOUT, 40000);
        }
        countDownTimer = new CountDownTimer(timeout, timeout) {

            @Override
            public void onTick(long millisUntilFinished) {
                // do nothing
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "Authentication timeout. Closing the activity");
                displayStatusImage(true);
                //waitAndCloseActivity();
            }

        }.start();

    }

    public void onAuthenticationCompleted(final PingID.PIDActionStatus status, final PingID.PIDActionType actionType){


        if (actionType.equals(PingID.PIDActionType.PIDActionTypeApprove)){
            if (status.equals(PingID.PIDActionStatus.SUCCESS)) {
                //if the flow was completed successfully - display success msg
                displayStatusImage(false);
            }else{
                displayAlertDialog("", getString(R.string.error_authentication_failed), getString(R.string.ok), new Runnable() {
                    @Override
                    public void run() {
                        AuthenticationActivity.this.finish();
                    }
                });
            }
        }else{
            //this close block will be execute in case the user clicked on "Deny"
            //if the user clicked "deny" - display a msg
            if (isTransaction) {
                displayAlertDialog("", getString(R.string.transaction_denied), getString(R.string.ok), new Runnable() {
                    @Override
                    public void run() {
                        AuthenticationActivity.this.finish();
                    }
                });
            }else{
                displayAlertDialog("", getString(R.string.authentication_denied), getString(R.string.ok), new Runnable() {
                    @Override
                    public void run() {
                        AuthenticationActivity.this.finish();
                    }
                });
            }
        }
    }

    public void displayStatusImage(boolean isTimeout){
        LinearLayout viewSuccess = (LinearLayout)this.findViewById(R.id.viewSuccess);
        LinearLayout viewAuth = (LinearLayout)this.findViewById(R.id.viewAuth);
        ImageView approvedView = (ImageView)findViewById(R.id.approvedImage);
        ImageView transferredView = (ImageView)findViewById(R.id.transferredImage);
        ImageView timeoutView = (ImageView)findViewById(R.id.timeoutImage);

        if (isTimeout){
            timeoutView.setVisibility(View.VISIBLE);
        }else {
            if (isTransaction) {
                transferredView.setVisibility(View.VISIBLE);
            } else {
                approvedView.setVisibility(View.VISIBLE);
            }
        }
        viewSuccess.setVisibility(View.VISIBLE);
        viewAuth.setVisibility(View.GONE);
        waitAndCloseActivity();
    }
    //finalize the authentication status based on the button the user clicked - approve/deny
    private void setAuthenticationStatus(PingID.PIDActionType actionType){

        try {
            Log.i(TAG, "setAuthenticationStatus triggered. actionType=" + actionType.name());
            countDownTimer.cancel();
            PingID.getInstance().setAuthenticationUserSelection(actionType);

            //waitAndCloseActivity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Wait for 3 seconds and them close the activity
     */
    private void waitAndCloseActivity(){
        //wait 3 seconds before closing the activity
        new CountDownTimer(4000, 4000) {

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                finish();
            }

        }.start();
    }

}

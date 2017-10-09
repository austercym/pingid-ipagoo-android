package pingidsdk.pingidentity.com.simpledemo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;

import pingidsdkclient.PIDUserSelectionObject;
import pingidsdkclient.PingID;

import static pingidsdk.pingidentity.com.simpledemo.PingIDSdkDemoApplication.addLogLine;

//
// Class Name : BaseActivity
// App name : Moderno
//
// This base activity class contains the boiler plate code to
// support PingID for Customers functionality across an application
//
// See LICENSE.txt for this sample’s licensing information and LICENSE_SDK.txt for the PingID SDK library licensing information.
// Created by Ping Identity on 3/23/17.
// Copyright © 2017 Ping Identity. All rights reserved.
//
class BaseActivity extends Activity  {

    public final static String TAG = BaseActivity.class.getName();
    public static final int PLAY_SERVICES_UPDATE_REQUEST = 9001;
    public static final String TRUST_LEVELS = "TRUST_LEVELS";


    /*
    Create a dialog that enables the user to approve or deny the addition of a new device
    to his network of trusted devices.
     */
    public void displayAddDeviceToNetworkDialog(final List<String> availableTrustLevels) {

        if (availableTrustLevels.contains(PingID.PIDTrustLevel.PIDTrustLevelPrimary.getName()) || availableTrustLevels.contains(PingID.PIDTrustLevel.PIDTrustLevelTrusted.getName())) {

            //ask the user for approval
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    try{
                        PingID.PIDTrustLevel trustLevel = availableTrustLevels.contains(PingID.PIDTrustLevel.PIDTrustLevelPrimary.getName()) ? PingID.PIDTrustLevel.PIDTrustLevelPrimary : PingID.PIDTrustLevel.PIDTrustLevelTrusted;
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                try {
                                    PIDUserSelectionObject pidUserSelectionObject = new PIDUserSelectionObject();
                                    pidUserSelectionObject.setPidActionType(PingID.PIDActionType.PIDActionTypeApprove);
                                    pidUserSelectionObject.setPidTrustLevel(trustLevel);
                                    PingID.getInstance().setUserSelection(pidUserSelectionObject);
                                    //display a message to the user with status
                                    if (availableTrustLevels.contains(PingID.PIDTrustLevel.PIDTrustLevelPrimary.getName())) {
                                        Toast.makeText(getApplicationContext(), R.string.pairing_pairing_now, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.pairing_new_device_will_be_paired, Toast.LENGTH_LONG).show();
                                    }
                                    changeScreenAvailability(false, ((LoginActivity)BaseActivity.this).getProgressBar() );
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                    Toast.makeText(getApplicationContext(), R.string.pairing_problem, Toast.LENGTH_LONG).show();
                                    //get reference to the progress because we need to activate it
                                    changeScreenAvailability(true, ((LoginActivity)BaseActivity.this).getProgressBar() );
                                }
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                try {
                                    PIDUserSelectionObject pidUserSelectionObject = new PIDUserSelectionObject();
                                    pidUserSelectionObject.setPidActionType(PingID.PIDActionType.PIDActionTypeDeny);
                                    PingID.getInstance().setUserSelection(pidUserSelectionObject);
                                    Toast.makeText(getApplicationContext(), R.string.pairing_denied, Toast.LENGTH_LONG).show();
                                    if (BaseActivity.this instanceof LoginActivity && availableTrustLevels.contains(PingID.PIDTrustLevel.PIDTrustLevelPrimary.getName())) {
                                        ((LoginActivity) BaseActivity.this).launchHomeActivity();
                                    }
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                    Toast.makeText(getApplicationContext(), R.string.pairing_problem, Toast.LENGTH_LONG).show();

                                } finally {
                                    changeScreenAvailability(true, null);
                                }

                                break;
                        }

                    }catch(WindowManager.BadTokenException badTokenException){
                        //in case the problem is that the app is in the background and cannot display the dialog
                        Log.e(TAG,"displayAddDeviceToNetworkDialog : The application is in the background and therefore cannot display the dialog");
                        badTokenException.printStackTrace();
                    }catch(Throwable throwable){
                        throwable.printStackTrace();
                    }
                }
            };

            DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //enable the login button if the dialog is cancelled
                    changeScreenAvailability(true, null);
                }
            };

            //get the text to display to the user according to the available that were received from the server
            String msg = availableTrustLevels.contains(PingID.PIDTrustLevel.PIDTrustLevelPrimary.getName()) ? getString(R.string.add_primary_device_prompt) : getString(R.string.add_trusted_device_prompt);
            //display the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(PingIDSdkDemoApplication.getInstance().getCurrentActivity()); //BaseActivity.this
            try{
                builder.setMessage(msg)
                    .setTitle(R.string.add_device_title)
                    .setPositiveButton(getString(R.string.approve), dialogClickListener)
                    .setNegativeButton(getString(R.string.deny), dialogClickListener)
                    .setOnCancelListener(cancelListener)
                    .show();
            }catch(WindowManager.BadTokenException badTokenException){
                //in case the problem is that the app is in the background and cannot display the dialog
                Log.e(TAG,"displayAddDeviceToNetworkDialog : The application is in the background and therefore cannot display the dialog");
                badTokenException.printStackTrace();
            }catch(Throwable throwable){
                throwable.printStackTrace();
            }

        }else{
            addLogLine("No available trust level to prompt the user.");
        }

    }

    //This function will remove all PingID SDK related data LOCALLY. If the device is
    //paired - it will still be considered paired on the server.
    protected void unpairFromPingID(final Runnable runAfter){
        //ask the user for approval
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        PingID.getInstance().removePingIDSDKLocalData();
                        Toast.makeText(getApplicationContext(), "Device unpaired", Toast.LENGTH_LONG).show();
                        if (runAfter!=null){
                            runAfter.run();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.app_prefs_are_you_sure_you_want_to_unpair).setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener).show();
    }


    //display a general alert dialog
    protected void displayAlertDialog(String title, String msg, String buttonText, final Runnable runAfter) {

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(PingIDSdkDemoApplication.getInstance().getCurrentActivity());

            builder.setTitle(title);
            builder.setMessage(msg);

            builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (runAfter!=null){ //if we have a runnable to run - run it
                        runAfter.run();
                    }
                }
            });

            AlertDialog alert = builder.create();

            try{
                alert.show();
            }catch(WindowManager.BadTokenException badTokenException){
                //in case the problem is that the app is in the background and cannot display the dialog
                Log.e(TAG,"displayAlertDialog : The application is in the background and therefore cannot display the dialog");
            }catch(Throwable throwable){
                throwable.printStackTrace();
            }
        }catch(Throwable throwable){
            throwable.printStackTrace();
        }
    }



    //launch the authentication activity
    protected void launchAuthenticationActivity(Bundle data) {
        Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.putExtras(data);
        startActivity(intent);
    }

    //launch the Login activity
    protected void launchLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    /*
     * Shows the progress UI and hides the login form.
    */
    protected void changeScreenAvailability(final boolean enable, ProgressBar progressBar) {

        Log.i(TAG, "changeScreenAvailability called with enable=" + enable);
        if (enable) {
            if (progressBar!=null) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        }else{
            if (progressBar!=null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
        Button mSignInButton = (Button) findViewById(R.id.signin);
        if (mSignInButton!=null) {
            mSignInButton.setEnabled(enable);
        }

    }

}

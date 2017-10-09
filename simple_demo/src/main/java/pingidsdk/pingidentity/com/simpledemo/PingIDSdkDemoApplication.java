package pingidsdk.pingidentity.com.simpledemo;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pingidsdk.pingidentity.com.simpledemo.beans.AuthenticationData;
import pingidsdkclient.DeviceDetails;
import pingidsdkclient.PingID;

import static pingidsdk.pingidentity.com.simpledemo.AuthenticationActivity.KEY_CLIENT_CONTEXT;
import static pingidsdk.pingidentity.com.simpledemo.BaseActivity.PLAY_SERVICES_UPDATE_REQUEST;
import static pingidsdk.pingidentity.com.simpledemo.BaseActivity.TRUST_LEVELS;
import static pingidsdk.pingidentity.com.simpledemo.NotificationActionReceiver.INTENT_FILTER_APPROVE_AUTHENTICATION;
import static pingidsdk.pingidentity.com.simpledemo.NotificationActionReceiver.INTENT_FILTER_APPROVE_PAIRING;
import static pingidsdk.pingidentity.com.simpledemo.NotificationActionReceiver.INTENT_FILTER_DENY_AUTHENTICATION;
import static pingidsdk.pingidentity.com.simpledemo.NotificationActionReceiver.INTENT_FILTER_DENY_PAIRING;
import static pingidsdk.pingidentity.com.simpledemo.beans.AuthenticationData.TRANSACTION_TYPE_AUTHENTICATION;
import static pingidsdkclient.PingID.PIDTrustLevel.PIDTrustLevelPrimary;
import static pingidsdkclient.PingID.PIDTrustLevel.PIDTrustLevelTrusted;

//
// Class Name : PingIDSdkDemoApplication
// App name : Moderno
//
// This is the customized application object. It implements the PingIDSDKEvents interface and handles all the events
// triggered by the library. It also handle activity life cycle events to get reference to the currently loaded activity
// in order to know if the events are to be presented to the user on the activity or in a notification.
//
// See LICENSE.txt for this sample’s licensing information and LICENSE_SDK.txt for the PingID SDK library licensing information.
// Created by Ping Identity on 3/23/17.
// Copyright © 2017 Ping Identity. All rights reserved.
//
public class PingIDSdkDemoApplication extends Application implements Application.ActivityLifecycleCallbacks, PingID.PingIdSdkEvents {

    public static final String TAG=PingIDSdkDemoApplication.class.getName();
    private Activity _activity;
    public static final Map<String, String> globalData= new HashMap<>(); //global object for application wide data
    public static PingIDSdkDemoApplication _pingIDSdkDemoApplication;
    int mNotificationId = 1;
    private int googlePlayServicesAvailabilityCheckResult=-1;

    @Override
    public void onCreate() {
        super.onCreate();

        _pingIDSdkDemoApplication = this;
        this.registerActivityLifecycleCallbacks(PingIDSdkDemoApplication.this);

        //initialize PingID
        try {
            PingID.init(this, AppPreferences.DEFAULT_APP_ID, PingIDSdkDemoApplication.getInstance(), AppPreferences.DEFAULT_PUSH_SENDER_ID);
            PingIDSdkDemoApplication.addLogLine("PingID Consumer lib initialization succeeded");
        } catch (Exception e) {
            e.printStackTrace();
            addLogLine("PingID Consumer lib initialization failed : " + e.getMessage());
        }
    }

    public static PingIDSdkDemoApplication getInstance(){
        return _pingIDSdkDemoApplication;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.w(TAG, "Activity resumed:" + activity.getLocalClassName());
        _activity = activity;
        //if we have a googlePlayServicesAvailabilityCheckResult value we didn't handle bacause there
        //was not activity loaded - handle it now.
        if (googlePlayServicesAvailabilityCheckResult>-1){
            handleGooglePlayServicesAvailabilityCheckResult(googlePlayServicesAvailabilityCheckResult);
            googlePlayServicesAvailabilityCheckResult=-1 ;//reset the value
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.w(TAG, "Activity paused:" + activity.getLocalClassName());
        if (_activity==activity){
            _activity=null;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        //Log.w(TAG, "Activity destroyed:" + activity.getLocalClassName());
    }

    public Activity getCurrentActivity(){
        return _activity;
    }

    @Override
    public void onPairingOptionsRequired(final List<String> availableTrustLevels, DeviceDetails deviceDetails) {
        Log.i(TAG, "onPairingOptionsRequired triggered");
        if (getCurrentActivity()!=null) {
            getCurrentActivity().runOnUiThread(new Runnable() {
                public void run() {
                    //this will display the "add device" dialog to the user
                    //the device type will be auto selected according to the
                    //options coming from the server - primary/trusted
                    addLogLine("onPairingOptionsRequired request triggered by PingID");
                    ((BaseActivity) getCurrentActivity()).displayAddDeviceToNetworkDialog(availableTrustLevels);
                }
            });
        }else{
            createPairingNotification(availableTrustLevels, deviceDetails);
        }
    }



    @Override
    public void onPairingOptionsRequiredWithPasscode(List<String> list, String s) {
        //not supported in this demo
    }

    @Override
    public void onPairingCompleted(final PingID.PIDActionStatus status, final PingID.PIDErrorDomain pidErrorDomain) {
        Log.i(TAG, "onPairingCompleted triggered with status " + status.name() + (pidErrorDomain!=null ? ", error=" + pidErrorDomain.getResultCode() + " " + pidErrorDomain.getResultDescription() : ""));
        final String msg = "Pairing completed with status " + status + (pidErrorDomain != null ? ", error=" + pidErrorDomain.getResultCode() + " " + pidErrorDomain.getResultDescription() : "");
        if (getCurrentActivity()!=null) {
            getCurrentActivity().runOnUiThread(new Runnable() {
                public void run() {
                    //display the results of the pairing operation
                    showStatus(msg);

                    if (getCurrentActivity() instanceof BaseActivity) {
                        ((BaseActivity) getCurrentActivity()).changeScreenAvailability(true, null);
                    }

                    if (status.name().equalsIgnoreCase(PingID.PIDActionStatus.SUCCESS.name())) {
                        if (getCurrentActivity() instanceof LoginActivity) {
                            ((LoginActivity) getCurrentActivity()).launchHomeActivity();
                        }

                    } else {
                        ((BaseActivity) getCurrentActivity()).displayAlertDialog("Pairing", "Pairing failed, please review the log for details.", "OK", null);
                    }
                }
            });
        }else{
            Log.i(TAG, msg);
            removeNotification();
        }
    }

    @Override
    public void onIgnoreDeviceCompleted(final PingID.PIDActionStatus status, PingID.PIDErrorDomain pidErrorDomain) {
        //not supported in this demo
    }

    @Override
    public void onAuthenticationCompleted(final PingID.PIDActionStatus status, final PingID.PIDActionType actionType, final PingID.PIDErrorDomain pidErrorDomain) {
        String msg = "Authentication completed with " + (actionType != null ? "actionType " + actionType.name() : null) + ", status=" + " " + status.name() + (pidErrorDomain != null ? ", error=" + pidErrorDomain.getResultCode() + " " + pidErrorDomain.getResultDescription() : "");
        if (PingIDSdkDemoApplication.globalData.containsKey(KEY_CLIENT_CONTEXT) &&  PingIDSdkDemoApplication.globalData.get(KEY_CLIENT_CONTEXT).toLowerCase().contains("transfer")) {
            msg = "Transaction completed with " + (actionType != null ? "actionType " + actionType.name() : null) + ", status=" + " " + status.name() + (pidErrorDomain != null ? ", error=" + pidErrorDomain.getResultCode() + " " + pidErrorDomain.getResultDescription() : "");
        }
        final String authenticationStatus = msg;
        Log.i(TAG, authenticationStatus);
        if (getCurrentActivity()!=null) {
            getCurrentActivity().runOnUiThread(new Runnable() {
                public void run() {
                    showStatus(authenticationStatus);
                    if (getCurrentActivity() instanceof BaseActivity) {
                        ((BaseActivity) getCurrentActivity()).changeScreenAvailability(true, null);
                    }
                    if (getCurrentActivity() instanceof AuthenticationActivity) {
                        ((AuthenticationActivity) getCurrentActivity()).onAuthenticationCompleted(status, actionType);
                    }
                }
            });
        }else{
            Log.e(TAG, msg);
        }
        removeNotification();
    }

    @Override
    public void onGeneralMessage(final String msg) {
        Log.i(TAG, "onGeneralMessage triggered. Msg=" + msg);
    }

    @Override
    public void onPairingProgress(String msg) {
        addLogLine("onPairingProgress : " + msg);
    }

    @Override
    public void onAuthenticationRequired(final Bundle data) {
        Log.i(TAG, "onAuthenticationRequired triggered. Bundle size=" + data.keySet().size());

        if (getCurrentActivity()!=null) { //if there is an active activity
            getCurrentActivity().runOnUiThread(new Runnable() {
                public void run() {
                    //incoming authentication - launch the authentication activity
                    ((BaseActivity) getCurrentActivity()).launchAuthenticationActivity(data);
                }
            });
        }else{
            createAuthenticationNotification(data);
        }

    }



    @Override
    public void onError(final Throwable throwable, final String description) {
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                //display the error
                showStatus("onError msg: " + description + "; " + throwable.getMessage());
            }
        });
    }

    @Override
    public void onLogsSentToServer(final PingID.PIDActionStatus status, final String supportId) {
        Log.i(TAG, "onLogsSentToServer triggered. supportId=" + supportId);
        //bringAppToFront();
        getCurrentActivity().runOnUiThread(new Runnable() {
            public void run() {
                //display the results of this operation
                if (status.equals(PingID.PIDActionStatus.SUCCESS)) {
                    addLogLine("Logs sent to PingID SDK server : supportId=" + supportId);
                    Toast.makeText(getApplicationContext(), "Log upload succeeded, supportId=" + supportId, Toast.LENGTH_LONG).show();
                } else {
                    addLogLine("Log upload failed");
                    Toast.makeText(getApplicationContext(), "Log upload failed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onAuthenticationCancelled() {
        //not supported in this demo
    }

    @Override
    public void onGooglePlayServicesStatusReceived(int status) {

        //if we have an activity loaded when we get the result of the Google Play Services availability check
        //we can display the dialog . if not - we'll have to wait until an activity is loaded
        if (getCurrentActivity()!=null){
            handleGooglePlayServicesAvailabilityCheckResult(status);
        }else{
            googlePlayServicesAvailabilityCheckResult=status;
        }


    }

    @Override
    public void onOneTimePasscodeChanged(String newOneTimePasscode) {
        addLogLine("One time passcode changed. new value : " + newOneTimePasscode);
    }


    private void createAuthenticationNotification(Bundle data) {

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        Intent resultIntent = new Intent(getApplicationContext(), AuthenticationActivity.class);
        resultIntent.putExtras(data);
        resultIntent.setAction("auth");

        PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AuthenticationData authenticationData = new Gson().fromJson(data.getString(AuthenticationActivity.KEY_CLIENT_CONTEXT), AuthenticationData.class);
        String notificationText;
        String notificationTitle;
        if (authenticationData.getTransactionType().equals(TRANSACTION_TYPE_AUTHENTICATION)) {
            //regular authentication
            notificationText = getString(R.string.new_auth_notification_body);
            notificationTitle = getString(R.string.new_auth_notification_title);
        }else{
            //step up
            notificationText = getString(R.string.step_up_accounts) + "\n" + authenticationData.getMsg();
            notificationTitle = getString(R.string.step_up_transfer_between_accounts);
        }

        //prepare notification style
        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(notificationText);
        bigText.setBigContentTitle(notificationTitle);

        //prepare notification buttons
        Intent approveIntent = new Intent(this, NotificationActionReceiver.class);
        approveIntent.setAction(INTENT_FILTER_APPROVE_AUTHENTICATION); //
        approveIntent.putExtras(data);
        PendingIntent pendingApproveIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, approveIntent, 0);
        NotificationCompat.Action approveAction = new NotificationCompat.Action.Builder(0, getString(R.string.approve), pendingApproveIntent).build();

        Intent denyIntent = new Intent(this, NotificationActionReceiver.class);
        denyIntent.setAction(INTENT_FILTER_DENY_AUTHENTICATION);
        denyIntent.putExtras(data);
        PendingIntent pendingDenyIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, denyIntent, 0);
        NotificationCompat.Action denyAction = new NotificationCompat.Action.Builder(0, getString(R.string.deny), pendingDenyIntent).build();

        //prepare notification builder
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.app_icon_moderno_1024)
                        .setContentTitle(getString(R.string.app_name))
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.app_icon_moderno_1024))
                        .setContentIntent(resultPendingIntent)
                        .setContentText(notificationText)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setStyle(bigText)
                        .addAction(approveAction)
                        .addAction(denyAction)
                        .setExtras(data);

        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

        wakeupDevice();

    }

    private void removeNotification() {
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mNotifyMgr.cancel(mNotificationId);
    }

    private void wakeupDevice(){
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();
    }

    private void createPairingNotification(List<String> availableTrustLevels, DeviceDetails deviceDetails) {
        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        try {
            Intent resultIntent = new Intent(getApplicationContext(), LoginActivity.class);
            resultIntent.setAction("pairing");
            resultIntent.putStringArrayListExtra(TRUST_LEVELS, new ArrayList<>(availableTrustLevels));

            PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            String notificationText = availableTrustLevels.contains(PIDTrustLevelPrimary.getName()) ? getString(R.string.add_primary_device_prompt) : getString(R.string.add_trusted_device_prompt);
            String trustLevel = availableTrustLevels.contains(PIDTrustLevelPrimary.getName()) ? PIDTrustLevelPrimary.getName() : PIDTrustLevelTrusted.getName();

            //prepare notification style
            NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.bigText(notificationText);
            bigText.setBigContentTitle(getString(R.string.app_name));

            //prepare notification buttons
            Intent approveIntent = new Intent(this, NotificationActionReceiver.class);
            approveIntent.setAction(INTENT_FILTER_APPROVE_PAIRING);
            approveIntent.putExtra(NotificationActionReceiver.TRUST_LEVEL, trustLevel);
            PendingIntent pendingApproveIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, approveIntent, 0);
            NotificationCompat.Action approveAction = new NotificationCompat.Action.Builder(0, getString(R.string.approve), pendingApproveIntent).build();

            Intent denyIntent = new Intent(this, NotificationActionReceiver.class);
            denyIntent.setAction(INTENT_FILTER_DENY_PAIRING);
            PendingIntent pendingDenyIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, denyIntent, 0);
            NotificationCompat.Action denyAction = new NotificationCompat.Action.Builder(0, getString(R.string.deny), pendingDenyIntent).build();

            //prepare notification builder
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.app_icon_moderno_1024)
                            .setContentTitle(getString(R.string.app_name))
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.app_icon_moderno_1024))
                            .setContentIntent(resultPendingIntent)
                            .setContentText(notificationText)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setStyle(bigText)
                            .addAction(approveAction)
                            .addAction(denyAction);

            // Gets an instance of the NotificationManager service
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());

            wakeupDevice();
        }catch(Throwable throwable){
            throwable.printStackTrace();
        }
    }

    public static void addLogLine(String line) {
        String logLine = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "  - " + line + "\n";
        Log.i(TAG, logLine);
    }

    protected static void showStatus(final String info) {
        if (getInstance().getCurrentActivity()!=null) {
            getInstance().getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getInstance().getCurrentActivity(), info, Toast.LENGTH_LONG).show();
                }
            });
        }
        Log.i(TAG, info);
    }

    protected void handleGooglePlayServicesAvailabilityCheckResult(int status){
        if (status == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            //Google Play services needs to be updated - display a dialog tha requests the user to upgrade
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(getCurrentActivity(), ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, PLAY_SERVICES_UPDATE_REQUEST, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    addLogLine("Google Play services upgrade dialog has been dismissed");
                }
            });

            try {
                dialog.show();
            } catch (WindowManager.BadTokenException badTokenException) {
                //in case the problem is that the app is in the background and cannot display the dialog
                Log.e(TAG, "onGooglePlayServicesStatusReceived : The application is in the background and therefore cannot display the dialog");
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        } else {
            addLogLine("Google Play Services unavailable on this device. Authentication will work in offline mode only");
            ((BaseActivity) getCurrentActivity()).displayAlertDialog("Google Play Services", "Google Play Services unavailable on this device. Authentication will work in offline mode only", "OK", null);
        }

    }

}

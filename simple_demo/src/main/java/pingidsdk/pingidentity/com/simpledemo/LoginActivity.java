package pingidsdk.pingidentity.com.simpledemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import pingidsdkclient.PingID;

import static pingidsdk.pingidentity.com.simpledemo.PingIDSdkDemoApplication.addLogLine;
import static pingidsdk.pingidentity.com.simpledemo.PingIDSdkDemoApplication.showStatus;

//
// Class Name : LoginActivity
// App name : Moderno
//
// A login screen that offers login via username/password + MFA by PingID for Customers
//
// See LICENSE.txt for this sample’s licensing information and LICENSE_SDK.txt for the PingID SDK library licensing information.
// Created by Ping Identity on 3/23/17.
// Copyright © 2017 Ping Identity. All rights reserved.
//
public class LoginActivity extends BaseActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public final String TAG = LoginActivity.class.getName();
    private String sum = "";
    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private ProgressBar progressBar;
    //popup menu
    private FrameLayout popupMenuContainer;
    private TextView mUnpairView;

    //auth types for requests to the hosting server
    public enum OperationType {
        AUTH_USER("auth_user");
        private String name;

        OperationType(String name) {
            this.name = name;
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if this activity was launched from a click on a notification
        //display the addDeviceDialog
        if (getIntent() != null && getIntent().getExtras() != null && getIntent().getExtras().containsKey(TRUST_LEVELS) && getIntent().getExtras().getStringArrayList(TRUST_LEVELS) != null) {
            List<String> trustLevels = getIntent().getExtras().getStringArrayList(TRUST_LEVELS);
            displayAddDeviceToNetworkDialog(trustLevels);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    login();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.signin);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

        //load username, password and auth type from the app preferences
        mUsernameView.setText(AppPreferences.getUsername(this));
        mPasswordView.setText(AppPreferences.getPassword(this));

        //handle app config values
        try {
            PingID.getInstance().setPushDisabled(AppPreferences.DEFAULT_FORCE_PUSHLESS);
        } catch (Exception e) {
            e.printStackTrace();
            addLogLine("PingID Consumer lib error : " + e.getMessage());
        }

        popupMenuContainer = (FrameLayout) findViewById(R.id.popupMenuContainer);

        ImageButton mMenuButton = (ImageButton) findViewById(R.id.buttonMenu);
        mMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupMenuContainer.getVisibility() == View.INVISIBLE) {
                    popupMenuContainer.setVisibility(View.VISIBLE);
                } else {
                    popupMenuContainer.setVisibility(View.INVISIBLE);
                }
            }
        });

        //clicking on the area outside the menu should close the menu
        popupMenuContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenuContainer.setVisibility(View.INVISIBLE);
            }
        });

        //prepare listeners for the menu items
        mUnpairView = (TextView) findViewById(R.id.buttonUnpair);
        mUnpairView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenuContainer.setVisibility(View.INVISIBLE);
                unpairFromPingID(null);
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        //set version number in the bottom of the login screen
        checkPlayServices();

        addLogLine("App initialized");

        changeScreenAvailability(true, progressBar);

    }

    @Override
    public void onBackPressed() {
        //close the menu if it's open
        if (popupMenuContainer.getVisibility() == View.VISIBLE) {
            popupMenuContainer.setVisibility(View.INVISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    /*
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void login() {

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        changeScreenAvailability(false, progressBar);

        //save the username and password
        AppPreferences.setUsername(this, mUsernameView.getText().toString());
        AppPreferences.setPassword(this, mPasswordView.getText().toString());

        //start a new authentication
        try {
            attemptLogin(username, password, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Attempts to contact the server to authenticate the user
     *
     * @param username - Username
     * @param password - Password
     * @param clientPayload - A string containing a client payload in case this function is execute a second time in a flow
     * @param userAnswer - A string containing the type if device pairing in case this function is execute a second time in a flow
     */
    public void attemptLogin(String username, String password, String clientPayload, final String userAnswer) {

        Log.d(TAG, "attemptLoginRequest start");

        try {
            //this may be a second attempt to login (if a primary device of this account is offline) so we generate a payload if
            //the one passed as a parameter is null
            final String payload;
            if (clientPayload == null) {
                payload = PingID.getInstance().generatePayload();
            } else {
                payload = clientPayload;
            }

            //prepare the LoginRequest data
            final pingidsdk.pingidentity.com.simpledemo.beans.LoginRequest request = new pingidsdk.pingidentity.com.simpledemo.beans.LoginRequest();
            request.setAuthType("pingID_online");
            request.setOperation(OperationType.AUTH_USER.name);
            request.setUser(username);
            request.setPassword(password);
            request.setPingIdPayload(payload);

            String requestUrl = AppPreferences.DEFAULT_HOSTING_SERVER_BASE_URL + "pidc";

            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(40000);

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            String json = gson.toJson(request);
            Log.d(TAG, "requestUrl=" + requestUrl + " request=" + json);
            StringEntity entity = new StringEntity(json, "UTF-8");
            changeScreenAvailability(false, progressBar);
            client.post(this, requestUrl, null, entity, "application/json", new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String responseBodyString = "";
                    try {
                        responseBodyString = new String(responseBody);

                        //check if the response is empty (error)
                        if (responseBodyString.length() == 0) {
                            addLogLine("response is empty");
                            changeScreenAvailability(true, progressBar);
                            return;
                        }
                        Log.d(TAG, "attemptLogin onSuccess, statusCode=" + statusCode + ",responseBody" + responseBodyString);
                        addLogLine("authenticate response received : " + responseBodyString);

                        Gson gson = new Gson();
                        pingidsdk.pingidentity.com.simpledemo.beans.LoginResponse response = gson.fromJson(responseBodyString, pingidsdk.pingidentity.com.simpledemo.beans.LoginResponse.class);

                        //for automatic pairing
                        if (response.getStatus() == 1018) {
                            if (response.getAvailableDevicesForAuthentication() != null && response.getAvailableDevicesForAuthentication().size() > 0) {
                                addLogLine("Select device Scenario not supported in this demo.");
                            } else {
                                showStatus("Login request, status : " + response.getStatus() + " - " + response.getDescription());
                            }
                        } else if (response.getStatus() == 1015) {
                            showStatus("Login request, status : " + response.getStatus() + " - " + response.getDescription());
                            PingID.getInstance().setServerPayload(response.getServerPayload(), null, response.getCurrentAuthenticatingDeviceData() != null ? response.getCurrentAuthenticatingDeviceData().getName() : null);
                        } else if (response.getStatus() == 1006) {
                            showStatus("Login request, status : " + response.getStatus() + " - " + response.getDescription());
                            displayAlertDialog(getString(R.string.error_authentication_failed), response.getDescription(), getString(R.string.ok), null);
                        } else {
                            showStatus("Login request, status : " + response.getStatus() + " - " + response.getDescription());


                            LoginActivity.this.sum = response.getSum();

                            if (response.getServerPayload() != null) {
                                PingID.getInstance().setServerPayload(response.getServerPayload(), userAnswer, response.getCurrentAuthenticatingDeviceData() != null ? response.getCurrentAuthenticatingDeviceData().getName() : null);
                            }

                            if (response.isPingIdAuthenticated()) {
                                launchHomeActivity();
                            }
                        }

                        //enable the Sign On button
                        changeScreenAvailability(true, progressBar);

                    } catch (Exception e) {
                        changeScreenAvailability(true, progressBar);
                        showStatus("Error processing auth response -  " + e.getMessage() + ", response=" + responseBodyString);
                        displayAlertDialog(getString(R.string.error), getString(R.string.error_connection_failed), getString(R.string.close), null);
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.d(TAG, "attemptLogin onFailure, statusCode=" + statusCode);
                    error.printStackTrace();
                    changeScreenAvailability(true, progressBar);
                    displayAlertDialog(getString(R.string.error), getString(R.string.error_connection_failed), getString(R.string.close), null);
                    showStatus(error.getMessage());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showStatus("Error logging into the system -  " + e.getMessage());
            changeScreenAvailability(true, progressBar);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLAY_SERVICES_UPDATE_REQUEST) {
            addLogLine("onActivityResult from Play services triggered");
            //we need to initialize the mobile PingID Consumer client again to verify that Google Play services is updated,
            //or simply go on without online authentication...
        }
    }

    /*
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     * NOTE : If Google Play services are not supported on the device -
     * online authentication will not be possible with PingID for Customers.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                try {
                    apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                } catch (WindowManager.BadTokenException badTokenException) {
                    //in case the problem is that the app is in the background and cannot display the dialog
                    Log.e(TAG, "The application is in the background and therefore cannot display the dialog");
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            } else {
                Log.i(TAG, "Google Play services are not supported on this device.");
                finish();
            }
            return false;
        }
        return true;
    }

    //launch the home activity
    protected void launchHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        if (sum != null) {
            intent.putExtra(HomeActivity.KEY_SUM, sum);
        }
        startActivity(intent);
        finish();
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }


}


package pingidsdk.pingidentity.com.simpledemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

//
// Class Name : AppPreferences
// App name : Moderno
//
// Application preferences class
//
// See LICENSE.txt for this sample’s licensing information and LICENSE_SDK.txt for the PingID SDK library licensing information.
// Created by Ping Identity on 3/23/17.
// Copyright © 2017 Ping Identity. All rights reserved.
//
public class AppPreferences {

    /**
     * The constant DEFAULT_HOSTING_SERVER_BASE_URL.
     */
    public static String DEFAULT_HOSTING_SERVER_BASE_URL = "<Hosting server URL>";
    /**
     * The constant DEFAULT_APP_ID.
     */
    public static String DEFAULT_APP_ID = "<App_Id>";
    /**
     * The constant DEFAULT_PUSH_SENDER_ID.
     */
    public static String DEFAULT_PUSH_SENDER_ID = "<Fcm/Gcm Sender Id>";
    /**
     * The constant DEFAULT_FORCE_PUSHLESS.
     */
    public static boolean DEFAULT_FORCE_PUSHLESS = false ;

    //in-app settings
    private final static String USERNAME = "USERNAME";
    private final static String PASSWORD = "PASSWORD";

    /*
     * Gets username.
     *
     * @param context the context
     * @return the username
     */
    public static String getUsername(Context context) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return _sharedPrefs.getString(USERNAME, "");
    }

    /*
     * Sets username.
     *
     * @param context the context
     * @param text    the text
     */
    public static void setUsername(Context context, String text) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor _prefsEditor;
        _prefsEditor = _sharedPrefs.edit();
        _prefsEditor.putString(USERNAME, text);
        _prefsEditor.apply();
    }

    /*
     * Gets password.
     *
     * @param context the context
     * @return the password
     */
    public static String getPassword(Context context) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return _sharedPrefs.getString(PASSWORD, "");
    }

    /*
     * Sets password.
     *
     * @param context the context
     * @param text    the text
     */
    public static void setPassword(Context context, String text) {
        SharedPreferences _sharedPrefs;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor _prefsEditor;
        _prefsEditor = _sharedPrefs.edit();
        _prefsEditor.putString(PASSWORD, text);
        _prefsEditor.apply();
    }

}
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
    public static String DEFAULT_HOSTING_SERVER_BASE_URL = "http://demoshop.orwellg.com:8080/hosting-server/";
    /**
     * The constant DEFAULT_APP_ID.
     */
    public static String DEFAULT_APP_ID = "dc06b031-7109-44d3-9ae8-f02c31441407";
    /**
     * The constant DEFAULT_PUSH_SENDER_ID.
     */
    public static String DEFAULT_PUSH_SENDER_ID = "768403677106";
    /**
     * The constant DEFAULT_FORCE_PUSHLESS.
     */
    public static boolean DEFAULT_FORCE_PUSHLESS = false;

    //in-app settings
    private final static String USERNAME = "vad";
    private final static String PASSWORD = "password";

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
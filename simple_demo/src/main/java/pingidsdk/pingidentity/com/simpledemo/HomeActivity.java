package pingidsdk.pingidentity.com.simpledemo;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

//
// Class Name : HomeActivity
// App name : Moderno
//
// This activity is the main activity.
//
// See LICENSE.txt for this sample’s licensing information and LICENSE_SDK.txt for the PingID SDK library licensing information.
// Created by Ping Identity on 3/23/17.
// Copyright © 2017 Ping Identity. All rights reserved.
//
public class HomeActivity extends BaseActivity {

    //popup menu
    private TextView mUnpairView;
    private TextView mLogoutView;

    private FrameLayout popupMenuContainer;

    public static final String KEY_SUM = "KEY_SUM";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        popupMenuContainer = (FrameLayout)findViewById(R.id.popupMenuContainer);

        ImageButton mMenuButton = (ImageButton) findViewById(R.id.buttonMenu);
        mMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupMenuContainer.getVisibility()==View.INVISIBLE){
                    popupMenuContainer.setVisibility(View.VISIBLE);
                }else{
                    popupMenuContainer.setVisibility(View.INVISIBLE);
                }
            }
        });

        //clicking on the area outside the menu should close the menu
        popupMenuContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenuContainer.setVisibility(View.INVISIBLE);
            }
        });

        //prepare listeners for the menu items
        mUnpairView = (TextView)findViewById(R.id.buttonUnpair);
        mUnpairView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenuContainer.setVisibility(View.INVISIBLE);
                unpairFromPingID(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        launchLoginActivity();
                    }
                });
            }
        });

        mLogoutView= (TextView)findViewById(R.id.buttonLogout);
        mLogoutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenuContainer.setVisibility(View.INVISIBLE);
                finish();
                launchLoginActivity();
            }
        });
    }

    @Override
    public void onBackPressed() {
        //close the menu if it's open
        if (popupMenuContainer.getVisibility()==View.VISIBLE){
            popupMenuContainer.setVisibility(View.INVISIBLE);
        }else {
            super.onBackPressed();
        }
    }
}

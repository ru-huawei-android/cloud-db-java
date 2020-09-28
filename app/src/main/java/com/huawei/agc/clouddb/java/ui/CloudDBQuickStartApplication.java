package com.huawei.agc.clouddb.java.ui;

import android.app.Application;

import com.huawei.agconnect.cloud.database.AGConnectCloudDB;

public class CloudDBQuickStartApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AGConnectCloudDB.initialize(this);
    }
}

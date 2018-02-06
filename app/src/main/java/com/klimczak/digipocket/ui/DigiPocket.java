package com.klimczak.digipocket.ui;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.bitcoin.crypto.KeyCrypter;
import com.klimczak.digipocket.core.storage.HierarchicalStorage;
import com.klimczak.digipocket.utils.PRNGFixes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class DigiPocket extends Application {

    private static Logger log = LoggerFactory.getLogger(DigiPocket.class);

    private static final String WALLET_NAME = "wallet";

    private String mPassword;
    private KeyCrypter mKeyCrypter;
    private KeyParameter mAesKey;
    private HierarchicalStorage storage;


    @Override
    public void onCreate()
    {
        // PRNGFixes.
        PRNGFixes.apply();
        super.onCreate();

        log.info("Digipocket created");
    }

    public void doExit() {
        log.info("Application exiting");

        // Kill everything
        log.info("Exiting");
        System.exit(0);
    }


    public File getWalletDir() {
        return new File(getFilesDir(), ".");
    }


    public File getStorageFile(String suffix) {
        String filename = WALLET_NAME + ".storage";
        if (suffix != null)
            filename = filename + suffix;
        return new File(getWalletDir(), filename);
    }

    public KeyCrypter getKeyCrypter() {return mKeyCrypter;}

    public KeyParameter getAesKey() {return mAesKey;}

    public String getPassword() {return mPassword;}

    public HierarchicalStorage getStorage() {return storage;}

    public void setStorage(HierarchicalStorage storage){this.storage = storage;}

    public void setAesKey(KeyParameter mAesKey) {this.mAesKey = mAesKey;}

    public void setKeyCrypter(KeyCrypter mKeyCrypter) {this.mKeyCrypter = mKeyCrypter;}

    public void setPassword(String mPassword) {this.mPassword = mPassword;}
}

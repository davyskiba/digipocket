package com.klimczak.digipocket.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;

import com.klimczak.digipocket.R;
import com.klimczak.digipocket.ui.DigiPocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class StartupActivity extends Activity {

    private static Logger log = LoggerFactory.getLogger(StartupActivity.class);

    private DigiPocket pocket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        log.info("Startup activity onCreate");

        pocket = (DigiPocket) getApplicationContext();
        doOpenWallet();
        finish();

    }

    @Override
    protected void onResume() {
        super.onResume();
        log.info("Startup activity resumed");
    }


    public void doOpenWallet() {

        File walletFile = pocket.getStorageFile(null);
        if (walletFile.exists()) {

            log.info("Existing wallet found");
            Intent intent = new Intent(this, PasswordActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("action", "login");
            intent.putExtras(bundle);
            startActivity(intent);

        } else {

            log.info("No existing wallet");
            Intent intent = new Intent(this, CreateRestoreActivity.class);
            startActivity(intent);
        }
    }


}

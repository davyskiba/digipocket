package com.klimczak.digipocket.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import com.klimczak.digipocket.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateRestoreActivity extends ToolbarActivity {


    private static Logger log = LoggerFactory.getLogger(CreateRestoreActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_restore);
        setupToolbar();
    }


    public void createWallet(View view) {
        log.info("Creating wallet");

        Intent intent = new Intent(this, PasswordActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("action", "create");
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    public void restoreWallet(View view) {
        log.info("Restoring walllet");

        Intent intent = new Intent(this, PasswordActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("action", "restore");
        intent.putExtras(bundle);
        startActivity(intent);

        finish();
    }
}

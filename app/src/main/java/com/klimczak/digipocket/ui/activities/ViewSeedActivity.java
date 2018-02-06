package com.klimczak.digipocket.ui.activities;


import android.os.Bundle;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.klimczak.digipocket.R;

public class ViewSeedActivity extends ToolbarActivity{

    private static Logger log = LoggerFactory.getLogger(ViewSeedActivity.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log.info("in viewseed activity");
        setContentView(R.layout.activity_view_seed);
        setupToolbar();
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        log.info("after setup viewseed activity");
    }


    public void encryptQr(View view) {

        log.info("encrypt qr code");
        //Intent intent = new Intent(this, WAlletActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //startActivity(intent);
        //finish();	.
    }


}

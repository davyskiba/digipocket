package com.klimczak.digipocket.ui.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.klimczak.digipocket.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestoreWalletActivity extends ToolbarActivity {

    private static final int SCANNER_ACTIVITY_REQUEST_CODE = 21331;

    private static Logger log = LoggerFactory.getLogger(RestoreWalletActivity.class);

    private Resources mRes;

    private String qrCodeReadingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mRes = getApplicationContext().getResources();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_wallet);
        setupToolbar();
    }


    public void restoreWallet(View view) {
        log.info("restore wallet");

    }

    public void readEncryptedQrCode(View view) {
        log.info("read encrypted qr");
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static class PasswordDialogFragment extends DialogFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            String msg = getArguments().getString("msg");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(msg);

            final EditText input = new EditText(getActivity());
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

            builder.setView(input);

            builder.setPositiveButton(R.string.base_error_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface di, int id) {
                    //
                }
            });
            return builder.create();
        }
    }

    private void showPasswordDialog(String msg) {
        DialogFragment df = new PasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString("msg", msg);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "Błąd");
    }
}

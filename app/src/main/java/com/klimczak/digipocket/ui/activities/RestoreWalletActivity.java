package com.klimczak.digipocket.ui.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.google.bitcoin.core.NetworkParameters;
import com.klimczak.digipocket.R;
import com.klimczak.digipocket.core.crypto.MnemonicCode;
import com.klimczak.digipocket.core.crypto.MnemonicException;
import com.klimczak.digipocket.core.storage.HierarchicalStorage;
import com.klimczak.digipocket.ui.DigiPocket;
import com.klimczak.digipocket.utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestoreWalletActivity extends ToolbarActivity {

    private static Logger log = LoggerFactory.getLogger(RestoreWalletActivity.class);

    private Resources mRes;

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

    public void readEncryptedQrCode(View view){
        log.info("read encrypted qr");
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

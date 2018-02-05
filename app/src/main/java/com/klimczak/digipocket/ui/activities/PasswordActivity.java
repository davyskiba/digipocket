package com.klimczak.digipocket.ui.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.res.Resources;
import android.os.AsyncTask;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.bitcoin.core.Wallet;
import com.klimczak.digipocket.R;
import com.klimczak.digipocket.utils.WalletUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;


public class PasswordActivity extends AppCompatActivity {

    private static Logger log = LoggerFactory.getLogger(PasswordActivity.class);

    private enum Action {
        ACTION_CREATE,
        ACTION_RESTORE,
        ACTION_LOGIN,
    }

    // UI references.
    private EditText mPasswordView;
    private EditText mPasswordConfirmView;

    private Resources mRes;

    private Action mAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordConfirmView = (EditText) findViewById(R.id.password_confirm);

        mRes = getResources();

        Bundle bundle = getIntent().getExtras();
        String action = bundle.getString("action");

        if (action == null) {
            String msg = "missing action in PasswordActivity";
            log.error(msg);
            throw new RuntimeException(msg);
        } else if (action.equals("create")) {
            mAction = Action.ACTION_CREATE;
            log.info("ACTION_CREATE");
        } else if (action.equals("load")) {
            mAction = Action.ACTION_RESTORE;
            log.info("ACTION_RESTORE");
        } else if (action.equals("login")) {
            mAction = Action.ACTION_LOGIN;
            mPasswordConfirmView.setVisibility(View.GONE);
            log.info("ACTION_LOGIN");
        } else {
            String msg = "unknown action value " + action;
            log.error(msg);
            throw new RuntimeException(msg);
        }

        //Button for password check setting
        Button passwordCheck = (Button) findViewById(R.id.password_check);
        passwordCheck.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attempt();
            }
        });

    }

    private void attempt(){
        switch(mAction) {
            case ACTION_CREATE:
            case ACTION_RESTORE: attemptCreate();
                                break;
            case ACTION_LOGIN: attemptLogin();
                                break;
        }
    }

    private void attemptLogin(){
        mPasswordView.setError(null);
        mPasswordConfirmView.setError(null);
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
            // Check for a valid password, if the user entered one.
        }else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            new ValidatePasswordTask().execute(password);

        }
    }

    private void attemptCreate() {

        // Reset errors.
        mPasswordView.setError(null);
        mPasswordConfirmView.setError(null);

        // Store values at the time of the login attempt.
        String password = mPasswordView.getText().toString();
        String passwordCheck = mPasswordConfirmView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
            // Check for a valid password, if the user entered one.
        }else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }else {
            if (TextUtils.isEmpty(password)) {
                mPasswordConfirmView.setError(getString(R.string.error_field_required));
                focusView = mPasswordView;
                cancel = true;
            } else if (!isPasswordValid(password)) {
                mPasswordConfirmView.setError(getString(R.string.error_invalid_password));
                focusView = mPasswordConfirmView;
                cancel = true;
            } else if (!password.equals(passwordCheck)) {
                mPasswordConfirmView.setError(getString(R.string.error_invalid_password_confirm));
                focusView = mPasswordConfirmView;
                cancel = true;
            }
        }
        // Checked for all?
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            new SetupPasswordTask().execute(password);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4 && password.length() <= 32;
    }


    private void validateComplete(boolean isValid) {

        if (!isValid) {
            log.info("password invalid");
            mPasswordView.setText("");
            showErrorDialog(mRes.getString(R.string.password_errortitle), mRes.getString(R.string.password_invalid));
            return;
        }else{
           try {
               WalletUtils.restore(getApplicationContext());
           } catch (InvalidCipherTextException ex) {
               log.error("wallet load failed: " + ex.toString());
           } catch (IOException ex) {
               log.error("wallet load failed: " + ex.toString());
           } catch (RuntimeException ex) {
               log.error("wallet load failed: " + ex.toString());
           }
            Intent intent = new Intent(this, WalletActivity.class);
            startActivity(intent);
            finish();

        }

    }

    private void setupComplete() {

        Intent intent;

        switch (mAction) {
            case ACTION_CREATE:
                // Create the wallet.
                WalletUtils.createWallet(getApplicationContext());

                intent = new Intent(this, WalletActivity.class);
                Bundle bundle2 = new Bundle();
                bundle2.putBoolean("showDone", true);
                intent.putExtras(bundle2);
                startActivity(intent);
                finish();
                break;

        }
    }

    private class SetupPasswordTask extends AsyncTask<String, Void, Void> {

        DialogFragment df;

        @Override
        protected void onPreExecute() {

            df = showModalDialog(mRes.getString(R.string.password_waittitle), mRes.getString(R.string.password_waitsetup));
        }

        protected Void doInBackground(String... arg0)
        {
            String password = arg0[0];
            // This takes a while (scrypt) ...
            WalletUtils.setPasswordForWallet(getApplicationContext(), password);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            df.dismissAllowingStateLoss();
            setupComplete();
        }
    }

    private class ValidatePasswordTask extends AsyncTask<String, Void, Boolean> {

        DialogFragment df;

        @Override
        protected void onPreExecute() {
            df = showModalDialog(mRes.getString(R.string.password_waittitle),  mRes.getString(R.string.password_wait_decrypt));
        }

        protected Boolean doInBackground(String... arg0)
        {
            String passcode = arg0[0];
            // This takes a while (scrypt) ...
            return WalletUtils.isPasswordValid(getApplicationContext(), passcode);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            df.dismissAllowingStateLoss();
            validateComplete(result.booleanValue());
        }
    }

    public static class MyDialogFragment extends DialogFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);
            String msg = getArguments().getString("msg");
            String title = getArguments().getString("title");
            boolean hasOK = getArguments().getBoolean("hasOK");
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(title);
            builder.setMessage(msg);
            if (hasOK) {
                builder
                        .setPositiveButton(R.string.base_error_ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface di,
                                                        int id) {
                                    }
                                });
            }
            return builder.create();
        }
    }

    protected DialogFragment showErrorDialog(String title, String msg) {
        DialogFragment df = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("msg", msg);
        args.putBoolean("hasOK", true);
        df.setArguments(args);
        df.show(getSupportFragmentManager(), "error");
        return df;
    }

    protected DialogFragment showModalDialog(String title, String msg) {
        DialogFragment df = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("msg", msg);
        args.putBoolean("hasOK", false);
        df.setArguments(args);
        df.setCancelable(false);
        df.show(getSupportFragmentManager(), "note");
        return df;
    }
}


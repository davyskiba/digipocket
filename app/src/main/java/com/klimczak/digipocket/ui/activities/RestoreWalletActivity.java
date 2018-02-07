package com.klimczak.digipocket.ui.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;
import com.klimczak.digipocket.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestoreWalletActivity extends ToolbarActivity {

    private static Logger log = LoggerFactory.getLogger(RestoreWalletActivity.class);

    private String qrCodeScanningResult;

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

    public void onQrImageClicked(View view){
        if(qrCodeScanningResult !=null){
            showGetPasswordDialog();
        }
    }

    private void showGetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Podaj hasło");
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onUserEnteredPassword(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void onUserEnteredPassword(String password) {
        Toast.makeText(this, "Odszyfruj mnie "+password+" "+ qrCodeScanningResult, Toast.LENGTH_SHORT).show();
    }

    public void readEncryptedQrCode(View view) {
        log.info("read encrypted qr");
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                onQrCodeScanned(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onQrCodeScanned(String scanResult) {
        qrCodeScanningResult = scanResult;
        Bitmap qrCodeBitmap = getQrCodeBitmap(scanResult);
        if (qrCodeBitmap != null) {
            ((ImageView) findViewById(R.id.qr_code)).setImageBitmap(qrCodeBitmap);
        } else {
            Toast.makeText(this, "Qr code generation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getQrCodeBitmap(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}

package com.klimczak.digipocket.utils;

import android.content.Context;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;
import com.google.protobuf.ByteString;
import com.klimczak.digipocket.core.storage.HierarchicalStorage;
import com.klimczak.digipocket.ui.DigiPocket;

import org.bitcoinj.wallet.Protos;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;


public class WalletUtils {

    private static Logger log = LoggerFactory.getLogger(WalletUtils.class);

    public static void createWallet(Context context) {

        DigiPocket pocket = (DigiPocket) context.getApplicationContext();

        NetworkParameters params = Constants.getNetworkParameters(context);

        // Generate a new seed.
        SecureRandom random = new SecureRandom();
        byte seed[] = new byte[16];
        random.nextBytes(seed);


        // Setup a wallet with the seed.
        HierarchicalStorage storage = new HierarchicalStorage(pocket, params, pocket.getAesKey(), seed);
        pocket.setStorage(storage);
        storage.persist(pocket);
    }

    public static void setPasswordForWallet(Context context, String password) {

        DigiPocket pocket = (DigiPocket) context.getApplicationContext();

        log.info("setting password starting");

        // Create salt and write to file.
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
        secureRandom.nextBytes(salt);
        writeSalt(context, salt);

        KeyCrypter keyCrypter = getKeyCrypter(salt);
        KeyParameter aesKey = keyCrypter.deriveKey(password);

        //
        pocket.setPassword(password);
        pocket.setKeyCrypter(keyCrypter);
        pocket.setAesKey(aesKey);

        log.info("setting password finished");
    }


    public static boolean isPasswordValid(Context context, String password) {
        DigiPocket pocket = (DigiPocket) context.getApplicationContext();

        byte[] salt = readSalt(context);
        KeyCrypter keyCrypter = getKeyCrypter(salt);
        KeyParameter aesKey = keyCrypter.deriveKey(password);

        // Can we parse our wallet file?
        try {
            HierarchicalStorage.deserialize(pocket, aesKey);
        } catch (Exception ex) {
            log.warn("password error - didn't deserialize wallet");
            return false;
        }

        // Set up credentials.
        pocket.setPassword(password);
        pocket.setKeyCrypter(keyCrypter);
        pocket.setAesKey(aesKey);

        return true;
    }

    public static void restore(Context context) throws InvalidCipherTextException, IOException {

        DigiPocket pocket = (DigiPocket) context.getApplicationContext();

        NetworkParameters params = Constants.getNetworkParameters(context);

        HierarchicalStorage storage = null;
        try {
            JSONObject node = HierarchicalStorage.deserialize(pocket, pocket.getAesKey());

            storage =  new HierarchicalStorage(pocket, params, pocket.getAesKey(), node);
        } catch (JSONException ex) {
            String msg = "trouble deserializing wallet: " + ex.toString();

            // Have to break the message into chunks for big messages ...
            while (msg.length() > 1024) {
                String chunk = msg.substring(0, 1024);
                log.error(chunk);
                msg = msg.substring(1024);
            }
            log.error(msg);

            throw new RuntimeException(msg);
        }
        pocket.setStorage(storage);
    }

    public static void writeSalt(Context context, byte[] salt) {
        DigiPocket wallapp = (DigiPocket) context.getApplicationContext();

        log.info("writing salt " + new String(Hex.encode(salt)));

        File saltFile = new File(wallapp.getWalletDir(), "salt");
        FileOutputStream saltStream;
        try {
            saltStream = new FileOutputStream(saltFile);
            saltStream.write(salt);
            saltStream.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static byte[] readSalt(Context context) {
        DigiPocket pocket = (DigiPocket) context.getApplicationContext();

        File saltFile = new File(pocket.getWalletDir(), "salt");

        byte[] salt = new byte[(int) saltFile.length()];
        DataInputStream dis;
        try {
            dis = new DataInputStream(new FileInputStream(saltFile));
            dis.readFully(salt);
            dis.close();
            // log.info("read salt " + new String(Hex.encode(salt)));
            return salt;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static KeyCrypter getKeyCrypter(byte[] salt) {
        Protos.ScryptParameters.Builder scryptParametersBuilder = Protos.ScryptParameters.newBuilder().setSalt(ByteString.copyFrom(salt));
        Protos.ScryptParameters scryptParameters = scryptParametersBuilder.build();
        return new KeyCrypterScrypt(scryptParameters);
    }

}

package com.klimczak.digipocket.core.storage;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.crypto.KeyCrypterScrypt;
import com.klimczak.digipocket.core.crypto.ChildNumber;
import com.klimczak.digipocket.core.crypto.DeterministicKey;
import com.klimczak.digipocket.core.crypto.KeyDerivation;
import com.klimczak.digipocket.core.crypto.MnemonicCode;
import com.klimczak.digipocket.ui.DigiPocket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;


public class HierarchicalStorage {

    private static Logger log = LoggerFactory.getLogger(HierarchicalStorage.class);

    private static final transient SecureRandom secureRandom = new SecureRandom();

    private KeyParameter mAesKey;

    private final DeterministicKey mMasterKey;
    private final DeterministicKey mWalletRoot;
    private final byte[] mWalletSeed;


    private HierarchicalWallet mWallet;

    public HierarchicalStorage(DigiPocket walletApp, NetworkParameters params, KeyParameter aesKey, byte[] walletSeed) {

        mAesKey = aesKey;
        mWalletSeed = walletSeed;

        byte[] hdseed;
        try {

            InputStream wis = walletApp.getAssets().open("wordlist/english.txt");
            MnemonicCode mc = new MnemonicCode(wis, MnemonicCode.BIP39_ENGLISH_SHA256);

            List<String> wordlist = mc.toMnemonic(mWalletSeed);
            hdseed = MnemonicCode.toSeed(wordlist);

        } catch (Exception ex) {
            throw new RuntimeException("Exception when decoding seed: " + ex);
        }

        mMasterKey = KeyDerivation.createMasterPrivateKey(hdseed);
        DeterministicKey t0 = KeyDerivation.deriveChildKey(mMasterKey, 0);
        mWalletRoot = KeyDerivation.deriveChildKey(t0, ChildNumber.PRIV_BIT);

        log.info("Created wallet " + mWalletRoot.getPath());

        mWallet = new HierarchicalWallet(mWalletRoot, params, "Main", 0);

    }

    public HierarchicalStorage(DigiPocket pocket, NetworkParameters params, KeyParameter aesKey, JSONObject storageNode) throws JSONException {

        mAesKey = aesKey;

        try {
            mWalletSeed = Base58.decode(storageNode.getString("seed"));
        } catch (AddressFormatException e) {
            throw new RuntimeException("trouble decoding wallet");
        }

        byte[] hdseed;
        try {
            InputStream wis = pocket.getAssets().open("wordlist/english.txt");
            MnemonicCode mc = new MnemonicCode(wis, MnemonicCode.BIP39_ENGLISH_SHA256);
            List<String> wordlist = mc.toMnemonic(mWalletSeed);
            hdseed = MnemonicCode.toSeed(wordlist);
        } catch (Exception ex) {
            throw new RuntimeException("trouble decoding seed");
        }

        // Standard derivation starts from M/0/0'
        mMasterKey = KeyDerivation.createMasterPrivateKey(hdseed);
        DeterministicKey t0 = KeyDerivation.deriveChildKey(mMasterKey, 0);
        mWalletRoot = KeyDerivation.deriveChildKey(t0, ChildNumber.PRIV_BIT);


        log.info("restoring hierarchical wallet " + mWalletRoot.getPath());

        JSONObject walletNode = storageNode.getJSONObject("wallet");

        log.info(String.format("deserializing account"));
        mWallet = new HierarchicalWallet(mWalletRoot, params, walletNode);
    }

    public static HierarchicalStorage load(DigiPocket pocket, NetworkParameters params, KeyParameter aesKey) throws InvalidCipherTextException, IOException {

        try {
            JSONObject node = deserialize(pocket, aesKey);

            return new HierarchicalStorage(pocket, params, aesKey, node);
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
    }

    // Deserialize the wallet data.
    public static JSONObject deserialize(DigiPocket pocket, KeyParameter aesKey) throws IOException, InvalidCipherTextException, JSONException {

        File file = pocket.getStorageFile(null);
        String path = file.getPath();

        try {
            log.info("restoring from " + path);
            int len = (int) file.length();

            // Open saved file.
            DataInputStream dis = new DataInputStream(new FileInputStream(file));

            // Read IV from file.
            byte[] iv = new byte[KeyCrypterScrypt.BLOCK_LENGTH];
            dis.readFully(iv);

            // Read the ciphertext from the file.
            byte[] cipherBytes = new byte[len - iv.length];
            dis.readFully(cipherBytes);
            dis.close();

            // Decrypt the ciphertext.
            ParametersWithIV keyWithIv =new ParametersWithIV(new KeyParameter(aesKey.getKey()), iv);
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
            cipher.init(false, keyWithIv);
            int minimumSize = cipher.getOutputSize(cipherBytes.length);
            byte[] outputBuffer = new byte[minimumSize];
            int length1 = cipher.processBytes(cipherBytes, 0, cipherBytes.length, outputBuffer, 0);
            int length2 = cipher.doFinal(outputBuffer, length1);
            int actualLength = length1 + length2;
            byte[] decryptedBytes = new byte[actualLength];
            System.arraycopy(outputBuffer, 0, decryptedBytes, 0, actualLength);

            // Parse the decryptedBytes.
            String jsonstr = new String(decryptedBytes);


            JSONObject node = new JSONObject(jsonstr);
            return node;

        } catch (IOException ex) {
            log.warn("trouble reading " + path + ": " + ex.toString());
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("trouble restoring wallet: " + ex.toString());
            throw ex;
        } catch (InvalidCipherTextException ex) {
            log.warn("wallet decrypt failed: " + ex.toString());
            throw ex;
        }
    }



    public JSONObject dumps() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("seed", Base58.encode(mWalletSeed));
            obj.put("wallet", mWallet.dumps());

            return obj;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);    // Shouldn't happen.
        }

    }

    public void persist(DigiPocket pocket) {
        File tmpFile = pocket.getStorageFile(".tmp");
        File newFile = pocket.getStorageFile(null);
        try {
            // Serialize into a byte array.
            JSONObject jsonobj = dumps();
            String jsonstr = jsonobj.toString(4);	// indentation
            byte[] plainBytes = jsonstr.getBytes(Charset.forName("UTF-8"));

            // Generate an IV.
            byte[] iv = new byte[KeyCrypterScrypt.BLOCK_LENGTH];
            secureRandom.nextBytes(iv);

            // Encrypt the serialized data.
            ParametersWithIV keyWithIv = new ParametersWithIV(mAesKey, iv);
            BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
            cipher.init(true, keyWithIv);
            byte[] encryptedBytes = new byte[cipher.getOutputSize(plainBytes.length)];
            int length = cipher.processBytes(plainBytes, 0, plainBytes.length, encryptedBytes, 0);
            cipher.doFinal(encryptedBytes, length);

            // Ready a tmp file.
            if (tmpFile.exists())
                tmpFile.delete();

            // Write the IV followed by the data.
            FileOutputStream ostrm = new FileOutputStream(tmpFile);
            ostrm.write(iv);
            ostrm.write(encryptedBytes);
            ostrm.close();

            // Swap the tmp file into place.
            if (!tmpFile.renameTo(newFile))
                log.warn("failed to rename to " + newFile.getPath());
            else
                log.info("persisted to " + newFile.getPath());

        } catch (JSONException ex) {
            log.warn("failed generating JSON: " + ex.toString());
        } catch (IOException ex) {
            log.warn("failed to write to " + tmpFile.getPath() + ": " +
                    ex.toString());
        } catch (DataLengthException ex) {
            log.warn("encryption failed: " + ex.toString());
        } catch (IllegalStateException ex) {
            log.warn("encryption failed: " + ex.toString());
        } catch (InvalidCipherTextException ex) {
            log.warn("encryption failed: " + ex.toString());
        }
    }

    public HierarchicalWallet getWallet() {
        return mWallet;
    }
}

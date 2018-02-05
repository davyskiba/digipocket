package com.klimczak.digipocket.core.storage;


import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.KeyCrypter;
import com.klimczak.digipocket.core.crypto.DeterministicKey;
import com.klimczak.digipocket.core.crypto.KeyDerivation;
import com.klimczak.digipocket.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.Arrays;
import java.util.List;

public class HierarchicalAddress {


    private static Logger log = LoggerFactory.getLogger(HierarchicalAddress.class);

    private NetworkParameters mParams;
    private int					mAddrNum;
    private String				mPath;
    private byte[]				mPrvBytes;
    private byte[]				mPubBytes;
    private ECKey mECKey;
    private byte[]				mPubKey;
    private byte[]				mPubKeyHash;
    private Address mAddress;


    public HierarchicalAddress(DeterministicKey chainKey, NetworkParameters params, int addrnum) {

        mAddrNum = addrnum;

        DeterministicKey dk = KeyDerivation.deriveChildKey(chainKey, addrnum);
        mPath = dk.getPath();

        // Derive ECKey.
        mPrvBytes = dk.getPrivKeyBytes();
        mPubBytes = dk.getPubKeyBytes(); // Expensive, save.
        mECKey = new ECKey(mPrvBytes, mPubBytes);

        // Derive public key, public hash and address.
        mPubKey = mECKey.getPubKey();
        mPubKeyHash = mECKey.getPubKeyHash();

        mAddress = mECKey.toAddress(params);


        log.info("created address " + mPath + ": " + mAddress.toString());
    }


    public HierarchicalAddress(DeterministicKey chainKey, NetworkParameters params, JSONObject addrNode) throws RuntimeException, JSONException {

        mAddrNum = addrNode.getInt("addrNum");

        if (!addrNode.has("path") || !addrNode.has("prvBytes")) {

            DeterministicKey dk = KeyDerivation.deriveChildKey(chainKey, mAddrNum);

            // Derive ECKey.
            mPrvBytes = dk.getPrivKeyBytes();
            mPath = dk.getPath();
        }
        else {
            try {
                mPrvBytes = Base58.decode(addrNode.getString("prvBytes"));
            } catch (AddressFormatException ex) {
                throw new RuntimeException("failed to decode prvBytes");
            }
            mPath = addrNode.getString("path");
        }

        try {
            mPubBytes = Base58.decode(addrNode.getString("pubBytes"));
        } catch (AddressFormatException ex) {
            throw new RuntimeException("failed to decode pubBytes");
        }

        mECKey = new ECKey(mPrvBytes, mPubBytes);


        // Derive public key, public hash and address.
        mPubKey = mECKey.getPubKey();
        mPubKeyHash = mECKey.getPubKeyHash();
        mAddress = mECKey.toAddress(params);

        log.info("read address " + mPath + ": " + mAddress.toString());
    }

    public JSONObject dumps() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("addrNum", mAddrNum);
            obj.put("path", mPath);
            obj.put("prvBytes", Base58.encode(mPrvBytes));
            obj.put("pubBytes", Base58.encode(mPubBytes));

            return obj;
        }
        catch (JSONException ex) {
            throw new RuntimeException(ex);	// Shouldn't happen.
        }
    }


    public String getPath() {
        return mPath;
    }

    public String getAddressString() {
        return mAddress.toString();
    }

    public String getAbbrev() {
        return mAddress.toString().substring(0, 12) + "...";
    }

    public String getPrivateKeyString() {
        return mECKey.getPrivateKeyEncoded(mParams).toString();
    }

    public Address getAddress() {
        return mAddress;
    }

}

package com.klimczak.digipocket.core.storage;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.KeyCrypter;
import com.klimczak.digipocket.core.crypto.ChildNumber;
import com.klimczak.digipocket.core.crypto.DeterministicKey;
import com.klimczak.digipocket.core.crypto.KeyDerivation;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.List;


public class HierarchicalWallet {

    private static Logger log = LoggerFactory.getLogger(HierarchicalWallet.class);
    private static int ADDRESSES_COUNT = 32;

    private DeterministicKey    mWalletKey;
    private String				mWalletName;
    private int					mWalletId;

    private HierarchicalChain	mReceiveChain;
    private HierarchicalChain	mChangeChain;


    public HierarchicalWallet(DeterministicKey parentKey, NetworkParameters params, String walletName, int acctnum) {

        mWalletName = walletName;
        mWalletId = acctnum;

        int childnum = acctnum | ChildNumber.PRIV_BIT;
        mWalletKey = KeyDerivation.deriveChildKey(parentKey, childnum);


        log.info("Created HierarchicalWallet" + mWalletName + ": " + parentKey.getPath());

        mReceiveChain = new HierarchicalChain(mWalletKey, params,  true, "Receive", ADDRESSES_COUNT);
        mChangeChain = new HierarchicalChain(mWalletKey, params, false, "Change", ADDRESSES_COUNT);
    }
    public HierarchicalWallet(DeterministicKey masterKey,NetworkParameters params, JSONObject walletNode) throws RuntimeException, JSONException {

        mWalletName = walletNode.getString("name");
        mWalletId = walletNode.getInt("id");

        int childnum = mWalletId;
         childnum |= ChildNumber.PRIV_BIT;


        mWalletKey = KeyDerivation.deriveChildKey(masterKey, childnum);

        log.info("created Heirarchical Wallet " + mWalletName + ": " + mWalletKey.getPath());

        mReceiveChain = new HierarchicalChain(mWalletKey, params, walletNode.getJSONObject("receive"));
        mChangeChain = new HierarchicalChain( mWalletKey, params, walletNode.getJSONObject("change"));
    }

    public JSONObject dumps() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("name", mWalletName);
            obj.put("id", mWalletId);
            obj.put("receive", mReceiveChain.dumps());
            obj.put("change", mChangeChain.dumps());

            return obj;
        }
        catch (JSONException ex) {
            throw new RuntimeException(ex);	// Shouldn't happen.
        }
    }


    public String getName() {
        return mWalletName;
    }

    public void setName(String name) {
        mWalletName = name;
    }

    public int getId() {
        return mWalletId;
    }

    public HierarchicalChain getReceiveChain() {
        return mReceiveChain;
    }

    public HierarchicalChain getChangeChain() {
        return mChangeChain;
    }

}

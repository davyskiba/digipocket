package com.klimczak.digipocket.core.storage;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.KeyCrypter;

import com.klimczak.digipocket.core.crypto.DeterministicKey;
import com.klimczak.digipocket.core.crypto.KeyDerivation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.ArrayList;
import java.util.List;


public class HierarchicalChain {

    private static Logger log = LoggerFactory.getLogger(HierarchicalChain.class);

    private DeterministicKey	mChainKey;
    private String				mChainName;
    private boolean				mIsReceive;

    private ArrayList<HierarchicalAddress> mAddrs;

    public HierarchicalChain(DeterministicKey accountKey, NetworkParameters params, boolean isReceive, String chainName, int numAddrs) {

        mIsReceive = isReceive;
        int chainnum = mIsReceive ? 0 : 1;
        mChainKey = KeyDerivation.deriveChildKey(accountKey, chainnum);
        mChainName = chainName;

        log.info("created hierarchical chain " + mChainName + ": " + mChainKey.getPath());

        mAddrs = new ArrayList<>();
        for (int ii = 0; ii < numAddrs; ++ii)
            mAddrs.add(new HierarchicalAddress(mChainKey, params, ii));
    }


    public HierarchicalChain(DeterministicKey accountKey, NetworkParameters params, JSONObject chainNode) throws RuntimeException, JSONException {

        mChainName = chainNode.getString("name");
        mIsReceive = chainNode.getBoolean("isReceive");

        int chainnum = mIsReceive ? 0 : 1;

        mChainKey = KeyDerivation.deriveChildKey(accountKey, chainnum);

        log.info("created hierarchical chain " + mChainName + ": " + mChainKey.getPath());

        mAddrs = new ArrayList<>();
        JSONArray addrobjs = chainNode.getJSONArray("addrs");
        for (int ii = 0; ii < addrobjs.length(); ++ii) {
            JSONObject addrNode = addrobjs.getJSONObject(ii);
            mAddrs.add(new HierarchicalAddress(mChainKey, params, addrNode));
        }
    }

    public JSONObject dumps() {
        try {
            JSONObject obj = new JSONObject();

            obj.put("name", mChainName);
            obj.put("isReceive", mIsReceive);

            JSONArray addrs = new JSONArray();
            for (HierarchicalAddress addr : mAddrs)
                addrs.put(addr.dumps());

            obj.put("addrs", addrs);

            return obj;
        }
        catch (JSONException ex) {
            throw new RuntimeException(ex);	// Shouldn't happen.
        }
    }



    public boolean isReceive() {
        return mIsReceive;
    }

    public List<HierarchicalAddress> getAddresses() {
        return mAddrs;
    }

    public int numAddrs() {
        return mAddrs.size();
    }


}

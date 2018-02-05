package com.klimczak.digipocket.utils;

import android.content.Context;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

public class Constants {

    public static final String APP_NAME = "DigiPocket";
    public static final String APP_NAME_NO_VISIBLE = " ";

    private static NetworkParameters networkParameters = null;
    public static String CHECKPOINTS_FILENAME = null;

    public static NetworkParameters getNetworkParameters(Context context)
    {
        if (networkParameters == null) {
            boolean TESTNET = true;

            networkParameters = TESTNET ? TestNet3Params.get() : MainNetParams.get();
            CHECKPOINTS_FILENAME = TESTNET ? "checkpoints-testnet" : "checkpoints";
        }

        return networkParameters;
    }


}

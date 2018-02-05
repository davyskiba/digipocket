package com.klimczak.digipocket.core.crypto;


import com.google.bitcoin.core.ECKey;
import com.google.common.collect.ImmutableList;

import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.math.ec.ECPoint;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DeterministicUtils {

    private DeterministicUtils() { }

    public  static HMac createHmacSha512Digest(byte[] key) {
        SHA512Digest digest = new SHA512Digest();
        HMac hMac = new HMac(digest);
        hMac.init(new KeyParameter(key));
        return hMac;
    }

    public static byte[] hmacSha512(HMac hmacSha512, byte[] input) {
        hmacSha512.reset();
        hmacSha512.update(input, 0, input.length);
        byte[] out = new byte[64];
        hmacSha512.doFinal(out, 0);
        return out;
    }

    public static byte[] hmacSha512(byte[] key, byte[] data) {
        return hmacSha512(createHmacSha512Digest(key), data);
    }

    public static ECPoint compressedCopy(ECPoint pubKPoint) {
        return ECKey.CURVE.getCurve().createPoint(pubKPoint.getX().toBigInteger(), pubKPoint.getY().toBigInteger(), true);
    }


    public static byte[] toCompressed(byte[] uncompressedPoint) {
        return compressedCopy(ECKey.CURVE.getCurve().decodePoint(uncompressedPoint)).getEncoded();
    }

    public static byte[] getBytes(ECPoint pubKPoint) {
        return compressedCopy(pubKPoint).getEncoded();
    }

    public static ImmutableList<ChildNumber> append(ImmutableList<ChildNumber> path, ChildNumber childNumber) {
        return ImmutableList.<ChildNumber>builder().addAll(path).add(childNumber).build();
    }

}


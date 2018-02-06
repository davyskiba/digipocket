package com.klimczak.digipocket.core.crypto;


import android.support.annotation.Nullable;

import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Utils;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class DeterministicKey implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Joiner PATH_JOINER = Joiner.on("/");

    private final DeterministicKey parent;
    private ECPoint publicAsPoint;
    private final BigInteger privateAsFieldElement;
    private final ImmutableList<ChildNumber> childNumberPath;

    /** 32 bytes */
    private final byte[] chainCode;

    DeterministicKey(ImmutableList<ChildNumber> childNumberPath, byte[] chainCode,
                     @Nullable ECPoint publicAsPoint, @Nullable BigInteger privateKeyFieldElt,
                     @Nullable DeterministicKey parent) {
        checkArgument(chainCode.length == 32);
        this.parent = parent;
        this.childNumberPath = childNumberPath;
        this.chainCode = Arrays.copyOf(chainCode, chainCode.length);
        this.publicAsPoint = publicAsPoint == null ? null : DeterministicUtils.compressedCopy(publicAsPoint);
        this.privateAsFieldElement = privateKeyFieldElt;
    }

    /**

     *  Zwraca ścieżkę w hierarchii kluczy
     */
    public ImmutableList<ChildNumber> getChildNumberPath() {
        return childNumberPath;
    }

    private int getDepth() {
        return childNumberPath.size();
    }

    /**
     * Zwraca numer dziecka - ostatni element w ścieżce
     */
    public ChildNumber getChildNumber() {
        return getDepth() == 0 ? ChildNumber.ZERO : childNumberPath.get(childNumberPath.size() - 1);
    }

    /**
     * Zwraca kod łańcucha
     */
    public byte[] getChainCode() {
        return chainCode;
    }

    /**
     * Zwraca ścieżkę tego klucze od master key.
     */
    public String getPath() {
        return PATH_JOINER.join(Iterables.concat(Collections.singleton("M"), getChildNumberPath()));
    }

    /**
     *  Zwraca klucz publiczy jak punkt krzywej eliptycznej
     */

    ECPoint getPubPoint() {
        if (publicAsPoint == null) {
            checkNotNull(privateAsFieldElement);
            publicAsPoint = ECKey.CURVE.getG().multiply(privateAsFieldElement);
        }
        return DeterministicUtils.compressedCopy(publicAsPoint);
    }

    /**
     * Zwraca klucz publiczny jako tablica bajtów
     */
    public byte[] getPubKeyBytes() {
        return getPubPoint().getEncoded();
    }


    @Nullable
    public BigInteger getPrivAsFieldElement() {
        return privateAsFieldElement;
    }

    @Nullable
    public DeterministicKey getParent() {
        return parent;
    }

    /**
     *  Zwraca klucz prywatny w formie tablicy bajtów
     */
    @Nullable
    public byte[] getPrivKeyBytes() {
        return privateAsFieldElement == null ? null : privateAsFieldElement.toByteArray();
    }

    /**
     * Zwraca klucz prywatny  - jeden bajt padu
     */
    public byte[] getPrivKeyBytes33() {
        byte[] bytes33 = new byte[33];
        byte[] priv = checkNotNull(getPrivKeyBytes(), "Private key missing");
        System.arraycopy(priv, 0, bytes33, 33 - priv.length, priv.length);
        return bytes33;
    }

    public boolean hasPrivate() {
        return privateAsFieldElement != null;
    }


    @Override
    public String toString() {
        return MessageFormat.format("ExtendedHierarchicKey[pub: {0}]", new String(Hex.encode(getPubKeyBytes())));
    }
}


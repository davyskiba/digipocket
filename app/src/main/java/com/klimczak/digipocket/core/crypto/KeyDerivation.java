package com.klimczak.digipocket.core.crypto;


import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.crypto.*;
import com.google.common.collect.ImmutableList;
import org.spongycastle.crypto.macs.HMac;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class KeyDerivation {

    private KeyDerivation() { }

    private static final HMac MASTER_HMAC_SHA512 = DeterministicUtils.createHmacSha512Digest("Bitcoin seed".getBytes());

    /**
     *  Generuje nowy klucz deterministyczny za pomocą zadanego ziarna - klucz główny
     */
    public static DeterministicKey createMasterPrivateKey(byte[] seed) throws KeyDerivationException {
        checkArgument(seed.length > 8, "Seed is too short and could be brute forced");
        // Calculate I = HMAC-SHA512(key="Bitcoin seed", msg=S)
        byte[] i = DeterministicUtils.hmacSha512(MASTER_HMAC_SHA512, seed);
        // Split I into two 32-byte sequences, Il and Ir.
        // Use Il as master secret key, and Ir as master chain code.
        checkState(i.length == 64, i.length);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] ir = Arrays.copyOfRange(i, 32, 64);
        Arrays.fill(i, (byte)0);
        DeterministicKey masterPrivKey = createMasterPrivKeyFromBytes(il, ir);
        Arrays.fill(il, (byte)0);
        Arrays.fill(ir, (byte)0);
        return masterPrivKey;
    }

    /**
     * Rzuca błędem jeżęli privKeyBytes 0 albo >=n
     */
    public static DeterministicKey createMasterPrivKeyFromBytes( byte[] privKeyBytes, byte[] chainCode) throws KeyDerivationException {
        BigInteger privateKeyFieldElt = new BigInteger(1, privKeyBytes);
        assertNonZero(privateKeyFieldElt, "Generated master key is invalid.");
        assertLessThanN(privateKeyFieldElt, "Generated master key is invalid.");
        return new DeterministicKey(ImmutableList.<ChildNumber>of(), chainCode, null, privateKeyFieldElt, null);
    }

    public static DeterministicKey createMasterPubKeyFromBytes(byte[] pubKeyBytes, byte[] chainCode) {
        return new DeterministicKey(ImmutableList.<ChildNumber>of(), chainCode, ECKey.CURVE.getCurve().decodePoint(pubKeyBytes), null, null);
    }

    /**
     * 0x80000000 - określa prywatny/publiczny konktekst dostarczania kluczy
     */
    public static DeterministicKey deriveChildKey(DeterministicKey parent, int childNumber) {
        return deriveChildKey(parent, new ChildNumber(childNumber));
    }

    /**
     * Rzuca błędem jeżeli klucz jest nieprawidłowy lub rodzic ma tylko klucz publiczny
     */
    public static DeterministicKey deriveChildKey(DeterministicKey parent, ChildNumber childNumber)
            throws KeyDerivationException {

        KeyDerivation.KeyBytes rawKey = deriveChildKeyBytes(parent, childNumber);

        return new DeterministicKey(
                DeterministicUtils.append(parent.getChildNumberPath(), childNumber),
                rawKey.chainCode,
                parent.hasPrivate() ? null : ECKey.CURVE.getCurve().decodePoint(rawKey.keyBytes),
                parent.hasPrivate() ? new BigInteger(1, rawKey.keyBytes) : null,
                parent);
    }

    private static KeyDerivation.KeyBytes deriveChildKeyBytes(DeterministicKey parent, ChildNumber childNumber)
            throws KeyDerivationException {

        byte[] parentPublicKey = DeterministicUtils.getBytes(parent.getPubPoint());
        assert parentPublicKey.length == 33 : parentPublicKey.length;
        ByteBuffer data = ByteBuffer.allocate(37);
        if (childNumber.isPrivateDerivation()) {
            data.put(parent.getPrivKeyBytes33());
        } else {
            data.put(parentPublicKey);
        }
        data.putInt(childNumber.getI());
        byte[] i = DeterministicUtils.hmacSha512(parent.getChainCode(), data.array());
        assert i.length == 64 : i.length;
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] chainCode = Arrays.copyOfRange(i, 32, 64);
        BigInteger ilInt = new BigInteger(1, il);
        assertLessThanN(ilInt, "Illegal derived key: I_L >= n");
        byte[] keyBytes;
        final BigInteger privAsFieldElement = parent.getPrivAsFieldElement();
        if (privAsFieldElement != null) {
            BigInteger ki = privAsFieldElement.add(ilInt).mod(ECKey.CURVE.getN());
            assertNonZero(ki, "Illegal derived key: derived private key equals 0.");
            keyBytes = ki.toByteArray();
        } else {
            checkArgument(!childNumber.isPrivateDerivation(), "Can't use private derivation with public keys only.");
            ECPoint Ki = ECKey.CURVE.getG().multiply(ilInt).add(parent.getPubPoint());
            checkArgument(!Ki.equals(ECKey.CURVE.getCurve().getInfinity()), "Illegal derived key: derived public key equals infinity.");
            keyBytes = DeterministicUtils.toCompressed(Ki.getEncoded());
        }
        return new KeyDerivation.KeyBytes(keyBytes, chainCode);
    }

    private static void assertNonZero(BigInteger integer, String errorMessage) {
        checkArgument(!integer.equals(BigInteger.ZERO), errorMessage);
    }

    private static void assertLessThanN(BigInteger integer, String errorMessage) {
        checkArgument(integer.compareTo(ECKey.CURVE.getN()) < 0, errorMessage);
    }

    private static class KeyBytes {
        private final byte[] keyBytes, chainCode;

        private KeyBytes(byte[] keyBytes, byte[] chainCode) {
            this.keyBytes = keyBytes;
            this.chainCode = chainCode;
        }
    }
}


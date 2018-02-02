package com.klimczak.digipocket.core.net;

import android.annotation.SuppressLint;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.KeyCrypter;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.store.WalletProtobufSerializer;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;


public class SPVBlockStoreSuite extends AbstractIdleService {

    private static Logger mLogger = LoggerFactory.getLogger(SPVBlockStoreSuite.class);

    private final String filePrefix;
    private final NetworkParameters params;
    private volatile BlockChain vChain;
    private volatile SPVBlockStore vStore;
    private volatile Wallet vWallet;
    private volatile PeerGroup vPeerGroup;

    private final File directory;
    private volatile File vWalletFile;

    private boolean useAutoSave = true;
    private PeerAddress[] peerAddresses;
    private SPVBlockDowloadListener downloadListener;
    private boolean autoStop = true;
    private InputStream checkpoints;
    private boolean blockingStartup = true;
    private final KeyCrypter keyCrypter;

    private final long scanTime;

    public SPVBlockStoreSuite(NetworkParameters params, File directory, String filePrefix, KeyCrypter keyCrypter, long scanTime) {
        this.params = params;
        this.directory = directory;
        this.filePrefix = filePrefix;
        this.keyCrypter = keyCrypter;
        this.scanTime = scanTime;
    }


    public SPVBlockStoreSuite setBlockDownloadListener(SPVBlockDowloadListener listener) {
        this.downloadListener = listener;
        return this;
    }

    /** If true, will register a shutdown hook to stop the library. Defaults to true. */
    public SPVBlockStoreSuite setAutoStop(boolean autoStop) {
        this.autoStop = autoStop;
        return this;
    }

    /**
     * If set, the file is expected to contain a checkpoints file calculated with BuildCheckpoints. It makes initial
     * block sync faster for new users - please refer to the documentation on the bitcoinj website for further details.
     */
    public SPVBlockStoreSuite setCheckpoints(InputStream checkpoints) {
        this.checkpoints = checkpoints;
        return this;
    }


    /**
     * This method is invoked on a background thread after all objects are initialised, but before the peer group
     * or block chain download is started. You can tweak the objects configuration here.
     */
    protected void onSetupCompleted() { }

    @Override
    protected void startUp() throws Exception {
        // Runs in a separate thread.
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Could not create named directory.");
            }
        }
        FileInputStream walletStream = null;
        try {
            File chainFile = new File(directory, filePrefix + ".spvchain");
            boolean chainFileExists = chainFile.exists();
            vWalletFile = new File(directory, filePrefix + ".wallet");
            boolean shouldReplayWallet = vWalletFile.exists() && !chainFileExists;

            vStore = new SPVBlockStore(params, chainFile);
            if (!chainFileExists && checkpoints != null) {
                mLogger.info(String.format("checkpoint at time %d", scanTime));
                CheckpointManager.checkpoint(params, checkpoints, vStore, scanTime);
            }
            vChain = new BlockChain(params, vStore);
            vPeerGroup = new PeerGroup(params, vChain);
            if (vWalletFile.exists()) {
                walletStream = new FileInputStream(vWalletFile);
                vWallet = new Wallet(params);
                new WalletProtobufSerializer().readWallet(WalletProtobufSerializer.parseToProto(walletStream), vWallet);
                if (shouldReplayWallet)
                    vWallet.clearTransactions(0);
            } else {
                vWallet = new Wallet(params, keyCrypter);
            }
            if (useAutoSave) vWallet.autosaveToFile(vWalletFile, 1, TimeUnit.SECONDS, null);
            // Set up peer addresses or discovery first, so if wallet extensions try to broadcast a transaction
            // before we're actually connected the broadcast waits for an appropriate number of connections.
            if (peerAddresses != null) {
                for (PeerAddress addr : peerAddresses) vPeerGroup.addAddress(addr);
                peerAddresses = null;
            } else {
                vPeerGroup.addPeerDiscovery(new DnsDiscovery(params));
            }
            vChain.addWallet(vWallet);
            vPeerGroup.addWallet(vWallet);
            onSetupCompleted();

            if (blockingStartup) {
                vPeerGroup.startAndWait();
                // Make sure we shut down cleanly.
                installShutdownHook();
                SPVBlockDowloadListener listener = (this.downloadListener != null) ? this.downloadListener : new SPVBlockDowloadListener();
                vPeerGroup.startBlockChainDownload(listener);
                listener.await();
            } else {
                Futures.addCallback(vPeerGroup.start(), new FutureCallback<State>() {
                    @Override
                    public void onSuccess(State result) {
                        final PeerEventListener l = downloadListener == null ? new SPVBlockDowloadListener() : downloadListener;
                        vPeerGroup.startBlockChainDownload(l);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            }
            vWallet.autosaveToFile(vWalletFile, 1, TimeUnit.SECONDS, null);
            //Wyszukiwanie dostępnych peerów
            vPeerGroup.addPeerDiscovery(new DnsDiscovery(params));
            //Inicjalizacja portfela w sieci
            vChain.addWallet(vWallet);
            vPeerGroup.addWallet(vWallet);

            onSetupCompleted();
            vPeerGroup.startAndWait();
            // Make sure we shut down cleanly.
            installShutdownHook();
            SPVBlockDowloadListener listener = this.downloadListener;
            vPeerGroup.startBlockChainDownload(listener);
            listener.await();
        } catch (BlockStoreException e) {
            throw new IOException(e);
        } finally {
            if (walletStream != null) walletStream.close();
        }
    }

    private void installShutdownHook() {
        if (autoStop) Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                try {
                    mLogger.info("SPVBlockStoreSuite autoStop starting");
                    // Skip if shutDown has already been called.
                    if (vPeerGroup != null)
                        SPVBlockStoreSuite.this.stopAndWait();
                    mLogger.info("SPVBlockStoreSuite autoStop finished");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @SuppressLint("NewApi")
    @Override
    protected void shutDown() throws Exception {
        mLogger.info("SPVBlockStoreSuite shutDown starting");
        setAutoStop(false);	// Won't need this anymore.
        // Runs in a separate thread.
        try {
            vPeerGroup.stopAndWait();
            vWallet.saveToFile(vWalletFile);
            vStore.close();

            vPeerGroup = null;
            vWallet = null;
            vStore = null;
            vChain = null;
        } catch (BlockStoreException e) {
            throw new IOException(e);
        }
        mLogger.info("SPVBlockStoreSuite shutDown finished");
    }

    public NetworkParameters params() {
        return params;
    }

    public Wallet wallet() {
        checkState(state() == State.STARTING || state() == State.RUNNING, "Cannot call until startup is complete");
        return vWallet;
    }

    public PeerGroup peerGroup() {
        checkState(state() == State.STARTING || state() == State.RUNNING, "Cannot call until startup is complete");
        return vPeerGroup;
    }

    public File directory() {
        return directory;
    }


}

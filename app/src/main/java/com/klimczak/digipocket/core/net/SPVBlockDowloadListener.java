package com.klimczak.digipocket.core.net;


import com.google.bitcoin.core.AbstractPeerEventListener;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Peer;
import com.google.common.util.concurrent.SettableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class SPVBlockDowloadListener extends AbstractPeerEventListener {

    private static final Logger log = LoggerFactory.getLogger(SPVBlockDowloadListener.class);

    private int originalBlocksLeft = -1;
    private int lastPercent = 0;

    private SettableFuture<Long> future = SettableFuture.create();
    private boolean caughtUp = false;

    @Override
    public void onChainDownloadStarted(Peer peer, int blocksLeft) {
        if (blocksLeft > 0 && originalBlocksLeft == -1)
            startDownload(blocksLeft);
        // Only mark this the first time, because this method can be called more than once during a chain download
        // if we switch peers during it.
        if (originalBlocksLeft == -1)
            originalBlocksLeft = blocksLeft;
        else
            log.info("Chain download switched to {}", peer);
        if (blocksLeft == 0) {
            doneDownload();
            future.set(peer.getBestHeight());
        }
    }

    @Override
    public void onBlocksDownloaded(Peer peer, Block block, int blocksLeft) {
        if (caughtUp)
            return;

        if (blocksLeft == 0) {
            caughtUp = true;
            doneDownload();
            future.set(peer.getBestHeight());
        }

        if (blocksLeft < 0 || originalBlocksLeft <= 0)
            return;

        double pct = 100.0 - (100.0 * (blocksLeft / (double) originalBlocksLeft));
        if ((int) pct != lastPercent) {
            progress(pct, blocksLeft);
            lastPercent = (int) pct;
        }
    }

    /**
     * Called when download progress is made.
     *
     * @param pct  wartość procentowa ściągniętych bloków
     * @param blocksLeft ilość blocków pozostałych do ściągnięcia
     */
    protected void progress(double pct, int blocksLeft) {
        log.info(String.format( "Chain download %d%% done with %d blocks to go", (int) pct, blocksLeft));
    }

    /**
     * Called when download is initiated.
     *
     * @param blocks the number of blocks to download, estimated
     */
    protected void startDownload(int blocks) {
        log.info("Downloading block chain of size " + blocks + ". " +
                (blocks > 1000 ? "This may take a while." : ""));
    }

    /**
     * Called when we are done downloading the block chain.
     */
    protected void doneDownload() {
    }

    /**
     * Wait for the chain to be downloaded.
     */
    public void await() throws InterruptedException {
        try {
            future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}

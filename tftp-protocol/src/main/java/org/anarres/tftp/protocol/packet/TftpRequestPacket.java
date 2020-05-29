/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.packet;

import com.google.common.base.Preconditions;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

/**
 *
 * @author shevek
 */
public abstract class TftpRequestPacket extends TftpPacket {

    private static final Logger LOG = LoggerFactory.getLogger(TftpRequestPacket.class);
    private String filename;
    private TftpMode mode;
    private int blockSize = TftpDataPacket.BLOCK_SIZE;

    private boolean mGotBlkSize = false;	// Set true if received explicit block size
    private long mTSize = -1;	// Set to a >= if received

    public String getFilename() {
        return filename;
    }

	/**
	 PIXILAB added check to avoid .. to escape the TDTP root, and prefix with leading / if none there, since this
	 TFT server seem to assume that.
	 */
    public void setFilename(@Nonnull String filename) {
		filename = Preconditions.checkNotNull(filename, "Filename was null.");
		Preconditions.checkArgument(!filename.contains(".."), ".. not allowed in path; %s", filename);
		if (!filename.startsWith(kReqFilenamePrefix))	// PXE boot requests sends filename without this
			filename = kReqFilenamePrefix + filename;
		this.filename = filename;
    }
	private static String kReqFilenamePrefix = "/";

    public TftpMode getMode() {
        return mode;
    }

    public void setMode(@Nonnull TftpMode mode) {
        this.mode = mode;
    }

	// True if got explicit block size
	public boolean gotBlockSize() {
    	return mGotBlkSize;
	}

    @Nonnegative
    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(@Nonnegative int blockSize) {
        this.blockSize = blockSize;
    }

    // Truw if received a tsize parameter
    public boolean gotTSize() {
    	return mTSize >= 0;
	}

	// The value of tsize param, if gotTSize
	public long getTSize() {
    	return mTSize;
	}

    @Override
    public void toWire(ByteBuffer buffer) {
        super.toWire(buffer);
        putString(buffer, getFilename());
        putString(buffer, getMode().name());
    }

    @Override
    public void fromWire(ByteBuffer buffer) {
        setFilename(getString(buffer));
        setMode(TftpMode.forMode(getString(buffer)));
        // This is enough to go on with, so we'll do our best with the rest.
        try {
            // RFC2348: blocksize option
            // TODO: Send an OACK to the client.
            while (buffer.hasRemaining()) {
                String word = getString(buffer);
                if ("blksize".equalsIgnoreCase(word)) {
					mGotBlkSize = true;
                    blockSize = Integer.parseInt(getString(buffer));
                    // TODO: Assert blockSize < 16K for safety.
                } else if ("timeout".equalsIgnoreCase(word)) {
                    // Read past timeout option
					int timeout = Integer.parseInt(getString(buffer));
                    LOG.error("Unhandled TFTP timeout");
                } else if ("tsize".equalsIgnoreCase(word)) {
                    this.mTSize = Integer.parseInt(getString(buffer));
                    // LOG.error("Unhandled TFTP tsize");
                } else {
                    LOG.error("Unknown TFTP command word " + word);
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse optional TFTP trailer: continuing anyway", e);
        }
    }

    @Override
    protected MoreObjects.ToStringHelper toStringHelper() {
        return super.toStringHelper()
                .add("filename", getFilename())
                .add("mode", getMode())
                .add("blockSize", getBlockSize());
    }
}
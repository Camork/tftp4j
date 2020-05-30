/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.engine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.IntMath;
import com.google.common.primitives.Chars;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.GuardedBy;

import org.anarres.tftp.protocol.packet.TftpAckPacket;
import org.anarres.tftp.protocol.packet.TftpDataPacket;
import org.anarres.tftp.protocol.packet.TftpErrorCode;
import org.anarres.tftp.protocol.packet.TftpErrorPacket;
import org.anarres.tftp.protocol.packet.TftpOptAckPackage;
import org.anarres.tftp.protocol.packet.TftpPacket;
import org.anarres.tftp.protocol.resource.TftpData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author shevek
 */
public abstract class AbstractTftpReadTransfer<TftpTransferContext> extends AbstractTftpTransfer<TftpTransferContext> {
    private static final Logger LOG = LogManager.getLogger();
    public static final int MAX_RETRIES = 3;
    private final TftpData source;
    private final int blockSize;
    private final int blockCount;
    // Runtime
    /** The next block to send, indexed from 0. */
    @GuardedBy("lock")
    private int sendBlock = 0;
//    @GuardedBy("lock")
//    private int sendWindow = 1;
    /** The last block acked, indexed from 0. */
    // We start at -2, then -1 on open, causing us to send 0.
    @GuardedBy("lock")
    private int recvBlock = -2;
    @GuardedBy("lock")
    private int recvRetry = 0;
    private final Object lock = new Object();

    private final TftpOptAckPackage mOptAck;	// To send at start, and await ack for, if not null

    public AbstractTftpReadTransfer(
    	@Nonnull SocketAddress remoteAddress,
    	@Nonnull TftpData source,
    	@Nonnegative int blockSize,
		TftpOptAckPackage optAck	// Send first, then await ack for it before sending data, if specified
	) {
        super(remoteAddress);
        this.source = source;
        this.blockSize = blockSize;
        this.blockCount = IntMath.divide(source.getSize() + 1, blockSize, RoundingMode.CEILING);
		mOptAck = optAck;
    }

    @Nonnull
    public abstract ByteBuffer allocate(@Nonnull TftpTransferContext context, @Nonnegative int length);

    /**
     * @param blockNumber indexed from 0
     */
    @Nonnull
    @GuardedBy("lock")
    private TftpDataPacket newPacket(@Nonnull TftpTransferContext context, int blockNumber) throws IOException {
        ByteBuffer buf = allocate(context, blockSize);
        // Note that if length is 0, we still construct a "final" zero-length packet.
        source.read(buf, blockNumber * blockSize);
        buf.flip();
        return new TftpDataPacket(Chars.checkedCast(blockNumber + 1), buf);
    }

    /**
     * @param ackBlock for real data block indexed from 0. I will get -1 to get started sending first data
     * block, which can either happen immediately (through my open handler), or as a result of getting
     * the ack for TftpOptAckPackage (if any sent).
     */
    @VisibleForTesting
    void ack(@Nonnull TftpTransferContext context, int ackBlock) throws Exception {
        // if (LOG.isDebugEnabled()) LOG.debug("<- Ack protocol-block {} (index {})", (ackBlock + 1), ackBlock);

        synchronized (lock) {
            if (ackBlock < recvBlock) {
                LOG.warn("{}: Out of order ack {} < {} previously received", this, ackBlock, recvBlock);
                // An ack got out of order?
            } else {
                sendBlock = ackBlock + 1;
                if (ackBlock == recvBlock) {
                    LOG.warn("{}: re-send packet {}", this, sendBlock);
                } else {
                    recvBlock = ackBlock;
                    recvRetry = 0;
                }
            }

            // We are done.
            if(sendBlock >= blockCount){
                close(context);
                return;
            }

            send(context, newPacket(context, sendBlock));
        }
    }

    @Override
    public void open(@Nonnull TftpTransferContext context) throws Exception {
    	if (mOptAck == null)
			ack(context, -1);	// Get us going with 1st block
		else
			send(context, mOptAck);	    // Else will get explicit ACK -1 originating from client
		flush(context);
    }

    @Override
    public void handle(@Nonnull TftpTransferContext context, @Nonnull TftpPacket packet) throws Exception {
        switch (packet.getOpcode()) {
            case ACK:
                TftpAckPacket ack = (TftpAckPacket) packet;
                // Below will yield -1 as block number passed to ack for any TftpOptAckPackage ack
                ack(context, ack.getBlockNumber() - 1);
                break;
            case RRQ:
            case WRQ:
            case DATA:
            default:
                LOG.warn("{}: Unexpected TFTP " + packet.getOpcode() + " packet: " + packet, this);
                send(context, new TftpErrorPacket(packet.getRemoteAddress(), TftpErrorCode.ILLEGAL_OPERATION));
                close(context);
                break;
            case ERROR:
                TftpErrorPacket error = (TftpErrorPacket) packet;
                LOG.error("{}: Received TFTP error packet: {}", this, error);
                close(context);
                break;
        }
        flush(context);
    }

    @Override
    public void timeout(@Nonnull TftpTransferContext context) throws Exception {
        //client has the timeout mechanism. just the close transmission.
        close(context);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close(@Nonnull TftpTransferContext context) throws Exception {
        source.close();
    }
}
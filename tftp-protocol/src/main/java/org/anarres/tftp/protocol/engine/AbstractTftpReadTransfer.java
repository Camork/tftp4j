/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.engine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.IntMath;
import com.google.common.primitives.Chars;
import org.anarres.tftp.protocol.packet.*;
import org.anarres.tftp.protocol.resource.TftpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 *
 * @author shevek
 */
public abstract class AbstractTftpReadTransfer<TftpTransferContext> extends AbstractTftpTransfer<TftpTransferContext> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTftpReadTransfer.class);
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

    public AbstractTftpReadTransfer(@Nonnull SocketAddress remoteAddress, @Nonnull TftpData source, @Nonnegative int blockSize) throws IOException {
        super(remoteAddress);
        this.source = source;
        this.blockSize = blockSize;
        this.blockCount = IntMath.divide(source.getSize() + 1, blockSize, RoundingMode.CEILING);
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
     * @param ackBlock indexed from 0
     */
    @VisibleForTesting
    /* pp */ void ack(@Nonnull TftpTransferContext context, /* @Nonnegative */ int ackBlock) throws Exception {
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
        ack(context, -1);
        flush(context);
    }

    @Override
    public void handle(@Nonnull TftpTransferContext context, @Nonnull TftpPacket packet) throws Exception {
        switch (packet.getOpcode()) {
            case ACK:
                TftpAckPacket ack = (TftpAckPacket) packet;
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
/*
        if (recvRetry++ > MAX_RETRIES){
            LOG.error("{}: Retries exceeded {} at packet {}", this, AbstractTftpReadTransfer.MAX_RETRIES, recvBlock + 1);
            close(context);
        } else {
            ack(context, recvBlock);
        }
*/
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void close(@Nonnull TftpTransferContext context) throws Exception {
        source.close();
    }
}
package org.anarres.tftp.protocol.engine;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.LongMath;

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
 * @author Changshun Wu
 */
public abstract class AbstractTftpWriteTransfer<TftpTransferContext> extends AbstractTftpTransfer<TftpTransferContext> {

    private static final Logger LOG = LogManager.getLogger();
    private final TftpData source;
    private final int blockSize;
    private final long tsize;
    private final long blockCount;
    /**
     * The next block to send, indexed from 0.
     */
    @GuardedBy("lock")
    private int ackBlock = 0;

    /**
     * The last data, indexed from 0.
     */
    @GuardedBy("lock")
    private int recvBlock = 0;
    @GuardedBy("lock")
    private int recvRetry = 0;
    private final Object lock = new Object();

    private final TftpOptAckPackage mOptAck;

    public AbstractTftpWriteTransfer(
        @Nonnull SocketAddress remoteAddress,
        @Nonnull TftpData source,
        @Nonnegative int blockSize,
        @Nonnegative long tsize,
        TftpOptAckPackage optAck
    ) {
        super(remoteAddress);
        this.source = source;
        this.blockSize = blockSize;
        this.tsize = tsize;
        this.blockCount = LongMath.divide(tsize + 1, blockSize, RoundingMode.CEILING);
        mOptAck = optAck;
    }

    @GuardedBy("lock")
    protected abstract void write(TftpTransferContext context, ByteBuffer in, int offset) throws IOException;

    @VisibleForTesting
    void data(@Nonnull TftpTransferContext context, TftpDataPacket data) throws Exception {
        int dataBlock = data.getBlockNumber();
        int size = data.getData().remaining();
        synchronized (lock) {
            if (dataBlock < recvBlock) {
                LOG.warn("{}: Out of order data {} < {} previously received", this, dataBlock, this.recvBlock);
                // An data got out of order?
            } else {
                ackBlock = dataBlock;
                if (dataBlock == recvBlock) {
                    LOG.warn("{}: re-send packet {}", this, ackBlock);
                } else {
                    recvBlock = dataBlock;
                    recvRetry = 0;
                }
            }

            if (ackBlock < blockCount && size != blockSize) {
                LOG.warn("Unexpected data size:" + size);
            }

            //data packet index from 1
            write(context, data.getData(), (dataBlock - 1) * blockSize);
            send(context, new TftpAckPacket(ackBlock));
        }
    }

    @Override
    public void open(@Nonnull TftpTransferContext context) throws Exception {
        if (mOptAck == null) {
            send(context, new TftpAckPacket(0));
        } else {
            send(context, mOptAck);
        }
        flush(context);
    }

    @Override
    public void handle(@Nonnull TftpTransferContext context, @Nonnull TftpPacket packet) throws Exception {
        switch (packet.getOpcode()) {
            case DATA:
                TftpDataPacket data = (TftpDataPacket)packet;
                data(context, data);
                break;
            case ACK:
            case RRQ:
            case WRQ:
            default:
                LOG.warn("{}: Unexpected TFTP " + packet.getOpcode() + " packet: " + packet, this);
                send(context, new TftpErrorPacket(packet.getRemoteAddress(), TftpErrorCode.ILLEGAL_OPERATION));
                close(context);
                break;
            case ERROR:
                TftpErrorPacket error = (TftpErrorPacket)packet;
                LOG.error("{}: Received TFTP error packet: {}", this, error);
                close(context);
                break;
        }
        flush(context);

        // We are done.
        if (ackBlock >= blockCount) {
            close(context);
        }
    }

    public TftpData getSource() {
        return source;
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
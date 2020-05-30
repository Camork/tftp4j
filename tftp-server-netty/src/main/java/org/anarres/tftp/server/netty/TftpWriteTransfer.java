/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.server.netty;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

import org.anarres.tftp.protocol.engine.AbstractTftpWriteTransfer;
import org.anarres.tftp.protocol.packet.TftpOptAckPackage;
import org.anarres.tftp.protocol.packet.TftpPacket;
import org.anarres.tftp.protocol.resource.TftpData;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * @author Charles Wu
 */
public class TftpWriteTransfer extends AbstractTftpWriteTransfer<Channel> {

    public TftpWriteTransfer(
        @Nonnull SocketAddress remoteAddress,
        @Nonnull TftpData source,
        int blockSize,
        long tsize,
        TftpOptAckPackage optAck) {
        super(remoteAddress, source, blockSize, tsize, optAck);
    }

    @Override
    public void write(Channel context, ByteBuffer in, int offset) throws IOException {
        getSource().write(in, offset);
    }

    @Override
    public void send(@Nonnull Channel channel, @Nonnull TftpPacket packet) throws Exception {
        packet.setRemoteAddress(getRemoteAddress());
        channel.write(packet, channel.voidPromise());
    }

    @Override
    public void flush(@Nonnull Channel channel) throws Exception {
        channel.flush();
    }

    @Override
    public void close(@Nonnull Channel channel) throws Exception {
        channel.close().addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        super.close(channel);
    }
}

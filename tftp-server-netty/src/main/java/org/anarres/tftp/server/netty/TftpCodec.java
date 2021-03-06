/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import javax.annotation.Nonnull;
import org.anarres.tftp.protocol.codec.TftpPacketDecoder;
import org.anarres.tftp.protocol.packet.TftpPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author shevek
 */
@ChannelHandler.Sharable
public class TftpCodec extends ChannelDuplexHandler {

    private static final Logger LOG = LogManager.getLogger();
    private final TftpPacketDecoder decoder = new TftpPacketDecoder();

    @Nonnull
    public TftpPacket decode(@Nonnull ChannelHandlerContext ctx, @Nonnull DatagramPacket packet) throws Exception {
        return decoder.decode(packet.sender(), packet.content().nioBuffer());
    }

    @Nonnull
    public DatagramPacket encode(@Nonnull ChannelHandlerContext ctx, @Nonnull TftpPacket packet) throws Exception {
        ByteBuf buf = ctx.alloc().buffer(packet.getWireLength());
        ByteBuffer buffer = buf.nioBuffer(buf.writerIndex(), buf.writableBytes());
        packet.toWire(buffer);
        buffer.flip();
        buf.writerIndex(buf.writerIndex() + buffer.remaining());
        return new DatagramPacket(buf, (InetSocketAddress) packet.getRemoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            ctx.fireChannelRead(decode(ctx, (DatagramPacket) msg));
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            ctx.write(encode(ctx, (TftpPacket) msg), promise);
            // LOG.info("Write: OK");
        } catch (Throwable e) {
            // LOG.info("Write: " + e);
            // https://github.com/netty/netty/issues/3060 - exception not reported by pipeline.
            promise.tryFailure(e);
        } finally {
            // It isn't, but it might become so?
            ReferenceCountUtil.release(msg);
        }
    }
}
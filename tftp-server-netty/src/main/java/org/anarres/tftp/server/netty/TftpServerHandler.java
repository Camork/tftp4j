/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.server.netty;

import javax.annotation.Nonnull;

import org.anarres.tftp.protocol.engine.TftpTransfer;
import org.anarres.tftp.protocol.packet.TftpErrorCode;
import org.anarres.tftp.protocol.packet.TftpErrorPacket;
import org.anarres.tftp.protocol.packet.TftpOptAckPackage;
import org.anarres.tftp.protocol.packet.TftpPacket;
import org.anarres.tftp.protocol.packet.TftpRequestPacket;
import org.anarres.tftp.protocol.packet.TftpWriteRequestPacket;
import org.anarres.tftp.protocol.resource.TftpData;
import org.anarres.tftp.protocol.resource.TftpDataProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 *
 * @author shevek
 */
public class TftpServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LogManager.getLogger();
    private final TftpPipelineInitializer.SharedHandlers sharedHandlers;
    private final TftpDataProvider provider;

    public TftpServerHandler(@Nonnull TftpPipelineInitializer.SharedHandlers sharedHandlers, @Nonnull TftpDataProvider provider) {
        this.sharedHandlers = sharedHandlers;
        this.provider = provider;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            final TftpPacket packet = (TftpPacket) msg;
            Channel channel = ctx.channel();

            switch (packet.getOpcode()) {
                case RRQ: {
                    TftpRequestPacket request = (TftpRequestPacket) packet;
                    TftpData source = provider.open(request.getFilename());
                    if (source == null) {
                        ctx.writeAndFlush(new TftpErrorPacket(packet.getRemoteAddress(), TftpErrorCode.FILE_NOT_FOUND), ctx.voidPromise());
                    } else {
                        TftpTransfer<Channel> transfer = new TftpReadTransfer(
                        	packet.getRemoteAddress(),
                        	source,
                        	request.getBlockSize(),
							TftpOptAckPackage.makeIfDesired(packet.getRemoteAddress(), request, provider)
						);
                        Bootstrap bootstrap = new Bootstrap()
                                .group(ctx.channel().eventLoop())
                                .channel(channel.getClass())
                                .handler(new TftpPipelineInitializer(sharedHandlers, new TftpTransferHandler(transfer)));
                        bootstrap.connect(packet.getRemoteAddress());
                    }
                    break;
                }
                case WRQ: {
                    TftpWriteRequestPacket request = (TftpWriteRequestPacket) packet;
                    long tsize = request.getTSize() == -1 ? 0 : request.getTSize();
                    TftpData source = provider.openForWrite(request.getFilename(), tsize);
                    if (source == null) {
                        ctx.writeAndFlush(new TftpErrorPacket(packet.getRemoteAddress(), TftpErrorCode.FILE_NOT_FOUND), ctx.voidPromise());
                    } else {
                        TftpTransfer<Channel> transfer = new TftpWriteTransfer(
                            packet.getRemoteAddress(),
                            source,
                            request.getBlockSize(),
                            request.getTSize(),
                            TftpOptAckPackage.makeIfDesired(packet.getRemoteAddress(), request, provider)
                        );
                        Bootstrap bootstrap = new Bootstrap()
                            .group(ctx.channel().eventLoop())
                            .channel(channel.getClass())
                            .handler(new TftpPipelineInitializer(sharedHandlers, new TftpTransferHandler(transfer)));
                        bootstrap.connect(packet.getRemoteAddress());
                    }

                    break;
                }
                case ACK: {
                    break;
                }
                case DATA: {
                    LOG.warn("Unexpected TFTP " + packet.getOpcode() + " packet: " + packet);
                    ctx.writeAndFlush(new TftpErrorPacket(packet.getRemoteAddress(), TftpErrorCode.ILLEGAL_OPERATION), ctx.voidPromise());
                    break;
                }
                case ERROR: {
                    LOG.error("Received TFTP error packet: {}", packet);
                    break;
                }
                default: {
                    LOG.error("Received unknown TFTP packet: {}", packet);
                    break;
                }
            }

        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
            throw e;
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("Error on channel: " + cause, cause);
        // LOG.error("Reported here: " + cause, new Exception("Here"));
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.server.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LoggingHandler;
import javax.annotation.Nonnull;

import io.netty.handler.timeout.IdleStateHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author shevek
 */
public class TftpPipelineInitializer extends ChannelInitializer<Channel> {

    private static final Logger LOG = LogManager.getLogger();

    public static class SharedHandlers {
        // These are all singleton instances.

        // private final TftpExceptionHandler exceptionHandler = new TftpExceptionHandler();
        private final LoggingHandler wireLogger = new LoggingHandler("tftp-datagram");
        private final TftpCodec codec = new TftpCodec();
        private final LoggingHandler packetLogger = new LoggingHandler("tftp-packet");/* {
         @Override
         public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
         try {
         super.write(ctx, msg, promise);
         } catch (Exception e) {
         LOG.error("Failed in packet", e);
         throw e;
         }
         }

         @Override
         public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
         LOG.warn("Closed here", new Exception());
         super.close(ctx, promise);
         }
         };*/

        private boolean debug = false;

        public void setDebug(boolean debug) {
            this.debug = debug;
        }
    }
    private final SharedHandlers sharedHandlers;
    private final ChannelHandler handler;

    public TftpPipelineInitializer(@Nonnull SharedHandlers sharedHandlers, @Nonnull ChannelHandler handler) {
        this.sharedHandlers = sharedHandlers;
        this.handler = handler;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        // LOG.info("Initialize " + this);
        ChannelPipeline pipeline = ch.pipeline();
        // pipeline.addLast(sharedHandlers.exceptionHandler);
        // if (sharedHandlers.debug) pipeline.addLast(sharedHandlers.wireLogger);
        pipeline.addLast(sharedHandlers.codec);
        if (sharedHandlers.debug){
            pipeline.addLast(sharedHandlers.packetLogger);
        }
        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(handler);
    }
}
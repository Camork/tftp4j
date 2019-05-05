/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.server.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;

/**
 *	Tweaked by Mike@Pixilab to remove EPOLL dependency and taking an ExecutorService
 * instead of a thread factory, to make sure no threads are wasted when TFTP not used
 * for some time.
 *
 * Original author shevek
 */
public enum TftpChannelType {

    NIO {
        @Override
        public EventLoopGroup newEventLoopGroup(ExecutorService es) {
            return new NioEventLoopGroup(0, es);
        }

        @Override
        public Class<? extends DatagramChannel> getChannelType() {
            return NioDatagramChannel.class;
        }
    } , EPOLL {
        @Override
        public EventLoopGroup newEventLoopGroup(ExecutorService es) {
			throw new RuntimeException("EPOLL Not implemented");
            // return new EpollEventLoopGroup(0, es);
        }

        @Override
        public Class<? extends DatagramChannel> getChannelType() {
        	throw new RuntimeException("EPOLL Not implemented");
            // return EpollDatagramChannel.class;
        }
    } ;

    @Nonnull
    public abstract EventLoopGroup newEventLoopGroup(@Nonnull ExecutorService factory);

    @Nonnull
    public abstract Class<? extends DatagramChannel> getChannelType();
}

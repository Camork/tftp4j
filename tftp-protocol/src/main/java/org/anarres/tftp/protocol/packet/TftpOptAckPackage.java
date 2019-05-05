/*
 * Copyright (c) 2019 PIXILAB Technologies AB, Sweden (http://pixilab.se). All Rights Reserved.
 */

package org.anarres.tftp.protocol.packet;

import org.anarres.tftp.protocol.resource.TftpDataProvider;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * A stab at implementing Options ACK according to http://tools.ietf.org/html/rfc2347
 */
public class TftpOptAckPackage extends TftpPacket {
	private final TftpRequestPacket mReqToAck;	// The request containing options to acknowledge
	private final TftpDataProvider mProvider;

	private TftpOptAckPackage(SocketAddress remoteAddress, TftpRequestPacket reqToAck, TftpDataProvider provider) {
		mReqToAck = reqToAck;
		mProvider = provider;
		setRemoteAddress(remoteAddress);
	}

	/**
	 Ret TftpOptAckPackage if I should send one for this request.
	 */
	public static TftpOptAckPackage makeIfDesired(SocketAddress remote, TftpRequestPacket request, TftpDataProvider provider) {
		return request.gotTSize() || request.gotBlockSize() ?
			new TftpOptAckPackage(remote, request, provider) :
			null;	// No interestig options found - no opt ack needed
	}

	@Nonnull
	@Override
	public TftpOpcode getOpcode() {
		return TftpOpcode.ACK_WITH_OPTIONS;
	}

	@Override
	public void fromWire(@Nonnull ByteBuffer buffer) {
		// We don't expect to receive this packet type from client
	}

	/**
	 Currently handles ONLY options I care about.
	 */
	@Override
	public void toWire(@Nonnull ByteBuffer buffer) {
		super.toWire(buffer);	// Writes 2-byte opcode
		if (mReqToAck.gotTSize()) {
			// Reply with actual total transfer (e.g. file) size
			long size = 0;
			try {
				size = mProvider.dataSize(mReqToAck.getFilename());
			} catch (IOException iox) {
				// Just leave size 0 for now - should cause error later...
			}
			putString(buffer, "tsize");
			putString(buffer, Long.toString(size));
		}

		if (mReqToAck.gotBlockSize()) {
			putString(buffer, "blksize");
			putString(buffer, Long.toString( mReqToAck.getBlockSize()));
		}
	}
}

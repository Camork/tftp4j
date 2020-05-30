/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.resource;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author shevek
 */
public class TftpByteBufferData extends AbstractTftpData {

    private final ByteBuffer data;

    public TftpByteBufferData(@Nonnull ByteBuffer data) {
        this.data = data;
    }

    @Override
    public int getSize() {
        return data.remaining();
    }

    @Override
    public int read(@Nonnull ByteBuffer out, int offset) throws IOException {
        Preconditions.checkPositionIndex(offset, getSize(), "Illegal data offset.");
        int length = Math.min(getSize() - offset, out.remaining());
        ByteBuffer slice = data.slice();    // We might not be reading from a full buffer.
        slice.position(offset).limit(offset + length);
        out.put(slice);
        return length;
    }

    @Override
    public int write(ByteBuffer in, int offset){
        data.put(in);

        return in.capacity();
    }

}
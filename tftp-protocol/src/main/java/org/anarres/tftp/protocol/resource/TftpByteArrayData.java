/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.resource;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;

import javax.annotation.Nonnull;

/**
 * @author shevek
 */
public class TftpByteArrayData extends AbstractTftpData {

    private final byte[] data;

    public TftpByteArrayData(@Nonnull byte[] data) {
        this.data = data;
    }

    @Override
    public int getSize() {
        return data.length;
    }

    @Override
    public int read(@Nonnull ByteBuffer out, int offset) {
        Preconditions.checkPositionIndex(offset, getSize(), "Illegal data offset.");
        int length = Math.min(getSize() - offset, out.remaining());
        out.put(data, offset, length);
        return length;
    }

    @Override
    public int write(ByteBuffer in, int offset) {
        byte[] array = in.array();
        System.arraycopy(array, 0, data, offset, array.length);
        return array.length;
    }

}
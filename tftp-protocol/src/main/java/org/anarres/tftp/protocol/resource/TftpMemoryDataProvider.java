/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.resource;

import com.google.common.base.Charsets;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shevek
 */
public class TftpMemoryDataProvider extends AbstractTftpDataProvider {

    private final Map<String,TftpByteArrayData> map = new HashMap<>();

    public void setData(@Nonnull String name, @CheckForNull byte[] data) {
        if (data == null) {
            map.remove(name);
        } else {
            map.put(name, new TftpByteArrayData(data));
        }
    }

    public void setData(@Nonnull String name, @Nonnull String data) {
        setData(name, data.getBytes(Charsets.ISO_8859_1));
    }

    @CheckForNull
    public TftpByteArrayData getData(String name) {
        return map.get(name);
    }

    @CheckForNull
    @Override
    public TftpData openForWrite(@Nonnull String filename, int tsize) {
        TftpByteArrayData data = new TftpByteArrayData(new byte[tsize]);
        map.put(filename, data);
        return data;
    }

    @Override
    public TftpData open(@Nonnull String filename) {
        return getData(filename);
    }

    @Override
    public long dataSize(String filename) {
        TftpData data = getData(filename);
        return (data == null) ? 0 : data.getSize();
    }

}
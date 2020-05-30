/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.resource;

import com.google.common.primitives.Ints;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class TftpFileChannelDataProvider extends AbstractTftpDataProvider {

    public static final String DEFAULT_PREFIX = "/tftproot";
    private final String prefix;

    public TftpFileChannelDataProvider(@Nonnull String prefix) {
        this.prefix = prefix;
    }

    public TftpFileChannelDataProvider() {
        this(DEFAULT_PREFIX);
    }

    @Nonnull
    public String getPrefix() {
        return prefix;
    }

    /**
     * Prepends the prefix to the filename, then returns an ordinary file, or null.
     */
    @CheckForNull
    protected File toFile(@Nonnull String filename) {
        String path = toPath(getPrefix(), filename);
        if (path == null)
            return null;
        File file = new File(path);
        if (!file.isFile())
            return null;
        if (!file.canRead())
            return null;
        return file;
    }

    @CheckForNull
    @Override
    public TftpData openForWrite(@Nonnull String filename, int tsize) throws IOException {
        File file = toFile(filename);
        if (file == null)
            return null;
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        FileChannel channel = raf.getChannel();
        return new TftpFileChannelData(channel, Ints.checkedCast(channel.size()));
    }

    @Override
    // @IgnoreJRERequirement
    public TftpData open(@Nonnull String filename) throws IOException {
        File file = toFile(filename);
        if (file == null)
            return null;
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        return new TftpFileChannelData(channel, Ints.checkedCast(channel.size()));
    }

	@Override
	public long dataSize(String filename) throws IOException {
		File file = toFile(filename);
		return file.length();
	}

}

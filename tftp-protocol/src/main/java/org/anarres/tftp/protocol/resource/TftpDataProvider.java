/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.resource;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author shevek
 */
public interface TftpDataProvider {

    /**
     * Request for write
     */
    @CheckForNull
    public TftpData openForWrite(@Nonnull String filename, int tsize) throws IOException;

    /**
     * Returns the resource with the given name.
     */
    @CheckForNull
    public TftpData open(@Nonnull String filename) throws IOException;

    // Number of bytes that can be provided (e.g., length of file)
    public long dataSize(String filename) throws IOException;
}

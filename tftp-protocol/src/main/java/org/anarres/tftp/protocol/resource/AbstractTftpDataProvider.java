/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.resource;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public abstract class AbstractTftpDataProvider implements TftpDataProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTftpDataProvider.class);

    @CheckForNull
    protected static String toPath(@Nonnull String prefix, @Nonnull String path) {
        path = Files.simplifyPath(path);
        if (!path.startsWith("/")) {
            LOG.error("Not absolute: " + path);
            return null;
        }
        return prefix + path;
    }
}

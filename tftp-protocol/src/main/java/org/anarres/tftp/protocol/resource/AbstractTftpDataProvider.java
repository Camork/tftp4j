/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.protocol.resource;

import com.google.common.io.Files;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author shevek
 */
public abstract class AbstractTftpDataProvider implements TftpDataProvider {

    private static final Logger LOG = LogManager.getLogger();

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

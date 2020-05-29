/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.tftp.server.netty;

import org.anarres.tftp.protocol.engine.TftpServerTester;
import org.junit.Before;
import org.junit.Test;

import io.netty.util.ResourceLeakDetector;

/**
 *
 * @author shevek
 */
public class TftpServerTest {

    @Before
    public void setUp() {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    @Test
    public void testServer() throws Exception {
        TftpServerTester tester = new TftpServerTester();
        TftpServer server = new TftpServer(tester.getProvider(), tester.getPort());
        server.setDebug(true);
        server.setChannelType(TftpChannelType.NIO);
        try {
            server.start();
            tester.run();
             Thread.sleep(1000000);
        } finally {
            server.stop();
        }
    }
}
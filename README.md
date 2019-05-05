tftp4j
======

A Java TFTP server and protocol handler.

This code originally came from a variety of third party open source
projects, and the objective is to give it a complete overhaul into
a modern, correct and complete TFTP implementation.

The [JavaDoc API](http://shevek.github.io/tftp4j/docs/javadoc/)
is available.

My Notes
====

This is a fork of a fork. I started from https://github.com/RoxLiu/tftp4j which
adds timeout to avoid leaking sockets on stale TFTP transfers. I then added
basic support for Option Acknowledgement according to https://tools.ietf.org/html/rfc2347
in order to support PXE booting. In particular the need for tsize preflighting, used to
probe for total transfer size before starting the transfer.


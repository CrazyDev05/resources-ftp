package de.crazydev22.resourcesftp;

import it.sauronsoftware.ftp4j.FTPClient;

@FunctionalInterface
public interface FtpClientTask<T> {

    T run(FTPClient client);
}

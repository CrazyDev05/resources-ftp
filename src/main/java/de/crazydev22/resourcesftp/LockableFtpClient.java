package de.crazydev22.resourcesftp;

import org.gradle.internal.concurrent.Stoppable;

public interface LockableFtpClient extends Stoppable {
    FtpHost getHost();
    boolean isConnected();
    <T> T run(FtpClientTask<T> task);
    <T> Runnable runWithLock(FtpClientTask<T> task);
}

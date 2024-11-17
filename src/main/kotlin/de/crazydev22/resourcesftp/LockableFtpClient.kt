package de.crazydev22.resourcesftp

import org.apache.commons.net.ftp.FTPClient
import org.gradle.internal.concurrent.Stoppable

interface LockableFtpClient : Stoppable {
    val host: FtpHost
    val isConnected: Boolean
    fun <T> run(task: (FTPClient) -> T): T
}

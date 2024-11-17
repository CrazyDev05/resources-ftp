package de.crazydev22.resourcesftp

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.resources.ResourceException
import org.gradle.internal.concurrent.CompositeStoppable
import org.gradle.internal.concurrent.Stoppable
import org.gradle.internal.service.scopes.Scope
import org.gradle.internal.service.scopes.ServiceScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
@ServiceScope(Scope.Global::class)
class FtpClientFactory : Stoppable {
    private val lock = Any()
    private val allClients: MutableList<LockableFtpClient?> = mutableListOf()
    private val idleClients: MutableMap<FtpHost, MutableList<LockableFtpClient>> = mutableMapOf()

    fun createFtpClient(uri: URI, credentials: PasswordCredentials?): LockableFtpClient {
        synchronized(lock) {
            val sftpHost = FtpHost(uri, credentials)
            return acquireClient(sftpHost)
        }
    }

    private fun acquireClient(ftpHost: FtpHost): LockableFtpClient {
        return if (idleClients.containsKey(ftpHost)) reuseExistingOrCreateNewClient(ftpHost) else createNewClient(ftpHost)
    }

    private fun reuseExistingOrCreateNewClient(ftpHost: FtpHost): LockableFtpClient {
        val clientsByHost = idleClients[ftpHost] ?: mutableListOf()

        var client: LockableFtpClient
        if (clientsByHost.isEmpty()) {
            LOGGER.debug("No existing sftp clients.  Creating a new one.")
            client = createNewClient(ftpHost)
        } else {
            client = clientsByHost.removeAt(0)
            if (!client.isConnected) {
                LOGGER.info("Tried to reuse an existing sftp client, but unexpectedly found it disconnected.  Discarding and trying again.")
                discard(client)
                client = reuseExistingOrCreateNewClient(ftpHost)
            } else {
                LOGGER.debug("Reusing an existing sftp client.")
            }
        }

        return client
    }

    private fun createNewClient(host: FtpHost): LockableFtpClient {
        val client = createFtpClient(host)
        allClients.add(client)
        return client
    }

    private fun discard(client: LockableFtpClient) {
        try {
            client.stop()
        } finally {
            allClients.remove(client)
        }
    }

    fun releaseFtpClient(ftpClient: LockableFtpClient) {
        synchronized(lock) {
            idleClients.computeIfAbsent(ftpClient.host) { mutableListOf() }.add(ftpClient)
        }
    }

    override fun stop() {
        synchronized(lock) {
            try {
                CompositeStoppable.stoppable(allClients).stop()
            } finally {
                allClients.clear()
                idleClients.clear()
            }
        }
    }


    private class DefaultLockableFtpClient constructor(
        override val host: FtpHost,
        private val ftpClient: FTPClient
    ) : LockableFtpClient {
        private val lock = ReentrantLock(true)

        override val isConnected: Boolean
            get() = ftpClient.isConnected

        override fun <T> run(task: (FTPClient) -> T): T {
            lock.lock()
            try {
                return task(ftpClient)
            } finally {
                lock.unlock()
            }
        }

        override fun stop() {
            try {
                ftpClient.disconnect()
            } catch (ignored: IOException) {}
        }

    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(FtpClientFactory::class.java)

        private fun createFtpClient(ftpHost: FtpHost): LockableFtpClient {
            val client = if (ftpHost.ssl) FTPSClient() else FTPClient()
            try {
                client.connect(ftpHost.hostname, ftpHost.port)
                if (ftpHost.shouldLogin() && !client.login(ftpHost.username, ftpHost.password))
                    throw IOException("Auth fail")

                return DefaultLockableFtpClient(ftpHost, client)
            } catch (e: IOException) {
                val serverUri = URI.create(String.format("ftp%s://%s:%d", if (ftpHost.ssl) "s" else "", ftpHost.hostname, ftpHost.port))
                if (e.message == "Auth fail")
                    throw ResourceException(serverUri, String.format("Password authentication not supported or invalid credentials for FTP server at %s", serverUri), e)
                throw ResourceException(serverUri, String.format("Could not connect to FTP server at %s", serverUri), e)
            }
        }
    }
}

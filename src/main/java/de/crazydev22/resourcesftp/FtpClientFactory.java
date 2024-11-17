package de.crazydev22.resourcesftp;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.concurrent.CompositeStoppable;
import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
@ServiceScope(Scope.Global.class)
public class FtpClientFactory implements Stoppable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpClientFactory.class);

    private final Object lock = new Object();
    private final List<LockableFtpClient> allClients = new ArrayList<>();
    private final Map<FtpHost, List<LockableFtpClient>> idleClients = new HashMap<>();

    public LockableFtpClient createFtpClient(URI uri, PasswordCredentials credentials) {
        synchronized (lock) {
            FtpHost ftpHost = new FtpHost(uri, credentials);
            return acquireClient(ftpHost);
        }
    }

    private LockableFtpClient acquireClient(FtpHost ftpHost) {
        return idleClients.containsKey(ftpHost) ? reuseExistingOrCreateNewClient(ftpHost) : createNewClient(ftpHost);
    }

    private LockableFtpClient reuseExistingOrCreateNewClient(FtpHost ftpHost) {
        List<LockableFtpClient> clientsByHost = idleClients.computeIfAbsent(ftpHost, k -> new ArrayList<>());

        LockableFtpClient client;
        if (clientsByHost.isEmpty()) {
            LOGGER.debug("No existing sftp clients. Creating a new one.");
            client = createNewClient(ftpHost);
        } else {
            client = clientsByHost.remove(0);
            if (!client.isConnected()) {
                LOGGER.info("Tried to reuse an existing sftp client, but unexpectedly found it disconnected. Discarding and trying again.");
                discard(client);
                client = reuseExistingOrCreateNewClient(ftpHost);
            } else {
                LOGGER.debug("Reusing an existing sftp client.");
            }
        }

        return client;
    }

    private LockableFtpClient createNewClient(FtpHost host) {
        LockableFtpClient client = createFtpClient(host);
        allClients.add(client);
        return client;
    }

    private void discard(LockableFtpClient client) {
        try {
            client.stop();
        } finally {
            allClients.remove(client);
        }
    }

    public void releaseFtpClient(LockableFtpClient ftpClient) {
        synchronized (lock) {
            idleClients.computeIfAbsent(ftpClient.getHost(), k -> new ArrayList<>()).add(ftpClient);
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            try {
                CompositeStoppable.stoppable(allClients).stop();
            } finally {
                allClients.clear();
                idleClients.clear();
            }
        }
    }

    private class DefaultLockableFtpClient implements LockableFtpClient {
        private final FtpHost host;
        private final FTPClient ftpClient;
        private final ReentrantLock lock = new ReentrantLock(true);

        DefaultLockableFtpClient(FtpHost host, FTPClient ftpClient) {
            this.host = host;
            this.ftpClient = ftpClient;
        }

        @Override
        public FtpHost getHost() {
            return host;
        }

        @Override
        public boolean isConnected() {
            return ftpClient.isConnected();
        }

        @Override
        public <T> T run(FtpClientTask<T> task, boolean release) {
            lock.lock();
            try {
                return task.run(ftpClient);
            } finally {
                lock.unlock();
                if (release) {
                    FtpClientFactory.this.releaseFtpClient(this);
                }
            }
        }

        @Override
        public void stop() {
            try {
                ftpClient.disconnect(true);
            } catch (IOException | FTPIllegalReplyException | FTPException ignored) {}
        }
    }

    private LockableFtpClient createFtpClient(FtpHost ftpHost) {
        FTPClient client = new FTPClient();
        client.setSecurity(ftpHost.getSecurity().ordinal());
        try {
            client.connect(ftpHost.getHostname(), ftpHost.getPort());
            if (ftpHost.shouldLogin())
                client.login(ftpHost.getUsername(), ftpHost.getPassword());

            return new DefaultLockableFtpClient(ftpHost, client);
        } catch (FTPException e) {
            URI serverUri = URI.create(String.format("%s://%s:%d", ftpHost.getSecurity().name().toLowerCase(), ftpHost.getHostname(), ftpHost.getPort()));
            throw new ResourceException(serverUri, String.format("Password authentication not supported or invalid credentials for FTP server at %s", serverUri), e);
        } catch (IOException | FTPIllegalReplyException e) {
            URI serverUri = URI.create(String.format("%s://%s:%d", ftpHost.getSecurity().name().toLowerCase(), ftpHost.getHostname(), ftpHost.getPort()));
            throw new ResourceException(serverUri, String.format("Could not connect to FTP server at %s", serverUri), e);
        }
    }
}


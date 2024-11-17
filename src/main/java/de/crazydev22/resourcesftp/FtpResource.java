package de.crazydev22.resourcesftp;

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.ResourceExceptions;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.ExternalResourceReadResponse;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;

public class FtpResource implements ExternalResourceReadResponse {
    private final FtpClientFactory clientFactory;
    private final PasswordCredentials credentials;
    private final ExternalResourceMetaData metaData;
    private final URI uri;
    private LockableFtpClient client;

    public FtpResource(FtpClientFactory clientFactory, PasswordCredentials credentials,
                       ExternalResourceMetaData metaData, URI uri) {
        this.clientFactory = clientFactory;
        this.credentials = credentials;
        this.metaData = metaData;
        this.uri = uri;
    }

    @NotNull
    @Override
    public InputStream openStream() throws ResourceException {
        client = clientFactory.createFtpClient(uri, credentials);
        return client.run(ftpClient -> {
            try {
                PipedInputStream inputStream = new PipedInputStream();
                PipedOutputStream outputStream = new PipedOutputStream(inputStream);

                ftpClient.download(uri.getPath(), outputStream, 0, null);

                return inputStream;
            } catch (IOException | FTPIllegalReplyException | FTPException | FTPDataTransferException |
                     FTPAbortedException e) {
                throw new ResourceException(uri, "Could not download resource", e);
            }
        }, false);
    }

    public URI getURI() {
        return uri;
    }

    public boolean isLocal() {
        return false;
    }

    @NotNull
    @Override
    public ExternalResourceMetaData getMetaData() {
        return metaData;
    }

    @Override
    public void close() {
        if (client != null) {
            clientFactory.releaseFtpClient(client);
        }
    }
}


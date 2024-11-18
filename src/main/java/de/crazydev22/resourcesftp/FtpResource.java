package de.crazydev22.resourcesftp;

import it.sauronsoftware.ftp4j.*;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.resources.ResourceException;
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
    private FtpInputStream stream;

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
        stream = new FtpInputStream();
        return stream;
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
        if (stream == null) return;

        try {
            stream.close();
        } catch (IOException ignored) {}
    }

    private class FtpInputStream extends InputStream {
        private final PipedInputStream inputStream = new PipedInputStream();
        private final Runnable release;

        private FtpInputStream() {
            release = clientFactory
                    .createFtpClient(uri, credentials)
                    .runWithLock(ftpClient -> {
                try {
                    PipedOutputStream outputStream = new PipedOutputStream(inputStream);
                    ftpClient.download(uri.getPath(), outputStream, 0, new FTPDataTransferListener() {

                        @Override
                        public void started() {}

                        @Override
                        public void transferred(int length) {}

                        @Override
                        public void completed() {
                            close();
                        }

                        @Override
                        public void aborted() {
                            close();
                        }

                        @Override
                        public void failed() {
                            close();
                        }

                        private void close() {
                            try {
                                outputStream.close();
                            } catch (IOException ignored) {}
                        }
                    });
                    return null;
                } catch (IOException | FTPIllegalReplyException | FTPException | FTPDataTransferException |
                         FTPAbortedException e) {
                    throw new ResourceException(uri, "Could not download resource", e);
                }
            });

        }

        @Override
        public int read() throws IOException {
            int i = inputStream.read();
            if (i == -1) release.run();
            return i;
        }

        @Override
        public void close() throws IOException {
            try {
                inputStream.close();
            } finally {
                release.run();
            }
        }
    }
}


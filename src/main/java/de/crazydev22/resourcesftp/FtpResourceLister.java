package de.crazydev22.resourcesftp;

import it.sauronsoftware.ftp4j.*;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.transfer.ExternalResourceLister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class FtpResourceLister implements ExternalResourceLister {
    private final FtpClientFactory clientFactory;
    private final PasswordCredentials credentials;

    public FtpResourceLister(FtpClientFactory clientFactory, PasswordCredentials credentials) {
        this.clientFactory = clientFactory;
        this.credentials = credentials;
    }

    @Override
    @Nullable
    public List<String> list(@NotNull ExternalResourceName parent) throws ResourceException {
        LockableFtpClient client = clientFactory.createFtpClient(parent.getUri(), credentials);
        return client.run(ftpClient -> {
            try {
                return Arrays.stream(ftpClient.list(parent.getPath()))
                        .map(FTPFile::getName)
                        .toList();
            } catch (IOException | FTPIllegalReplyException | FTPException | FTPDataTransferException |
                     FTPAbortedException | FTPListParseException e) {
                throw new ResourceException(parent.getUri(), String.format("Could not list children for resource '%s'.", parent.getUri()), e);
            }
        }, true);
    }
}

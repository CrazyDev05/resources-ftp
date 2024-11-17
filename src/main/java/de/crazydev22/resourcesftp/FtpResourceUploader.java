package de.crazydev22.resourcesftp;

import it.sauronsoftware.ftp4j.*;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ReadableContent;
import org.gradle.internal.resource.ResourceExceptions;
import org.gradle.internal.resource.transfer.ExternalResourceUploader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;

public class FtpResourceUploader implements ExternalResourceUploader {
    private final FtpClientFactory clientFactory;
    private final PasswordCredentials credentials;

    public FtpResourceUploader(FtpClientFactory clientFactory, PasswordCredentials credentials) {
        this.clientFactory = clientFactory;
        this.credentials = credentials;
    }

    @Override
    public void upload(@NotNull ReadableContent resource, ExternalResourceName destination) {
        LockableFtpClient client = clientFactory.createFtpClient(destination.getUri(), credentials);

        client.run(ftpClient -> {
            try {
                ensureParentDirectoryExists(ftpClient, destination.getUri());

                try (var stream = resource.open()) {
                    ftpClient.upload(destination.getPath(), stream, 0, 0, null);
                }
                return null;
            } catch (FTPIllegalReplyException | FTPAbortedException | FTPDataTransferException | IOException |
                     FTPException e) {
                throw ResourceExceptions.putFailed(destination.getUri(), e);
            }
        }, true);
    }

    private void ensureParentDirectoryExists(FTPClient channel, URI uri) throws ResourceException {
        String parentPath = FilenameUtils.getFullPathNoEndSeparator(uri.getPath());
        if (parentPath.equals("/")) return;
        URI parent = uri.resolve(parentPath);

        try {
            FTPFile[] files = channel.list(parentPath);
            if (files.length > 0)
                return;
        } catch (FTPIllegalReplyException | FTPAbortedException | FTPDataTransferException | FTPListParseException |
                 FTPException | IOException e) {
            if (e instanceof FTPException ex && ex.getCode() == 450)
                return;

            throw new ResourceException(parent, String.format("Could not mlist resource '%s'.", parent), e);
        }

        ensureParentDirectoryExists(channel, parent);
        try {
            channel.createDirectory(parentPath);
        } catch (IOException | FTPIllegalReplyException | FTPException e) {
            throw new ResourceException(parent, String.format("Could not create directory '%s'.", parentPath), e);
        }
    }
}


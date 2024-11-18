package de.crazydev22.resourcesftp;

import it.sauronsoftware.ftp4j.*;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.resources.ResourceException;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.AbstractExternalResourceAccessor;
import org.gradle.internal.resource.transfer.ExternalResourceAccessor;
import org.gradle.internal.resource.transfer.ExternalResourceReadResponse;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class FtpResourceAccessor extends AbstractExternalResourceAccessor implements ExternalResourceAccessor {

    private final FtpClientFactory clientFactory;
    private final PasswordCredentials credentials;

    public FtpResourceAccessor(FtpClientFactory clientFactory, PasswordCredentials credentials) {
        this.clientFactory = clientFactory;
        this.credentials = credentials;
    }

    @Override
    public ExternalResourceMetaData getMetaData(ExternalResourceName location, boolean revalidate) {
        LockableFtpClient client = clientFactory.createFtpClient(location.getUri(), credentials);

        return client.run(ftpClient -> {
            try {
                FTPFile[] files = ftpClient.list(location.getPath());
                if (files.length == 0) return null;
                return new DefaultExternalResourceMetaData(location.getUri(), files[0].getModifiedDate().getTime(), files[0].getSize());
            } catch (FTPIllegalReplyException | FTPAbortedException | FTPDataTransferException | IOException |
                     FTPListParseException | FTPException e) {
                if (e instanceof FTPException ex && (ex.getCode() == 450 || ex.getCode() == 550))
                    return null;
                throw new ResourceException(location.getUri(), "Could not get metadata for resource", e);
            }
        });
    }

    @Override
    public ExternalResourceReadResponse openResource(@NotNull ExternalResourceName location, boolean revalidate) {
        ExternalResourceMetaData metaData = getMetaData(location, revalidate);
        if (metaData == null) {
            return null;
        }
        return new FtpResource(clientFactory, credentials, metaData, location.getUri());
    }
}


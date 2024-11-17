package de.crazydev22.resourcesftp;

import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.authentication.Authentication;
import org.gradle.internal.authentication.AllSchemesAuthentication;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.resource.connector.ResourceConnectorSpecification;
import org.gradle.internal.resource.transfer.DefaultExternalResourceConnector;
import org.gradle.internal.resource.transfer.ExternalResourceConnector;

import java.util.HashSet;
import java.util.Set;

public class FtpConnectorFactory implements ResourceConnectorFactory {
    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("ftp", "ftps", "ftpes");
    private final FtpClientFactory clientFactory;

    public FtpConnectorFactory(FtpClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Set<String> getSupportedProtocols() {
        return SUPPORTED_PROTOCOLS;
    }

    @Override
    public Set<Class<? extends Authentication>> getSupportedAuthentication() {
        Set<Class<? extends Authentication>> authentications = new HashSet<>();
        authentications.add(AllSchemesAuthentication.class);
        return authentications;
    }

    @Override
    public ExternalResourceConnector createResourceConnector(ResourceConnectorSpecification connectionDetails) {
        PasswordCredentials credentials = connectionDetails.getCredentials(PasswordCredentials.class);
        FtpResourceAccessor accessor = new FtpResourceAccessor(clientFactory, credentials);
        FtpResourceLister lister = new FtpResourceLister(clientFactory, credentials);
        FtpResourceUploader uploader = new FtpResourceUploader(clientFactory, credentials);

        return new DefaultExternalResourceConnector(accessor, lister, uploader);
    }
}


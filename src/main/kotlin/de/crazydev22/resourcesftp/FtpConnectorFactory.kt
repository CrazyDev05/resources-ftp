package de.crazydev22.resourcesftp

import org.gradle.api.credentials.PasswordCredentials
import org.gradle.authentication.Authentication
import org.gradle.internal.authentication.AllSchemesAuthentication
import org.gradle.internal.resource.connector.ResourceConnectorFactory
import org.gradle.internal.resource.connector.ResourceConnectorSpecification
import org.gradle.internal.resource.transfer.DefaultExternalResourceConnector
import org.gradle.internal.resource.transfer.ExternalResourceConnector

class FtpConnectorFactory(private val clientFactory: FtpClientFactory) : ResourceConnectorFactory {
    override fun getSupportedProtocols(): Set<String> {
        return setOf("ftp", "ftps")
    }

    override fun getSupportedAuthentication(): Set<Class<out Authentication?>> {
        return setOf(AllSchemesAuthentication::class.java)
    }

    override fun createResourceConnector(connectionDetails: ResourceConnectorSpecification): ExternalResourceConnector {
        val credentials = connectionDetails.getCredentials(PasswordCredentials::class.java)
        val accessor = FtpResourceAccessor(clientFactory, credentials)
        val lister = FtpResourceLister(clientFactory, credentials)
        val uploader = FtpResourceUploader(clientFactory, credentials)

        return DefaultExternalResourceConnector(accessor, lister, uploader)
    }
}

package de.crazydev22.resourcesftp

import org.apache.commons.net.ftp.FTPFile
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.internal.resource.ExternalResourceName
import org.gradle.internal.resource.metadata.DefaultExternalResourceMetaData
import org.gradle.internal.resource.metadata.ExternalResourceMetaData
import org.gradle.internal.resource.transfer.AbstractExternalResourceAccessor
import org.gradle.internal.resource.transfer.ExternalResourceAccessor
import org.gradle.internal.resource.transfer.ExternalResourceReadResponse

class FtpResourceAccessor constructor(
    private val clientFactory: FtpClientFactory,
    private val credentials: PasswordCredentials?
): AbstractExternalResourceAccessor(), ExternalResourceAccessor {

    override fun getMetaData(location: ExternalResourceName, revalidate: Boolean): ExternalResourceMetaData? {
        val client = clientFactory.createFtpClient(location.uri, credentials)

        try {
            return client.run {
                val file: FTPFile = it.mlistFile(location.path) ?: return@run null
                return@run DefaultExternalResourceMetaData(location.uri, file.timestampInstant?.toEpochMilli() ?: -1, file.size)
            }
        } finally {
            clientFactory.releaseFtpClient(client)
        }
    }

    override fun openResource(location: ExternalResourceName, revalidate: Boolean): ExternalResourceReadResponse? {
        val metaData = getMetaData(location, revalidate) ?: return null
        return FtpResource(clientFactory, credentials, metaData, location.uri)
    }
}

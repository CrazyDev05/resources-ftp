package de.crazydev22.resourcesftp

import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.resources.ResourceException
import org.gradle.internal.resource.ResourceExceptions
import org.gradle.internal.resource.metadata.ExternalResourceMetaData
import org.gradle.internal.resource.transfer.ExternalResourceReadResponse
import java.io.IOException
import java.io.InputStream
import java.net.URI
import kotlin.jvm.Throws

class FtpResource constructor(
    private val clientFactory: FtpClientFactory,
    private val credentials: PasswordCredentials?,
    private val metaData: ExternalResourceMetaData,
    private val uri: URI
) : ExternalResourceReadResponse {
    private var client: LockableFtpClient? = null

    @Throws(ResourceException::class)
    override fun openStream(): InputStream {
        client = clientFactory.createFtpClient(uri, credentials)
        try {
            return client!!.run { it.retrieveFileStream(uri.path) }
        } catch (e: IOException) {
            throw ResourceExceptions.getFailed(uri, e)
        }
    }

    fun getURI(): URI {
        return uri
    }

    fun isLocal(): Boolean {
        return false
    }

    override fun getMetaData(): ExternalResourceMetaData {
        return metaData
    }

    override fun close() {
        client?.let { clientFactory.releaseFtpClient(it) }
    }
}
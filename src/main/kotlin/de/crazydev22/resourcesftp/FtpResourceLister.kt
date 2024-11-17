package de.crazydev22.resourcesftp

import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.resources.ResourceException
import org.gradle.internal.resource.ExternalResourceName
import org.gradle.internal.resource.transfer.ExternalResourceLister
import java.io.IOException


class FtpResourceLister constructor(
    private val clientFactory: FtpClientFactory,
    private val credentials: PasswordCredentials?
) : ExternalResourceLister {

    override fun list(directory: ExternalResourceName): MutableList<String>? {
        val client = clientFactory.createFtpClient(directory.uri, credentials)

        try {
            return client.run {
                val files = it.listNames(directory.path) ?: return@run null
                return@run mutableListOf(*files)
            }
        } catch (e: IOException) {
            throw ResourceException(directory.uri, String.format("Could not list children for resource '%s'.", directory.uri), e)
        } finally {
            clientFactory.releaseFtpClient(client)
        }
    }
}
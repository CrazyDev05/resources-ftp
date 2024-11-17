package de.crazydev22.resourcesftp

import org.apache.commons.io.FilenameUtils
import org.apache.commons.net.ftp.FTPClient
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.resources.ResourceException
import org.gradle.internal.resource.ExternalResourceName
import org.gradle.internal.resource.ReadableContent
import org.gradle.internal.resource.ResourceExceptions
import org.gradle.internal.resource.transfer.ExternalResourceUploader
import java.io.IOException
import java.net.URI


class FtpResourceUploader constructor(
    private val clientFactory: FtpClientFactory,
    private val credentials: PasswordCredentials?
) : ExternalResourceUploader {

    override fun upload(resource: ReadableContent, destination: ExternalResourceName) {
        val client = clientFactory.createFtpClient(destination.uri, credentials)

        try {
            client.run {
                ensureParentDirectoryExists(it, destination.uri)

                resource.open().use { stream ->
                    it.storeFile(destination.path, stream)
                }
            }
        } catch (e: IOException) {
            throw ResourceExceptions.putFailed(destination.uri, e)
        } finally {
            clientFactory.releaseFtpClient(client)
        }
    }

    private fun ensureParentDirectoryExists(channel: FTPClient, uri: URI) {
        val parentPath = FilenameUtils.getFullPathNoEndSeparator(uri.path)
        if (parentPath == "/") return
        val parent = uri.resolve(parentPath)

        try {
            if (channel.mlistFile(parentPath) != null)
                return
        } catch (e: IOException) {
            throw ResourceException(parent, String.format("Could not mlist resource '%s'.", parent), e)
        }

        ensureParentDirectoryExists(channel, parent)
        try {
            if (!channel.makeDirectory(parentPath))
                throw ResourceException(parent, String.format("Could not create directory '%s'.", parent))
        } catch (e: IOException) {
            throw ResourceException(parent, String.format("Could not create resource '%s'.", parent), e)
        }
    }
}

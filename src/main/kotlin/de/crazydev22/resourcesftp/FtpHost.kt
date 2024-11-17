package de.crazydev22.resourcesftp

import org.gradle.api.credentials.PasswordCredentials
import java.net.URI
import java.util.*

class FtpHost(uri: URI, credentials: PasswordCredentials?) {
    val hostname: String = uri.host
    val port: Int = uri.port
    val username: String? = credentials?.username
    val password: String? = credentials?.password
    val ssl: Boolean = uri.scheme == "ftps"

    fun shouldLogin(): Boolean {
        return username != null && password != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val ftpHost = other as FtpHost
        return port == ftpHost.port && hostname == ftpHost.hostname && username == ftpHost.username && password == ftpHost.password
    }

    override fun hashCode(): Int {
        return Objects.hash(hostname, port, username, password)
    }

    override fun toString(): String {
        return String.format("%s:%d (Username: %s)", hostname, port, username)
    }
}

package de.crazydev22.resourcesftp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.resource.connector.ResourceConnectorFactory
import java.lang.IllegalStateException

@Suppress("UNCHECKED_CAST")
class ResourcePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        try {
            val factory = (target as ProjectInternal).services.get(RepositoryTransportFactory::class.java)
            val field = factory.javaClass.getDeclaredField("registeredProtocols")
            field.isAccessible = true
            val protocols = field.get(factory) as ArrayList<ResourceConnectorFactory>
            protocols.add(FtpConnectorFactory(FtpClientFactory()))
        } catch (e: Throwable) {
            throw IllegalStateException("Could not register FtpResourceConnector", e)
        }
    }
}

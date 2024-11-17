package de.crazydev22.resourcesftp;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ResourcePlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        try {
            RepositoryTransportFactory factory = ((ProjectInternal) target).getServices().get(RepositoryTransportFactory.class);
            Field field = factory.getClass().getDeclaredField("registeredProtocols");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ArrayList<ResourceConnectorFactory> protocols = (ArrayList<ResourceConnectorFactory>) field.get(factory);
            protocols.add(new FtpConnectorFactory(new FtpClientFactory()));
        } catch (Throwable e) {
            throw new IllegalStateException("Could not register FtpResourceConnector", e);
        }
    }
}


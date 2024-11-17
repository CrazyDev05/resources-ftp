package de.crazydev22.resourcesftp;

import org.gradle.api.credentials.PasswordCredentials;
import java.net.URI;
import java.util.Objects;

public class FtpHost {
    private final String hostname;
    private final int port;
    private final String username;
    private final String password;
    private final Security security;

    public FtpHost(URI uri, PasswordCredentials credentials) {
        this.hostname = uri.getHost();
        this.port = uri.getPort();
        this.username = credentials != null ? credentials.getUsername() : null;
        this.password = credentials != null ? credentials.getPassword() : null;
        this.security = Security.fromURI(uri);
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Security getSecurity() {
        return security;
    }

    public boolean shouldLogin() {
        return username != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FtpHost ftpHost = (FtpHost) o;
        return port == ftpHost.port &&
                Objects.equals(hostname, ftpHost.hostname) &&
                Objects.equals(username, ftpHost.username) &&
                Objects.equals(password, ftpHost.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port, username, password);
    }

    @Override
    public String toString() {
        return String.format("%s:%d (Username: %s)", hostname, port, username);
    }
}


package de.crazydev22.resourcesftp;

import java.net.URI;

public enum Security {
    FTP,
    FTPS,
    FTPES;

    public static Security fromURI(URI uri) {
        try {
            return valueOf(uri.getScheme().toUpperCase());
        } catch (Exception e) {
            return FTP;
        }
    }
}

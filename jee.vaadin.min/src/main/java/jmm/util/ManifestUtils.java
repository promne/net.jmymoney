package jmm.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

import jee.vaadin.min.OneUI;

public final class ManifestUtils {

    private static Manifest manifest;
    
    public static Manifest getManifest() {
        if (manifest == null) {
            Enumeration<URL> resources;
            try {
                resources = ManifestUtils.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
                while (resources.hasMoreElements()) {
                    Manifest manifest = new Manifest(resources.nextElement().openStream());
                    //TODO: replace with better matching
                    if (OneUI.class.getPackage().getName().equals(manifest.getMainAttributes().getValue("Implementation-Title"))) {
                        ManifestUtils.manifest = manifest;
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }            
        }
        return ManifestUtils.manifest;
    }
    
    public static String getVersion() {
        Manifest manifest = getManifest();
        String version = null;
        if (manifest != null) {
            return manifest.getMainAttributes().getValue("Implementation-Version");
        }
        return version;
    }
}

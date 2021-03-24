package com.ainq.fhir.saner.sampledata.classpath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.ainq.fhir.saner.sampledata.Loader;

/** A {@link URLStreamHandler} that handles resources on the classpath. */
public class Handler extends URLStreamHandler {
    /** The classloader to find resources from. */
    private final ClassLoader classLoader;

    public Handler() {
        this.classLoader = getClass().getClassLoader();
    }

    public Handler(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        // synthetic-data.zip
        URL resourceUrl = classLoader.getResource(u.getPath());
        return resourceUrl.openConnection();
    }
}
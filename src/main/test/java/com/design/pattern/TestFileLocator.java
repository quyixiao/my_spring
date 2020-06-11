package com.design.pattern;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.net.URL;

public class TestFileLocator {

    private static Method equinoxResolveMethod;

    static {
        try {
            // Detect Equinox OSGi (e.g. on WebSphere 6.1)
            Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
                    PathMatchingResourcePatternResolver.class.getClassLoader());
            equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
        } catch (Throwable ex) {
            equinoxResolveMethod = null;
        }
    }






}

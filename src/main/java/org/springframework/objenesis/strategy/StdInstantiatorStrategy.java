//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.strategy;

import java.io.Serializable;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.instantiator.android1.Android10Instantiator;
import org.springframework.objenesis.instantiator.android1.Android17Instantiator;
import org.springframework.objenesis.instantiator.android1.Android18Instantiator;
import org.springframework.objenesis.instantiator.basic.AccessibleInstantiator;
import org.springframework.objenesis.instantiator.basic.ObjectInputStreamInstantiator;
import org.springframework.objenesis.instantiator.gcj.GCJInstantiator;
import org.springframework.objenesis.instantiator.jrockit.JRockitLegacyInstantiator;
import org.springframework.objenesis.instantiator.perc.PercInstantiator;
import org.springframework.objenesis.instantiator.sun.SunReflectionFactoryInstantiator;
import org.springframework.objenesis.instantiator.sun.UnsafeFactoryInstantiator;

public class StdInstantiatorStrategy extends BaseInstantiatorStrategy {
    public StdInstantiatorStrategy() {
    }

    public <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> type) {
        if (!PlatformDescription.isThisJVM("Java HotSpot") && !PlatformDescription.isThisJVM("OpenJDK")) {
            if (!PlatformDescription.isThisJVM("BEA")) {
                if (PlatformDescription.isThisJVM("Dalvik")) {
                    if (PlatformDescription.ANDROID_VERSION <= 10) {
                        return new Android10Instantiator(type);
                    } else {
                        return (ObjectInstantiator)(PlatformDescription.ANDROID_VERSION <= 17 ? new Android17Instantiator(type) : new Android18Instantiator(type));
                    }
                } else if (PlatformDescription.isThisJVM("GNU libgcj")) {
                    return new GCJInstantiator(type);
                } else {
                    return (ObjectInstantiator)(PlatformDescription.isThisJVM("PERC") ? new PercInstantiator(type) : new UnsafeFactoryInstantiator(type));
                }
            } else {
                return (ObjectInstantiator)(!PlatformDescription.VM_VERSION.startsWith("1.4") || PlatformDescription.VENDOR_VERSION.startsWith("R") || PlatformDescription.VM_INFO != null && PlatformDescription.VM_INFO.startsWith("R25.1") && PlatformDescription.VM_INFO.startsWith("R25.2") ? new SunReflectionFactoryInstantiator(type) : new JRockitLegacyInstantiator(type));
            }
        } else if (PlatformDescription.isGoogleAppEngine()) {
            return (ObjectInstantiator)(Serializable.class.isAssignableFrom(type) ? new ObjectInputStreamInstantiator(type) : new AccessibleInstantiator(type));
        } else {
            return new SunReflectionFactoryInstantiator(type);
        }
    }
}

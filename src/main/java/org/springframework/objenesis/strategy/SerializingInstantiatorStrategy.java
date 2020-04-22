//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.strategy;

import java.io.NotSerializableException;
import java.io.Serializable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.instantiator.android1.AndroidSerializationInstantiator;
import org.springframework.objenesis.instantiator.basic.ObjectInputStreamInstantiator;
import org.springframework.objenesis.instantiator.basic.ObjectStreamClassInstantiator;
import org.springframework.objenesis.instantiator.gcj.GCJSerializationInstantiator;
import org.springframework.objenesis.instantiator.perc.PercSerializationInstantiator;

public class SerializingInstantiatorStrategy extends BaseInstantiatorStrategy {
    public SerializingInstantiatorStrategy() {
    }

    public <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> type) {
        if (!Serializable.class.isAssignableFrom(type)) {
            throw new ObjenesisException(new NotSerializableException(type + " not serializable"));
        } else if (!PlatformDescription.JVM_NAME.startsWith("Java HotSpot") && !PlatformDescription.isThisJVM("OpenJDK")) {
            if (PlatformDescription.JVM_NAME.startsWith("Dalvik")) {
                return new AndroidSerializationInstantiator(type);
            } else if (PlatformDescription.JVM_NAME.startsWith("GNU libgcj")) {
                return new GCJSerializationInstantiator(type);
            } else {
                return (ObjectInstantiator)(PlatformDescription.JVM_NAME.startsWith("PERC") ? new PercSerializationInstantiator(type) : new ObjectStreamClassInstantiator(type));
            }
        } else {
            return (ObjectInstantiator)(PlatformDescription.isGoogleAppEngine() ? new ObjectInputStreamInstantiator(type) : new ObjectStreamClassInstantiator(type));
        }
    }
}

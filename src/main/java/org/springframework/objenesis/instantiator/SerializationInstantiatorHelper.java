//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator;

import java.io.Serializable;

public class SerializationInstantiatorHelper {
    public SerializationInstantiatorHelper() {
    }

    public static <T> Class<? super T> getNonSerializableSuperClass(Class<T> type) {
        Class result = type;

        do {
            if (!Serializable.class.isAssignableFrom(result)) {
                return result;
            }

            result = result.getSuperclass();
        } while(result != null);

        throw new Error("Bad class hierarchy: No non-serializable parents");
    }
}

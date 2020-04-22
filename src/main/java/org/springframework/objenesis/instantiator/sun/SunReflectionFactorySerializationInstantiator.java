//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.sun;

import java.io.NotSerializableException;
import java.lang.reflect.Constructor;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.instantiator.SerializationInstantiatorHelper;

public class SunReflectionFactorySerializationInstantiator<T> implements ObjectInstantiator<T> {
    private final Constructor<T> mungedConstructor;

    public SunReflectionFactorySerializationInstantiator(Class<T> type) {
        Class nonSerializableAncestor = SerializationInstantiatorHelper.getNonSerializableSuperClass(type);

        Constructor nonSerializableAncestorConstructor;
        try {
            nonSerializableAncestorConstructor = nonSerializableAncestor.getConstructor((Class[])null);
        } catch (NoSuchMethodException var5) {
            throw new ObjenesisException(new NotSerializableException(type + " has no suitable superclass constructor"));
        }

        this.mungedConstructor = SunReflectionFactoryHelper.newConstructorForSerialization(type, nonSerializableAncestorConstructor);
        this.mungedConstructor.setAccessible(true);
    }

    public T newInstance() {
        try {
            return this.mungedConstructor.newInstance((Object[])null);
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }
}

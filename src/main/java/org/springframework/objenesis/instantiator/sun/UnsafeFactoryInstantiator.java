//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.sun;

import java.lang.reflect.Field;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import sun.misc.Unsafe;

public class UnsafeFactoryInstantiator<T> implements ObjectInstantiator<T> {
    private static Unsafe unsafe;
    private final Class<T> type;

    public UnsafeFactoryInstantiator(Class<T> type) {
        if (unsafe == null) {
            Field f;
            try {
                f = Unsafe.class.getDeclaredField("theUnsafe");
            } catch (NoSuchFieldException var5) {
                throw new ObjenesisException(var5);
            }

            f.setAccessible(true);

            try {
                unsafe = (Unsafe)f.get((Object)null);
            } catch (IllegalAccessException var4) {
                throw new ObjenesisException(var4);
            }
        }

        this.type = type;
    }

    public T newInstance() {
        try {
            return this.type.cast(unsafe.allocateInstance(this.type));
        } catch (InstantiationException var2) {
            throw new ObjenesisException(var2);
        }
    }
}

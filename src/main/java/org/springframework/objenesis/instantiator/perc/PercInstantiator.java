//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.perc;

import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class PercInstantiator<T> implements ObjectInstantiator<T> {
    private final Method newInstanceMethod;
    private final Object[] typeArgs;

    public PercInstantiator(Class<T> type) {
        this.typeArgs = new Object[]{null, Boolean.FALSE};
        this.typeArgs[0] = type;

        try {
            this.newInstanceMethod = ObjectInputStream.class.getDeclaredMethod("newInstance", Class.class, Boolean.TYPE);
            this.newInstanceMethod.setAccessible(true);
        } catch (RuntimeException var3) {
            throw new ObjenesisException(var3);
        } catch (NoSuchMethodException var4) {
            throw new ObjenesisException(var4);
        }
    }

    public T newInstance() {
        try {
            return (T)this.newInstanceMethod.invoke((Object)null, this.typeArgs);
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }
}

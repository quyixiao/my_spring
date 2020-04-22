//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.android1;

import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class Android10Instantiator<T> implements ObjectInstantiator<T> {
    private final Class<T> type;
    private final Method newStaticMethod;

    public Android10Instantiator(Class<T> type) {
        this.type = type;
        this.newStaticMethod = getNewStaticMethod();
    }

    public T newInstance() {
        try {
            return this.type.cast(this.newStaticMethod.invoke((Object)null, this.type, Object.class));
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }

    private static Method getNewStaticMethod() {
        try {
            Method newStaticMethod = ObjectInputStream.class.getDeclaredMethod("newInstance", Class.class, Class.class);
            newStaticMethod.setAccessible(true);
            return newStaticMethod;
        } catch (RuntimeException var1) {
            throw new ObjenesisException(var1);
        } catch (NoSuchMethodException var2) {
            throw new ObjenesisException(var2);
        }
    }
}

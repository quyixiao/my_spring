//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.jrockit;

import java.lang.reflect.Method;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class JRockitLegacyInstantiator<T> implements ObjectInstantiator<T> {
    private static Method safeAllocObjectMethod = null;
    private final Class<T> type;

    private static void initialize() {
        if (safeAllocObjectMethod == null) {
            try {
                Class<?> memSystem = Class.forName("jrockit.vm.MemSystem");
                safeAllocObjectMethod = memSystem.getDeclaredMethod("safeAllocObject", Class.class);
                safeAllocObjectMethod.setAccessible(true);
            } catch (RuntimeException var2) {
                throw new ObjenesisException(var2);
            } catch (ClassNotFoundException var3) {
                throw new ObjenesisException(var3);
            } catch (NoSuchMethodException var4) {
                throw new ObjenesisException(var4);
            }
        }

    }

    public JRockitLegacyInstantiator(Class<T> type) {
        initialize();
        this.type = type;
    }

    public T newInstance() {
        try {
            return this.type.cast(safeAllocObjectMethod.invoke((Object)null, this.type));
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }
}

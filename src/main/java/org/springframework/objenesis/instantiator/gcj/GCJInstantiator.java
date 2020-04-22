//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.gcj;

import java.lang.reflect.InvocationTargetException;
import org.springframework.objenesis.ObjenesisException;

public class GCJInstantiator<T> extends GCJInstantiatorBase<T> {
    public GCJInstantiator(Class<T> type) {
        super(type);
    }

    public T newInstance() {
        try {
            return this.type.cast(newObjectMethod.invoke(dummyStream, this.type, Object.class));
        } catch (RuntimeException var2) {
            throw new ObjenesisException(var2);
        } catch (IllegalAccessException var3) {
            throw new ObjenesisException(var3);
        } catch (InvocationTargetException var4) {
            throw new ObjenesisException(var4);
        }
    }
}

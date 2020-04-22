//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.basic;

import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class NewInstanceInstantiator<T> implements ObjectInstantiator<T> {
    private final Class<T> type;

    public NewInstanceInstantiator(Class<T> type) {
        this.type = type;
    }

    public T newInstance() {
        try {
            return this.type.newInstance();
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.basic;

import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class FailingInstantiator<T> implements ObjectInstantiator<T> {
    public FailingInstantiator(Class<T> type) {
    }

    public T newInstance() {
        throw new ObjenesisException("Always failing");
    }
}

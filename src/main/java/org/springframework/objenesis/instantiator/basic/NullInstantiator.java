//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.basic;

import org.springframework.objenesis.instantiator.ObjectInstantiator;

public class NullInstantiator<T> implements ObjectInstantiator<T> {
    public NullInstantiator(Class<T> type) {
    }

    public T newInstance() {
        return null;
    }
}

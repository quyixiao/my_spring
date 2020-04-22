//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis;

import org.springframework.objenesis.instantiator.ObjectInstantiator;

public interface Objenesis {
    <T> T newInstance(Class<T> var1);

    <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> var1);
}

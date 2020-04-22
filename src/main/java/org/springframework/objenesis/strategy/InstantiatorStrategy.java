//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.strategy;

import org.springframework.objenesis.instantiator.ObjectInstantiator;

public interface InstantiatorStrategy {
    <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> var1);
}

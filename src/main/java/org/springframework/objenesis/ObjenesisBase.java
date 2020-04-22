//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.strategy.InstantiatorStrategy;

public class ObjenesisBase implements Objenesis {
    protected final InstantiatorStrategy strategy;
    protected ConcurrentHashMap<String, ObjectInstantiator<?>> cache;

    public ObjenesisBase(InstantiatorStrategy strategy) {
        this(strategy, true);
    }

    public ObjenesisBase(InstantiatorStrategy strategy, boolean useCache) {
        if (strategy == null) {
            throw new IllegalArgumentException("A strategy can't be null");
        } else {
            this.strategy = strategy;
            this.cache = useCache ? new ConcurrentHashMap() : null;
        }
    }

    public String toString() {
        return this.getClass().getName() + " using " + this.strategy.getClass().getName() + (this.cache == null ? " without" : " with") + " caching";
    }

    public <T> T newInstance(Class<T> clazz) {
        return this.getInstantiatorOf(clazz).newInstance();
    }

    public <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> clazz) {
        if (this.cache == null) {
            return this.strategy.newInstantiatorOf(clazz);
        } else {
            ObjectInstantiator<T> instantiator = (ObjectInstantiator)this.cache.get(clazz.getName());
            if (instantiator == null) {
                ObjectInstantiator<T> newInstantiator = this.strategy.newInstantiatorOf(clazz);
                instantiator = (ObjectInstantiator)this.cache.putIfAbsent(clazz.getName(), newInstantiator);
                if (instantiator == null) {
                    instantiator = newInstantiator;
                }
            }

            return instantiator;
        }
    }
}

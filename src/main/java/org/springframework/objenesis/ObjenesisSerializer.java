//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis;

import org.springframework.objenesis.strategy.SerializingInstantiatorStrategy;

public class ObjenesisSerializer extends ObjenesisBase {
    public ObjenesisSerializer() {
        super(new SerializingInstantiatorStrategy());
    }

    public ObjenesisSerializer(boolean useCache) {
        super(new SerializingInstantiatorStrategy(), useCache);
    }
}

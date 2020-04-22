//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.gcj;

import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.instantiator.SerializationInstantiatorHelper;

public class GCJSerializationInstantiator<T> extends GCJInstantiatorBase<T> {
    private Class<? super T> superType;

    public GCJSerializationInstantiator(Class<T> type) {
        super(type);
        this.superType = SerializationInstantiatorHelper.getNonSerializableSuperClass(type);
    }

    public T newInstance() {
        try {
            return this.type.cast(newObjectMethod.invoke(dummyStream, this.type, this.superType));
        } catch (Exception var2) {
            throw new ObjenesisException(var2);
        }
    }
}

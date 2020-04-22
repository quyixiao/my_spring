//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.sun;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.springframework.objenesis.ObjenesisException;

class SunReflectionFactoryHelper {
    SunReflectionFactoryHelper() {
    }

    public static <T> Constructor<T> newConstructorForSerialization(Class<T> type, Constructor<?> constructor) {
        Class<?> reflectionFactoryClass = getReflectionFactoryClass();
        Object reflectionFactory = createReflectionFactory(reflectionFactoryClass);
        Method newConstructorForSerializationMethod = getNewConstructorForSerializationMethod(reflectionFactoryClass);

        try {
            return (Constructor)newConstructorForSerializationMethod.invoke(reflectionFactory, type, constructor);
        } catch (IllegalArgumentException var6) {
            throw new ObjenesisException(var6);
        } catch (IllegalAccessException var7) {
            throw new ObjenesisException(var7);
        } catch (InvocationTargetException var8) {
            throw new ObjenesisException(var8);
        }
    }

    private static Class<?> getReflectionFactoryClass() {
        try {
            return Class.forName("sun.reflect.ReflectionFactory");
        } catch (ClassNotFoundException var1) {
            throw new ObjenesisException(var1);
        }
    }

    private static Object createReflectionFactory(Class<?> reflectionFactoryClass) {
        try {
            Method method = reflectionFactoryClass.getDeclaredMethod("getReflectionFactory");
            return method.invoke((Object)null);
        } catch (NoSuchMethodException var2) {
            throw new ObjenesisException(var2);
        } catch (IllegalAccessException var3) {
            throw new ObjenesisException(var3);
        } catch (IllegalArgumentException var4) {
            throw new ObjenesisException(var4);
        } catch (InvocationTargetException var5) {
            throw new ObjenesisException(var5);
        }
    }

    private static Method getNewConstructorForSerializationMethod(Class<?> reflectionFactoryClass) {
        try {
            return reflectionFactoryClass.getDeclaredMethod("newConstructorForSerialization", Class.class, Constructor.class);
        } catch (NoSuchMethodException var2) {
            throw new ObjenesisException(var2);
        }
    }
}

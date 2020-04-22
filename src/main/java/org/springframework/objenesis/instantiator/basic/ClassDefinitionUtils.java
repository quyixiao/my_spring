//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.springframework.objenesis.instantiator.basic;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.objenesis.ObjenesisException;

public final class ClassDefinitionUtils {
    public static final byte OPS_aload_0 = 42;
    public static final byte OPS_invokespecial = -73;
    public static final byte OPS_return = -79;
    public static final byte OPS_new = -69;
    public static final byte OPS_dup = 89;
    public static final byte OPS_areturn = -80;
    public static final int CONSTANT_Utf8 = 1;
    public static final int CONSTANT_Integer = 3;
    public static final int CONSTANT_Float = 4;
    public static final int CONSTANT_Long = 5;
    public static final int CONSTANT_Double = 6;
    public static final int CONSTANT_Class = 7;
    public static final int CONSTANT_String = 8;
    public static final int CONSTANT_Fieldref = 9;
    public static final int CONSTANT_Methodref = 10;
    public static final int CONSTANT_InterfaceMethodref = 11;
    public static final int CONSTANT_NameAndType = 12;
    public static final int CONSTANT_MethodHandle = 15;
    public static final int CONSTANT_MethodType = 16;
    public static final int CONSTANT_InvokeDynamic = 18;
    public static final int ACC_PUBLIC = 1;
    public static final int ACC_FINAL = 16;
    public static final int ACC_SUPER = 32;
    public static final int ACC_INTERFACE = 512;
    public static final int ACC_ABSTRACT = 1024;
    public static final int ACC_SYNTHETIC = 4096;
    public static final int ACC_ANNOTATION = 8192;
    public static final int ACC_ENUM = 16384;
    public static final byte[] MAGIC = new byte[]{-54, -2, -70, -66};
    public static final byte[] VERSION = new byte[]{0, 0, 0, 49};
    private static Method DEFINE_CLASS;
    private static final ProtectionDomain PROTECTION_DOMAIN = (ProtectionDomain)AccessController.doPrivileged(new PrivilegedAction<ProtectionDomain>() {
        public ProtectionDomain run() {
            return ClassDefinitionUtils.class.getProtectionDomain();
        }
    });

    private ClassDefinitionUtils() {
    }

    public static <T> Class<T> defineClass(String className, byte[] b, ClassLoader loader) throws Exception {
        Object[] args = new Object[]{className, b, new Integer(0), new Integer(b.length), PROTECTION_DOMAIN};
        Class<T> c = (Class)DEFINE_CLASS.invoke(loader, args);
        Class.forName(className, true, loader);
        return c;
    }

    public static byte[] readClass(String className) throws IOException {
        className = classNameToResource(className);
        byte[] b = new byte[2500];
        InputStream in = ClassDefinitionUtils.class.getClassLoader().getResourceAsStream(className);

        int length;
        try {
            length = in.read(b);
        } finally {
            in.close();
        }

        if (length >= 2500) {
            throw new IllegalArgumentException("The class is longer that 2500 bytes which is currently unsupported");
        } else {
            byte[] copy = new byte[length];
            System.arraycopy(b, 0, copy, 0, length);
            return copy;
        }
    }

    public static void writeClass(String fileName, byte[] bytes) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));

        try {
            out.write(bytes);
        } finally {
            out.close();
        }

    }

    public static String classNameToInternalClassName(String className) {
        return className.replace('.', '/');
    }

    public static String classNameToResource(String className) {
        return classNameToInternalClassName(className) + ".class";
    }

    public static <T> Class<T> getExistingClass(ClassLoader classLoader, String className) {
        try {
            return (Class<T>)Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException var3) {
            return null;
        }
    }

    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                try {
                    Class<?> loader = Class.forName("java.lang.ClassLoader");
                    ClassDefinitionUtils.DEFINE_CLASS = loader.getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE, ProtectionDomain.class);
                    ClassDefinitionUtils.DEFINE_CLASS.setAccessible(true);
                    return null;
                } catch (ClassNotFoundException var2) {
                    throw new ObjenesisException(var2);
                } catch (NoSuchMethodException var3) {
                    throw new ObjenesisException(var3);
                }
            }
        });
    }
}

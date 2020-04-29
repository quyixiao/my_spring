package com.design.pattern.proxy.dynamic1;

import java.lang.reflect.Method;

public class $Proxy0 implements com.design.pattern.proxy.dynamic1.Student {
    GPInvocationHandler h;

    public $Proxy0(GPInvocationHandler h) {
        this.h = h;
    }

    public int learn(java.lang.Integer index0) {
        try {
            Method m = com.design.pattern.proxy.dynamic1.Student.class.getMethod("learn", new Class[]{java.lang.Integer.class});
            return ((java.lang.Integer) this.h.invoke(this, m, new Object[]{index0})).intValue();
        } catch (Throwable e) {
        }
        return 0;
    }

    public java.lang.String love(java.lang.String index0, java.lang.Integer index1) {
        try {
            Method m = com.design.pattern.proxy.dynamic1.Student.class.getMethod("love", new Class[]{java.lang.String.class, java.lang.Integer.class});
            return ((java.lang.String) this.h.invoke(this, m, new Object[]{index0, index1}));
        } catch (Throwable e) {
        }
        return null;
    }

    public void changge(java.lang.String index0) {
        try {
            Method m = com.design.pattern.proxy.dynamic1.Student.class.getMethod("changge", new Class[]{java.lang.String.class});
            this.h.invoke(this, m, new Object[]{index0});
        } catch (Throwable e) {
        }
    }
}
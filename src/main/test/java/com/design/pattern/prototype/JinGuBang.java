package com.design.pattern.prototype;

import java.io.Serializable;

public class JinGuBang implements Serializable {
    public float h = 100;
    public float d = 10;


    public void big() {
        this.d *= 2;
        this.h *= 2;
    }

    public void mall() {
        this.d /= 2;
        this.h /= 2;
    }


}

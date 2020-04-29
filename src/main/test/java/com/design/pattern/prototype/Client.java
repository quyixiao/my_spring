package com.design.pattern.prototype;

public class Client {

    private Prototype prototype;

    public Client(Prototype prototype){
        this.prototype = prototype;
    }

    public Prototype startClone(Prototype prototype){
        return (Prototype)prototype.clone();
    }


}

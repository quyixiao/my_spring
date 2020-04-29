package com.design.pattern.proxy.statics;

public class Farther  {
    private Son son;

    public Farther(Son son){
        this.son = son;
    }

    public void findLove(){
        System.out.println("父亲物色的对象");
        this.son.findLove();
        System.out.println("双方同意交往，确定关系");
    }

    public static void main(String[] args) {

        Farther farther = new Farther(new Son());
        farther.findLove();
    }
}

package com.design.pattern.singon;

import java.io.*;
//反序列化导致破坏单例

public class SeriableSingleton implements Serializable {
    // 序列化把内存中的新动态通过转换成字节码的形式
    // 从而转换一个I/O流，写入其他的地方，可以是磁盘，网络，IO
    //
    public final static SeriableSingleton instance = new SeriableSingleton();


    private SeriableSingleton() {

    }

    public static SeriableSingleton getInstance() {
        return instance;
    }


    private Object readResolve() {
        return instance;
    }


    public static void main(String[] args) {
        SeriableSingleton s1 = null;
        SeriableSingleton s2 = SeriableSingleton.getInstance();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("SeriableSingleton.obj");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(s2);
            oos.flush();
            oos.close();

            FileInputStream fis = new FileInputStream("SeriableSingleton.obj");
            ObjectInputStream ois = new ObjectInputStream(fis);
            s1 = (SeriableSingleton) ois.readObject();
            ois.close();


            System.out.println(s1);
            System.out.println(s2);
            System.out.println(s1 == s2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

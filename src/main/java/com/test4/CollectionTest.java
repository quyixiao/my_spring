package com.test4;


public class CollectionTest {


    public static <T> T get(int flag) {
        if(flag == 1 ){
            return (T)new Integer(1);
        }else if (flag == 2){
            return (T)new Long(2);
        }else if(flag == 3){
            return (T)new Double(3.0);
        }
        return null;
    }


    public static void main(String[] args) {


        Integer a = get(1);
        Long b = get(2);
        Double c = get(3);
        System.out.println(a);
    }
}

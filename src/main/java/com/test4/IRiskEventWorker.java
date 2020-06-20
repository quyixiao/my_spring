package com.test4;

public interface IRiskEventWorker<T> {

    //填充数据
    void fillData();

    //生成签名
    void sign();

    void httpPost();


}
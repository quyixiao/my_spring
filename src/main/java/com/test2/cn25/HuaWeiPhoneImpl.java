package com.test2.cn25;

import com.test.LogUtils;
import org.springframework.stereotype.Service;

@Service
public class HuaWeiPhoneImpl implements Phone{


    @Override
    public void call() {
        LogUtils.info(" huawei call");
    }
}

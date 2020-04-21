package com.test2;

import com.test.LogUtils;
import com.test.Performer;

import java.util.Map;

public class OneManBand1 implements Performer {
    private Map<String, Instrument> maps;

    public OneManBand1() {

    }
    public void setMaps(Map<String, Instrument> maps) {
        this.maps = maps;
    }


    @Override
    public void perform() throws Exception {
        for (String key : maps.keySet()) {
            LogUtils.info(key + " : ");
            Instrument instrument = maps.get(key);
            instrument.play();
        }
    }
}

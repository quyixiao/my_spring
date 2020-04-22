package com.test3;

import com.test2.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class InstrumentTest3 {

    @Autowired
    @Qualifier("xxxxxxxxxxxx")
    private Instrument instrument;

    public Instrument getInstrument() {
        return instrument;
    }

}

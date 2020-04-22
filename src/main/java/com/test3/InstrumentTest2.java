package com.test3;

import com.test2.Instrument;
import org.springframework.beans.factory.annotation.Autowired;

public class InstrumentTest2 {

    private Instrument instrument;


    @Autowired
    public InstrumentTest2(Instrument instrument) {
        this.instrument = instrument;
    }

    public Instrument getInstrument() {
        return instrument;
    }

}

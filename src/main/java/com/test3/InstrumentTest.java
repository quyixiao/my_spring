package com.test3;

import com.test2.Instrument;
import org.springframework.beans.factory.annotation.Autowired;

public class InstrumentTest {

    @Autowired
    private Instrument instrument;


    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
}

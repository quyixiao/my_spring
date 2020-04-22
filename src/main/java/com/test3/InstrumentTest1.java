package com.test3;

import com.test2.Instrument;
import org.springframework.beans.factory.annotation.Autowired;

public class InstrumentTest1 {

    private Instrument instrument;


    public Instrument getInstrument() {
        return instrument;
    }

    @Autowired
    public void setMyPiano(Instrument instrument) {
        this.instrument = instrument;
    }
}

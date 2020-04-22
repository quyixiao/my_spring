package com.test3;

import com.test2.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class InstrumentTest4 {

    @Autowired
    @StringedInstrument
    private Instrument instrument;

    public Instrument getInstrument() {
        return instrument;
    }


}

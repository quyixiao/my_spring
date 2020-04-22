package com.test4;

import com.test3.StringedInstrument;
import org.springframework.beans.factory.annotation.Autowired;

public class InstrumentTest5 {

    @Autowired
    @StringedInstrument
    @Strummed
    private Instrument4 instrument;

    public Instrument4 getInstrument() {
        return instrument;
    }


}

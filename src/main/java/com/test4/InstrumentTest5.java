package com.test4;

import com.test2.Instrument;
import com.test3.StringedInstrument;
import org.springframework.beans.factory.annotation.Autowired;

public class InstrumentTest5 {

    @Autowired
    @StringedInstrument
    @Strummed
    private Instrument instrument;

    public Instrument getInstrument() {
        return instrument;
    }


}

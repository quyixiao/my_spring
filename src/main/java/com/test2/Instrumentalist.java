package com.test2;

import com.test.LogUtils;
import com.test.Performer;

public class Instrumentalist implements Performer {
    public Instrumentalist() {

    }

    @Override
    public void perform() throws Exception {
        LogUtils.info("Playing " + song + ":");
        instrument.play();
    }

    private String song;

    public void setSong(String song) {
        this.song = song;
    }

    public String getSong() {
        return this.song;
    }

    public String screamSong() {

        return song;
    }

    private Instrument instrument;

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
}

package com.test2.cn21;

import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class MyPerson {

    @Inject
    @StringedInstrument2
    private Person person;

    public Person getPerson() {
        return person;
    }


}

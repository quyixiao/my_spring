package com.test2.cn20;

import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class AnimalFactory {


    @Inject
    @Named("dog")
    private Animal animal;

    public Animal getAnimal() {
        return animal;
    }
}

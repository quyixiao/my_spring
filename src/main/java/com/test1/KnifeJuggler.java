package com.test1;

import com.test.LogUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@Component
public class KnifeJuggler {

    private Set<Knife> knives;

    @Inject
    public KnifeJuggler(Provider<Knife> knifeProvider){
        knives = new HashSet<Knife>();
        for(int i =0;i < 5;i++){
            Knife knife = knifeProvider.get();
            LogUtils.info("get Knife :" + knife);
            knives.add(knife);
        }
    }

    public Set<Knife> getKnives() {
        return knives;
    }
}

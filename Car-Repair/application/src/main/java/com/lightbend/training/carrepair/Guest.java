package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

public class Guest extends AbstractLoggingActor {
    @Override
    public Receive createReceive() {
        return emptyBehavior();
    }

    public static Props props(){
        return Props.create(Guest.class,Guest::new);
    }
}

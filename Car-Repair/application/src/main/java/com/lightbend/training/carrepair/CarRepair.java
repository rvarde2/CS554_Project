package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

public class CarRepair extends AbstractLoggingActor {
    public CarRepair() {
        log().debug("Car Repair Initiated");
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .matchAny(msg -> sender().tell(msg.toString(),self())) //If matched send message
                .build();
    }

    public static Props props(){
        return Props.create(CarRepair.class,CarRepair::new);
    }
}

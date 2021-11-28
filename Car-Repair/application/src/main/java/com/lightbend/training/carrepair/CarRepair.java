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
                //.matchAny(msg -> sender().tell(msg.toString(),self())) //If matched send message
                .match(CreateGuest.class, createGuest -> createGuest())
                .build();
    }

    public static Props props(){

        return Props.create(CarRepair.class,CarRepair::new);
    }

    //Creating Child actor on receiving a message
    protected void createGuest(){
        context().actorOf(Guest.props());
    }
    public static final class CreateGuest{
        //Allow only one instance of Create Guest they will be distributed later
        //Singleton
        public static final CreateGuest Instance = new CreateGuest();

        private CreateGuest(){}
    }
}

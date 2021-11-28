package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class Guest extends AbstractLoggingActor {
    //Map Guest to Mechanic
    private final ActorRef mechanic;
    private final Repair regularRepair;

    private int visits = 0;

    public Guest(ActorRef mechanic, Repair regularRepair) {
        this.mechanic = mechanic;
        this.regularRepair = regularRepair;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Mechanic.ServiceProvided.class,serviceProvided -> {
                    visits++;
                    log().info("Returned {} times for {}",(visits-1),serviceProvided.repair);
                })//After repair customer will check and find that issue is not resolved
                .match(IssueNotResolved.class, issueNotResolved -> {
                    this.mechanic.tell(new Mechanic.ServiceRequest(this.regularRepair),self());
                })
                .build();
    }

    public static Props props(final ActorRef mechanic, final Repair regularRepair){
        return Props.create(Guest.class,() -> new Guest(mechanic,regularRepair));
    }

    public static final class IssueNotResolved {
        public static final IssueNotResolved Instance = new IssueNotResolved();

        public IssueNotResolved() {
        }
    }
}

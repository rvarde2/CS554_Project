package com.lightbend.training.carrepair;

import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

public class Guest extends AbstractLoggingActorWithTimers {
    //Map Guest to Mechanic
    private final ActorRef mechanic;
    private final Repair regularRepair;
    private final int initial_credit;
    private final int testDriveDuration;


    public Guest(ActorRef mechanic, Repair regularRepair, int initial_credit, int testDriveDuration) {
        this.mechanic = mechanic;
        this.regularRepair = regularRepair;
        this.initial_credit = initial_credit;
        this.testDriveDuration = testDriveDuration;
        //First Message after entering Car Repair Center
        reportRepairIssue();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Mechanic.ServiceProvided.class,serviceProvided -> {
                    System.out.println(java.time.LocalTime.now()+":: Received Service Provided from "+sender()+". Going for TestDrive.");
                    //Once Service is provided by mechanic
                    goForTestDrive();
                })//After repair customer will go for test drive  and find that issue is not resolved
                .match(IssueNotResolved.class, issueNotResolved ->
                    //If Issue is not resolved then request again for the service
                        reportRepairIssue()
                )
                .build();
    }

    @Override
    public void postStop() throws Exception, Exception {
        log().info("Guest {} going away",self());
        super.postStop();
    }

    private void reportRepairIssue(){
        this.mechanic.tell(new Mechanic.ServiceRequest(this.regularRepair),self());
        System.out.println(java.time.LocalTime.now()+":: Sending Service Request from "+self()+" to "+this.mechanic);
    }
    public static Props props(final ActorRef mechanic, final Repair regularRepair,int initial_credit ,int testDriveDuration){
        return Props.create(Guest.class,() -> new Guest(mechanic,regularRepair, initial_credit, testDriveDuration));
    }

    private void goForTestDrive(){
        getTimers().startSingleTimer("issue-not-resolved",IssueNotResolved.Instance, Duration.create(testDriveDuration, TimeUnit.SECONDS));
    }

    public static final class IssueNotResolved {
        public static final IssueNotResolved Instance = new IssueNotResolved();

        public IssueNotResolved() {
        }
    }
}

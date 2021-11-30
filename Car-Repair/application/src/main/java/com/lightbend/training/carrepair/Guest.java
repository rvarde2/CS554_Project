package com.lightbend.training.carrepair;

import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.FiniteDuration;

public class Guest extends AbstractLoggingActorWithTimers {
    //Map Guest to Mechanic
    private final ActorRef mechanic;
    private final Repair regularRepair;
    private final FiniteDuration testDriveDuration;

    public int visits = 0;

    public Guest(ActorRef mechanic, Repair regularRepair, FiniteDuration testDriveDuration) {
        this.mechanic = mechanic;
        this.regularRepair = regularRepair;
        this.testDriveDuration = testDriveDuration;
        //First Message after entering Car Repair Center
        reportRepairIssue();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Mechanic.ServiceProvided.class,serviceProvided -> {
                    visits++;
                    log().info("Returned {} times for {}",(visits-1),serviceProvided.repair);
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
    }
    public static Props props(final ActorRef mechanic, final Repair regularRepair, final FiniteDuration testDriveDuration){
        return Props.create(Guest.class,() -> new Guest(mechanic,regularRepair, testDriveDuration));
    }

    private void goForTestDrive(){
        getTimers().startSingleTimer("issue-not-resolved",IssueNotResolved.Instance,testDriveDuration);
    }

    public static final class IssueNotResolved {
        public static final IssueNotResolved Instance = new IssueNotResolved();

        public IssueNotResolved() {
        }
    }
}

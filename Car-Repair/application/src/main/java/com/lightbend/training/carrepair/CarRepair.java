package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class CarRepair extends AbstractLoggingActor {

    //Get Inspection Duration from config
    private final FiniteDuration inspectionDuration = FiniteDuration.create(context().system().settings().config()
            .getDuration("car-repair.inspector.inspection-duration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);

    //Get Test Drive Duration from config
    private final FiniteDuration testDriveDuration = FiniteDuration.create(context().system().settings().config()
            .getDuration("car-repair.guest.test-drive-duration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);

    //Car Repair shop is aware of mapping between mechanic and inspector
    private final ActorRef inspector = createInspector();
    private final ActorRef mechanic = createMechanic();
    private final int serviceLimit;
    private final Map<ActorRef,Integer> ledger = new HashMap<>();

    public CarRepair(int serviceLimit) {
        this.serviceLimit = serviceLimit;
        log().debug("Car Repair Available");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateGuest.class, createGuest -> {
                    final ActorRef guest = createGuest(createGuest.regularRepair);
                    addGuestToLedger(guest);
                })
                .match(ApproveRepair.class,this::repairApproved,approveRepair -> {
                    inspector.forward(new Inspector.InspectionRequest(approveRepair.repair,approveRepair.guest),context());
                })
                .match(ApproveRepair.class,approveRepair -> {
                    log().info("Service Request Denied due to frequent visits for {}",approveRepair.guest);
                    context().stop(approveRepair.guest);
                })
                .build();
    }

    private boolean repairApproved(ApproveRepair approveRepair) {
        final int guestVisitCount = ledger.get(approveRepair.guest);
        if(guestVisitCount < serviceLimit){
            ledger.put(approveRepair.guest,guestVisitCount + 1);
            log().info("Guest {} visit count incremented to {}",approveRepair.guest,guestVisitCount + 1);
            return true;
        }
        log().info("Guest {} visit count has reached the limit of {}",approveRepair.guest,serviceLimit);
        return false;
    }

    private void addGuestToLedger(ActorRef guest) {
        ledger.put(guest,0);
        log().debug("Guest {} added to the ledger",guest);
    }

    public static Props props(int serviceLimit){
        return Props.create(CarRepair.class,() -> new CarRepair(serviceLimit));
    }

    //Creating Child actor on receiving a message
    protected ActorRef createGuest(Repair regularRepair){
        return context().actorOf(Guest.props(mechanic,regularRepair,testDriveDuration));
    }
    private ActorRef createInspector() {
        return getContext().actorOf(Inspector.props(inspectionDuration),"inspector");
    }

    protected ActorRef createMechanic(){
        return getContext().actorOf(Mechanic.props(self()),"mechanic");
    }
    public static final class CreateGuest{
        public final Repair regularRepair;
        public CreateGuest(Repair regularRepair) {
            checkNotNull(regularRepair,"regular repair cannot be null");
            this.regularRepair = regularRepair;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CreateGuest that = (CreateGuest) o;
            return Objects.equals(regularRepair, that.regularRepair);
        }

        @Override
        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= regularRepair.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "CreateGuest{" +
                    "regularRepair=" + regularRepair +
                    '}';
        }
    }

    public static final class ApproveRepair{
        public final Repair repair;
        public final ActorRef guest;

        public ApproveRepair(Repair repair, ActorRef guest) {
            checkNotNull(repair,"repair cannot be null");
            checkNotNull(guest,"guest cannot be null");
            this.repair = repair;
            this.guest = guest;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApproveRepair that = (ApproveRepair) o;
            return Objects.equals(repair, that.repair) && Objects.equals(guest, that.guest);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repair, guest);
        }

        @Override
        public String toString() {
            return "ApproveRepair{" +
                    "repair=" + repair +
                    ", guest=" + guest +
                    '}';
        }
    }

}

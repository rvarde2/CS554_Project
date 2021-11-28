package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.FiniteDuration;

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

    public CarRepair() {
        log().debug("Car Repair Initiated");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(CreateGuest.class, createGuest ->
                createGuest(createGuest.regularRepair)
        ).build();
    }

    public static Props props(){

        return Props.create(CarRepair.class,CarRepair::new);
    }

    //Creating Child actor on receiving a message
    protected void createGuest(Repair regularRepair){
        context().actorOf(Guest.props(mechanic,regularRepair,testDriveDuration));
    }
    private ActorRef createInspector() {
        return getContext().actorOf(Inspector.props(inspectionDuration),"inspector");
    }

    protected ActorRef createMechanic(){

        return getContext().actorOf(Mechanic.props(inspector),"mechanic");
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
}

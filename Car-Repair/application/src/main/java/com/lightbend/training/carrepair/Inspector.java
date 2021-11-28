package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.FiniteDuration;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Inspector extends AbstractLoggingActor {
    //Will take some amount of time for inspection
    private final FiniteDuration inspectionTime;

    public Inspector(FiniteDuration inspectionTime) {
        this.inspectionTime = inspectionTime;
    }

    public  static Props props(FiniteDuration inspectionTime){
        return Props.create(Inspector.class, () -> new Inspector(inspectionTime));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InspectionRequest.class, inspectionRequest -> {
                    //Inspection will take some time
                    Busy.busy(this.inspectionTime);
                    //Once Inspection is complete Inspector will send message back to the mechanic
                    sender().tell(new InspectionComplete(inspectionRequest.repair,inspectionRequest.guest),self());
                })
                .build();
    }

    public static final class InspectionRequest{
      //Inspector should get information about inspection and guest in message
      public final Repair repair;
      public final ActorRef guest;

        public InspectionRequest(Repair repair, ActorRef guest) {
            checkNotNull(repair,"repair request cannot be null");
            checkNotNull(guest,"guest cannot be null");
            this.repair = repair;
            this.guest = guest;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inspector inspector = (Inspector) o;
        return Objects.equals(inspectionTime, inspector.inspectionTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inspectionTime);
    }

    @Override
    public String toString() {
        return "Inspector{" +
                "inspectionTime=" + inspectionTime +
                '}';
    }

    public static final class InspectionComplete{
        //Inspector should send information about inspection and guest in message
        public final Repair repair;
        public final ActorRef guest;

        public InspectionComplete(Repair repair, ActorRef guest) {
            checkNotNull(repair,"repair request cannot be null");
            checkNotNull(guest,"guest cannot be null");
            this.repair = repair;
            this.guest = guest;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InspectionComplete that = (InspectionComplete) o;
            return Objects.equals(repair, that.repair) && Objects.equals(guest, that.guest);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repair, guest);
        }

        @Override
        public String toString() {
            return "InspectionComplete{" +
                    "repair=" + repair +
                    ", guest=" + guest +
                    '}';
        }
    }
}

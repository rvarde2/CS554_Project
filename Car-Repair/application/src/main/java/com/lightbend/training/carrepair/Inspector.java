package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class Inspector extends AbstractLoggingActor {

    private static int insp_engine;
    private static int insp_body;
    private static int insp_wheels;

    public Inspector(int insp_engine,int insp_body,int insp_wheels) {
        this.insp_engine = insp_engine;
        this.insp_body = insp_body;
        this.insp_wheels = insp_wheels;
    }

    public  static Props props(int insp_engine,int insp_body,int insp_wheels){
        return Props.create(Inspector.class, () -> new Inspector(insp_engine,insp_body,insp_wheels));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InspectionRequest.class, inspectionRequest -> {
                    log().info("Received Inspection Request for guest {}",inspectionRequest.guest);
                    //Inspection will take some time
                    //Dirty fix, do it later: low priority
                    switch(inspectionRequest.repair.getClass().getName()){
                        case "com.lightbend.training.carrepair.Repair$Engine":{
                                Busy.busy(Duration.create(this.insp_engine, TimeUnit.SECONDS));
                                break;
                        }
                        case "com.lightbend.training.carrepair.Repair$Body":{
                                Busy.busy(Duration.create(this.insp_body, TimeUnit.SECONDS));
                                break;
                        }
                        case "com.lightbend.training.carrepair.Repair$Wheels": {
                                Busy.busy(Duration.create(this.insp_wheels, TimeUnit.SECONDS));
                                break;
                        }
                    }
                    log().info("Completed Inspection Request for guest {}",inspectionRequest.guest);
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
    public String toString() {
        return "Inspector{}";
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

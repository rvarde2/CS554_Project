package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.Duration;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class Mechanic extends AbstractLoggingActor {

    //Mechanic should be aware about Car Repair shop decision
    private ActorRef carrepair;

    private static int rep_engine;
    private static int rep_body;
    private static int rep_wheels;
    private int index;
    public Mechanic(ActorRef carrepair,int rep_engine,int rep_body,int rep_wheels,int index) {
        this.rep_engine = rep_engine;
        this.rep_body = rep_body;
        this.rep_wheels = rep_wheels;
        this.carrepair = carrepair;
        this.index = index;
    }

    @Override
    public String toString() {
        return "Mechanic{" +
                "index=" + index +
                '}';
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServiceRequest.class,serviceRequest ->{
                        //send message on receiving request for the service
                        log().info("Mechanic {} received Service Request",this.index);
                        this.carrepair.tell(new CarRepair.ApproveRepairRequest(serviceRequest.repair,sender(),this.index),self());
                }).match(CarRepair.ApproveRepairResponse.class,approveRepairResponse -> {
                    log().info("Received Approval for guest {}",approveRepairResponse.guest);
                    switch(approveRepairResponse.repair.getClass().getName()){
                        case "com.lightbend.training.carrepair.Repair$Engine":{
                            Busy.busy(Duration.create(this.rep_engine, TimeUnit.SECONDS));
                            break;
                        }
                        case "com.lightbend.training.carrepair.Repair$Body":{
                            Busy.busy(Duration.create(this.rep_body, TimeUnit.SECONDS));
                            break;
                        }
                        case "com.lightbend.training.carrepair.Repair$Wheels": {
                            Busy.busy(Duration.create(this.rep_wheels, TimeUnit.SECONDS));
                            break;
                        }
                    }
                    log().info("Completed Repair work for guest {}",approveRepairResponse.guest);
                    approveRepairResponse.inspector.tell(new Inspector.InspectionRequest(approveRepairResponse.repair,approveRepairResponse.guest),self());
                }).match(Inspector.InspectionComplete.class,inspectionComplete -> {
                    inspectionComplete.guest.tell(new ServiceProvided(inspectionComplete.repair), self());
                }).build();
    }

    public static Props props(ActorRef carrepair,int rep_engine,int rep_body,int rep_wheels,int index){
        return Props.create(Mechanic.class,() -> new Mechanic(carrepair,rep_engine,rep_body,rep_wheels,index));
    }
    public static final class ServiceRequest{
        public final Repair repair;

        public ServiceRequest(final Repair repair) {
            //Make sure you properly receive repair request
            checkNotNull(repair,"Repair request cannot be null");
            this.repair = repair;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceRequest that = (ServiceRequest) o;
            return Objects.equals(repair, that.repair);
        }

        @Override
        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= repair.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "ServiceRequest{" +
                    "repair=" + repair +
                    '}';
        }
    }

    public static final class ServiceProvided{
        public final Repair repair;

        public ServiceProvided(Repair repair) {
            //Make sure you properly receive repair request
            checkNotNull(repair,"Repair response cannot be null");
            this.repair = repair;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServiceProvided that = (ServiceProvided) o;
            return Objects.equals(repair, that.repair);
        }

        @Override
        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= repair.hashCode();
            return h;
        }

        @Override
        public String toString() {
            return "ServiceProvided{" +
                    "repair=" + repair +
                    '}';
        }
    }
}

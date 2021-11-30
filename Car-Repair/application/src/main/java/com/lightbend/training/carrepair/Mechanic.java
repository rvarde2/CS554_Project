package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Mechanic extends AbstractLoggingActor {

    //Mechanic should be aware about Car Repair shop decision
    private ActorRef carrepair;

    public Mechanic(ActorRef carrepair) {
        this.carrepair = carrepair;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ServiceRequest.class,serviceRequest ->
                        //send message on receiving request for the service
                        this.carrepair.tell(new CarRepair.ApproveRepairRequest(serviceRequest.repair,sender()),self()))
                .match(CarRepair.ApproveRepairResponse.class,approveRepairResponse -> {
                    log().info("Received Approval for guest {}",approveRepairResponse.guest);
                    approveRepairResponse.inspector.tell(new Inspector.InspectionRequest(approveRepairResponse.repair,approveRepairResponse.guest),self());
                }).match(Inspector.InspectionComplete.class,inspectionComplete -> {
                    inspectionComplete.guest.tell(new ServiceProvided(inspectionComplete.repair), self());
                }).build();
    }

    public static Props props(ActorRef carrepair){
        return Props.create(Mechanic.class,() -> new Mechanic(carrepair));
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

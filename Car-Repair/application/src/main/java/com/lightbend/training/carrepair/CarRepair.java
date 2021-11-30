package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import org.yaml.snakeyaml.Yaml;
import scala.concurrent.duration.FiniteDuration;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class CarRepair extends AbstractLoggingActor {

    //Get Inspection Duration from yaml
    private final int insp_engine;
    private final int insp_body;
    private final int insp_wheels;

    //Get Inspection Duration from yaml
    private final int rep_engine;
    private final int rep_body;
    private final int rep_wheels;

    //Get Test Drive Duration from config
    private final FiniteDuration testDriveDuration;

    //Car Repair shop is aware of mapping between mechanic and inspector
    private final ActorRef inspector;
    private final ActorRef mechanic;
    private final int serviceLimit;
    private final Map<ActorRef,Integer> ledger = new HashMap<>();


    public CarRepair(int serviceLimit) {
        log().debug("Reading from inspector.yml");
        Yaml inspector_yaml = new Yaml();
        InputStream inputStream = CarRepair.class
                .getClassLoader()
                .getResourceAsStream("inspector.yml");
        Map<String, Object> inspector_obj = (Map<String, Object>) inspector_yaml.load(inputStream);

        if(inspector_obj.get("time_required_for_engine_inspection")==null){
            log().debug("Engine Inspection Time are not provided");
            System.exit(0);
        }
        if(inspector_obj.get("time_required_for_body_inspection")==null){
            log().debug("Body Inspection Time are not provided");
            System.exit(0);
        }
        if(inspector_obj.get("time_required_for_wheels_inspection")==null){
            log().debug("Wheels Inspection Time are not provided");
            System.exit(0);
        }

        this.insp_engine = (int) inspector_obj.get("time_required_for_engine_inspection");
        this.insp_body = (int) inspector_obj.get("time_required_for_body_inspection");
        this.insp_wheels = (int) inspector_obj.get("time_required_for_wheels_inspection");

        log().info("Reading from mechanic.yml");
        Yaml mechanic_yaml = new Yaml();
        InputStream inputStream2 = CarRepair.class
                .getClassLoader()
                .getResourceAsStream("mechanic.yml");
        Map<String, Object> mechanic_obj = (Map<String, Object>) mechanic_yaml.load(inputStream2);
        if(mechanic_obj.get("time_required_for_engine_repair")==null){
            log().debug("Engine Repair Time are not provided");
            System.exit(0);
        }
        if(mechanic_obj.get("time_required_for_body_repair")==null){
            log().debug("Body Repair Time are not provided");
            System.exit(0);
        }
        if(mechanic_obj.get("time_required_for_wheels_repair")==null){
            log().debug("Wheels Repair Time are not provided");
            System.exit(0);
        }

        this.rep_engine = (int) mechanic_obj.get("time_required_for_engine_repair");
        this.rep_body = (int) mechanic_obj.get("time_required_for_body_repair");
        this.rep_wheels = (int) mechanic_obj.get("time_required_for_wheels_repair");

        this.testDriveDuration = FiniteDuration.create(context().system().settings().config()
                .getDuration("car-repair.guest.test-drive-duration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);

        this.serviceLimit = serviceLimit;
        this.inspector = createInspector();
        this.mechanic = createMechanic();
        log().info("Car Repair Available");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateGuest.class, createGuest -> {
                    final ActorRef guest = createGuest(createGuest.regularRepair);
                    addGuestToLedger(guest);
                })
                .match(ApproveRepairRequest.class,this::repairApproved,approveRepairRequest -> {
                    mechanic.tell(new ApproveRepairResponse(approveRepairRequest.repair,approveRepairRequest.guest,inspector),self());
                    //inspector.forward(new Inspector.InspectionRequest(approveRepairRequest.repair,approveRepairRequest.guest),context());
                })
                .match(ApproveRepairRequest.class,approveRepairRequest -> {
                    log().info("Service Request Denied due to frequent visits for {}",approveRepairRequest.guest);
                    context().stop(approveRepairRequest.guest);
                })
                .build();
    }

    private boolean repairApproved(ApproveRepairRequest approveRepairRequest) {
        final int guestVisitCount = ledger.get(approveRepairRequest.guest);
        if(guestVisitCount < serviceLimit){
            ledger.put(approveRepairRequest.guest,guestVisitCount + 1);
            log().info("Guest {} visit count incremented to {}",approveRepairRequest.guest,guestVisitCount + 1);
            return true;
        }
        log().info("Guest {} visit count has reached the limit of {}",approveRepairRequest.guest,serviceLimit);
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

        return getContext().actorOf(Inspector.props(this.insp_engine,this.insp_body,this.insp_wheels),"inspector");
    }

    protected ActorRef createMechanic(){

        return getContext().actorOf(Mechanic.props(self(),this.rep_engine,this.rep_body,this.rep_wheels),"mechanic");
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

    public static final class ApproveRepairRequest{
        public final Repair repair;
        public final ActorRef guest;

        public ApproveRepairRequest(Repair repair, ActorRef guest) {
            checkNotNull(repair,"repair cannot be null");
            checkNotNull(guest,"guest cannot be null");
            this.repair = repair;
            this.guest = guest;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApproveRepairRequest that = (ApproveRepairRequest) o;
            return Objects.equals(repair, that.repair) && Objects.equals(guest, that.guest);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repair, guest);
        }

        @Override
        public String toString() {
            return "ApproveRepairRequest{" +
                    "repair=" + repair +
                    ", guest=" + guest +
                    '}';
        }
    }

    public static final class ApproveRepairResponse {
        public final Repair repair;
        public final ActorRef guest;
        public final ActorRef inspector;

        public ApproveRepairResponse(Repair repair, ActorRef guest, ActorRef inspector) {
            this.repair = repair;
            this.guest = guest;
            this.inspector = inspector;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApproveRepairResponse that = (ApproveRepairResponse) o;
            return Objects.equals(repair, that.repair) && Objects.equals(guest, that.guest) && Objects.equals(inspector, that.inspector);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repair, guest, inspector);
        }

        @Override
        public String toString() {
            return "ApproveRepairResponse{" +
                    "repair=" + repair +
                    ", guest=" + guest +
                    ", inspector=" + inspector +
                    '}';
        }
    }
}

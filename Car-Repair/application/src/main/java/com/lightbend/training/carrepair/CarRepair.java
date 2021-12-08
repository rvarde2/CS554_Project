package com.lightbend.training.carrepair;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import org.yaml.snakeyaml.Yaml;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.InputStream;
import java.util.ArrayList;
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

    //Get Repair Duration from yaml
    private final int rep_engine;
    private final int rep_body;
    private final int rep_wheels;

    //Get Repair Duration from yaml
    private final int credits_engine;
    private final int credits_body;
    private final int credits_wheels;

    private final int mechanic_no;
    private final int inspector_no;

    //Car Repair shop is aware of mapping between mechanic and inspector
    private final ArrayList<ActorRef> inspectors;
    private final ArrayList<ActorRef> mechanics;
    private final int serviceLimit;
    private final Map<ActorRef,Integer> ledger = new HashMap<>();
    private final Map<ActorRef,Integer> guesttoMechanic = new HashMap<>();

    public static int guest_count = 0;
    public static int inspection_requests = 0;
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
        this.inspector_no = (int) inspector_obj.get("inspector_no");
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
        this.mechanic_no = (int) mechanic_obj.get("mechanic_no");
        this.rep_engine = (int) mechanic_obj.get("time_required_for_engine_repair");
        this.rep_body = (int) mechanic_obj.get("time_required_for_body_repair");
        this.rep_wheels = (int) mechanic_obj.get("time_required_for_wheels_repair");

        Yaml carrepair_yaml = new Yaml();
        InputStream inputStream3 = CarRepair.class
                .getClassLoader()
                .getResourceAsStream("carrepair.yml");
        Map<String, Object> carrepair_obj = (Map<String, Object>) carrepair_yaml.load(inputStream3);


        this.credits_engine = (int) carrepair_obj.get("credits_required_for_engine_repair");
        this.credits_body = (int) carrepair_obj.get("credits_required_for_body_repair");
        this.credits_wheels = (int) carrepair_obj.get("credits_required_for_wheels_repair");

        inspectors = new ArrayList<ActorRef>();
        mechanics = new ArrayList<ActorRef>();

        this.serviceLimit = serviceLimit;

        for(int itr=0;itr<this.mechanic_no;itr++){
            this.inspectors.add(createInspector(itr));
        }

        for(int itr2=0;itr2<this.mechanic_no;itr2++){
            this.mechanics.add(createMechanic(itr2));
        }



        log().info("Car Repair Available");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateGuest.class, createGuest -> {
                    int index = this.guest_count%this.mechanic_no;
                    final ActorRef guest = createGuest(createGuest.regularRepair,createGuest.initial_credit,createGuest.testDriveDuration,index);
                    addGuestToLedger(guest,createGuest.initial_credit);
                    logGuestToMechanic(guest,index);
                    this.guest_count++;
                })
                .match(ApproveRepairRequest.class,this::repairApproved,approveRepairRequest -> {
                    log().info("Approve Repair Request from mechanic-{} for guest {}, repair {}",approveRepairRequest.index,approveRepairRequest.guest,approveRepairRequest.repair);
                    int inspector_index = this.inspection_requests%this.inspector_no;
                    mechanics.get(approveRepairRequest.index).tell(new ApproveRepairResponse(approveRepairRequest.repair,approveRepairRequest.guest,inspectors.get(inspector_index)),self());
                    this.inspection_requests++;
                    //inspector.forward(new Inspector.InspectionRequest(approveRepairRequest.repair,approveRepairRequest.guest),context());
                })
                .match(ApproveRepairRequest.class,approveRepairRequest -> {
                    log().info("Service Request Denied due to insufficient credits for {}",approveRepairRequest.guest);
                    context().stop(approveRepairRequest.guest);
                })
                .build();
    }

    private boolean repairApproved(ApproveRepairRequest approveRepairRequest) {
        int credit_remaining = ledger.get(approveRepairRequest.guest);
        switch(approveRepairRequest.repair.getClass().getName()){
            case "com.lightbend.training.carrepair.Repair$Engine":{
                if(credit_remaining<this.credits_engine){
                    return false;
                }
                credit_remaining -= this.credits_engine;
                ledger.put(approveRepairRequest.guest,credit_remaining);
                log().info("Guest {} have {} credits remaining",approveRepairRequest.guest,credit_remaining);
                return true;
            }
            case "com.lightbend.training.carrepair.Repair$Body":{
                if(credit_remaining<this.credits_body){
                    return false;
                }
                credit_remaining -= this.credits_body;
                ledger.put(approveRepairRequest.guest,credit_remaining);
                log().info("Guest {} have {} credits remaining",approveRepairRequest.guest,credit_remaining);
                return true;
            }
            case "com.lightbend.training.carrepair.Repair$Wheels": {
                if(credit_remaining<this.credits_wheels){
                    return false;
                }
                credit_remaining -= this.credits_wheels;
                ledger.put(approveRepairRequest.guest,credit_remaining);
                log().info("Guest {} have {} credits remaining",approveRepairRequest.guest,credit_remaining);
                return true;
            }
        }
        log().info("Guest {} do not have sufficient credits",approveRepairRequest.guest);
        return false;
    }

    private void addGuestToLedger(ActorRef guest,int initial_credit) {
        ledger.put(guest,initial_credit);
        log().debug("Guest {} added to the ledger with initial credit of {}",guest,initial_credit);
    }

    private void logGuestToMechanic(ActorRef guest,int mechanic_index) {
        guesttoMechanic.put(guest,mechanic_index);
    }

    public static Props props(int serviceLimit){
        return Props.create(CarRepair.class,() -> new CarRepair(serviceLimit));
    }

    //Creating Child actor on receiving a message
    protected ActorRef createGuest(Repair regularRepair,int initial_credit,int testDriveDuration,int mech_index){
        log().debug("Mechanic {} assigned to guest {}",mech_index,this.guest_count);
        return context().actorOf(Guest.props(mechanics.get(mech_index),regularRepair,initial_credit,testDriveDuration));
    }

    private ActorRef createInspector(int index) {
        return getContext().actorOf(Inspector.props(this.insp_engine,this.insp_body,this.insp_wheels,index),"inspector"+Integer.toString(index));
    }

    protected ActorRef createMechanic(int index){

        return getContext().actorOf(Mechanic.props(self(),this.rep_engine,this.rep_body,this.rep_wheels,index),"mechanic-"+Integer.toString(index));
    }

    public static final class CreateGuest{
        public final Repair regularRepair;
        public final int initial_credit;
        public final int testDriveDuration;
        public CreateGuest(Repair regularRepair, int initial_credit, int testDriveDuration) {
            checkNotNull(regularRepair,"regular repair cannot be null");
            this.initial_credit = initial_credit;
            this.testDriveDuration = testDriveDuration;
            this.regularRepair = regularRepair;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CreateGuest that = (CreateGuest) o;
            return initial_credit == that.initial_credit && testDriveDuration == that.testDriveDuration && Objects.equals(regularRepair, that.regularRepair);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regularRepair, initial_credit, testDriveDuration);
        }

        @Override
        public String toString() {
            return "CreateGuest{" +
                    "regularRepair=" + regularRepair +
                    ", initial_credit=" + initial_credit +
                    ", testDriveDuration=" + testDriveDuration +
                    '}';
        }
    }

    public static final class ApproveRepairRequest{
        public final Repair repair;
        public final ActorRef guest;
        public final int index;

        public ApproveRepairRequest(Repair repair, ActorRef guest,int index) {
            checkNotNull(repair,"repair cannot be null");
            checkNotNull(guest,"guest cannot be null");
            this.repair = repair;
            this.guest = guest;
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ApproveRepairRequest that = (ApproveRepairRequest) o;
            return index == that.index && Objects.equals(repair, that.repair) && Objects.equals(guest, that.guest);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repair, guest, index);
        }

        @Override
        public String toString() {
            return "ApproveRepairRequest{" +
                    "repair=" + repair +
                    ", guest=" + guest +
                    ", index=" + index +
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

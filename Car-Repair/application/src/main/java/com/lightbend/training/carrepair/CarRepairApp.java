/**
 * Copyright © 2014, 2015, 2016 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */
package com.lightbend.training.carrepair;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.yaml.snakeyaml.Yaml;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarRepairApp{

    public static final Pattern optPattern = Pattern.compile("(\\S+)=(\\S+)");

    private final ActorSystem system;

    private final LoggingAdapter log;

    String engine = "Engine";
    String body = "Body";
    String wheels = "Wheels";

    private final ActorRef carRepair;

    public CarRepairApp(final ActorSystem system){
        this.system = system;
        log = Logging.getLogger(system, getClass().getName());
        carRepair = createCarRepair();
    }

    public static void main(final String[] args) throws Exception{
        //Entry Point to Java Code
        final Map<String, String> opts = argsToOpts(Arrays.asList(args));
        applySystemProperties(opts);
        final String name = opts.getOrDefault("name", "car-repair");
        //Create System for Actors
        final ActorSystem system = ActorSystem.create(String.format("%s-system", name));
        final CarRepairApp carRepairApp = new CarRepairApp(system);
        //Starting Application: Will move into commandloop eventually
        carRepairApp.run();
    }

    public static Map<String, String> argsToOpts(final List<String> args){
        final Map<String, String> opts = new HashMap<>();
        for (final String arg : args) {
            final Matcher matcher = optPattern.matcher(arg);
            if (matcher.matches()) opts.put(matcher.group(1), matcher.group(2));
        }
        return opts;
    }

    public static void applySystemProperties(final Map<String, String> opts){
        opts.forEach((key, value) -> {
            if (key.startsWith("-D")) System.setProperty(key.substring(2), value);
        });
    }

    private void run() throws IOException, TimeoutException, InterruptedException {
        commandLoop();//Infinite Loop
        Await.ready(system.whenTerminated(), Duration.Inf());
    }

    protected ActorRef createCarRepair(){
        final int serviceLimit = system.settings().config().getInt("car-repair.serviceLimit");
        //return Actor Reference to Car Repair from our actor system
        return system.actorOf(CarRepair.props(serviceLimit),"car-repair");
    }

    private void commandLoop() throws IOException{
        System.out.println("##Provide Duration between snapshots:");
        Scanner in = new Scanner(System.in);
        int duration = in.nextInt();
        Yaml yaml = new Yaml();
        InputStream inputStream = CarRepairApp.class
                .getClassLoader()
                .getResourceAsStream("guestList.yml");
        for (Object object : yaml.loadAll(inputStream)) {
            Map<String, Object> guest = (Map<String, Object>) object;
            String repairString = (String) guest.get("repair");
            Repair repair;
            if(repairString.equals(engine)){
                repair = new Repair.Engine();
            }else if(repairString.equals(body)){
                repair = new Repair.Body();
            }else if(repairString.equals(wheels)){
                repair = new Repair.Wheels();
            }
            else{
                continue;
            }
            int initial_credit = (int) guest.get("initial_credit");
            int testDriveDuration = (int) guest.get("testDriveDuration");
            carRepair.tell(new CarRepair.CreateGuest(repair, initial_credit, testDriveDuration),ActorRef.noSender());
        }
        while (true) {
            Busy.busy(Duration.create(duration, TimeUnit.SECONDS));
            CarRepair.snapshot();
        }
    }


}

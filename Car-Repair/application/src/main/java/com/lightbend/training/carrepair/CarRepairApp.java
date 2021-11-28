/**
 * Copyright © 2014, 2015, 2016 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */
package com.lightbend.training.carrepair;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarRepairApp implements Terminal{

    public static final Pattern optPattern = Pattern.compile("(\\S+)=(\\S+)");

    private final ActorSystem system;

    private final LoggingAdapter log;


    private final ActorRef carRepair;

    public CarRepairApp(final ActorSystem system){
        this.system = system;
        log = Logging.getLogger(system, getClass().getName());
        carRepair = createCarRepair();
        //carRepair.tell("First Message to Car Repair", Actor.noSender());
        //New Anonymous actor will send message
        //system.actorOf(printerProps(carRepair));
    }

   /* private Props printerProps(ActorRef carRepair){
        return Props.create(AbstractLoggingActor.class, () -> new AbstractLoggingActor() {
            {
                carRepair.tell("Anonymous Message",self());
            }
            @Override
            public Receive createReceive() {
                return ReceiveBuilder.create()
                        .matchAny(msg-> log().info(msg.toString()))
                        .build();
            }
        });
    }*/

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
        log.warning(
            String.format("{} running%nEnter commands into the terminal, e.g. 'q' or 'quit'"),
            getClass().getSimpleName()
        );
        commandLoop();//Infinite Loop
        Await.ready(system.whenTerminated(), Duration.Inf());
    }

    protected ActorRef createCarRepair(){
        //return Actor Reference to Car Repair from our actor system
        return system.actorOf(CarRepair.props(),"car-repair");
    }

    private void commandLoop() throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                system.terminate();
                break;
            } else {
                TerminalCommand tc = Terminal.create(line);
                //Three Options:
                //1)Create Guest and entertain the request
                //2)Acquire current status of the system
                //3)Quit
                if (tc instanceof TerminalCommand.Guest) {
                    TerminalCommand.Guest tcg = (TerminalCommand.Guest) tc;
                    createGuest(tcg.count, tcg.repair, tcg.maxRepairCount);
                } else if (tc == TerminalCommand.Status.Instance) {
                    getStatus();
                } else if (tc == TerminalCommand.Quit.Instance) {
                    system.terminate();
                    break;
                } else {
                    TerminalCommand.Unknown u = (TerminalCommand.Unknown) tc;
                    log.warning("Unknown terminal command {}!", u.command);
                }
            }
        }
    }

    protected void createGuest(int count, Repair repair, int maxRepairCount){
        //Later use YAML for no. of messages
        for(int i=0;i<count;i++){
            carRepair.tell(new CarRepair.CreateGuest(repair),ActorRef.noSender());
        }
    }

    protected void getStatus(){
    }
}

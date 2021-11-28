/**
 * Copyright © 2014, 2015, 2016 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */
package com.lightbend.training.carrepair;

import com.google.common.collect.ImmutableSet;

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public interface Repair extends Serializable{

    long serialVersionUID = 1;
    // NOTE return copy of static list???
    ImmutableSet<Repair> REPAIRS = ImmutableSet.of(
        new Engine(), new Body(), new Wheels());

    static Repair order(final String code){
        //Expecting Engine Failure, Body damage or Wheel Puncture
        switch (code.toLowerCase()) {
            case Engine.CODE:
                return new Engine();
            case Body.CODE:
                return new Body();
            case Wheels.CODE:
                return new Wheels();
            default:
                throw new IllegalArgumentException(String.format("Unknown repair code \"%s\"!", code));
        }
    }

    static Repair orderOther(final Repair repair){
        //Randomly generate repair if not mentioned
        Random rnd = new Random();
        List<Repair> filtered = REPAIRS.stream().filter(c -> !c.equals(repair)).collect(Collectors.toList());
        return filtered.get(rnd.nextInt(filtered.size()));
    }
    // e for Engine Failure
    final class Engine implements Repair{

        private static final long serialVersionUID = 1L;

        public static final String CODE = "e";

        @Override
        public String toString(){
            return this.getClass().getSimpleName();
        }

        @Override
        public boolean equals(Object o){
            return o == this || o instanceof Engine;
        }

        @Override
        public int hashCode(){
            return 1;
        }
    }
    // b for Body Damage
    final class Body implements Repair{

        private static final long serialVersionUID = 1L;

        public static final String CODE = "b";

        @Override
        public String toString(){
            return this.getClass().getSimpleName();
        }

        @Override
        public boolean equals(Object o){
            return o == this || o instanceof Body;
        }

        @Override
        public int hashCode(){
            return 1;
        }
    }
    // w for Wheel Puncture
    final class Wheels implements Repair{

        private static final long serialVersionUID = 1L;

        public static final String CODE = "w";

        @Override
        public String toString(){
            return this.getClass().getSimpleName();
        }

        @Override
        public boolean equals(Object o){
            return o == this || o instanceof Wheels;
        }

        @Override
        public int hashCode(){
            return 1;
        }
    }
}

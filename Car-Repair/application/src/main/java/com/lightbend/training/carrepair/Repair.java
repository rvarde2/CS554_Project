/**
 * Copyright © 2014, 2015, 2016 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */
package com.lightbend.training.carrepair;

import com.google.common.collect.ImmutableSet;

import java.io.Serializable;

public interface Repair extends Serializable{

    long serialVersionUID = 1;
    // NOTE return copy of static list???
    ImmutableSet<Repair> REPAIRS = ImmutableSet.of(
        new Engine(), new Body(), new Wheels());

    final class Engine implements Repair{

        private static final long serialVersionUID = 1L;

        public static final String CODE = "Engine";

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

    final class Body implements Repair{

        private static final long serialVersionUID = 1L;

        public static final String CODE = "Body";

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

    final class Wheels implements Repair{

        private static final long serialVersionUID = 1L;

        public static final String CODE = "Wheels";

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

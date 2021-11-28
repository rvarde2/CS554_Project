/**
 * Copyright © 2014, 2015, 2016 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */
package com.lightbend.training.carrepair;

import java.io.Serializable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public interface TerminalCommand extends Serializable{

    long serialVersionUID = 1;

    final class Guest implements TerminalCommand{

        private static final long serialVersionUID = 1L;

        public final int count;

        public final Repair repair;

        public final int maxRepairCount;

        public Guest(final int count, final Repair repair, final int maxRepairCount){
            checkNotNull(repair, "Repair cannot be null");
            this.count = count;
            this.repair = repair;
            this.maxRepairCount = maxRepairCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Guest guest = (Guest) o;
            return count == guest.count && maxRepairCount == guest.maxRepairCount && Objects.equals(repair, guest.repair);
        }

        @Override
        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= count;
            h *= 1000003;
            h ^= repair.hashCode();
            h *= 1000003;
            h ^= maxRepairCount;
            return h;
        }

        @Override
        public String toString() {
            return "Guest{" +
                    "count=" + count +
                    ", repair=" + repair +
                    ", maxRepairCount=" + maxRepairCount +
                    '}';
        }
    }

    final class Status implements TerminalCommand{

        private static final long serialVersionUID = 1L;

        public static final Status Instance = new Status();

        private Status(){
        }
    }

    final class Quit implements TerminalCommand{

        private static final long serialVersionUID = 1L;

        public static final Quit Instance = new Quit();

        private Quit(){
        }
    }

    final class Unknown implements TerminalCommand{

        private static final long serialVersionUID = 1L;

        public final String command;

        public Unknown(final String command){
            checkNotNull(command, "Command cannot be null");
            this.command = command;
        }

        @Override
        public String toString(){
            return "Unknown{command=" + command + "}";
        }

        @Override
        public boolean equals(Object o){
            if (o == this) return true;
            if (o instanceof Unknown) {
                Unknown that = (Unknown) o;
                return (this.command.equals(that.command));
            }
            return false;
        }

        @Override
        public int hashCode(){
            int h = 1;
            h *= 1000003;
            h ^= command.hashCode();
            return h;
        }
    }
}

/**
 * Copyright © 2014, 2015, 2016 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */
package com.lightbend.training.carrepair;

import com.lightbend.training.carrepair.TerminalCommand.Guest;
import com.lightbend.training.carrepair.TerminalCommand.Quit;
import com.lightbend.training.carrepair.TerminalCommand.Status;
import com.lightbend.training.carrepair.TerminalCommand.Unknown;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Terminal{

    Pattern createGuestPattern = Pattern.compile("(\\d+)?\\s*(?:guest|g)\\s*(E|e|B|b|W|w)?\\s*(\\d+)?");
    Pattern getStatusPattern = Pattern.compile("status|s");
    Pattern quitPattern = Pattern.compile("quit|q");

    static TerminalCommand create(final String s){

        final Matcher guestMatcher = createGuestPattern.matcher(s);
        if (guestMatcher.matches()) {

            final String countGroup = guestMatcher.group(1);
            final int count = countGroup != null ? Integer.parseInt(countGroup) : 1;

            final String repairGroup = guestMatcher.group(2);
            final Repair repair = repairGroup != null ? Repair.order(repairGroup) : new Repair.Engine();

            final String maxRepairCountGroup = guestMatcher.group(3);
            final int maxRepairCount =
                maxRepairCountGroup != null ? Integer.parseInt(maxRepairCountGroup) : Integer.MAX_VALUE;

            return new Guest(count, repair, maxRepairCount);
        }
        if (getStatusPattern.matcher(s).matches()) return Status.Instance;
        if (quitPattern.matcher(s).matches()) return Quit.Instance;
        return new Unknown(s);
    }
}

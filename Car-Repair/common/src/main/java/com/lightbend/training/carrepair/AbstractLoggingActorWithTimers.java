package com.lightbend.training.carrepair;

import akka.actor.AbstractActorWithTimers;
import akka.event.LoggingAdapter;

abstract class AbstractLoggingActorWithTimers extends AbstractActorWithTimers {
    private LoggingAdapter _log = null;

    public LoggingAdapter log() {
        if (_log == null) {
            _log = akka.event.Logging.apply(this);
        }
        return _log;
    }
}

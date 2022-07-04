/*
 * httpq - the webhooks sending server
 * Copyright © 2022 Edward Swiac (eswiac@fastmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.httpq.server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public abstract class AbstractDequeuerVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(AbstractDequeuerVerticle.class);

  private final Duration executionWindow = Duration.ofSeconds(3);

  @Override
  public void start() throws Exception {
    recursiveTimedDequeue();
  }

  public void recursiveTimedDequeue() {
    Instant start = Instant.now(Clock.systemUTC());
    dequeue().onComplete(h -> {
      Instant end = Instant.now();
      Duration elapsed = Duration.between(start, end);

      Duration remainder = executionWindow.minus(elapsed);
      LOGGER.info("elapsed={} remainder={}", elapsed, remainder);

      vertx.setTimer(Math.max(remainder.toMillis(), 1), hh -> {
        recursiveTimedDequeue();
      });
    });
  }


  public abstract Future<Void> dequeue();
}

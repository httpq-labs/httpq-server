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

import io.vertx.core.*;
import org.httpq.db.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



// lP4qA6uM8wR9uF2kL9yJ1oL8zH9fQ7tZ
public class MainVerticle extends AbstractVerticle {
  private final static Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle())
      .onSuccess(v -> LOGGER.info("httpq server started!"))
      .onFailure(ex -> {
        LOGGER.error("[!] httpq server cannot start", ex);
        vertx.close();
      });
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    LOGGER.info("starting up...");

    Future<Void> migrateBlocking =  new Database(vertx).migrateBlocking();
    Future<Void> startFuture = migrateBlocking
      .compose((v) -> vertx.deployVerticle(HttpClientVerticle::new,new DeploymentOptions().setInstances(Runtime.getRuntime().availableProcessors()*4)))
      .compose((v) -> vertx.deployVerticle(PartitionManagerVerticle::new, new DeploymentOptions()))
      .compose((v) -> vertx.deployVerticle(MainDequeuerVerticle::new,new DeploymentOptions()))
      .compose((v) -> vertx.deployVerticle(RetryDequeuerVerticle::new, new DeploymentOptions()))
      .compose((v) -> vertx.deployVerticle(HttpServerVerticle::new,new DeploymentOptions()))
      .mapEmpty();

    startFuture.onComplete(startPromise);
  }
}

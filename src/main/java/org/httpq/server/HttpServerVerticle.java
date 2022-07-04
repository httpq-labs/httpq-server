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
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.UserImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.pgclient.PgPool;
import org.httpq.db.Database;
import org.httpq.exception.NotFoundException;
import org.httpq.exception.UnauthorizedException;
import org.httpq.exception.ValidationException;
import org.httpq.http.handlers.*;
import org.httpq.injector.Injector;
import org.httpq.lib.JacksonModule;
import org.httpq.manager.EnqueuerManager;
import org.httpq.model.SecurityKey;
import org.httpq.model.TypedConf;
import org.httpq.repository.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  static {
    JacksonModule.init();
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    TypedConf config = TypedConf.load();
    String host = config.httpHost();
    int port = config.httpPort();

    makeRouter().onSuccess(router -> {
      vertx.createHttpServer()
        .requestHandler(router).listen(port, host, http -> {
          if (http.succeeded()) {
            startPromise.complete();
            LOGGER.info("http server listening on {}:{}", host, port);
          } else {
            startPromise.fail(http.cause());
          }
        });
    }).onFailure(startPromise::fail);
  }

  public Future<Router> makeRouter() {
    PgPool pool = new Database(vertx).getSharedPool();
    Tenants tenants = new Tenants(pool);
    SecurityKeys securityKeys = new SecurityKeys(pool);
    Versions versions = new Versions(pool);
    Consumers consumers = new Consumers(pool);
    Topics topics = new Topics(pool);
    Subscriptions subscriptions = new Subscriptions(pool);
    Injector injector = new Injector();
    EnqueuerManager enqueuerManager = injector.getEnqueuerManager(pool);
    WebhookEvents webhookEvents = new WebhookEvents(pool, enqueuerManager);

    return RouterBuilder
      .create(vertx, "https://raw.githubusercontent.com/httpq-labs/httpq-api-spec/main/openapi.yml")
      .map(rb -> rb.securityHandler("ApiKeyAuth")
        .bindBlocking(config -> createAuthHandler(pool)))
      .onSuccess(rb -> {
        rb.operation("createTenant").handler(tenants::create);
        rb.operation("retrieveTenant").handler(tenants::get);
        rb.operation("createSecurityKey").handler(securityKeys::create);
        rb.operation("listVersions").handler(versions::list);
        rb.operation("createVersion").handler(versions::create);
        rb.operation("createConsumer").handler(consumers::create);
        rb.operation("listConsumers").handler(consumers::list);
        rb.operation("createTopic").handler(topics::create);
        rb.operation("listTopics").handler(topics::list);
        rb.operation("createSubscription").handler(subscriptions::create);
        rb.operation("listSubscriptions").handler(subscriptions::list);
        rb.operation("updateSubscription").handler(subscriptions::update);
        rb.operation("createWebhook").handler(webhookEvents::create);
        rb.operation("retrieveWebhook").handler(webhookEvents::get);
        rb.operation("ping").handler(ctx -> ctx.end("pong"));
      })
      .map(RouterBuilder::createRouter)
      .map(router -> {
        router
          .route()
          .handler(LoggerHandler.create(LoggerFormat.SHORT))
          .failureHandler(ctx -> {
            Throwable failure = ctx.failure();
            if (failure instanceof NotFoundException) {
              ctx.response().setStatusCode(404).end();
            } else if (failure instanceof ValidationException) {
              String message = "invalid request";
              if (failure.getMessage() != null) {
                message += ": %s".formatted(failure.getMessage());
              }
              JsonObject o = new JsonObject().put("message", message);
              ctx.response().setStatusCode(400).end(o.encodePrettily());
            } else if (failure instanceof UnauthorizedException) {
              ctx.response().setStatusCode(401).end();
            } else if (failure instanceof VertxException) {
              JsonObject o = new JsonObject().put("message", failure.getMessage());
              ctx.response().setStatusCode(400).end(o.encodePrettily());
            } else {
              LOGGER.error("error", failure);
              JsonObject o = new JsonObject().put("message", "internal error");
              if (failure instanceof HttpException httpException) {
                ctx.response().setStatusCode(httpException.getStatusCode()).end(o.encodePrettily());
              } else {
                ctx.response().setStatusCode(500).end(o.encodePrettily());
              }
            }
          });
        return router;
      });
  }

  public AuthenticationHandler createAuthHandler(PgPool pool) {
    return APIKeyHandler.create((credentials, resultHandler) -> {
      Future<SecurityKey> f = pool.withConnection(conn -> new Repositories(conn).securityKey().getBySecurityKey(credentials.getString("token")));
      f.onComplete(ar -> {
        if (ar.succeeded()) {
          JsonObject o = new JsonObject();
          o.put("security_key", JsonObject.mapFrom(ar.result()));
          resultHandler.handle(Future.succeededFuture(new UserImpl(o)));
        } else {
          LOGGER.error("failure", ar.cause());
          resultHandler.handle(Future.failedFuture(new UnauthorizedException()));
        }
      });
    });
  }
}

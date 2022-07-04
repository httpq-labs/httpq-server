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
package org.httpq.injector;

import io.vertx.core.eventbus.EventBus;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.httpq.manager.*;
import org.httpq.repository.Repositories;

import java.util.function.Function;

public class Injector {

  public Injector() {
  }

  // get all db backed services on the same connection for transactional stuff
  Function<SqlConnection, Repositories> getServices() {
    return Repositories::new;
  }

  WebhookHttpRequestManager getWebhookHttpRequestManager(final EventBus eb) {
    return new WebhookHttpRequestManager(eb);
  }

  WebhookPreparerManager getWebhookPreparer(final SqlConnection conn) {
    return new WebhookPreparerManager(getServices().apply(conn).webhookEvent());
  }


  WebhookHttpResponseManager getWebhookHttpResponseManager(final SqlConnection conn) {
    Repositories repositories = getServices().apply(conn);
    return new WebhookHttpResponseManager(repositories.retryQueue(), repositories.webhookEvent(), repositories.subscription());
  }

  Function<SqlConnection, WebhookSendPipeline> getWebhookManager(final EventBus eb) {
    return conn -> new WebhookSendPipeline(getWebhookPreparer(conn), getWebhookHttpRequestManager(eb), getWebhookHttpResponseManager(conn));
  }

  // root deps
  public EnqueuerManager getEnqueuerManager(final PgPool pool) {
    return new EnqueuerManager(pool, getServices());
  }

  public DequeuerManager getDequeuerManager(final PgPool pool, final EventBus eb) {
    return new DequeuerManager(pool, getServices(), getWebhookManager(eb));
  }


}

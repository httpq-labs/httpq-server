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
package org.httpq.repository;

import io.vertx.sqlclient.SqlConnection;

public class Repositories {

  private final SqlConnection conn;

  public Repositories(SqlConnection conn) {
    this.conn = conn;
  }

  public ConsumerRepository consumer() {
    return new ConsumerRepository(conn);
  }

  public MainQueueRepository mainQueue() {
    return new MainQueueRepository(conn);
  }

  public RetryQueueRepository retryQueue() {
    return new RetryQueueRepository(conn);
  }

  public SecurityKeyRepository securityKey() {
    return new SecurityKeyRepository(conn);
  }

  public SubscriptionRepository subscription() {
    return new SubscriptionRepository(conn);
  }

  public TenantRepository tenant() {
    return new TenantRepository(conn);
  }

  public TopicRepository topic() {
    return new TopicRepository(conn);
  }

  public VersionRepository version() {
    return new VersionRepository(conn);
  }

  public WebhookEventRepository webhookEvent() {
    return new WebhookEventRepository(conn);
  }
}

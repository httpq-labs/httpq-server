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

import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.SqlConnection;
import org.flywaydb.core.Flyway;
import org.httpq.db.Database;
import org.httpq.db.FlywayFactory;
import org.httpq.model.TypedConf;
import org.httpq.server.HttpApiFixtures;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@ExtendWith(VertxExtension.class)
public abstract class TestWithRepositoryFixtures {
  static TypedConf typedConf;
  static PgPool pool;
  static Flyway flyway;
  static SqlConnection conn;

  static UUID tenantId;
  static UUID versionId;
  static UUID consumerId;
  static UUID topicId;
  static UUID subscriptionId;

  static HttpApiFixtures httpApiFixtures;
  static UUID rootSecurityKeyId;
  static String SUBSCRIPTION_URL = "http://suburl.net";
  static String tenantName = "edward";
  static String topicName = "user.created";
  static String versionStr = "v1";
  static LocalDate today = LocalDate.now(Clock.systemUTC());


  @BeforeAll
  static void initFixtures(Vertx vertx, VertxTestContext tc) {
    typedConf = TypedConf.load();
    flyway = FlywayFactory.make(typedConf);
    flyway.clean();
    flyway.migrate();
    httpApiFixtures = new HttpApiFixtures(typedConf);

    pool = new Database(vertx).getSharedPool();

    Checkpoint cp = tc.checkpoint(2);

    pool.getConnection().onComplete(tc.succeeding(c -> {
      conn = c;
      new TenantRepository(conn).create(tenantName).onComplete(tc.succeeding(tid -> {
        tenantId = tid;
        new VersionRepository(conn).create(tid, versionStr).onComplete(tc.succeeding(vid -> {
          versionId = vid;
          new ConsumerRepository(conn).create(tenantId, "789", versionId).onComplete(tc.succeeding(cid -> {
            consumerId = cid;
            new TopicRepository(conn).create(tenantId, topicName).onComplete(tc.succeeding(topId -> {
              topicId = topId;
              new SubscriptionRepository(conn).create(tenantId, consumerId, topicId, SUBSCRIPTION_URL).onComplete(tc.succeeding(sid -> {
                subscriptionId = sid;
                cp.flag();
              }));
            }));
          }));
        }));
      }));


      new SecurityKeyRepository(conn).getBySecurityKey(httpApiFixtures.getRootApiKey().toString()).onComplete(tc.succeeding(k -> {
        rootSecurityKeyId = k.id();
        cp.flag();
      }));
    }));
  }

  @AfterAll
  static void afterAll(VertxTestContext tc) {
    pool.close().onComplete(tc.succeedingThenComplete());
  }

}

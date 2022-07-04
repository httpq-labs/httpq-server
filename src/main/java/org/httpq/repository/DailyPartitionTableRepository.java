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

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.httpq.model.DailyPartitionTable;
import org.httpq.model.StateReconciliationSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DailyPartitionTableRepository extends AbstractRepository<String> {
  private final static Logger LOGGER = LoggerFactory.getLogger(DailyPartitionTableRepository.class);

  private final int pastPartitionsCount;
  private final int futurePartitionsCount;
  private final String tableName;
  private final LocalDate today;

  public DailyPartitionTableRepository(SqlConnection conn, String tableName, LocalDate today) {
    this(conn, tableName, today, 7, 7);
  }

  public DailyPartitionTableRepository(SqlConnection conn, String tableName, LocalDate today, int pastPartitionsCount, int futurePartitionsCount) {
    super(conn);
    this.pastPartitionsCount = Math.max(pastPartitionsCount, 2);
    this.futurePartitionsCount = Math.max(futurePartitionsCount, 2);
    this.tableName = tableName;
    this.today = today;
  }

  public Set<DailyPartitionTable> computeDesiredPartitionsState() {
    Set<DailyPartitionTable> desiredState = new HashSet<>();
    desiredState.addAll(
      today.minusDays(pastPartitionsCount).datesUntil(today)
        .map(day -> new DailyPartitionTable(tableName, day)).toList());
    desiredState.addAll(
      today.datesUntil(today.plusDays(futurePartitionsCount))
        .map(day -> new DailyPartitionTable(tableName, day)).toList());
    return desiredState;
  }

  public Future<Set<DailyPartitionTable>> listCurrentPartitions() {
    return list("""
      SELECT table_name
      FROM information_schema.tables
      WHERE table_name LIKE '%s__%%'
      ORDER BY table_name
      """.formatted(tableName), Tuple.tuple(), row -> DailyPartitionTable.parse(row.getString(0))).map(Set::copyOf);
  }

  public Future<StateReconciliationSet<DailyPartitionTable>> reconcile() {
    Set<DailyPartitionTable> desiredPartitions = computeDesiredPartitionsState();
    return listCurrentPartitions()
      .map(currentPartitions -> StateReconciliationSet.make(currentPartitions, desiredPartitions))
      .flatMap(reconciledState -> {
        LOGGER.info("successful partition reconciliation {}", reconciledState);
        return CompositeFuture.all(create(reconciledState.toAdd()), drop(reconciledState.toRemove())).map(reconciledState);
      });
  }

  public Future<StateReconciliationSet<DailyPartitionTable>> reconcile(Set<DailyPartitionTable> desiredPartitions) {
   return listCurrentPartitions()
     .map(currentPartitions -> StateReconciliationSet.make(currentPartitions, desiredPartitions))
     .flatMap(reconciledState -> CompositeFuture.all(create(reconciledState.toAdd()), drop(reconciledState.toRemove())).map(reconciledState));
  }

  public Future<Void> create(Set<DailyPartitionTable> partitions) {
    List<Future> ca = partitions.stream().map(p -> executeRaw(
      "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s')".formatted(p.partitionName(), p.tableName(), p.from(), p.to()))).toList();
    return CompositeFuture.all(ca).mapEmpty();
  }


  public Future<Void> drop(Set<DailyPartitionTable> partitions) {
    List<String> tables = partitions.stream().map(DailyPartitionTable::partitionName).toList();
    if (tables.isEmpty()) {
      return Future.succeededFuture();
    }
    String tablesString = String.join(", ", tables);
    return executeRaw("DROP TABLE %s".formatted(tablesString));
  }
}

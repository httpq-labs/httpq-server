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

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.httpq.exception.NotFoundException;
import org.httpq.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public abstract class AbstractRepository<T> {
  protected SqlConnection conn;
  private final static Logger LOGGER = LoggerFactory.getLogger(AbstractRepository.class);

  public AbstractRepository(SqlConnection conn) {
    this.conn = conn;
  }

  protected Future<Void> create(String sql, Tuple params) {
    return conn
      .preparedQuery(sql)
      .execute(params)
      .otherwise(ex -> {
        LOGGER.warn(ex.getMessage());
        throw new ValidationException();
      }).mapEmpty();
  }

  protected Future executeRaw(String sql) {
    return conn
      .preparedQuery(sql)
      .execute()
      .otherwise(ex -> {
        LOGGER.warn(ex.getMessage());
        throw new ValidationException();
      }).mapEmpty();
  }
  protected Future<Void> createBatch(String sql, List<Tuple> params) {
    return conn
      .preparedQuery(sql)
      .executeBatch(params)
      .otherwise(ex -> {
        LOGGER.warn(ex.getMessage());
        throw new ValidationException();
      }).mapEmpty();
  }

  protected Future<T> find(String sql, Tuple params, Function<Row, T> mapper) {
    return conn
      .preparedQuery(sql)
      .execute(params)
      .map(rs -> {
        if (rs.rowCount() != 1) {
          throw new NotFoundException();
        }
        return mapper.apply(rs.iterator().next());
      });
  }

  protected Future<Integer> count(String sql) {
    return conn
      .preparedQuery(sql)
      .execute()
      .map(rs -> {
        if (rs.rowCount() != 1) {
          throw new NotFoundException();
        }
        return rs.iterator().next().getInteger(0);
      });
  }

  protected <U> Future<List<U>> list(String sql, Tuple params, Function<Row, U> mapper) {
    return conn
      .preparedQuery(sql)
      .execute(params)
      .map(rows -> {
        List<U> result = new ArrayList<>();
        for(Row row: rows) {
          result.add(mapper.apply(row));
        }
        return result;
      });
  }

  protected UUID[] toArray(List<UUID> list) {
    UUID[] itemsArray = new UUID[list.size()];
    return list.toArray(itemsArray);
  }

  protected UUID[] toArray(Collection<UUID> list) {
    UUID[] itemsArray = new UUID[list.size()];
    return list.toArray(itemsArray);
  }


  protected Future<Void> update(String sql, List<Tuple> params) {
    return conn
      .preparedQuery(sql)
      .executeBatch(params)
      .mapEmpty();
  }

  protected Future<Void> updateBulk(String sql, Tuple params) {
    return conn
      .preparedQuery(sql)
      .execute(params)
      .mapEmpty();
  }


  protected <U> Future<List<U>> updateReturning(String sql, List<Tuple> params, Function<Row, U> mapper) {
    return conn
      .preparedQuery(sql)
      .executeBatch(params)
      .map(rows -> {
        List<U> result = new ArrayList<>();
        for(Row row: rows) {
          result.add(mapper.apply(row));
        }
        return result;
      });
  }

}

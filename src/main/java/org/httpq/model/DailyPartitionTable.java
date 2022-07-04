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
package org.httpq.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public record DailyPartitionTable(String tableName, LocalDate day) {
  public static DailyPartitionTable parse(String tableName) {
    String[] parts = tableName.split("__");
    if (parts.length != 2) {
      throw new IllegalArgumentException(tableName + " is not a valid partition name");
    }
    return new DailyPartitionTable(parts[0], LocalDate.parse(parts[1].replaceAll("_", "-")));
  }
  public String partitionName() {

    String suffix = DateTimeFormatter.ofPattern("yyyy_MM_dd").format(day);
    return "%s__%s".formatted(tableName, suffix);
  }

  public String from() {
    return DateTimeFormatter.ISO_DATE.format(day);
  }

  public String to() {
    return DateTimeFormatter.ISO_DATE.format(day.plus(1, ChronoUnit.DAYS));
  }

}

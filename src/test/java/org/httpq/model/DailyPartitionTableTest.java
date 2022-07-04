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

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class DailyPartitionTableTest {

  @Test
  public void test_partition_name_zero_prefix() {
    LocalDate now = LocalDate.parse("2020-01-01");
    DailyPartitionTable partition = new DailyPartitionTable("table", now);
    assertEquals("table__2020_01_01", partition.partitionName());
    assertEquals("2020-01-01", partition.from());
    assertEquals("2020-01-02", partition.to());

    assertEquals(DailyPartitionTable.parse("table__2020_01_01"), partition);

    assertThrows(IllegalArgumentException.class, () -> {
      DailyPartitionTable.parse("boo");
    });

    assertThrows(DateTimeParseException.class, () -> {
      DailyPartitionTable.parse("table__2020_1_1");
    });

    assertThrows(DateTimeParseException.class, () -> {
      DailyPartitionTable.parse("table__2020_13_1");
    });
  }

  @Test
  public void test_partition_name() {
    LocalDate now = LocalDate.parse("2021-12-31");
    DailyPartitionTable partition = new DailyPartitionTable("table", now);
    assertEquals("table__2021_12_31", partition.partitionName());
    assertEquals("2021-12-31", partition.from());
    assertEquals("2022-01-01", partition.to());

    assertEquals(DailyPartitionTable.parse("table__2021_12_31"), partition);

  }

}

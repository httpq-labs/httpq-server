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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class StateReconciliationSetTest {
  @Test
  public void test_empty() {
    assertSet(Set.of(), Set.of(), new StateReconciliationSet<>(Set.of(), Set.of()));
  }

  @Test
  public void test_add() {
    assertSet(Set.of(), Set.of(1,2,3), new StateReconciliationSet<>(Set.of(1,2,3), Set.of()));
  }

  @Test
  public void test_remove_one() {
    assertSet(Set.of(1,2,3), Set.of(1,3), new StateReconciliationSet<>(Set.of(), Set.of(2)));
  }

  @Test
  public void test_remove_multiple() {
    assertSet(Set.of(1,2,3), Set.of(1), new StateReconciliationSet<>(Set.of(), Set.of(2,3)));
  }

  @Test
  public void test_reconcile() {
    assertSet(Set.of(1,2,3), Set.of(2,3,4,5), new StateReconciliationSet<>(Set.of(4,5), Set.of(1)));
  }

  private void assertSet(Set<Integer> current, Set<Integer> desired, StateReconciliationSet<Integer> expected) {
    assertEquals(expected, StateReconciliationSet.make(current, desired));
  }
}

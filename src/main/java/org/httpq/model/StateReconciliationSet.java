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

import java.util.Set;
import java.util.stream.Collectors;

public record StateReconciliationSet<T>(Set<T> toAdd, Set<T> toRemove) {
  public static <T> StateReconciliationSet<T> make(Set<T> currentState, Set<T> desiredState) {
    Set<T> partitionsToAdd = desiredState.stream().filter(e -> !currentState.contains(e)).collect(Collectors.toSet());
    Set<T> partitionsToRemove = currentState.stream().filter(e -> !desiredState.contains(e)).collect(Collectors.toSet());
    return new StateReconciliationSet<T>(partitionsToAdd, partitionsToRemove);
  }
}

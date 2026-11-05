package com.florachain.delivery;

import java.util.List;
import java.util.Optional;

/** 派单策略接口（FR-D02）。 */
public interface DispatchStrategy {
    Optional<Courier> assign(String area, List<Courier> couriers);
}

package com.florachain.delivery;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** 同区域内选当前任务最少的配送员。 */
public class AreaDispatchStrategy implements DispatchStrategy {
    @Override
    public Optional<Courier> assign(String area, List<Courier> couriers) {
        return couriers.stream()
                .filter(c -> c.area().equals(area))
                .min(Comparator.comparingInt(Courier::load));
    }
}

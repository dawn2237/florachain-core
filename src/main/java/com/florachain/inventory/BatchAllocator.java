package com.florachain.inventory;

import com.florachain.common.BusinessException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 出库分配：优先使用最早到期的批次（先进先出），已过期批次不参与。 */
public class BatchAllocator {
    public record Allocation(String batchId, int quantity) {
    }

    public List<Allocation> allocate(List<FlowerBatch> batches, int quantity, LocalDate today) {
        List<FlowerBatch> candidates = batches.stream()
                .filter(b -> b.remaining() > 0 && !b.expireDate().isBefore(today))
                .sorted(Comparator.comparing(FlowerBatch::expireDate))
                .toList();
        List<Allocation> result = new ArrayList<>();
        int need = quantity;
        for (FlowerBatch b : candidates) {
            if (need == 0) {
                break;
            }
            int take = Math.min(need, b.remaining());
            result.add(new Allocation(b.batchId(), take));
            need -= take;
        }
        if (need > 0) {
            throw new BusinessException(4100, "可用库存不足，缺少" + need);
        }
        return result;
    }
}

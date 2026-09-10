package com.qa.bolags.constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Computes per-shareholder share counts from percentages of company total shares.
 * Last row receives the remainder so allocated shares always equal total (e.g. 70+20+10 of 100).
 */
public final class ShareholderAllocationPlan {

    private final List<ShareholderAllocation> allocations;

    private ShareholderAllocationPlan(List<ShareholderAllocation> allocations) {
        this.allocations = allocations;
    }

    public static ShareholderAllocationPlan fromPercentages(
            int totalShares, List<ShareholderVariantType> types, List<Integer> percents) {
        if (types.size() != percents.size() || types.isEmpty()) {
            throw new IllegalArgumentException("types and percents must be same non-empty size");
        }
        int percentSum = percents.stream().mapToInt(Integer::intValue).sum();
        if (percentSum != 100) {
            throw new IllegalArgumentException("share percentages must sum to 100, was " + percentSum);
        }

        List<ShareholderAllocation> result = new ArrayList<>();
        int allocated = 0;
        for (int i = 0; i < types.size(); i++) {
            int shares;
            if (i == types.size() - 1) {
                shares = totalShares - allocated;
            } else {
                shares = (totalShares * percents.get(i)) / 100;
                allocated += shares;
            }
            if (shares <= 0) {
                throw new IllegalArgumentException(
                        "Computed non-positive share count for " + types.get(i) + ": " + shares
                                + " (totalShares=" + totalShares + ")");
            }
            result.add(new ShareholderAllocation(types.get(i), shares, percents.get(i)));
        }
        return new ShareholderAllocationPlan(result);
    }

    public List<ShareholderAllocation> allocations() {
        return Collections.unmodifiableList(allocations);
    }

    public static final class ShareholderAllocation {
        private final ShareholderVariantType type;
        private final int shares;
        private final int percent;

        public ShareholderAllocation(ShareholderVariantType type, int shares, int percent) {
            this.type = type;
            this.shares = shares;
            this.percent = percent;
        }

        public ShareholderVariantType getType() {
            return type;
        }

        public int getShares() {
            return shares;
        }

        public int getPercent() {
            return percent;
        }
    }
}

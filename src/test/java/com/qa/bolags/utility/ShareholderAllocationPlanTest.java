package com.qa.bolags.utility;

import com.qa.bolags.constants.ShareholderAllocationPlan;
import com.qa.bolags.constants.ShareholderVariantType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class ShareholderAllocationPlanTest {

    @Test
    public void splitsHundredSharesSeventyTwentyTen() {
        List<ShareholderVariantType> types = Arrays.asList(
                ShareholderVariantType.SWEDISH_PERSON,
                ShareholderVariantType.SWEDISH_LEGAL_ENTITY,
                ShareholderVariantType.FOREIGN_PERSON);
        List<Integer> percents = Arrays.asList(70, 20, 10);

        ShareholderAllocationPlan plan = ShareholderAllocationPlan.fromPercentages(100, types, percents);

        Assert.assertEquals(plan.allocations().get(0).getShares(), 70);
        Assert.assertEquals(plan.allocations().get(1).getShares(), 20);
        Assert.assertEquals(plan.allocations().get(2).getShares(), 10);
        Assert.assertEquals(
                plan.allocations().stream().mapToInt(a -> a.getShares()).sum(),
                100);
    }

    @Test
    public void lastShareholderReceivesRemainderForOddTotals() {
        List<ShareholderVariantType> types = Arrays.asList(
                ShareholderVariantType.SWEDISH_PERSON,
                ShareholderVariantType.SWEDISH_LEGAL_ENTITY,
                ShareholderVariantType.FOREIGN_PERSON);
        List<Integer> percents = Arrays.asList(70, 20, 10);

        ShareholderAllocationPlan plan = ShareholderAllocationPlan.fromPercentages(1000, types, percents);

        Assert.assertEquals(plan.allocations().get(0).getShares(), 700);
        Assert.assertEquals(plan.allocations().get(1).getShares(), 200);
        Assert.assertEquals(plan.allocations().get(2).getShares(), 100);
    }
}

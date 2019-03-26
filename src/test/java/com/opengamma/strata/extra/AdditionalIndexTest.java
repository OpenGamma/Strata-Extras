/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FloatingRateName;
import com.opengamma.strata.basics.index.IborIndex;

/**
 * Test.
 */
@Test
public class AdditionalIndexTest {

  public void testIlsTelebor() {
    IborIndex index = IborIndex.of("ILS-TELBOR-3M");
    assertEquals(index.getCurrency(), Currency.ILS);
    assertEquals(index.getDayCount(), DayCounts.ACT_360);
  }

  public void testIlsTelebor_frName() {
    FloatingRateName frName = FloatingRateName.of("ILS-TELBOR");
    assertEquals(frName.getCurrency(), Currency.ILS);
    assertEquals(frName.getTenors(), ImmutableSet.of(Tenor.TENOR_3M));
  }

}

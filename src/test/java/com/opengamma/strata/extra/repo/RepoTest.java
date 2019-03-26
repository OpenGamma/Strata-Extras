/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.extra.bondcurve.SimpleLegalEntitySecurity;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;

/**
 * Test {@link Repo}.
 */
@Test
public class RepoTest {

  private static final LocalDate START_DATE = LocalDate.of(2017, 2, 6);
  private static final LocalDate END_DATE = LocalDate.of(2017, 2, 20);
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(FOLLOWING, USNY);
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG", "ABC");
  private static final SecurityId SECURITY_ID_1 = SecurityId.of("OG", "bond1");
  private static final SecurityId SECURITY_ID_2 = SecurityId.of("OG", "bond2");
  private static final SecurityPosition COLLATERAL_1 = SecurityPosition.ofNet(SECURITY_ID_1, 1d);
  private static final SecurityPosition COLLATERAL_2 = SecurityPosition.ofNet(SECURITY_ID_2, 2d);
  private static final SimpleLegalEntitySecurity SECURITY = SimpleLegalEntitySecurity.of(ISSUER_ID);
  private static final double NOTIONAL = 3_000_000;
  private static final double RATE = 0.005;
  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ReferenceData.of(ImmutableMap.of(SECURITY_ID_1, SECURITY, SECURITY_ID_2, SECURITY)));

  public void test_builder() {
    Repo test = Repo.builder()
        .collateral(COLLATERAL_1, COLLATERAL_2)
        .currency(USD)
        .businessDayAdjustment(BDA)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .buySell(BUY)
        .dayCount(ACT_365F)
        .notional(NOTIONAL)
        .rate(RATE)
        .build();
    assertEquals(test.getBusinessDayAdjustment().get(), BDA);
    assertEquals(test.getBuySell(), BUY);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getCollateral(), ImmutableList.of(COLLATERAL_1, COLLATERAL_2));
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getRate(), RATE);
  }

  public void test_builder_fail() {
    assertThrowsIllegalArg(() -> Repo.builder()
        .collateral(COLLATERAL_1, COLLATERAL_2)
        .currency(USD)
        .businessDayAdjustment(BDA)
        .startDate(START_DATE)
        .endDate(START_DATE.minusWeeks(1))
        .buySell(BUY)
        .dayCount(ACT_365F)
        .notional(NOTIONAL)
        .rate(RATE)
        .build());
  }

  public void test_resolve() {
    Repo base = Repo.builder()
        .collateral(COLLATERAL_1, COLLATERAL_2)
        .currency(USD)
        .businessDayAdjustment(BDA)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .buySell(SELL)
        .dayCount(ACT_365F)
        .notional(NOTIONAL)
        .rate(RATE)
        .build();
    ResolvedRepo computed = base.resolve(REF_DATA);
    ResolvedRepo expected = ResolvedRepo.builder()
        .legalEntityId(ISSUER_ID)
        .securityIds(SECURITY_ID_1, SECURITY_ID_2)
        .currency(USD)
        .startDate(START_DATE)
        .endDate(BDA.adjust(END_DATE, REF_DATA))
        .yearFraction(ACT_365F.relativeYearFraction(START_DATE, BDA.adjust(END_DATE, REF_DATA)))
        .notional(-NOTIONAL)
        .rate(RATE)
        .build();
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Repo test1 = Repo.builder()
        .collateral(COLLATERAL_1, COLLATERAL_2)
        .currency(USD)
        .businessDayAdjustment(BDA)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .buySell(BUY)
        .dayCount(ACT_365F)
        .notional(NOTIONAL)
        .rate(RATE)
        .build();
    coverImmutableBean(test1);
    Repo test2 = Repo.builder()
        .collateral(COLLATERAL_1)
        .currency(GBP)
        .businessDayAdjustment(BusinessDayAdjustment.NONE)
        .startDate(LocalDate.of(2017, 2, 7))
        .endDate(LocalDate.of(2017, 2, 14))
        .buySell(SELL)
        .dayCount(ACT_360)
        .notional(1.0e6)
        .rate(0.01)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    Repo test = Repo.builder()
        .collateral(COLLATERAL_1, COLLATERAL_2)
        .currency(USD)
        .businessDayAdjustment(BDA)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .buySell(BUY)
        .dayCount(ACT_365F)
        .notional(NOTIONAL)
        .rate(RATE)
        .build();
    assertSerialization(test);
  }

}

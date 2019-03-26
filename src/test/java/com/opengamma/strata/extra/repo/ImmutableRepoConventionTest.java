/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * Test {@link ImmutableRepoConvention}.
 */
@Test
public class ImmutableRepoConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA);
  private static final SecurityId SECURITY_ID = SecurityId.of(StandardId.of("OG", "bond"));
  private static final SecurityPosition COLLATERAL = SecurityPosition.ofNet(SECURITY_ID, 1d);

  public void test_builder_full() {
    ImmutableRepoConvention test = ImmutableRepoConvention.builder()
        .name("Test")
        .businessDayAdjustment(BDA_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_ONE_DAY)
        .build();
    assertEquals(test.getName(), "Test");
    assertEquals(test.getBusinessDayAdjustment(), BDA_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  public void test_of() {
    ImmutableRepoConvention test = ImmutableRepoConvention.of(
        "Test", EUR, BDA_FOLLOW, ACT_360, PLUS_ONE_DAY);
    assertEquals(test.getName(), "Test");
    assertEquals(test.getBusinessDayAdjustment(), BDA_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  public void test_toTrade() {
    ImmutableRepoConvention convention = ImmutableRepoConvention.builder()
        .name("Test")
        .businessDayAdjustment(BDA_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_ONE_DAY)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    Tenor period3M = Tenor.ofMonths(3);
    BuySell buy = BuySell.BUY;
    double notional = 2_000_000d;
    double rate = 0.0125;
    RepoTrade trade = convention.createTrade(
        tradeDate, period3M, ImmutableList.of(COLLATERAL), buy, notional, rate, REF_DATA);
    LocalDate startDateExpected = PLUS_ONE_DAY.adjust(tradeDate, REF_DATA);
    LocalDate endDateExpected = startDateExpected.plus(period3M);
    Repo repoExpected = Repo.builder()
        .buySell(buy)
        .currency(EUR)
        .collateral(ImmutableList.of(COLLATERAL))
        .notional(notional)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .businessDayAdjustment(BDA_FOLLOW)
        .rate(rate)
        .dayCount(ACT_360)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.of(tradeDate);
    assertEquals(trade.getProduct(), repoExpected);
    assertEquals(trade.getInfo(), tradeInfoExpected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableRepoConvention test1 = ImmutableRepoConvention.of(
        "Test", EUR, BDA_FOLLOW, ACT_360, PLUS_ONE_DAY);
    coverImmutableBean(test1);
    ImmutableRepoConvention test2 = ImmutableRepoConvention.of(
        "Test2", GBP, BusinessDayAdjustment.of(FOLLOWING, GBLO), ACT_365F, DaysAdjustment.ofBusinessDays(1, GBLO));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ImmutableRepoConvention test = ImmutableRepoConvention.of(
        "Test", EUR, BDA_FOLLOW, ACT_360, PLUS_ONE_DAY);
    assertSerialization(test);
  }

}

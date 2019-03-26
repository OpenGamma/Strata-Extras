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
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1W;
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
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * Test {@link RepoTemplate}.
 */
@Test
public class RepoTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA);
  private static final SecurityId SECURITY_ID = SecurityId.of(StandardId.of("OG", "bond"));
  private static final ImmutableList<SecurityPosition> COLLATERAL =
      ImmutableList.of(SecurityPosition.ofNet(SECURITY_ID, 1d));
  private static final String NAME = "CONV";
  private static final RepoConvention CONVENTION =
      ImmutableRepoConvention.of(NAME, EUR, BDA_FOLLOW, ACT_360, PLUS_ONE_DAY);

  public void test_builder() {
    RepoTemplate test = RepoTemplate.builder()
        .convention(CONVENTION)
        .tenor(TENOR_1M)
        .collateral(COLLATERAL)
        .build();
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getTenor(), TENOR_1M);
    assertEquals(test.getCollateral(), COLLATERAL);
  }

  public void test_of() {
    RepoTemplate test = RepoTemplate.of(TENOR_1M, COLLATERAL, CONVENTION);
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getTenor(), TENOR_1M);
    assertEquals(test.getCollateral(), COLLATERAL);
  }

  public void test_createTrade() {
    RepoTemplate template = RepoTemplate.of(TENOR_1M, COLLATERAL, CONVENTION);
    LocalDate tradeDate = LocalDate.of(2015, 1, 23);
    BuySell buy = BuySell.BUY;
    double notional = 2_000_000d;
    double rate = 0.0125;
    RepoTrade trade = template.createTrade(tradeDate, buy, notional, rate, REF_DATA);
    TradeInfo tradeInfoExpected = TradeInfo.of(tradeDate);
    LocalDate startDateExpected = PLUS_ONE_DAY.adjust(tradeDate, REF_DATA);
    LocalDate endDateExpected = startDateExpected.plus(TENOR_1M);
    Repo productExpected = Repo.builder()
        .buySell(buy)
        .currency(EUR)
        .collateral(COLLATERAL)
        .notional(notional)
        .businessDayAdjustment(BDA_FOLLOW)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .rate(rate)
        .dayCount(ACT_360)
        .build();
    assertEquals(trade.getInfo(), tradeInfoExpected);
    assertEquals(trade.getProduct(), productExpected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RepoTemplate test1 = RepoTemplate.of(TENOR_1M, COLLATERAL, CONVENTION);
    coverImmutableBean(test1);
    RepoTemplate test2 = RepoTemplate.builder()
        .tenor(TENOR_1W)
        .collateral(SecurityPosition.ofNet(SecurityId.of(StandardId.of("OG", "bond1")), 1d))
        .convention(ImmutableRepoConvention.of(
            NAME,
            GBP,
            BusinessDayAdjustment.of(FOLLOWING, GBLO),
            ACT_360,
            DaysAdjustment.ofBusinessDays(1, GBLO)))
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    RepoTemplate test = RepoTemplate.of(TENOR_1M, COLLATERAL, CONVENTION);
    assertSerialization(test);
  }

}

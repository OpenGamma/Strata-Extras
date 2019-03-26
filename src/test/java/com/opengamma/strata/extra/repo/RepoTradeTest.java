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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.extra.bondcurve.SimpleLegalEntitySecurity;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link RepoTrade}.
 */
@Test
public class RepoTradeTest {

  private static final LocalDate TRADE_DATE = LocalDate.of(2017, 2, 3);
  private static final LocalDate START_DATE = LocalDate.of(2017, 2, 6);
  private static final LocalDate END_DATE = LocalDate.of(2017, 2, 20);
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(FOLLOWING, USNY);
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG", "ABC");
  private static final SecurityId SECURITY_ID = SecurityId.of("OG", "bond");
  private static final SecurityPosition COLLATERAL = SecurityPosition.ofNet(SECURITY_ID, 2d);
  private static final SimpleLegalEntitySecurity SECURITY = SimpleLegalEntitySecurity.of(ISSUER_ID);
  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ReferenceData.of(ImmutableMap.of(SECURITY_ID, SECURITY)));
  private static final double NOTIONAL = 2_000_000;
  private static final double RATE = 0.005;
  private static final Repo PRODUCT = Repo.builder()
      .collateral(COLLATERAL)
      .currency(USD)
      .businessDayAdjustment(BDA)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .buySell(BUY)
      .dayCount(ACT_365F)
      .notional(NOTIONAL)
      .rate(RATE)
      .build();
  private static final TradeInfo INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .build();

  public void test_builder() {
    RepoTrade test = RepoTrade.builder()
        .product(PRODUCT)
        .info(INFO)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), INFO);
  }

  public void test_resolved() {
    RepoTrade base = RepoTrade.builder()
        .product(PRODUCT)
        .info(INFO)
        .build();
    ResolvedRepoTrade computed = base.resolve(REF_DATA);
    ResolvedRepoTrade expected = ResolvedRepoTrade.of(INFO, PRODUCT.resolve(REF_DATA));
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RepoTrade test1 = RepoTrade.builder()
        .product(PRODUCT)
        .build();
    coverImmutableBean(test1);
    Repo product = Repo.builder()
        .collateral(COLLATERAL)
        .currency(GBP)
        .businessDayAdjustment(BusinessDayAdjustment.NONE)
        .startDate(LocalDate.of(2017, 2, 7))
        .endDate(LocalDate.of(2017, 2, 14))
        .buySell(SELL)
        .dayCount(ACT_360)
        .notional(1.0e6)
        .rate(0.01)
        .build();
    RepoTrade test2 = RepoTrade.of(INFO, product);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    RepoTrade test = RepoTrade.builder()
        .product(PRODUCT)
        .info(INFO)
        .build();
    assertSerialization(test);
  }

}

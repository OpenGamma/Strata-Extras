/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedRepoTrade}.
 */
@Test
public class ResolvedRepoTradeTest {

  private static final LocalDate START_DATE = LocalDate.of(2017, 2, 7);
  private static final LocalDate END_DATE = LocalDate.of(2017, 5, 7);
  private static final LocalDate TRADE_DATE = LocalDate.of(2017, 2, 3);
  private static final double YEAR_FRACTION = 0.25d;
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG", "ABC");
  private static final SecurityId SECURITY_ID = SecurityId.of("OG", "bond");
  private static final double NOTIONAL = 15_000_000;
  private static final double RATE = 0.005;
  private static final ResolvedRepo PRODUCT = ResolvedRepo.builder()
      .currency(USD)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .legalEntityId(ISSUER_ID)
      .securityIds(SECURITY_ID)
      .notional(NOTIONAL)
      .rate(RATE)
      .yearFraction(YEAR_FRACTION)
      .build();
  private static final TradeInfo INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .build();

  public void test_builder() {
    ResolvedRepoTrade test = ResolvedRepoTrade.builder()
        .product(PRODUCT)
        .info(INFO)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), INFO);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedRepoTrade test1 = ResolvedRepoTrade.builder()
        .product(PRODUCT)
        .build();
    coverImmutableBean(test1);
    ResolvedRepo product = ResolvedRepo.builder()
        .currency(GBP)
        .startDate(LocalDate.of(2017, 2, 8))
        .endDate(LocalDate.of(2017, 2, 15))
        .legalEntityId(LegalEntityId.of("OG", "DEF"))
        .securityIds(SecurityId.of(StandardId.of("OG", "bond1")))
        .notional(-1.0e6)
        .rate(0.01)
        .yearFraction(0.0384)
        .build();
    ResolvedRepoTrade test2 = ResolvedRepoTrade.of(INFO, product);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedRepoTrade test = ResolvedRepoTrade.builder()
        .product(PRODUCT)
        .info(INFO)
        .build();
    assertSerialization(test);
  }

}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;

import java.time.LocalDate;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.extra.repo.Repo;
import com.opengamma.strata.extra.repo.RepoTrade;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingMarketDataLookup;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test data for repo trade calculations.
 */
public class RepoTradeTestData {
  /** reference data */
  public static final LocalDate VAL_DATE = date(2017, 1, 20);
  private static final LocalDate START_DATE = date(2017, 1, 24);
  private static final LocalDate END_DATE = date(2017, 3, 24);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0075;
  public static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG", "ABC");
  private static final SecurityId SECURITY_ID = SecurityId.of("OG", "bond");
  private static final SimpleLegalEntitySecurity SECURITY = SimpleLegalEntitySecurity.of(ISSUER_ID);
  private static final SecurityPosition COLLATERAL = SecurityPosition.ofNet(SECURITY_ID, 1d);
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final Repo PRODUCT = Repo.builder()
      .collateral(COLLATERAL)
      .currency(EUR)
      .businessDayAdjustment(BDA)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .buySell(BUY)
      .dayCount(ACT_365F)
      .notional(NOTIONAL)
      .rate(RATE)
      .build();
  /** repo trade */
  public static final RepoTrade TRADE = RepoTrade.of(TradeInfo.empty(), PRODUCT);
  /** resolved repo trade */
  public static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ReferenceData.of(ImmutableMap.of(SECURITY_ID, SECURITY)));
  public static final ResolvedRepoTrade RESOLVED_TRADE = TRADE.resolve(REF_DATA);
  public static final RepoGroup REPO_GROUP = RepoGroup.of("ABC");
  private static final LegalEntityGroup ISSUER_GROUP = LegalEntityGroup.of("ABC");
  /** repo curve ID */
  public static final CurveId REPO_CURVE_ID = CurveId.of("Default", "Repo");
  /** issuer curve ID */
  public static final CurveId ISSUER_CURVE_ID = CurveId.of("Default", "Issuer");
  /** rate lookup */
  public static final LegalEntityDiscountingMarketDataLookup LOOKUP = LegalEntityDiscountingMarketDataLookup.of(
      ImmutableMap.of(ISSUER_ID, REPO_GROUP),
      ImmutableMap.of(Pair.of(REPO_GROUP, EUR), REPO_CURVE_ID),
      ImmutableMap.of(ISSUER_ID, ISSUER_GROUP),
      ImmutableMap.of(Pair.of(ISSUER_GROUP, EUR), ISSUER_CURVE_ID));
  /** market data */
  public static final ScenarioMarketData MARKET_DATA;
  static {
    Curve curveRepo = ConstantCurve.of(Curves.discountFactors("TestRepo", ACT_360), 0.99);
    Curve curveIssuer = ConstantCurve.of(Curves.discountFactors("TestIssuer", ACT_360), 0.9);
    MARKET_DATA = ScenarioMarketData.of(
        1, MarketData.of(VAL_DATE, ImmutableMap.of(REPO_CURVE_ID, curveRepo, ISSUER_CURVE_ID, curveIssuer)));
  }

}

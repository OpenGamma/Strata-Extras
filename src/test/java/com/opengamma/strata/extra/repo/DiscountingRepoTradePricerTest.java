/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.extra.bondcurve.SimpleLegalEntitySecurity;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link DiscountingRepoTradePricer}.
 */
@Test
public class DiscountingRepoTradePricerTest {

  private static final LocalDate VAL_DATE = date(2017, 1, 20);
  private static final LocalDate START_DATE = date(2017, 1, 24);
  private static final LocalDate END_DATE = date(2017, 2, 24);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0075;
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG", "ABC");
  private static final SecurityId SECURITY_ID = SecurityId.of("OG", "bond");
  private static final SecurityPosition COLLATERAL = SecurityPosition.ofNet(SECURITY_ID, 1d);
  private static final SimpleLegalEntitySecurity SECURITY = SimpleLegalEntitySecurity.of(ISSUER_ID);
  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ReferenceData.of(ImmutableMap.of(SECURITY_ID, SECURITY)));
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final ResolvedRepo PRODUCT = Repo.builder()
      .collateral(COLLATERAL)
      .currency(EUR)
      .businessDayAdjustment(BDA)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .buySell(BUY)
      .dayCount(ACT_365F)
      .notional(NOTIONAL)
      .rate(RATE)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedRepoTrade TRADE = ResolvedRepoTrade.of(TradeInfo.empty(), PRODUCT);

  private static final InterpolatedNodalCurve REPO_CURVE;
  static {
    DoubleArray times = DoubleArray.of(0.01, 0.25, 0.5);
    DoubleArray rates = DoubleArray.of(0.01, 0.006, 0.015);
    REPO_CURVE = InterpolatedNodalCurve.of(Curves.zeroRates("Repo", ACT_360), times, rates, DOUBLE_QUADRATIC);
  }
  private static final RepoGroup GROUP_REPO = RepoGroup.of("ABC");
  private static final DiscountFactors DSC_REPO = ZeroRateDiscountFactors.of(EUR, VAL_DATE, REPO_CURVE);
  private static final LegalEntityDiscountingProvider RATES_PROVIDER = ImmutableLegalEntityDiscountingProvider.builder()
      .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), DSC_REPO))
      .repoCurveGroups(ImmutableMap.of(ISSUER_ID, GROUP_REPO))
      .valuationDate(VAL_DATE)
      .build();

  private static final double TOLERANCE = 1.0E-14;
  private static final DiscountingRepoProductPricer PRODUCT_PRICER = DiscountingRepoProductPricer.DEFAULT;
  private static final DiscountingRepoTradePricer TRADE_PRICER = DiscountingRepoTradePricer.DEFAULT;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount pvTrade = TRADE_PRICER.presentValue(TRADE, RATES_PROVIDER);
    CurrencyAmount pvProduct = PRODUCT_PRICER.presentValue(PRODUCT, RATES_PROVIDER);
    assertEquals(pvTrade.getCurrency(), pvProduct.getCurrency());
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount(), TOLERANCE * NOTIONAL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivities ptsTrade = TRADE_PRICER.presentValueSensitivity(TRADE, RATES_PROVIDER);
    PointSensitivities ptsProduct = PRODUCT_PRICER.presentValueSensitivity(PRODUCT, RATES_PROVIDER);
    assertTrue(ptsTrade.equalWithTolerance(ptsProduct, TOLERANCE * NOTIONAL));
  }

  //-------------------------------------------------------------------------
  public void test_parRate() {
    double psTrade = TRADE_PRICER.parRate(TRADE, RATES_PROVIDER);
    double psProduct = PRODUCT_PRICER.parRate(PRODUCT, RATES_PROVIDER);
    assertEquals(psTrade, psProduct, TOLERANCE);

  }

  //-------------------------------------------------------------------------
  public void test_parRateSensitivity() {
    PointSensitivities ptsTrade = TRADE_PRICER.parRateSensitivity(TRADE, RATES_PROVIDER);
    PointSensitivities ptsProduct = PRODUCT_PRICER.parRateSensitivity(PRODUCT, RATES_PROVIDER);
    assertTrue(ptsTrade.equalWithTolerance(ptsProduct, TOLERANCE));
  }

  //-------------------------------------------------------------------------
  public void test_parSpread() {
    double psTrade = TRADE_PRICER.parSpread(TRADE, RATES_PROVIDER);
    double psProduct = PRODUCT_PRICER.parSpread(PRODUCT, RATES_PROVIDER);
    assertEquals(psTrade, psProduct, TOLERANCE);

  }

  //-------------------------------------------------------------------------
  public void test_parSpreadSensitivity() {
    PointSensitivities ptsTrade = TRADE_PRICER.parSpreadSensitivity(TRADE, RATES_PROVIDER);
    PointSensitivities ptsProduct = PRODUCT_PRICER.parSpreadSensitivity(PRODUCT, RATES_PROVIDER);
    assertTrue(ptsTrade.equalWithTolerance(ptsProduct, TOLERANCE));
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    assertEquals(
        TRADE_PRICER.currencyExposure(TRADE, RATES_PROVIDER),
        MultiCurrencyAmount.of(TRADE_PRICER.presentValue(TRADE, RATES_PROVIDER)));
  }

  public void test_currentCash_onStartDate() {
    LocalDate startDate = TRADE.getProduct().getStartDate();
    DiscountFactors dsc = ZeroRateDiscountFactors.of(EUR, startDate, REPO_CURVE);
    LegalEntityDiscountingProvider prov = ImmutableLegalEntityDiscountingProvider.builder()
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), dsc))
        .repoCurveSecurityGroups(ImmutableMap.of(SECURITY_ID, GROUP_REPO))
        .valuationDate(startDate)
        .build();
    assertEquals(TRADE_PRICER.currentCash(TRADE, prov), CurrencyAmount.of(EUR, -NOTIONAL));
  }

  public void test_currentCash_onEndDate() {
    LocalDate endDate = TRADE.getProduct().getEndDate();
    DiscountFactors dsc = ZeroRateDiscountFactors.of(EUR, endDate, REPO_CURVE);
    LegalEntityDiscountingProvider prov = ImmutableLegalEntityDiscountingProvider.builder()
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), dsc))
        .repoCurveSecurityGroups(ImmutableMap.of(SECURITY_ID, GROUP_REPO))
        .valuationDate(endDate)
        .build();
    assertEquals(TRADE_PRICER.currentCash(TRADE, prov),
        CurrencyAmount.of(EUR, NOTIONAL + RATE * NOTIONAL * TRADE.getProduct().getYearFraction()));
  }

  public void test_currentCash_otherDate() {
    assertEquals(TRADE_PRICER.currentCash(TRADE, RATES_PROVIDER), CurrencyAmount.zero(EUR));
  }

}

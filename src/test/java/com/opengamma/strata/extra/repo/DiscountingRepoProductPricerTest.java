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
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.extra.bondcurve.SimpleLegalEntitySecurity;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;

/**
 * Test {@link DiscountingRepoProductPricer}
 */
@Test
public class DiscountingRepoProductPricerTest {

  private static final LocalDate VAL_DATE = date(2017, 1, 20);
  private static final LocalDate START_DATE = date(2017, 1, 24);
  private static final LocalDate END_DATE = date(2017, 2, 24);
  private static final double NOTIONAL = 300000000d;
  private static final double RATE = 0.0075;
  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG", "ABC");
  private static final SecurityId SECURITY_ID_1 = SecurityId.of("OG", "bond1");
  private static final SecurityId SECURITY_ID_2 = SecurityId.of("OG", "bond2");
  private static final SecurityId SECURITY_ID_3 = SecurityId.of("OG", "bond3");
  private static final SecurityPosition COLLATERAL_1 = SecurityPosition.ofNet(SECURITY_ID_1, 1d);
  private static final SecurityPosition COLLATERAL_2 = SecurityPosition.ofNet(SECURITY_ID_2, 1d);
  private static final SecurityPosition COLLATERAL_3 = SecurityPosition.ofNet(SECURITY_ID_3, 1d);
  private static final SimpleLegalEntitySecurity SECURITY = SimpleLegalEntitySecurity.of(ISSUER_ID);
  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ReferenceData.of(ImmutableMap.of(SECURITY_ID_1, SECURITY, SECURITY_ID_2, SECURITY, SECURITY_ID_3, SECURITY)));
  private static final BusinessDayAdjustment BDA = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final ResolvedRepo PRODUCT = Repo.builder()
      .collateral(COLLATERAL_1, COLLATERAL_2, COLLATERAL_3)
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

  private static final InterpolatedNodalCurve REPO_CURVE;
  static {
    DoubleArray times = DoubleArray.of(0.01, 0.25, 0.5);
    DoubleArray rates = DoubleArray.of(0.01, 0.006, 0.015);
    REPO_CURVE = InterpolatedNodalCurve.of(Curves.zeroRates("Repo", ACT_360), times, rates, DOUBLE_QUADRATIC);
  }
  private static final RepoGroup GROUP_REPO = RepoGroup.of("ABC");

  private static final double TOLERANCE = 1.0E-14;
  private static final double EPS_FD = 1.0E-7;
  private static final DiscountingRepoProductPricer PRICER = DiscountingRepoProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  public void test_presentValue_notStarted() {
    LegalEntityDiscountingProvider prov = createProvider(VAL_DATE);
    CurrencyAmount computed = PRICER.presentValue(PRODUCT, prov);
    DiscountFactors dsc = createDiscountFactors(VAL_DATE);
    double expected = ((1d + RATE * PRODUCT.getYearFraction()) * dsc.discountFactor(PRODUCT.getEndDate()) -
        dsc.discountFactor(PRODUCT.getStartDate())) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_onStart() {
    LegalEntityDiscountingProvider prov = createProvider(START_DATE);
    CurrencyAmount computed = PRICER.presentValue(PRODUCT, prov);
    DiscountFactors dsc = createDiscountFactors(START_DATE);
    double expected = ((1d + RATE * PRODUCT.getYearFraction()) * dsc.discountFactor(PRODUCT.getEndDate()) - 1d) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_started() {
    LocalDate valDate = date(2017, 2, 15);
    LegalEntityDiscountingProvider prov = createProvider(valDate);
    CurrencyAmount computed = PRICER.presentValue(PRODUCT, prov);
    DiscountFactors dsc = createDiscountFactors(valDate);
    double expected = (1d + RATE * PRODUCT.getYearFraction()) * dsc.discountFactor(PRODUCT.getEndDate()) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_onEnd() {
    LegalEntityDiscountingProvider prov = createProvider(END_DATE);
    CurrencyAmount computed = PRICER.presentValue(PRODUCT, prov);
    double expected = (1d + RATE * PRODUCT.getYearFraction()) * NOTIONAL;
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE * NOTIONAL);
  }

  public void test_presentValue_ended() {
    LegalEntityDiscountingProvider prov = createProvider(date(2017, 9, 27));
    CurrencyAmount computed = PRICER.presentValue(PRODUCT, prov);
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), 0.0d, TOLERANCE * NOTIONAL);
  }

  public void test_presentValueSensitivity() {
    LegalEntityDiscountingProvider prov = createProvider(VAL_DATE);
    PointSensitivities computed = PRICER.presentValueSensitivity(PRODUCT, prov);
    CurrencyParameterSensitivities sensiComputed = prov.parameterSensitivity(computed);
    CurrencyParameterSensitivities sensiExpected =
        CAL_FD.sensitivity(prov, (p) -> PRICER.presentValue(PRODUCT, (p)));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD));
  }

  public void test_parRate() {
    LegalEntityDiscountingProvider prov = createProvider(VAL_DATE);
    double parRate = PRICER.parRate(PRODUCT, prov);
    ResolvedRepo parProduct = Repo.builder()
        .collateral(COLLATERAL_1, COLLATERAL_2, COLLATERAL_3)
        .currency(EUR)
        .businessDayAdjustment(BDA)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .buySell(BUY)
        .dayCount(ACT_365F)
        .notional(NOTIONAL)
        .rate(parRate)
        .build()
        .resolve(REF_DATA);
    double pvPar = PRICER.presentValue(parProduct, prov).getAmount();
    assertEquals(pvPar, 0.0, NOTIONAL * TOLERANCE);
  }

  public void test_parSpread() {
    LegalEntityDiscountingProvider prov = createProvider(VAL_DATE);
    double parSpread = PRICER.parSpread(PRODUCT, prov);
    ResolvedRepo parProduct = Repo.builder()
        .collateral(COLLATERAL_1, COLLATERAL_2, COLLATERAL_3)
        .currency(EUR)
        .businessDayAdjustment(BDA)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .buySell(BUY)
        .dayCount(ACT_365F)
        .notional(NOTIONAL)
        .rate(RATE + parSpread)
        .build()
        .resolve(REF_DATA);
    double pvPar = PRICER.presentValue(parProduct, prov).getAmount();
    assertEquals(pvPar, 0.0, NOTIONAL * TOLERANCE);
  }

  public void test_parSpreadSensitivity() {
    LegalEntityDiscountingProvider prov = createProvider(VAL_DATE);
    PointSensitivities computed = PRICER.parSpreadSensitivity(PRODUCT, prov);
    CurrencyParameterSensitivities sensiComputed = prov.parameterSensitivity(computed);
    CurrencyParameterSensitivities sensiExpected =
        CAL_FD.sensitivity(prov, (p) -> CurrencyAmount.of(EUR, PRICER.parSpread(PRODUCT, (p))));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD));
  }

  public void test_parRateSensitivity() {
    LegalEntityDiscountingProvider prov = createProvider(VAL_DATE);
    PointSensitivities computedSpread = PRICER.parSpreadSensitivity(PRODUCT, prov);
    PointSensitivities computedRate = PRICER.parRateSensitivity(PRODUCT, prov);
    assertTrue(computedSpread.equalWithTolerance(computedRate, NOTIONAL * EPS_FD));
  }

  //-------------------------------------------------------------------------
  private static LegalEntityDiscountingProvider createProvider(LocalDate valuationDate) {
    DiscountFactors dscRepo = createDiscountFactors(valuationDate);
    LegalEntityDiscountingProvider provider = ImmutableLegalEntityDiscountingProvider.builder()
        .repoCurves(ImmutableMap.of(Pair.of(GROUP_REPO, EUR), dscRepo))
        .repoCurveGroups(ImmutableMap.of(ISSUER_ID, GROUP_REPO))
        .valuationDate(valuationDate)
        .build();
    return provider;
  }

  private static DiscountFactors createDiscountFactors(LocalDate valuationDate) {
    return ZeroRateDiscountFactors.of(EUR, valuationDate, REPO_CURVE);
  }

}

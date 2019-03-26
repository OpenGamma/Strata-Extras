/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.ISSUER_ID;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.REPO_CURVE_ID;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.REPO_GROUP;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.RESOLVED_TRADE;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.VAL_DATE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.extra.repo.DiscountingRepoTradePricer;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.SwapDummyData;

/**
 * Test {@link LegalEntityDiscountingCalibrationMeasures}.
 * <p>
 * Implementations of {@link LegalEntityDiscountingCalibrationMeasure} are also tested.
 */
@Test
public class LegalEntityDiscountingCalibrationMeasuresTest {

  private static final CurveName REPO_CURVE_NAME = REPO_CURVE_ID.getCurveName();
  private static final ImmutableList<CurveParameterSize> CURVE_ORDER = ImmutableList.of(
      CurveParameterSize.of(REPO_CURVE_NAME, 3));
  private static final InterpolatedNodalCurve REPO_CURVE;
  static {
    DoubleArray times = DoubleArray.of(0.01, 0.25, 0.5);
    DoubleArray rates = DoubleArray.of(0.01, 0.006, 0.015);
    JacobianCalibrationMatrix jac = JacobianCalibrationMatrix.of(
        CURVE_ORDER,
        DoubleMatrix.copyOf(new double[][] {{1d, 0.2d, 0.1d}, {0.1d, 1d, 0.2d}, {0.1d, 0.2d, 1d}}));
    CurveMetadata metadata = Curves.zeroRates(REPO_CURVE_NAME, ACT_360).withInfo(CurveInfoType.JACOBIAN, jac);
    REPO_CURVE = InterpolatedNodalCurve.of(metadata, times, rates, DOUBLE_QUADRATIC);
  }
  private static final LegalEntityDiscountingProvider PROVIDER = ImmutableLegalEntityDiscountingProvider.builder()
      .repoCurves(ImmutableMap.of(Pair.of(REPO_GROUP, EUR), ZeroRateDiscountFactors.of(EUR, VAL_DATE, REPO_CURVE)))
      .repoCurveGroups(ImmutableMap.of(ISSUER_ID, REPO_GROUP))
      .valuationDate(VAL_DATE)
      .build();
  private static final DiscountingRepoTradePricer PRICER = DiscountingRepoTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQS = MarketQuoteSensitivityCalculator.DEFAULT;

  //-------------------------------------------------------------------------
  public void test_par_spread() {
    assertThat(LegalEntityDiscountingCalibrationMeasures.PAR_SPREAD.getName()).isEqualTo("ParSpread");
    assertThat(LegalEntityDiscountingCalibrationMeasures.PAR_SPREAD.getTradeTypes()).contains(ResolvedRepoTrade.class);
  }

  public void test_market_quote() {
    assertThat(LegalEntityDiscountingCalibrationMeasures.MARKET_QUOTE.getName()).isEqualTo("MarketQuote");
    assertThat(LegalEntityDiscountingCalibrationMeasures.MARKET_QUOTE.getTradeTypes())
        .contains(ResolvedRepoTrade.class);
  }

  public void test_present_value() {
    assertThat(LegalEntityDiscountingCalibrationMeasures.PRESENT_VALUE.getName()).isEqualTo("PresentValue");
    assertThat(LegalEntityDiscountingCalibrationMeasures.PRESENT_VALUE.getTradeTypes())
        .contains(ResolvedRepoTrade.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_array() {
    LegalEntityDiscountingCalibrationMeasures test = LegalEntityDiscountingCalibrationMeasures.of(
        "Test", LegalEntityDiscountingTradeCalibrationMeasure.REPO_PAR_SPREAD);
    assertThat(test.getName()).isEqualTo("Test");
    assertThat(test.getTradeTypes()).containsOnly(ResolvedRepoTrade.class);
    assertThat(test.toString()).isEqualTo("Test");
  }

  public void test_of_list() {
    LegalEntityDiscountingCalibrationMeasures test = LegalEntityDiscountingCalibrationMeasures.of(
        "Test", ImmutableList.of(LegalEntityDiscountingMarketQuoteMeasure.REPO_MQ));
    assertThat(test.getName()).isEqualTo("Test");
    assertThat(test.getTradeTypes()).containsOnly(ResolvedRepoTrade.class);
    assertThat(test.toString()).isEqualTo("Test");
  }

  public void test_of_duplicate() {
    assertThrowsIllegalArg(() -> LegalEntityDiscountingCalibrationMeasures.of(
        "Test",
        LegalEntityDiscountingTradeCalibrationMeasure.REPO_PAR_SPREAD,
        LegalEntityDiscountingPresentValueCalibrationMeasure.REPO_PV));
    assertThrowsIllegalArg(() -> LegalEntityDiscountingCalibrationMeasures.of(
        "Test",
        ImmutableList.of(
            LegalEntityDiscountingTradeCalibrationMeasure.REPO_PAR_SPREAD,
            LegalEntityDiscountingMarketQuoteMeasure.REPO_MQ)));
  }

  //-------------------------------------------------------------------------
  public void test_value() {
    assertEquals(
        LegalEntityDiscountingCalibrationMeasures.PAR_SPREAD.value(RESOLVED_TRADE, PROVIDER),
        PRICER.parSpread(RESOLVED_TRADE, PROVIDER));
    assertEquals(
        LegalEntityDiscountingCalibrationMeasures.MARKET_QUOTE.value(RESOLVED_TRADE, PROVIDER),
        PRICER.parRate(RESOLVED_TRADE, PROVIDER));
    assertEquals(
        LegalEntityDiscountingCalibrationMeasures.PRESENT_VALUE.value(RESOLVED_TRADE, PROVIDER),
        PRICER.presentValue(RESOLVED_TRADE, PROVIDER).getAmount());
  }

  public void test_derivative_parSpread() {
    PointSensitivities parSpreadSens = PRICER.parSpreadSensitivity(RESOLVED_TRADE, PROVIDER);
    assertEquals(
        LegalEntityDiscountingCalibrationMeasures.PAR_SPREAD.derivative(RESOLVED_TRADE, PROVIDER, CURVE_ORDER),
        PROVIDER.parameterSensitivity(parSpreadSens)
            .getSensitivity(REPO_CURVE_NAME, EUR)
            .getSensitivity());
  }

  public void test_derivative_parRate() {
    PointSensitivities parRateSens = PRICER.parRateSensitivity(RESOLVED_TRADE, PROVIDER);
    assertEquals(
        LegalEntityDiscountingCalibrationMeasures.MARKET_QUOTE.derivative(RESOLVED_TRADE, PROVIDER, CURVE_ORDER),
        PROVIDER.parameterSensitivity(parRateSens)
            .getSensitivity(REPO_CURVE_NAME, EUR)
            .getSensitivity());
  }

  public void test_derivative_pv() {
    PointSensitivities pvSens = PRICER.presentValueSensitivity(RESOLVED_TRADE, PROVIDER);
    assertEquals(
        LegalEntityDiscountingCalibrationMeasures.PRESENT_VALUE.derivative(RESOLVED_TRADE, PROVIDER, CURVE_ORDER),
        MQS.sensitivity(PROVIDER.parameterSensitivity(pvSens), PROVIDER)
            .getSensitivity(REPO_CURVE_NAME, EUR)
            .getSensitivity());
  }

  public void test_measureNotKnown() {
    LegalEntityDiscountingCalibrationMeasures test = LegalEntityDiscountingCalibrationMeasures.of(
        "Test", LegalEntityDiscountingTradeCalibrationMeasure.REPO_PAR_SPREAD);
    assertThrowsIllegalArg(
        () -> test.value(SwapDummyData.SWAP_TRADE, PROVIDER),
        "Trade type 'ResolvedSwapTrade' is not supported for calibration");
  }

}

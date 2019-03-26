/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.market.ValueType.YEAR_FRACTION;
import static com.opengamma.strata.market.ValueType.ZERO_RATE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.extra.repo.DiscountingRepoTradePricer;
import com.opengamma.strata.extra.repo.ImmutableRepoConvention;
import com.opengamma.strata.extra.repo.RepoConvention;
import com.opengamma.strata.extra.repo.RepoTemplate;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;

/**
 * Test {@link LegalEntityDiscountingCurveCalibrator}.
 */
@Test
public class LegalEntityDiscountingCurveCalibratorTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2017, 12, 11);
  private static final String SCHEME = "OG";
  private static final CurveGroupName GROUP_NAME = CurveGroupName.of("LEGAL_ENTITY_DISCOUNTING_GROUP_1");
  private static final CurveGroupName GROUP_NAME_UK = CurveGroupName.of("LEGAL_ENTITY_DISCOUNTING_GROUP_UK");
  private static final CurveGroupName GROUP_NAME_US = CurveGroupName.of("LEGAL_ENTITY_DISCOUNTING_GROUP_US");
  private static final RepoGroup UK_REPO_GROUP = RepoGroup.of("UK_GROUP");
  private static final RepoGroup US_REPO_GROUP = RepoGroup.of("US_GROUP");
  private static final LegalEntityId UK_GOVT = LegalEntityId.of(SCHEME, "UK_GOVT");
  private static final LegalEntityId US_GOVT = LegalEntityId.of(SCHEME, "US_GOVT");
  private static final CurveName CURVE_NAME_UK = CurveName.of("UK_REPO");
  private static final CurveName CURVE_NAME_US = CurveName.of("US_REPO");
  private static final SecurityId UK_SECURITY_ID = SecurityId.of(StandardId.of(SCHEME, "UK_GOVT_10Y"));
  private static final SecurityPosition UK_COLLATERAL = SecurityPosition.ofNet(UK_SECURITY_ID, 1d);
  private static final SimpleLegalEntitySecurity UK_SECURITY = SimpleLegalEntitySecurity.of(UK_GOVT);
  private static final SecurityId US_SECURITY_ID = SecurityId.of(StandardId.of(SCHEME, "US_GOVT_10Y"));
  private static final SecurityPosition US_COLLATERAL = SecurityPosition.ofNet(US_SECURITY_ID, 1d);
  private static final SimpleLegalEntitySecurity US_SECURITY = SimpleLegalEntitySecurity.of(US_GOVT);
  private static final ReferenceData REF_DATA = ReferenceData.standard().combinedWith(
      ReferenceData.of(ImmutableMap.of(UK_SECURITY_ID, UK_SECURITY, US_SECURITY_ID, US_SECURITY)));
  // market data, curve definitions
  private static final QuoteId[] UK_REPO_IDS = new QuoteId[] {
      QuoteId.of(StandardId.of(SCHEME, "UK_REPO_1W")), QuoteId.of(StandardId.of(SCHEME, "UK_REPO_1M")),
      QuoteId.of(StandardId.of(SCHEME, "UK_REPO_3M"))};
  private static final Tenor[] UK_PERIODS = new Tenor[] {Tenor.TENOR_1W, Tenor.TENOR_1M, Tenor.TENOR_3M};
  private static final double[] UK_QUOTES = new double[] {0.00565, 0.0059, 0.00605};
  private static final RepoConvention UK_CONVENTION = ImmutableRepoConvention.of(
      "UK_REPO_CONV", GBP, BusinessDayAdjustment.of(FOLLOWING, GBLO), ACT_360, DaysAdjustment.ofBusinessDays(1, GBLO));
  private static final InterpolatedNodalCurveDefinition CURVE_DEFINITION_UK;
  private static final QuoteId[] US_REPO_IDS = new QuoteId[] {
      QuoteId.of(StandardId.of(SCHEME, "US_REPO_1W")), QuoteId.of(StandardId.of(SCHEME, "US_REPO_2W")),
      QuoteId.of(StandardId.of(SCHEME, "US_REPO_1M")), QuoteId.of(StandardId.of(SCHEME, "US_REPO_3M"))};
  private static final Tenor[] US_PERIODS = new Tenor[] {
      Tenor.TENOR_1W, Tenor.TENOR_2W, Tenor.TENOR_1M, Tenor.TENOR_3M};
  private static final double[] US_QUOTES = new double[] {0.0142, 0.0131, 0.0125, 0.0124};
  private static final RepoConvention US_CONVENTION = ImmutableRepoConvention.of(
      "US_REPO_CONV", USD, BusinessDayAdjustment.of(FOLLOWING, USNY), ACT_360, DaysAdjustment.ofBusinessDays(1, USNY));
  private static final InterpolatedNodalCurveDefinition CURVE_DEFINITION_US;
  private static final ImmutableMarketData MARKET_DATA;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VALUATION_DATE);
    // UK repo
    List<CurveNode> ukNodes = new ArrayList<>();
    for (int i = 0; i < UK_QUOTES.length; ++i) {
      ukNodes.add(RepoCurveNode.of(
          RepoTemplate.of(UK_PERIODS[i], ImmutableList.of(UK_COLLATERAL), UK_CONVENTION), UK_REPO_IDS[i]));
      builder.addValue(UK_REPO_IDS[i], UK_QUOTES[i]);
    }
    CURVE_DEFINITION_UK = InterpolatedNodalCurveDefinition.builder()
        .dayCount(DayCounts.ACT_365F)
        .name(CURVE_NAME_UK)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .nodes(ukNodes)
        .interpolator(LINEAR)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(FLAT)
        .build();
    // US repo
    List<CurveNode> usNodes = new ArrayList<>();
    for (int i = 0; i < US_QUOTES.length; ++i) {
      usNodes.add(RepoCurveNode.of(
          RepoTemplate.of(US_PERIODS[i], ImmutableList.of(US_COLLATERAL), US_CONVENTION), US_REPO_IDS[i]));
      builder.addValue(US_REPO_IDS[i], US_QUOTES[i]);
    }
    CURVE_DEFINITION_US = InterpolatedNodalCurveDefinition.builder()
        .dayCount(DayCounts.ACT_365F)
        .name(CURVE_NAME_US)
        .xValueType(YEAR_FRACTION)
        .yValueType(ZERO_RATE)
        .nodes(usNodes)
        .interpolator(LINEAR)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(FLAT)
        .build();
    MARKET_DATA = builder.build();
  }
  private static final RepoCurveEntry REPO_CURVE_ENTRY_UK = RepoCurveEntry.builder()
      .curveName(CURVE_NAME_UK)
      .repoCurveGroups(Pair.of(UK_REPO_GROUP, GBP))
      .build();
  private static final RepoCurveEntry REPO_CURVE_ENTRY_US = RepoCurveEntry.builder()
      .curveName(CURVE_NAME_US)
      .repoCurveGroups(Pair.of(US_REPO_GROUP, USD))
      .build();
  private static final LegalEntityDiscountingCurveGroupDefinition GROUP_DEFINITION;
  private static final LegalEntityDiscountingCurveGroupDefinition GROUP_DEFINITION_UK;
  private static final LegalEntityDiscountingCurveGroupDefinition GROUP_DEFINITION_US;
  static {
    GROUP_DEFINITION = LegalEntityDiscountingCurveGroupDefinition.builder()
        .name(GROUP_NAME)
        .repoCurveGroups(ImmutableMap.of(UK_GOVT, UK_REPO_GROUP, US_GOVT, US_REPO_GROUP))
        .issuerCurveGroups(ImmutableMap.of())
        .repoCurveEntries(REPO_CURVE_ENTRY_UK, REPO_CURVE_ENTRY_US)
        .issuerCurveEntries(ImmutableList.of())
        .curveDefinitions(CURVE_DEFINITION_UK, CURVE_DEFINITION_US)
        .computeJacobian(true)
        .computePvSensitivityToMarketQuote(true)
        .build();
    GROUP_DEFINITION_UK = LegalEntityDiscountingCurveGroupDefinition.builder()
        .name(GROUP_NAME_UK)
        .repoCurveGroups(ImmutableMap.of(UK_GOVT, UK_REPO_GROUP))
        .issuerCurveGroups(ImmutableMap.of())
        .repoCurveEntries(REPO_CURVE_ENTRY_UK)
        .issuerCurveEntries(ImmutableList.of())
        .curveDefinitions(CURVE_DEFINITION_UK)
        .computeJacobian(true)
        .computePvSensitivityToMarketQuote(true)
        .build();
    GROUP_DEFINITION_US = LegalEntityDiscountingCurveGroupDefinition.builder()
        .name(GROUP_NAME_US)
        .repoCurveGroups(ImmutableMap.of(US_GOVT, US_REPO_GROUP))
        .issuerCurveGroups(ImmutableMap.of())
        .repoCurveEntries(REPO_CURVE_ENTRY_US)
        .issuerCurveEntries(ImmutableList.of())
        .curveDefinitions(CURVE_DEFINITION_US)
        .computeJacobian(true)
        .computePvSensitivityToMarketQuote(true)
        .build();
  }
  private static final ImmutableList<LegalEntityDiscountingCurveGroupDefinition> GROUP_DEFINITIONS = ImmutableList.of(
      GROUP_DEFINITION_UK, GROUP_DEFINITION_US);
  // calculators
  private static final LegalEntityDiscountingCurveCalibrator CALIBRATOR =
      LegalEntityDiscountingCurveCalibrator.standard();
  private static final DiscountingRepoTradePricer TRADE_PRICER = DiscountingRepoTradePricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQ_CALC = MarketQuoteSensitivityCalculator.DEFAULT;
  private static final double TOL = 1.0e-12;
  private static final double EPS = 1.0e-7;
  // sample trades for testing Jacobian
  private static final double BASE_NOTIONAL = 1.0e7;
  private static final ResolvedRepoTrade UK_TRADE_3W = UK_CONVENTION.toTrade(
      VALUATION_DATE, VALUATION_DATE, VALUATION_DATE.plusWeeks(3), ImmutableList.of(UK_COLLATERAL), BUY, BASE_NOTIONAL,
      0.005).resolve(REF_DATA);
  private static final ResolvedRepoTrade UK_TRADE_2M = UK_CONVENTION.toTrade(
      VALUATION_DATE, VALUATION_DATE, VALUATION_DATE.plusMonths(2), ImmutableList.of(UK_COLLATERAL), SELL,
      5d * BASE_NOTIONAL, -0.005).resolve(REF_DATA);
  private static final ResolvedRepoTrade US_TRADE_3W = US_CONVENTION.toTrade(
      VALUATION_DATE, VALUATION_DATE, VALUATION_DATE.plusWeeks(4), ImmutableList.of(US_COLLATERAL), BUY, BASE_NOTIONAL,
      0.015).resolve(REF_DATA);
  private static final ResolvedRepoTrade US_TRADE_2M = US_CONVENTION.toTrade(
      VALUATION_DATE, VALUATION_DATE, VALUATION_DATE.plusMonths(2).plusWeeks(1), ImmutableList.of(US_COLLATERAL), SELL,
      5d * BASE_NOTIONAL, 0.01).resolve(REF_DATA);
  private static final ResolvedRepoTrade[] SAMPLE_REPO_TRADES = new ResolvedRepoTrade[] {
      UK_TRADE_3W, UK_TRADE_2M, US_TRADE_3W, US_TRADE_2M};

  public void test_calibrate_repo_oneGroup() {
    ImmutableLegalEntityDiscountingProvider result = CALIBRATOR.calibrate(GROUP_DEFINITION, MARKET_DATA, REF_DATA);
    testCalibration(result);
    for (int i = 0; i < SAMPLE_REPO_TRADES.length; ++i) {
      PointSensitivities pointSensi = TRADE_PRICER.presentValueSensitivity(SAMPLE_REPO_TRADES[i], result);
      CurrencyParameterSensitivities zeroSensi = result.parameterSensitivity(pointSensi);
      CurrencyParameterSensitivities quoteSensi = MQ_CALC.sensitivity(zeroSensi, result);
      testJacobian(SAMPLE_REPO_TRADES[i], quoteSensi, CURVE_NAME_UK, UK_REPO_IDS, UK_QUOTES);
      testJacobian(SAMPLE_REPO_TRADES[i], quoteSensi, CURVE_NAME_US, US_REPO_IDS, US_QUOTES);
    }
  }

  public void test_calibrate_repo_twoGroups() {
    ImmutableLegalEntityDiscountingProvider emptyProvider = ImmutableLegalEntityDiscountingProvider.builder()
        .valuationDate(VALUATION_DATE)
        .build();
    ImmutableLegalEntityDiscountingProvider result = CALIBRATOR.calibrate(
        GROUP_DEFINITIONS, emptyProvider, MARKET_DATA, REF_DATA);
    testCalibration(result);
    for (int i = 0; i < SAMPLE_REPO_TRADES.length; ++i) {
      PointSensitivities pointSensi = TRADE_PRICER.presentValueSensitivity(SAMPLE_REPO_TRADES[i], result);
      CurrencyParameterSensitivities zeroSensi = result.parameterSensitivity(pointSensi);
      CurrencyParameterSensitivities quoteSensi = MQ_CALC.sensitivity(zeroSensi, result);
      testJacobian(SAMPLE_REPO_TRADES[i], quoteSensi, CURVE_NAME_UK, UK_REPO_IDS, UK_QUOTES);
      if (SAMPLE_REPO_TRADES[i].getProduct().getCurrency().equals(USD)) {
        testJacobian(SAMPLE_REPO_TRADES[i], quoteSensi, CURVE_NAME_US, US_REPO_IDS, US_QUOTES);
      }
    }
  }

  //-------------------------------------------------------------------------
  // test calibration, PV_SENSITIVITY_TO_MARKET_QUOTE
  private void testCalibration(ImmutableLegalEntityDiscountingProvider result) {
    ZeroRateDiscountFactors dscUk =
        (ZeroRateDiscountFactors) result.repoCurveDiscountFactors(UK_GOVT, GBP).getDiscountFactors();
    DoubleArray pvSensiUk = dscUk.getCurve().getMetadata().findInfo(CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE).get();
    for (int i = 0; i < UK_REPO_IDS.length; ++i) {
      RepoCurveNode repoNode = (RepoCurveNode) CURVE_DEFINITION_UK.getNodes().get(i);
      ResolvedRepoTrade trade = repoNode.trade(1d, MARKET_DATA, REF_DATA).resolve(REF_DATA);
      PointSensitivities pointSensi = TRADE_PRICER.presentValueSensitivity(trade, result);
      CurrencyParameterSensitivities zeroSensi = result.parameterSensitivity(pointSensi);
      CurrencyParameterSensitivities quoteSensi = MQ_CALC.sensitivity(zeroSensi, result);
      double expected = quoteSensi.getSensitivity(CURVE_NAME_UK, GBP).getSensitivity().get(i);
      assertEquals(pvSensiUk.get(i), expected, TOL);
      assertEquals(TRADE_PRICER.presentValue(trade, result).getAmount(), 0d, TOL);
    }
    ZeroRateDiscountFactors dscUs =
        (ZeroRateDiscountFactors) result.repoCurveDiscountFactors(US_GOVT, USD).getDiscountFactors();
    DoubleArray pvSensiUs = dscUs.getCurve().getMetadata().findInfo(CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE).get();
    for (int i = 0; i < US_REPO_IDS.length; ++i) {
      RepoCurveNode repoNode = (RepoCurveNode) CURVE_DEFINITION_US.getNodes().get(i);
      ResolvedRepoTrade trade = repoNode.trade(1d, MARKET_DATA, REF_DATA).resolve(REF_DATA);
      PointSensitivities pointSensi = TRADE_PRICER.presentValueSensitivity(trade, result);
      CurrencyParameterSensitivities zeroSensi = result.parameterSensitivity(pointSensi);
      CurrencyParameterSensitivities quoteSensi = MQ_CALC.sensitivity(zeroSensi, result);
      double expected = quoteSensi.getSensitivity(CURVE_NAME_US, USD).getSensitivity().get(i);
      assertEquals(pvSensiUs.get(i), expected, TOL);
      assertEquals(TRADE_PRICER.presentValue(trade, result).getAmount(), 0d, TOL);
    }
  }

  // test Jacobian via market quote sensitivities
  private void testJacobian(
      ResolvedRepoTrade trade,
      CurrencyParameterSensitivities computed,
      CurveName curveName,
      QuoteId[] quoteIds,
      double[] quotes) {

    double[] expected = new double[quoteIds.length];
    for (int i = 0; i < quoteIds.length; ++i) {
      ImmutableMarketData dataUp = MARKET_DATA.toBuilder().addValue(quoteIds[i], quotes[i] + EPS).build();
      ImmutableLegalEntityDiscountingProvider resultUp = CALIBRATOR.calibrate(GROUP_DEFINITION, dataUp, REF_DATA);
      ImmutableMarketData dataDw = MARKET_DATA.toBuilder().addValue(quoteIds[i], quotes[i] - EPS).build();
      ImmutableLegalEntityDiscountingProvider resultDw = CALIBRATOR.calibrate(GROUP_DEFINITION, dataDw, REF_DATA);
      expected[i] = 0.5d / EPS * (TRADE_PRICER.presentValue(trade, resultUp).getAmount() -
          TRADE_PRICER.presentValue(trade, resultDw).getAmount());
    }
    assertTrue(computed.getSensitivity(curveName, trade.getProduct().getCurrency()).getSensitivity().equalWithTolerance(
        DoubleArray.ofUnsafe(expected), BASE_NOTIONAL * EPS));
  }

}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.extra.repo.DiscountingRepoTradePricer;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingScenarioMarketData;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;

/**
 * Multi-scenario measure calculations for repo trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class RepoMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final RepoMeasureCalculations DEFAULT = new RepoMeasureCalculations(
      DiscountingRepoTradePricer.DEFAULT);
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1.0e-4;

  /**
   * Pricer for {@link ResolvedRepoTrade}.
   */
  private final DiscountingRepoTradePricer tradePricer;

  /**
   * Creates an instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedRepoTrade}
   */
  RepoMeasureCalculations(
      DiscountingRepoTradePricer tradePricer) {
    this.tradePricer = ArgChecker.notNull(tradePricer, "tradePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  CurrencyScenarioArray presentValue(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return CurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> presentValue(trade, legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // present value for one scenario
  CurrencyAmount presentValue(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.presentValue(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return MultiCurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> pv01CalibratedSum(trade, legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01CalibratedSum(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return ScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> pv01CalibratedBucketed(trade, legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    PointSensitivities pointSensitivity = tradePricer.presentValueSensitivity(trade, discountingProvider);
    return discountingProvider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates par rate for all scenarios
  DoubleScenarioArray parRate(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return DoubleScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> parRate(trade, legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // par rate for one scenario
  double parRate(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.parRate(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates par spread for all scenarios
  DoubleScenarioArray parSpread(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return DoubleScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> parSpread(trade, legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // par spread for one scenario
  double parSpread(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.parSpread(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return MultiCurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> currencyExposure(trade, legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.currencyExposure(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData legalEntityMarketData) {

    return CurrencyScenarioArray.of(
        legalEntityMarketData.getScenarioCount(),
        i -> currentCash(trade, legalEntityMarketData.scenario(i).discountingProvider()));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return tradePricer.currentCash(trade, discountingProvider);
  }

}

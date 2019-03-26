/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.ISSUER_CURVE_ID;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.LOOKUP;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.MARKET_DATA;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.REF_DATA;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.REPO_CURVE_ID;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.RESOLVED_TRADE;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.TRADE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.extra.repo.DiscountingRepoProductPricer;
import com.opengamma.strata.extra.repo.DiscountingRepoTradePricer;
import com.opengamma.strata.extra.repo.ResolvedRepo;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;

/**
 * Test {@link RepoTradeCalculationFunction}.
 */
@Test
public class RepoTradeCalculationFunctionTest {

  private static final CalculationParameters PARAMS = CalculationParameters.of(LOOKUP);

  public void test_requirementsAndCurrency() {
    RepoTradeCalculationFunction function = new RepoTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(EUR);
    assertThat(reqs.getValueRequirements()).isEqualTo(ImmutableSet.of(REPO_CURVE_ID, ISSUER_CURVE_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(EUR);
  }

  public void test_simpleMeasures() {
    RepoTradeCalculationFunction function = new RepoTradeCalculationFunction();
    LegalEntityDiscountingProvider provider = LOOKUP.discountingProvider(MARKET_DATA.scenario(0));
    DiscountingRepoTradePricer pricer = DiscountingRepoTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RESOLVED_TRADE, provider);
    double expectedParRate = pricer.parRate(RESOLVED_TRADE, provider);
    double expectedParSpread = pricer.parSpread(RESOLVED_TRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RESOLVED_TRADE, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RESOLVED_TRADE, provider);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.PAR_RATE,
        Measures.PAR_SPREAD,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, MARKET_DATA, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PAR_RATE, Result.success(DoubleScenarioArray.of(ImmutableList.of(expectedParRate))))
        .containsEntry(
            Measures.PAR_SPREAD, Result.success(DoubleScenarioArray.of(ImmutableList.of(expectedParSpread))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RESOLVED_TRADE));
  }

  public void test_pv01() {
    RepoTradeCalculationFunction function = new RepoTradeCalculationFunction();
    LegalEntityDiscountingProvider provider = LOOKUP.discountingProvider(MARKET_DATA.scenario(0));
    DiscountingRepoProductPricer pricer = DiscountingRepoProductPricer.DEFAULT;
    ResolvedRepo resolved = RESOLVED_TRADE.getProduct();
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(resolved, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_CALIBRATED_SUM,
        Measures.PV01_CALIBRATED_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, MARKET_DATA, REF_DATA))
        .containsEntry(
            Measures.PV01_CALIBRATED_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.PV01_CALIBRATED_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedBucketedPv01))));
  }

}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.LOOKUP;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.MARKET_DATA;
import static com.opengamma.strata.extra.bondcurve.RepoTradeTestData.RESOLVED_TRADE;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.extra.repo.DiscountingRepoTradePricer;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;

/**
 * Test {@link RepoTradeCalculationsTest}.
 */
@Test
public class RepoTradeCalculationsTest {

  private static final RepoTradeCalculations CALC = RepoTradeCalculations.DEFAULT;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(MARKET_DATA.scenario(0)).discountingProvider();
    DiscountingRepoTradePricer pricer = DiscountingRepoTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RESOLVED_TRADE, provider);
    double expectedParRate = pricer.parRate(RESOLVED_TRADE, provider);
    double expectedParSpread = pricer.parSpread(RESOLVED_TRADE, provider);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RESOLVED_TRADE, provider);
    CurrencyAmount expectedCurrentCash = pricer.currentCash(RESOLVED_TRADE, provider);

    assertEquals(
        CALC.presentValue(RESOLVED_TRADE, LOOKUP, MARKET_DATA),
        CurrencyScenarioArray.of(ImmutableList.of(expectedPv)));
    assertEquals(
        CALC.parRate(RESOLVED_TRADE, LOOKUP, MARKET_DATA),
        DoubleScenarioArray.of(ImmutableList.of(expectedParRate)));
    assertEquals(
        CALC.parSpread(RESOLVED_TRADE, LOOKUP, MARKET_DATA),
        DoubleScenarioArray.of(ImmutableList.of(expectedParSpread)));
    assertEquals(
        CALC.currencyExposure(RESOLVED_TRADE, LOOKUP, MARKET_DATA),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure)));
    assertEquals(
        CALC.currentCash(RESOLVED_TRADE, LOOKUP, MARKET_DATA),
        CurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash)));
    assertEquals(CALC.presentValue(RESOLVED_TRADE, provider), expectedPv);
    assertEquals(CALC.parRate(RESOLVED_TRADE, provider), expectedParRate);
    assertEquals(CALC.parSpread(RESOLVED_TRADE, provider), expectedParSpread);
    assertEquals(CALC.currencyExposure(RESOLVED_TRADE, provider), expectedCurrencyExposure);
    assertEquals(CALC.currentCash(RESOLVED_TRADE, provider), expectedCurrentCash);
  }

  public void test_pv01() {
    LegalEntityDiscountingProvider provider = LOOKUP.marketDataView(MARKET_DATA.scenario(0)).discountingProvider();
    DiscountingRepoTradePricer pricer = DiscountingRepoTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivity(RESOLVED_TRADE, provider);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    assertEquals(
        CALC.pv01CalibratedSum(RESOLVED_TRADE, LOOKUP, MARKET_DATA),
        MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal)));
    assertEquals(
        CALC.pv01CalibratedBucketed(RESOLVED_TRADE, LOOKUP, MARKET_DATA),
        ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed)));
    assertEquals(CALC.pv01CalibratedSum(RESOLVED_TRADE, provider), expectedPv01Cal);
    assertEquals(CALC.pv01CalibratedBucketed(RESOLVED_TRADE, provider), expectedPv01CalBucketed);
  }

}

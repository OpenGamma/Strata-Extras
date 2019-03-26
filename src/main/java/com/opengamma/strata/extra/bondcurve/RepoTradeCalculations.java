/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.extra.repo.DiscountingRepoTradePricer;
import com.opengamma.strata.extra.repo.RepoTrade;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingMarketDataLookup;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;

/**
 * Calculates pricing and risk measures for repo trades.
 * <p>
 * This provides a high-level entry point for repo pricing and risk measures.
 * <p>
 * Each method takes a {@link ResolvedRepoTrade}, whereas application code will
 * typically work with {@link RepoTrade}. Call
 * {@link RepoTrade#resolve(com.opengamma.strata.basics.ReferenceData) RepoTrade::resolve(ReferenceData)}
 * to convert {@code RepoTrade} to {@code ResolvedRepoTrade}.
 */
public class RepoTradeCalculations {

  /**
   * Default implementation.
   */
  public static final RepoTradeCalculations DEFAULT = new RepoTradeCalculations(
      DiscountingRepoTradePricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedRepoTrade}.
   */
  private final RepoMeasureCalculations calc;

  /**
   * Creates an instance.
   * <p>
   * In most cases, applications should use the {@link #DEFAULT} instance.
   * 
   * @param tradePricer  the pricer for {@link ResolvedRepoTrade}
   */
  public RepoTradeCalculations(
      DiscountingRepoTradePricer tradePricer) {
    this.calc = new RepoMeasureCalculations(tradePricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value across one or more scenarios.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value, one entry per scenario
   */
  public CurrencyScenarioArray presentValue(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.presentValue(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value for a single set of market data.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.presentValue(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedRepoTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public MultiCurrencyScenarioArray pv01CalibratedSum(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedSum(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedRepoTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is the sum of the sensitivities of all affected curves.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the present value sensitivity
   */
  public MultiCurrencyAmount pv01CalibratedSum(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.pv01CalibratedSum(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates present value sensitivity across one or more scenarios.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedRepoTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the present value sensitivity, one entry per scenario
   */
  public ScenarioArray<CurrencyParameterSensitivities> pv01CalibratedBucketed(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.pv01CalibratedBucketed(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates present value sensitivity for a single set of market data.
   * <p>
   * This is the sensitivity of
   * {@linkplain #presentValue(ResolvedRepoTrade, LegalEntityDiscountingMarketDataLookup, ScenarioMarketData) present value}
   * to a one basis point shift in the calibrated curves.
   * The result is provided for each affected curve and currency, bucketed by curve node.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the present value sensitivity
   */
  public CurrencyParameterSensitivities pv01CalibratedBucketed(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.pv01CalibratedBucketed(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates par rate across one or more scenarios.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the par rate, one entry per scenario
   */
  public DoubleScenarioArray parRate(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.parRate(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates par rate for a single set of market data.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the par rate
   */
  public double parRate(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.parRate(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates par spread across one or more scenarios.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the par spread, one entry per scenario
   */
  public DoubleScenarioArray parSpread(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.parSpread(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates par spread for a single set of market data.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the par spread
   */
  public double parSpread(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.parSpread(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates currency exposure across one or more scenarios.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the currency exposure, one entry per scenario
   */
  public MultiCurrencyScenarioArray currencyExposure(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.currencyExposure(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates currency exposure for a single set of market data.
   * <p>
   * The currency risk, expressed as the equivalent amount in each currency.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.currencyExposure(trade, discountingProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates current cash across one or more scenarios.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param lookup  the lookup used to query the market data
   * @param marketData  the market data
   * @return the current cash, one entry per scenario
   */
  public CurrencyScenarioArray currentCash(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingMarketDataLookup lookup,
      ScenarioMarketData marketData) {

    return calc.currentCash(trade, lookup.marketDataView(marketData));
  }

  /**
   * Calculates current cash for a single set of market data.
   * <p>
   * The sum of all cash flows paid on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the market data
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedRepoTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return calc.currentCash(trade, discountingProvider);
  }

}

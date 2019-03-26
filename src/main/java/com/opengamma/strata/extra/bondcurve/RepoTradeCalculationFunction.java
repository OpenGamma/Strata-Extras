/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.extra.repo.RepoTrade;
import com.opengamma.strata.extra.repo.ResolvedRepo;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingMarketDataLookup;
import com.opengamma.strata.measure.bond.LegalEntityDiscountingScenarioMarketData;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * Perform calculations on a single {@code RepoTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * An instance of {@link RatesMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#PAR_RATE Par rate}
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 */
public class RepoTradeCalculationFunction
    implements CalculationFunction<RepoTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, RepoMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, RepoMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, RepoMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PAR_RATE, RepoMeasureCalculations.DEFAULT::parRate)
          .put(Measures.PAR_SPREAD, RepoMeasureCalculations.DEFAULT::parSpread)
          .put(Measures.CURRENCY_EXPOSURE, RepoMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, RepoMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.RESOLVED_TARGET, (rt, smd) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public RepoTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<RepoTrade> targetType() {
    return RepoTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(RepoTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(RepoTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      RepoTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    ResolvedRepo product = trade.getProduct().resolve(refData);
    SecurityId securityId = product.getSecurityIds().get(0); // not used in pricing
    LegalEntityId standardId = product.getLegalEntityId();

    // use lookup to build requirements
    LegalEntityDiscountingMarketDataLookup ledLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    return ledLookup.requirements(securityId, standardId, product.getCurrency());
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      RepoTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedRepoTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    LegalEntityDiscountingMarketDataLookup ledLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    LegalEntityDiscountingScenarioMarketData marketData = ledLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, marketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedRepoTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for RepoTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedRepoTrade trade,
        LegalEntityDiscountingScenarioMarketData marketData);
  }

}

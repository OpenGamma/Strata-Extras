/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.UnitParameterSensitivities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Provides access to the measures needed to perform curve calibration.
 * <p>
 * The most commonly used measures are par spread and present value.
 */
public final class LegalEntityDiscountingCalibrationMeasures {

  /**
   * The par spread instance, which is the default used in curve calibration.
   */
  public static final LegalEntityDiscountingCalibrationMeasures PAR_SPREAD =
      LegalEntityDiscountingCalibrationMeasures.of(
          "ParSpread",
          LegalEntityDiscountingTradeCalibrationMeasure.REPO_PAR_SPREAD);
  /**
   * The market quote instance, which is the default used in synthetic curve calibration.
   */
  public static final LegalEntityDiscountingCalibrationMeasures MARKET_QUOTE =
      LegalEntityDiscountingCalibrationMeasures.of(
          "MarketQuote",
          LegalEntityDiscountingMarketQuoteMeasure.REPO_MQ);
  /**
   * The present value instance, which is the default used in present value sensitivity to market quote stored during 
   * curve calibration.
   */
  public static final LegalEntityDiscountingCalibrationMeasures PRESENT_VALUE =
      LegalEntityDiscountingCalibrationMeasures.of(
          "PresentValue",
          LegalEntityDiscountingPresentValueCalibrationMeasure.REPO_PV);

  /**
   * The name of the set of measures.
   */
  private final String name;
  /**
   * The calibration measure providers keyed by type.
   */
  private final ImmutableMap<Class<?>, LegalEntityDiscountingCalibrationMeasure<? extends ResolvedTrade>> measuresByTrade;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a list of individual trade-specific measures.
   * <p>
   * Each measure must be for a different trade type.
   * 
   * @param name  the name of the set of measures
   * @param measures  the list of measures
   * @return the calibration measures
   * @throws IllegalArgumentException if a trade type is specified more than once
   */
  public static LegalEntityDiscountingCalibrationMeasures of(
      String name,
      List<? extends LegalEntityDiscountingCalibrationMeasure<? extends ResolvedTrade>> measures) {

    return new LegalEntityDiscountingCalibrationMeasures(name, measures);
  }

  /**
   * Obtains an instance from a list of individual trade-specific measures.
   * <p>
   * Each measure must be for a different trade type.
   * 
   * @param name  the name of the set of measures
   * @param measures  the list of measures
   * @return the calibration measures
   * @throws IllegalArgumentException if a trade type is specified more than once
   */
  @SafeVarargs
  public static LegalEntityDiscountingCalibrationMeasures of(
      String name,
      LegalEntityDiscountingCalibrationMeasure<? extends ResolvedTrade>... measures) {

    return new LegalEntityDiscountingCalibrationMeasures(name, ImmutableList.copyOf(measures));
  }

  //-------------------------------------------------------------------------
  // restricted constructor
  private LegalEntityDiscountingCalibrationMeasures(
      String name,
      List<? extends LegalEntityDiscountingCalibrationMeasure<? extends ResolvedTrade>> measures) {

    this.name = ArgChecker.notEmpty(name, "name");
    this.measuresByTrade = measures.stream()
        .collect(toImmutableMap(LegalEntityDiscountingCalibrationMeasure::getTradeType, m -> m));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name of the set of measures.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the supported trade types.
   * 
   * @return the supported trade types
   */
  public ImmutableSet<Class<?>> getTradeTypes() {
    return measuresByTrade.keySet();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the value, such as par spread.
   * <p>
   * The value must be calculated using the specified legal entity discounting provider.
   * 
   * @param trade  the trade
   * @param provider  the legal entity discounting provider
   * @return the sensitivity
   * @throws IllegalArgumentException if the trade cannot be valued
   */
  public double value(ResolvedTrade trade, LegalEntityDiscountingProvider provider) {
    LegalEntityDiscountingCalibrationMeasure<ResolvedTrade> measure = getMeasure(trade);
    return measure.value(trade, provider);
  }

  /**
   * Calculates the sensitivity with respect to the legal entity discounting provider.
   * <p>
   * The result array is composed of the concatenated curve sensitivities from
   * all curves currently being processed.
   * 
   * @param trade  the trade
   * @param provider  the legal entity discounting provider
   * @param curveOrder  the order of the curves
   * @return the sensitivity
   */
  public DoubleArray derivative(
      ResolvedTrade trade,
      LegalEntityDiscountingProvider provider,
      List<CurveParameterSize> curveOrder) {

    UnitParameterSensitivities unitSens = extractSensitivities(trade, provider);

    // expand to a concatenated array
    DoubleArray result = DoubleArray.EMPTY;
    for (CurveParameterSize curveParams : curveOrder) {
      DoubleArray sens = unitSens.findSensitivity(curveParams.getName())
          .map(s -> s.getSensitivity())
          .orElseGet(() -> DoubleArray.filled(curveParams.getParameterCount()));
      result = result.concat(sens);
    }
    return result;
  }

  // determine the curve parameter sensitivities, removing the curency
  private UnitParameterSensitivities extractSensitivities(ResolvedTrade trade,
      LegalEntityDiscountingProvider provider) {
    LegalEntityDiscountingCalibrationMeasure<ResolvedTrade> measure = getMeasure(trade);
    CurrencyParameterSensitivities paramSens = measure.sensitivities(trade, provider);
    UnitParameterSensitivities unitSens = UnitParameterSensitivities.empty();
    for (CurrencyParameterSensitivity ccySens : paramSens.getSensitivities()) {
      unitSens = unitSens.combinedWith(ccySens.toUnitParameterSensitivity());
    }
    return unitSens;
  }

  //-------------------------------------------------------------------------
  // finds the correct measure implementation
  @SuppressWarnings("unchecked")
  private <T extends ResolvedTrade> LegalEntityDiscountingCalibrationMeasure<ResolvedTrade>
      getMeasure(ResolvedTrade trade) {
    Class<? extends ResolvedTrade> tradeType = trade.getClass();
    LegalEntityDiscountingCalibrationMeasure<? extends ResolvedTrade> measure = measuresByTrade.get(tradeType);
    if (measure == null) {
      throw new IllegalArgumentException(Messages.format(
          "Trade type '{}' is not supported for calibration", tradeType.getSimpleName()));
    }
    // cast makes life easier for the code using this method
    return (LegalEntityDiscountingCalibrationMeasure<ResolvedTrade>) measure;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}

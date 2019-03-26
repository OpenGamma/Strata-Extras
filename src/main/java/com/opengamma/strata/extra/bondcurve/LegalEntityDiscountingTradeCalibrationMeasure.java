/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.extra.repo.DiscountingRepoProductPricer;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Provides calibration measures for a single type of trade based on functions.
 * <p>
 * This is initialized using functions that typically refer to pricers.
 * 
 * @param <T> the trade type
 */
public final class LegalEntityDiscountingTradeCalibrationMeasure<T extends ResolvedTrade>
    implements LegalEntityDiscountingCalibrationMeasure<T> {

  /**
   * The calibrator for {@link ResolvedRepoTrade} using par spread discounting.
   */
  public static final LegalEntityDiscountingTradeCalibrationMeasure<ResolvedRepoTrade> REPO_PAR_SPREAD =
      LegalEntityDiscountingTradeCalibrationMeasure.of(
          "RepoParSpreadDiscounting",
          ResolvedRepoTrade.class,
          (trade, p) -> DiscountingRepoProductPricer.DEFAULT.parSpread(trade.getProduct(), p),
          (trade, p) -> DiscountingRepoProductPricer.DEFAULT.parSpreadSensitivity(
              trade.getProduct(), p));

  //-------------------------------------------------------------------------
  /**
   * The name.
   */
  private final String name;
  /**
   * The trade type.
   */
  private final Class<T> tradeType;
  /**
   * The value measure.
   */
  private final ToDoubleBiFunction<T, LegalEntityDiscountingProvider> valueFn;
  /**
   * The sensitivity measure.
   */
  private final BiFunction<T, LegalEntityDiscountingProvider, PointSensitivities> sensitivityFn;

  //-------------------------------------------------------------------------
  /**
   * Obtains a calibrator for a specific type of trade.
   * <p>
   * The functions typically refer to pricers.
   * 
   * @param <R>  the trade type
   * @param name  the name
   * @param tradeType  the trade type
   * @param valueFn  the function for calculating the value
   * @param sensitivityFn  the function for calculating the sensitivity
   * @return the calibrator
   */
  public static <R extends ResolvedTrade> LegalEntityDiscountingTradeCalibrationMeasure<R> of(
      String name,
      Class<R> tradeType,
      ToDoubleBiFunction<R, LegalEntityDiscountingProvider> valueFn,
      BiFunction<R, LegalEntityDiscountingProvider, PointSensitivities> sensitivityFn) {

    return new LegalEntityDiscountingTradeCalibrationMeasure<R>(name, tradeType, valueFn, sensitivityFn);
  }

  // restricted constructor
  private LegalEntityDiscountingTradeCalibrationMeasure(
      String name,
      Class<T> tradeType,
      ToDoubleBiFunction<T, LegalEntityDiscountingProvider> valueFn,
      BiFunction<T, LegalEntityDiscountingProvider, PointSensitivities> sensitivityFn) {

    this.name = name;
    this.tradeType = tradeType;
    this.valueFn = ArgChecker.notNull(valueFn, "valueFn");
    this.sensitivityFn = ArgChecker.notNull(sensitivityFn, "sensitivityFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<T> getTradeType() {
    return tradeType;
  }

  //-------------------------------------------------------------------------
  @Override
  public double value(T trade, LegalEntityDiscountingProvider provider) {
    return valueFn.applyAsDouble(trade, provider);
  }

  @Override
  public CurrencyParameterSensitivities sensitivities(T trade, LegalEntityDiscountingProvider provider) {
    PointSensitivities pts = sensitivityFn.apply(trade, provider);
    return provider.parameterSensitivity(pts);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}

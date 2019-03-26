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
import com.opengamma.strata.extra.repo.RepoTrade;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Provides calibration measures for a single type of trade based on functions.
 * <p>
 * This set of measures return the present value of the product. 
 * The sensitivities are with respect to the market quote sensitivities.
 * 
 * @param <T> the trade type
 */
public final class LegalEntityDiscountingPresentValueCalibrationMeasure<T extends ResolvedTrade>
    implements LegalEntityDiscountingCalibrationMeasure<T> {

  /**
   * Market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  /**
   * The calibrator for {@link RepoTrade} using present value discounting.
   */
  public static final LegalEntityDiscountingPresentValueCalibrationMeasure<ResolvedRepoTrade> REPO_PV =
      LegalEntityDiscountingPresentValueCalibrationMeasure.of(
          "RepoPresentValueDiscounting",
          ResolvedRepoTrade.class,
          (trade, p) -> DiscountingRepoProductPricer.DEFAULT.presentValue(trade.getProduct(), p).getAmount(),
          (trade, p) -> DiscountingRepoProductPricer.DEFAULT.presentValueSensitivity(trade.getProduct(), p));

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
  public static <R extends ResolvedTrade> LegalEntityDiscountingPresentValueCalibrationMeasure<R> of(
      String name,
      Class<R> tradeType,
      ToDoubleBiFunction<R, LegalEntityDiscountingProvider> valueFn,
      BiFunction<R, LegalEntityDiscountingProvider, PointSensitivities> sensitivityFn) {

    return new LegalEntityDiscountingPresentValueCalibrationMeasure<R>(name, tradeType, valueFn, sensitivityFn);
  }

  // restricted constructor
  private LegalEntityDiscountingPresentValueCalibrationMeasure(
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
    CurrencyParameterSensitivities ps = provider.parameterSensitivity(pts);
    return MQC.sensitivity(ps, provider);
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return name;
  }

}

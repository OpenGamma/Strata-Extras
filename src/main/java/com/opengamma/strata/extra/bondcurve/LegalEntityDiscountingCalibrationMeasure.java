/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Provides access to the measures needed to perform curve calibration for a single type of trade.
 * <p>
 * The most commonly used measures are par spread and converted present value.
 * <p>
 * See {@link LegalEntityDiscountingCalibrationMeasures} for constants defining measures for common trade types.
 * 
 * @param <T> the trade type
 */
public interface LegalEntityDiscountingCalibrationMeasure<T extends ResolvedTrade> {

  /**
   * Gets the trade type of the calibrator.
   * 
   * @return the trade type
   */
  public abstract Class<T> getTradeType();

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
  public abstract double value(T trade, LegalEntityDiscountingProvider provider);

  /**
   * Calculates the parameter sensitivities that relate to the value.
   * <p>
   * The sensitivities must be calculated using the specified legal entity discounting provider.
   * 
   * @param trade  the trade
   * @param provider  the legal entity discounting provider
   * @return the sensitivity
   * @throws IllegalArgumentException if the trade cannot be valued
   */
  public abstract CurrencyParameterSensitivities sensitivities(T trade, LegalEntityDiscountingProvider provider);

}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.product.ResolvedTrade;

/**
 * Provides the calibration derivative.
 * <p>
 * This provides the value sensitivity from the specified {@link LegalEntityDiscountingCalibrationMeasures}
 * instance in matrix form suitable for use in curve calibration root finding.
 * The value will typically be par spread or present value.
 */
class LegalEntityDiscountingCalibrationDerivative
    implements Function<DoubleArray, DoubleMatrix> {

  /**
   * The trades.
   */
  private final List<ResolvedTrade> trades;
  /**
   * The calibration measures.
   */
  private final LegalEntityDiscountingCalibrationMeasures measures;
  /**
   * The provider generator, used to create child providers.
   */
  private final LegalEntityDiscountingProviderGenerator providerGenerator;
  /**
   * Provide the order in which the curves appear in the long vector result.
   * The expected number of parameters for each curve is also provided.
   */
  private final List<CurveParameterSize> curveOrder;

  /**
   * Creates an instance.
   * 
   * @param trades  the trades
   * @param measures  the calibration measures
   * @param providerGenerator  the provider generator, used to create child providers
   * @param curveOrder  the curve order
   */
  public LegalEntityDiscountingCalibrationDerivative(
      List<ResolvedTrade> trades,
      LegalEntityDiscountingCalibrationMeasures measures,
      LegalEntityDiscountingProviderGenerator providerGenerator,
      List<CurveParameterSize> curveOrder) {

    this.measures = measures;
    this.trades = trades;
    this.providerGenerator = providerGenerator;
    this.curveOrder = curveOrder;
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleMatrix apply(DoubleArray x) {
    // create child provider from matrix
    ImmutableLegalEntityDiscountingProvider provider = providerGenerator.generate(x);
    // calculate derivative for each trade using the child provider
    int size = trades.size();
    return DoubleMatrix.ofArrayObjects(size, size, i -> measures.derivative(trades.get(i), provider, curveOrder));
  }

}

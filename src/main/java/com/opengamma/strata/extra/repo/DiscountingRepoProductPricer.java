/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.RepoCurveDiscountFactors;

/**
 * The methods associated to the pricing of repo by discounting.
 * <p>
 * This provides the ability to price {@link ResolvedRepo}.
 */
public class DiscountingRepoProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingRepoProductPricer DEFAULT = new DiscountingRepoProductPricer();

  /**
   * Creates an instance.
   */
  public DiscountingRepoProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param product  the product
   * @param ratesProvider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(ResolvedRepo product, LegalEntityDiscountingProvider ratesProvider) {
    Currency currency = product.getCurrency();
    if (ratesProvider.getValuationDate().isAfter(product.getEndDate())) {
      return CurrencyAmount.of(currency, 0.0d);
    }
    RepoCurveDiscountFactors discountFactors = ratesProvider.repoCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    double dfStart = discountFactors.discountFactor(product.getStartDate());
    double dfEnd = discountFactors.discountFactor(product.getEndDate());
    double pvStart = initialAmount(product, ratesProvider) * dfStart;
    double pvEnd = (product.getNotional() + product.getInterest()) * dfEnd;
    double pv = pvEnd - pvStart;
    return CurrencyAmount.of(currency, pv);
  }

  private double initialAmount(ResolvedRepo product, LegalEntityDiscountingProvider ratesProvider) {
    return ratesProvider.getValuationDate().isAfter(product.getStartDate()) ? 0d : product.getNotional();
  }

  /**
   * Calculates the present value sensitivity by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * 
   * @param product  the product
   * @param ratesProvider  the rates ratesProvider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedRepo product, LegalEntityDiscountingProvider ratesProvider) {
    // backward sweep
    double dfEndBar = product.getNotional() + product.getInterest();
    double dfStartBar = -initialAmount(product, ratesProvider);
    // sensitivity
    RepoCurveDiscountFactors discountFactors = ratesProvider.repoCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    PointSensitivityBuilder sensStart = discountFactors.zeroRatePointSensitivity(product.getStartDate())
        .multipliedBy(dfStartBar);
    PointSensitivityBuilder sensEnd = discountFactors.zeroRatePointSensitivity(product.getEndDate())
        .multipliedBy(dfEndBar);
    return sensStart.combinedWith(sensEnd).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the product fair rate given the start and end time and the accrual factor.
   * <p>
   * When the product has already started the number may not be meaningful as the remaining period
   * is not in line with the accrual factor.
   * 
   * @param product  the product
   * @param ratesProvider  the rates ratesProvider
   * @return the par rate
   */
  public double parRate(ResolvedRepo product, LegalEntityDiscountingProvider ratesProvider) {
    RepoCurveDiscountFactors discountFactors = ratesProvider.repoCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    double dfStart = discountFactors.discountFactor(product.getStartDate());
    double dfEnd = discountFactors.discountFactor(product.getEndDate());
    double accrualFactor = product.getYearFraction();
    return (dfStart / dfEnd - 1d) / accrualFactor;
  }

  /**
   * Calculates the par rate curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when product has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param product  the product
   * @param ratesProvider  the rates ratesProvider
   * @return the par rate curve sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedRepo product, LegalEntityDiscountingProvider ratesProvider) {
    return parSpreadSensitivity(product, ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the spread to be added to the product rate to have a zero present value.
   * <p>
   * The calculation is based on both the initial and final payments.
   * Thus the resulting number may not be meaningful when product has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param product  the product
   * @param ratesProvider  the rates ratesProvider
   * @return the par spread
   */
  public double parSpread(ResolvedRepo product, LegalEntityDiscountingProvider ratesProvider) {
    double parRate = parRate(product, ratesProvider);
    return parRate - product.getRate();
  }

  /**
   * Calculates the par spread curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when product has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param product  the product
   * @param ratesProvider  the rates ratesProvider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedRepo product, LegalEntityDiscountingProvider ratesProvider) {
    double accrualFactorInv = 1d / product.getYearFraction();
    RepoCurveDiscountFactors discountFactors = ratesProvider.repoCurveDiscountFactors(
        product.getLegalEntityId(), product.getCurrency());
    double dfStart = discountFactors.discountFactor(product.getStartDate());
    double dfEndInv = 1d / discountFactors.discountFactor(product.getEndDate());
    PointSensitivityBuilder sensStart = discountFactors.zeroRatePointSensitivity(product.getStartDate())
        .multipliedBy(dfEndInv * accrualFactorInv);
    PointSensitivityBuilder sensEnd = discountFactors.zeroRatePointSensitivity(product.getEndDate())
        .multipliedBy(-dfStart * dfEndInv * dfEndInv * accrualFactorInv);
    return sensStart.combinedWith(sensEnd).build();
  }

}

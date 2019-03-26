/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;

/**
 * The methods associated to the pricing of repo by discounting.
 * <p>
 * This provides the ability to price {@link ResolvedRepo}.
 */
public class DiscountingRepoTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingRepoTradePricer DEFAULT =
      new DiscountingRepoTradePricer(DiscountingRepoProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedRepo}.
   */
  private final DiscountingRepoProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedRepo}
   */
  public DiscountingRepoTradePricer(DiscountingRepoProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    return productPricer.presentValue(trade.getProduct(), ratesProvider);
  }

  /**
   * Calculates the present value sensitivity by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the fair product rate given the start and end time and the accrual factor.
   * <p>
   * When the repo has already started the number may not be meaningful as the remaining period
   * is not in line with the accrual factor.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the par rate
   */
  public double parRate(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    return productPricer.parRate(trade.getProduct(), ratesProvider);
  }

  /**
   * Calculates the par rate curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when repo has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the par rate curve sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    return productPricer.parRateSensitivity(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the spread to be added to the product rate to have a zero present value.
   * <p>
   * The calculation is based on both the initial and final payments.
   * Thus the resulting number may not be meaningful when repo has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the par spread
   */
  public double parSpread(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    return productPricer.parSpread(trade.getProduct(), ratesProvider);
  }

  /**
   * Calculates the par spread curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when repo has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    return productPricer.parSpreadSensitivity(trade.getProduct(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    return MultiCurrencyAmount.of(presentValue(trade, ratesProvider));
  }

  /**
   * Calculates the current cash.
   * 
   * @param trade  the trade
   * @param ratesProvider  the rates provider
   * @return the current cash
   */
  public CurrencyAmount currentCash(ResolvedRepoTrade trade, LegalEntityDiscountingProvider ratesProvider) {
    ResolvedRepo product = trade.getProduct();
    if (product.getStartDate().isEqual(ratesProvider.getValuationDate())) {
      return CurrencyAmount.of(product.getCurrency(), -product.getNotional());
    }
    if (product.getEndDate().isEqual(ratesProvider.getValuationDate())) {
      return CurrencyAmount.of(product.getCurrency(), product.getNotional() + product.getInterest());
    }
    return CurrencyAmount.zero(product.getCurrency());
  }

}

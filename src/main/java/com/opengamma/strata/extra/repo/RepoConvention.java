/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import java.time.LocalDate;
import java.util.List;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * A market convention for repo trades.
 * <p>
 * This defines the market convention for a repo.
 * <p>
 * To manually create a convention, see {@link ImmutableRepoConvention}.
 */
public interface RepoConvention
    extends TradeConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static RepoConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<RepoConvention> extendedEnum() {
    return RepoConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the primary currency.
   * <p>
   * This is the currency of the repo and the currency that payment is made in.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 1 business day".
   * 
   * @return the spot date offset, not null
   */
  public abstract DaysAdjustment getSpotDateOffset();

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified tenor.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the repo, the principal is paid at the start date and the
   * principal plus interest is received at the end date.
   * If selling the repo, the principal is received at the start date and the
   * principal plus interest is paid at the end date.
   * 
   * @param tradeDate  the date of the trade
   * @param tenor  the period between the start date and the end date
   * @param collateral  the collateral
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param rate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public default RepoTrade createTrade(
      LocalDate tradeDate,
      Tenor tenor,
      List<SecurityPosition> collateral,
      BuySell buySell,
      double notional,
      double rate,
      ReferenceData refData) {

    LocalDate startDate = calculateSpotDateFromTradeDate(tradeDate, refData);
    LocalDate endDate = startDate.plus(tenor);
    return toTrade(tradeDate, startDate, endDate, collateral, buySell, notional, rate);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the repo, the principal is paid at the start date and the
   * principal plus interest is received at the end date.
   * If selling the repo, the principal is received at the start date and the
   * principal plus interest is paid at the end date.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param collateral  the collateral
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param rate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public default RepoTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      List<SecurityPosition> collateral,
      BuySell buySell,
      double notional,
      double rate) {

    TradeInfo tradeInfo = TradeInfo.of(tradeDate);
    return toTrade(tradeInfo, startDate, endDate, collateral, buySell, notional, rate);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the repo, the principal is paid at the start date and the
   * principal plus interest is received at the end date.
   * If selling the repo, the principal is received at the start date and the
   * principal plus interest is paid at the end date.
   * 
   * @param tradeInfo  additional information about the trade 
   * @param startDate  the start date
   * @param endDate  the end date
   * @param collateral  the collateral
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param rate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public abstract RepoTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      List<SecurityPosition> collateral,
      BuySell buySell,
      double notional,
      double rate);

  //-------------------------------------------------------------------------
  /**
   * Calculates the spot date from the trade date.
   * 
   * @param tradeDate  the trade date
   * @param refData  the reference data, used to resolve the date
   * @return the spot date
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public default LocalDate calculateSpotDateFromTradeDate(LocalDate tradeDate, ReferenceData refData) {
    return getSpotDateOffset().adjust(tradeDate, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}

/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsWithCause;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.extra.repo.ImmutableRepoConvention;
import com.opengamma.strata.extra.repo.Repo;
import com.opengamma.strata.extra.repo.RepoConvention;
import com.opengamma.strata.extra.repo.RepoTemplate;
import com.opengamma.strata.extra.repo.RepoTrade;
import com.opengamma.strata.extra.repo.ResolvedRepoTrade;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * Test {@link RepoCurveNode}.
 */
@Test
public class RepoCurveNodeTest {

  private static final LegalEntityId ISSUER_ID = LegalEntityId.of("OG", "ABC");
  private static final SecurityId SECURITY_ID = SecurityId.of(StandardId.of("OG", "bond"));
  private static final SimpleLegalEntitySecurity SECURITY = SimpleLegalEntitySecurity.of(ISSUER_ID);
  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ReferenceData.of(ImmutableMap.of(SECURITY_ID, SECURITY)));
  private static final LocalDate VAL_DATE = date(2015, 6, 30);
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA);
  private static final ImmutableList<SecurityPosition> COLLATERAL = ImmutableList.of(SecurityPosition.ofNet(SECURITY_ID, 1d));
  private static final String NAME = "CONV";
  private static final RepoConvention CONVENTION = ImmutableRepoConvention.of(NAME, EUR, BDA_FOLLOW, ACT_360, PLUS_ONE_DAY);
  private static final Tenor PERIOD_1M = Tenor.ofMonths(1);
  private static final Tenor PERIOD_1W = Tenor.ofWeeks(1);
  private static final RepoTemplate TEMPLATE = RepoTemplate.of(PERIOD_1M, COLLATERAL, CONVENTION);
  private static final RepoTemplate TEMPLATE_SH = RepoTemplate.of(PERIOD_1W, COLLATERAL, CONVENTION);
  private static final QuoteId QUOTE_ID = QuoteId.of(StandardId.of("OG", "Repo1"));
  private static final double SPREAD = 0.0015;
  private static final String LABEL = "Label";
  private static final String LABEL_AUTO = "1M";

  public void test_builder() {
    RepoCurveNode test = RepoCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .date(CurveNodeDate.END)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.END);
  }

  public void test_builder_defaults() {
    RepoCurveNode test = RepoCurveNode.builder()
        .label(LABEL)
        .template(TEMPLATE)
        .rateId(QUOTE_ID)
        .additionalSpread(SPREAD)
        .build();
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
    assertEquals(test.getDate(), CurveNodeDate.END);
  }

  public void test_of_noSpread() {
    RepoCurveNode test = RepoCurveNode.of(TEMPLATE, QUOTE_ID);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), 0.0d);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpread() {
    RepoCurveNode test = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertEquals(test.getLabel(), LABEL_AUTO);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_of_withSpreadAndLabel() {
    RepoCurveNode test = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD, LABEL);
    assertEquals(test.getLabel(), LABEL);
    assertEquals(test.getRateId(), QUOTE_ID);
    assertEquals(test.getAdditionalSpread(), SPREAD);
    assertEquals(test.getTemplate(), TEMPLATE);
  }

  public void test_requirements() {
    RepoCurveNode test = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    Set<ObservableId> set = test.requirements();
    Iterator<ObservableId> itr = set.iterator();
    assertEquals(itr.next(), QUOTE_ID);
    assertFalse(itr.hasNext());
  }

  public void test_trade() {
    RepoCurveNode node = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    RepoTrade trade = node.trade(1d, marketData, REF_DATA);
    ResolvedRepoTrade resolvedTrade = node.resolvedTrade(1d, marketData, REF_DATA);
    LocalDate startDateExpected = PLUS_ONE_DAY.adjust(VAL_DATE, REF_DATA);
    LocalDate endDateExpected = startDateExpected.plus(PERIOD_1M);
    Repo repoExpected = Repo.builder()
        .buySell(BuySell.BUY)
        .currency(EUR)
        .dayCount(ACT_360)
        .collateral(COLLATERAL)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .notional(1.0d)
        .businessDayAdjustment(BDA_FOLLOW)
        .rate(rate + SPREAD)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(VAL_DATE)
        .build();
    assertEquals(trade.getProduct(), repoExpected);
    assertEquals(trade.getInfo(), tradeInfoExpected);
    assertEquals(resolvedTrade.getProduct(), repoExpected.resolve(REF_DATA));
    assertEquals(resolvedTrade.getInfo(), tradeInfoExpected);
  }

  public void test_trade_noMarketData() {
    RepoCurveNode node = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    MarketData marketData = MarketData.empty(valuationDate);
    assertThrows(() -> node.trade(1d, marketData, REF_DATA), MarketDataNotFoundException.class);
  }

  public void test_initialGuess() {
    RepoCurveNode node = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    double rate = 0.035;
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE).addValue(QUOTE_ID, rate).build();
    assertEquals(node.initialGuess(marketData, ValueType.ZERO_RATE), rate);
    assertEquals(node.initialGuess(marketData, ValueType.FORWARD_RATE), rate);
    assertEquals(node.initialGuess(marketData, ValueType.DISCOUNT_FACTOR), Math.exp(-rate / 12d), 1.0e-12);
    RepoCurveNode nodeShort = RepoCurveNode.of(TEMPLATE_SH, QUOTE_ID, SPREAD);
    assertEquals(nodeShort.initialGuess(marketData, ValueType.DISCOUNT_FACTOR), Math.exp(-rate * 7d / 365d), 1.0e-12);
  }

  public void test_metadata_end() {
    RepoCurveNode node = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    ParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(((TenorDateParameterMetadata) metadata).getDate(), LocalDate.of(2015, 2, 23));
    assertEquals(((TenorDateParameterMetadata) metadata).getTenor(), Tenor.TENOR_1M);
  }

  public void test_metadata_fixed() {
    LocalDate nodeDate = VAL_DATE.plusMonths(1);
    RepoCurveNode node =
        RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.of(nodeDate));
    LocalDate valuationDate = LocalDate.of(2015, 1, 22);
    DatedParameterMetadata metadata = node.metadata(valuationDate, REF_DATA);
    assertEquals(metadata.getDate(), nodeDate);
    assertEquals(metadata.getLabel(), node.getLabel());
  }

  public void test_metadata_last_fixing() {
    RepoCurveNode node = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD).withDate(CurveNodeDate.LAST_FIXING);
    assertThrowsWithCause(() -> node.metadata(VAL_DATE, REF_DATA), UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RepoCurveNode test = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    coverImmutableBean(test);
    RepoCurveNode test2 = RepoCurveNode.of(
        RepoTemplate.of(Tenor.ofWeeks(1), COLLATERAL, CONVENTION), QuoteId.of(StandardId.of("OG", "Repo2")));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RepoCurveNode test = RepoCurveNode.of(TEMPLATE, QUOTE_ID, SPREAD);
    assertSerialization(test);
  }

}

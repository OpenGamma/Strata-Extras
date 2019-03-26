/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.extra.bondcurve.SimpleLegalEntitySecurity;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link SimpleLegalEntitySecurity}.
 */
@Test
public class SimpleLegalEntitySecurityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LegalEntityId LEGAL_ENTITY = LegalEntityId.of("OG", "ABC");

  public void test_of() {
    SimpleLegalEntitySecurity test = SimpleLegalEntitySecurity.of(LEGAL_ENTITY);
    assertThrowsIllegalArg(() -> test.getCurrency());
    assertThrowsIllegalArg(() -> test.createProduct(REF_DATA));
    assertThrowsIllegalArg(() -> test.createTrade(TradeInfo.empty(), 1d, 1d, REF_DATA));
    assertThrowsIllegalArg(() -> test.getInfo());
    assertThrowsIllegalArg(() -> test.getSecurityId());
    assertEquals(test.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(test.getUnderlyingIds(), ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleLegalEntitySecurity test1 = SimpleLegalEntitySecurity.of(LEGAL_ENTITY);
    coverImmutableBean(test1);
    SimpleLegalEntitySecurity test2 = SimpleLegalEntitySecurity.of(LegalEntityId.of("OG", "DEF"));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SimpleLegalEntitySecurity test = SimpleLegalEntitySecurity.of(LEGAL_ENTITY);
    assertSerialization(test);
  }

}

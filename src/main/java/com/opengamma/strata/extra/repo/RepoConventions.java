/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Standardized repo conventions.
 */
public final class RepoConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<RepoConvention> ENUM_LOOKUP = ExtendedEnum.of(RepoConvention.class);

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private RepoConventions() {
  }

}

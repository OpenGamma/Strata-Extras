/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.gen.PropertyDefinition;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * Generates a legal entity discounting provider based on an existing provider.
 * <p>
 * This takes a base {@link ImmutableLegalEntityDiscountingProvider} and list of curve definitions
 * to generate a child provider.
 */
public final class ImmutableLegalEntityDiscountingProviderGenerator
    implements LegalEntityDiscountingProviderGenerator {

  /**
   * The underlying known data.
   * <p>
   * This includes known curves.
   */
  private final ImmutableLegalEntityDiscountingProvider knownProvider;
  /**
   * The curve definitions for the new curves to be generated.
   */
  private final ImmutableList<CurveDefinition> curveDefinitions;
  /**
   * The list of curve metadata associated with each definition.
   * <p>
   * The size of this list must match the size of the definition list.
   */
  private final ImmutableList<CurveMetadata> curveMetadata;
  /**
   * The groups used to find a repo curve by security.
   * <p>
   * This maps the security ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SecurityId, RepoGroup> repoCurveSecurityGroups;
  /**
   * The groups used to find a repo curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   * The group is used to find the curve in {@code repoCurves}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<LegalEntityId, RepoGroup> repoCurveGroups;
  /**
   * The repo curves in the curve group, keyed by repo group and currency.
   * <p>
   * The map should contains all the curves in the definition list but may have more names
   * than the curve definition list. Only the curves in the definitions list are created.
   */
  private final ImmutableSetMultimap<CurveName, Pair<RepoGroup, Currency>> repoCurveNames;
  /**
   * The groups used to find an issuer curve.
   * <p>
   * This maps the legal entity ID to a group.
   */
  private final ImmutableMap<LegalEntityId, LegalEntityGroup> issuerCurveGroups;
  /**
   * The issuer curves in the curve group, keyed by legal entity group and currency.
   * <p>
   * The map should contains all the curve in the definition list but may have more names
   * than the curve definition list. Only the curves in the definitions list are created
   */
  private final ImmutableSetMultimap<CurveName, Pair<LegalEntityGroup, Currency>> issuerCurveNames;

  /**
   * Obtains a generator from an existing provider and definition.
   * 
   * @param knownProvider  the underlying known provider
   * @param groupDefn  the curve group definition
   * @param refData  the reference data to use
   * @return the generator
   */
  public static ImmutableLegalEntityDiscountingProviderGenerator of(
      ImmutableLegalEntityDiscountingProvider knownProvider,
      LegalEntityDiscountingCurveGroupDefinition groupDefn,
      ReferenceData refData) {

    List<CurveDefinition> curveDefns = new ArrayList<>();
    List<CurveMetadata> curveMetadata = new ArrayList<>();
    SetMultimap<CurveName, Pair<RepoGroup, Currency>> repoNames = HashMultimap.create();
    SetMultimap<CurveName, Pair<LegalEntityGroup, Currency>> issuerNames = HashMultimap.create();

    for (CurveDefinition curveDefn : groupDefn.getCurveDefinitions()) {
      curveDefns.add(curveDefn);
      curveMetadata.add(curveDefn.metadata(knownProvider.getValuationDate(), refData));
      CurveName curveName = curveDefn.getName();
      // A curve group is guaranteed to include an entry for every definition
      Optional<RepoCurveEntry> repoCurveEntry = groupDefn.findRepoCurveEntry(curveName);
      if (repoCurveEntry.isPresent()) {
        repoNames.putAll(curveName, repoCurveEntry.get().getRepoCurveGroups());
      }
      Optional<IssuerCurveEntry> issuerCurveEntry = groupDefn.findIssuerCurveEntry(curveName);
      if (issuerCurveEntry.isPresent()) {
        issuerNames.putAll(curveName, issuerCurveEntry.get().getIssuerCurveGroups());
      }
    }
    return new ImmutableLegalEntityDiscountingProviderGenerator(
        knownProvider,
        curveDefns,
        curveMetadata,
        groupDefn.getRepoCurveSecurityGroups(),
        groupDefn.getRepoCurveGroups(),
        repoNames,
        groupDefn.getIssuerCurveGroups(),
        issuerNames);
  }

  // creates an instance
  private ImmutableLegalEntityDiscountingProviderGenerator(
      ImmutableLegalEntityDiscountingProvider knownProvider,
      List<CurveDefinition> curveDefinitions,
      List<CurveMetadata> curveMetadata,
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      SetMultimap<CurveName, Pair<RepoGroup, Currency>> repoCurveNames,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      SetMultimap<CurveName, Pair<LegalEntityGroup, Currency>> issuerCurveNames) {

    this.knownProvider = ArgChecker.notNull(knownProvider, "knownProvider");
    this.curveDefinitions = ImmutableList.copyOf(ArgChecker.notNull(curveDefinitions, "curveDefinitions"));
    this.curveMetadata = ImmutableList.copyOf(ArgChecker.notNull(curveMetadata, "curveMetadata"));
    this.repoCurveSecurityGroups = ImmutableMap.copyOf(
        ArgChecker.notNull(repoCurveSecurityGroups, "repoCurveSecurityGroups"));
    this.repoCurveGroups = ImmutableMap.copyOf(ArgChecker.notNull(repoCurveGroups, "repoCurveGroups"));
    this.repoCurveNames = ImmutableSetMultimap.copyOf(ArgChecker.notNull(repoCurveNames, "repoCurveNames"));
    this.issuerCurveGroups = ImmutableMap.copyOf(ArgChecker.notNull(issuerCurveGroups, "issuerCurveGroups"));
    this.issuerCurveNames = ImmutableSetMultimap.copyOf(ArgChecker.notNull(issuerCurveNames, "issuerCurveNames"));
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableLegalEntityDiscountingProvider generate(
      DoubleArray parameters,
      Map<CurveName, JacobianCalibrationMatrix> jacobians,
      Map<CurveName, DoubleArray> sensitivitiesMarketQuote) {

    // collect curves for child provider based on existing provider
    Map<SecurityId, RepoGroup> repoCurveSecurityGroupsNew = new HashMap<>(knownProvider.getRepoCurveSecurityGroups());
    Map<LegalEntityId, RepoGroup> repoCurveGroupsNew = new HashMap<>(knownProvider.getRepoCurveGroups());
    Map<LegalEntityId, LegalEntityGroup> issuerCurveGroupsNew = new HashMap<>(knownProvider.getIssuerCurveGroups());
    Map<Pair<RepoGroup, Currency>, DiscountFactors> repoCurves = new HashMap<>(knownProvider.getRepoCurves());
    Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> issuerCurves =
        new HashMap<>(knownProvider.getIssuerCurves());

    // generate curves from combined parameter array
    int startIndex = 0;
    for (int i = 0; i < curveDefinitions.size(); i++) {
      CurveDefinition curveDefn = curveDefinitions.get(i);
      CurveMetadata metadata = curveMetadata.get(i);
      CurveName name = curveDefn.getName();
      // extract parameters for the child curve
      int paramCount = curveDefn.getParameterCount();
      DoubleArray curveParams = parameters.subArray(startIndex, startIndex + paramCount);
      startIndex += paramCount;
      // create the child curve
      CurveMetadata childMetadata = childMetadata(metadata, curveDefn, jacobians, sensitivitiesMarketQuote);
      Curve curve = curveDefn.curve(knownProvider.getValuationDate(), childMetadata, curveParams);
      // put child curve into maps
      Set<Pair<RepoGroup, Currency>> repoCurveGroupsForName = repoCurveNames.get(name);
      for (Pair<RepoGroup, Currency> repoCurveGroupForName : repoCurveGroupsForName) {
        repoCurves.put(
            repoCurveGroupForName,
            DiscountFactors.of(repoCurveGroupForName.getSecond(), knownProvider.getValuationDate(), curve));
      }
      Set<Pair<LegalEntityGroup, Currency>> issuerCurveGroupsForName = issuerCurveNames.get(name);
      for (Pair<LegalEntityGroup, Currency> issuerCurveGroupForName : issuerCurveGroupsForName) {
        issuerCurves.put(
            issuerCurveGroupForName,
            DiscountFactors.of(issuerCurveGroupForName.getSecond(), knownProvider.getValuationDate(), curve));
      }
    }
    repoCurveSecurityGroupsNew.putAll(repoCurveSecurityGroups);
    repoCurveGroupsNew.putAll(repoCurveGroups);
    issuerCurveGroupsNew.putAll(issuerCurveGroups);

    return knownProvider.toBuilder()
        .repoCurveSecurityGroups(repoCurveSecurityGroupsNew)
        .repoCurveGroups(repoCurveGroupsNew)
        .repoCurves(repoCurves)
        .issuerCurveGroups(issuerCurveGroupsNew)
        .issuerCurves(issuerCurves)
        .build();
  }

  // build the map of additional info
  private CurveMetadata childMetadata(
      CurveMetadata metadata,
      CurveDefinition curveDefn,
      Map<CurveName, JacobianCalibrationMatrix> jacobians,
      Map<CurveName, DoubleArray> sensitivitiesMarketQuote) {

    JacobianCalibrationMatrix jacobian = jacobians.get(curveDefn.getName());
    CurveMetadata metadataResult = metadata;
    if (jacobian != null) {
      metadataResult = metadata.withInfo(CurveInfoType.JACOBIAN, jacobian);
    }
    DoubleArray sensitivity = sensitivitiesMarketQuote.get(curveDefn.getName());
    if (sensitivity != null) {
      metadataResult = metadataResult.withInfo(CurveInfoType.PV_SENSITIVITY_TO_MARKET_QUOTE, sensitivity);
    }
    return metadataResult;
  }

}

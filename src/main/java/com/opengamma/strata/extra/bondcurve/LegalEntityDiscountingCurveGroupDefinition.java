/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.bondcurve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.SecurityId;

/**
 * Provides the definition of how to calibrate a group of legal entity discounting curves.
 * <p>
 * A curve group contains one or more entries, each of which contains the definition of a curve, 
 * a set of currencies, and repo/issuer groups specifying how the curve is to be used.
 */
@BeanDefinition
public final class LegalEntityDiscountingCurveGroupDefinition
    implements ImmutableBean, Serializable {

  /**
  * The name of the curve group.
  */
  @PropertyDefinition(validate = "notNull")
  private final CurveGroupName name;
  /**
   * The groups used to find a repo curve by security.
   * <p>
   * This maps the security ID to a group.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SecurityId, RepoGroup> repoCurveSecurityGroups;
  /**
   * The groups used to find a repo curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<LegalEntityId, RepoGroup> repoCurveGroups;
  /**
   * The groups used to find an issuer curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<LegalEntityId, LegalEntityGroup> issuerCurveGroups;
  /**
   * The configuration for building the repo curves in the group.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<RepoCurveEntry> repoCurveEntries;
  /**
   * The configuration for building the issuer curves in the group.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<IssuerCurveEntry> issuerCurveEntries;
  /**
   * Definitions which specify how the curves are calibrated.
   * <p>
   * Curve definitions are required for curves that need to be calibrated.
   * A definition is not necessary if the curve is not built by the Strata curve calibrator.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends CurveDefinition>")
  private final ImmutableList<CurveDefinition> curveDefinitions;
  /**
   * The flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
   */
  @PropertyDefinition
  private final boolean computeJacobian;
  /**
   * The flag indicating if present value sensitivity to market quotes should be computed and stored in metadata or not.
   */
  @PropertyDefinition
  private final boolean computePvSensitivityToMarketQuote;

  /**
   * Entries for the repo curves, keyed by the curve name.
   */
  private final transient ImmutableMap<CurveName, RepoCurveEntry> repoCurveEntriesByName;  // not a property
  /**
   * Entries for the issuer curves, keyed by the curve name.
   */
  private final transient ImmutableMap<CurveName, IssuerCurveEntry> issuerCurveEntriesByName;  // not a property
  /**
   * Definitions for the curves, keyed by the curve name.
   */
  private final transient ImmutableMap<CurveName, CurveDefinition> curveDefinitionsByName;  // not a property

  //-------------------------------------------------------------------------
  /**
   * Returns a legal entity discounting curve group definition with the specified name,
   * curve groups and curve entries.
   * <p>
   * The Jacobian matrices are computed.
   * The Present Value sensitivity to Market quotes are not computed.
   *
   * @param name  the name of the curve group definition
   * @param repoCurveGroups  the repo curve groups
   * @param issuerCurveGroups  the issuer curve groups
   * @param repoCurveEntries  the repo curve entries
   * @param issuerCurveEntries  the issuer curve entries
   * @param curveDefinitions  the curve definitions
   * @return the curve group definition
   */
  public static LegalEntityDiscountingCurveGroupDefinition of(
      CurveGroupName name,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      List<RepoCurveEntry> repoCurveEntries,
      List<IssuerCurveEntry> issuerCurveEntries,
      List<? extends CurveDefinition> curveDefinitions) {

    return new LegalEntityDiscountingCurveGroupDefinition(
        name,
        ImmutableMap.of(),
        repoCurveGroups,
        issuerCurveGroups,
        repoCurveEntries,
        issuerCurveEntries,
        curveDefinitions,
        true,
        false);
  }

  /**
   * Returns a legal entity discounting curve group definition with the specified name,
   * curve groups and curve entries.
   * <p>
   * The Jacobian matrices are computed.
   * The Present Value sensitivity to Market quotes are not computed.
   *
   * @param name  the name of the curve group definition
   * @param repoCurveSecurityGroups  the repo curve security groups
   * @param repoCurveGroups  the repo curve groups
   * @param issuerCurveGroups  the issuer curve groups
   * @param repoCurveEntries  the repo curve entries
   * @param issuerCurveEntries  the issuer curve entries
   * @param curveDefinitions  the curve definitions
   * @return the curve group definition
   */
  public static LegalEntityDiscountingCurveGroupDefinition of(
      CurveGroupName name,
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      List<RepoCurveEntry> repoCurveEntries,
      List<IssuerCurveEntry> issuerCurveEntries,
      List<? extends CurveDefinition> curveDefinitions) {

    return new LegalEntityDiscountingCurveGroupDefinition(
        name,
        repoCurveSecurityGroups,
        repoCurveGroups,
        issuerCurveGroups,
        repoCurveEntries,
        issuerCurveEntries,
        curveDefinitions,
        true,
        false);
  }

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private LegalEntityDiscountingCurveGroupDefinition(
      CurveGroupName name,
      Map<SecurityId, RepoGroup> repoCurveSecurityGroups,
      Map<LegalEntityId, RepoGroup> repoCurveGroups,
      Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups,
      List<RepoCurveEntry> repoCurveEntries,
      List<IssuerCurveEntry> issuerCurveEntries,
      List<? extends CurveDefinition> curveDefinitions,
      boolean computeJacobian,
      boolean computePvSensitivityToMarketQuote) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(repoCurveSecurityGroups, "repoCurveSecurityGroups");
    JodaBeanUtils.notNull(repoCurveGroups, "repoCurveGroups");
    JodaBeanUtils.notNull(issuerCurveGroups, "issuerCurveGroups");
    JodaBeanUtils.notNull(repoCurveEntries, "repoCurveEntries");
    JodaBeanUtils.notNull(issuerCurveEntries, "issuerCurveEntries");
    JodaBeanUtils.notNull(curveDefinitions, "curveDefinitions");
    this.name = name;
    this.repoCurveSecurityGroups = ImmutableMap.copyOf(repoCurveSecurityGroups);
    this.repoCurveGroups = ImmutableMap.copyOf(repoCurveGroups);
    this.issuerCurveGroups = ImmutableMap.copyOf(issuerCurveGroups);
    this.repoCurveEntries = ImmutableList.copyOf(repoCurveEntries);
    this.issuerCurveEntries = ImmutableList.copyOf(issuerCurveEntries);
    this.curveDefinitions = ImmutableList.copyOf(curveDefinitions);
    this.computeJacobian = computeJacobian;
    this.computePvSensitivityToMarketQuote = computePvSensitivityToMarketQuote;
    this.repoCurveEntriesByName = repoCurveEntries.stream().collect(
        toImmutableMap(entry -> entry.getCurveName(), entry -> entry));
    this.issuerCurveEntriesByName = issuerCurveEntries.stream().collect(
        toImmutableMap(entry -> entry.getCurveName(), entry -> entry));
    this.curveDefinitionsByName = curveDefinitions.stream().collect(toImmutableMap(def -> def.getName(), def -> def));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a filtered version of this definition with no invalid nodes.
   * <p>
   * A curve is formed of a number of nodes, each of which has an associated date.
   * To be valid, the curve node dates must be in order from earliest to latest.
   * This method applies rules to remove invalid nodes.
   * 
   * @param valuationDate  the valuation date
   * @param refData  the reference data
   * @return the resolved definition, that should be used in preference to this one
   * @throws IllegalArgumentException if the curve nodes are invalid
   */
  public LegalEntityDiscountingCurveGroupDefinition filtered(LocalDate valuationDate, ReferenceData refData) {
    List<CurveDefinition> filtered = curveDefinitions.stream()
        .map(ncd -> ncd.filtered(valuationDate, refData))
        .collect(toImmutableList());
    return new LegalEntityDiscountingCurveGroupDefinition(
        name,
        repoCurveSecurityGroups,
        repoCurveGroups,
        issuerCurveGroups,
        repoCurveEntries,
        issuerCurveEntries,
        filtered,
        computeJacobian,
        computePvSensitivityToMarketQuote);
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the entry for the repo curve with the specified name.
   * <p>
   * If the repo curve is not found, optional empty is returned.
   *
   * @param curveName  the name of the repo curve
   * @return the entry for the repo curve with the specified name
   */
  public Optional<RepoCurveEntry> findRepoCurveEntry(CurveName curveName) {
    return Optional.ofNullable(repoCurveEntriesByName.get(curveName));
  }

  /**
   * Finds the entry for the issuer curve with the specified name.
   * <p>
   * If the issuer curve is not found, optional empty is returned.
   *
   * @param curveName  the name of the issuer curve
   * @return the entry for the issuer curve with the specified name
   */
  public Optional<IssuerCurveEntry> findIssuerCurveEntry(CurveName curveName) {
    return Optional.ofNullable(issuerCurveEntriesByName.get(curveName));
  }

  /**
   * Finds the definition for the curve with the specified name.
   * <p>
   * If the curve is not found, optional empty is returned.
   *
   * @param curveName  the name of the curve
   * @return the definition for the curve with the specified name
   */
  public Optional<CurveDefinition> findCurveDefinition(CurveName curveName) {
    return Optional.ofNullable(curveDefinitionsByName.get(curveName));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the curve metadata for each definition.
   * <p>
   * This method returns a list of metadata, one for each curve definition.
   *
   * @param valuationDate  the valuation date
   * @param refData  the reference data
   * @return the metadata
   */
  public ImmutableList<CurveMetadata> metadata(LocalDate valuationDate, ReferenceData refData) {
    return curveDefinitionsByName.values().stream()
        .map(curveDef -> curveDef.metadata(valuationDate, refData))
        .collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the total number of parameters in the group.
   * <p>
   * This returns the total number of parameters in the group, which equals the number of nodes.
   * The result of {@link #resolvedTrades(MarketData, ReferenceData)}, and
   * {@link #initialGuesses(MarketData)} will be of this size.
   * 
   * @return the number of parameters
   */
  public int getTotalParameterCount() {
    return curveDefinitionsByName.entrySet().stream().mapToInt(entry -> entry.getValue().getParameterCount()).sum();
  }

  /**
   * Creates a list of trades representing the instrument at each node.
   * <p>
   * This uses the observed market data to build the trade that each node represents.
   * The result combines the list of trades from each curve in order.
   * Each trade is created with a quantity of 1.
   * The valuation date is defined by the market data.
   *
   * @param marketData  the market data required to build a trade for the instrument, including the valuation date
   * @param refData  the reference data, used to resolve the trades
   * @return the list of all trades
   */
  public ImmutableList<ResolvedTrade> resolvedTrades(MarketData marketData, ReferenceData refData) {
    return curveDefinitionsByName.values().stream()
        .flatMap(curveDef -> curveDef.getNodes().stream())
        .map(node -> node.resolvedTrade(1d, marketData, refData))
        .collect(toImmutableList());
  }

  /**
   * Gets the list of all initial guesses.
   * <p>
   * This returns a list that combines the list of initial guesses from each curve in order.
   * The valuation date is defined by the market data.
   * 
   * @param marketData  the market data required to build a trade for the instrument, including the valuation date
   * @return the list of all initial guesses
   */
  public ImmutableList<Double> initialGuesses(MarketData marketData) {
    ImmutableList.Builder<Double> result = ImmutableList.builder();
    for (CurveDefinition defn : curveDefinitions) {
      result.addAll(defn.initialGuess(marketData));
    }
    return result.build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code LegalEntityDiscountingCurveGroupDefinition}.
   * @return the meta-bean, not null
   */
  public static LegalEntityDiscountingCurveGroupDefinition.Meta meta() {
    return LegalEntityDiscountingCurveGroupDefinition.Meta.INSTANCE;
  }

  static {
    MetaBean.register(LegalEntityDiscountingCurveGroupDefinition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static LegalEntityDiscountingCurveGroupDefinition.Builder builder() {
    return new LegalEntityDiscountingCurveGroupDefinition.Builder();
  }

  @Override
  public LegalEntityDiscountingCurveGroupDefinition.Meta metaBean() {
    return LegalEntityDiscountingCurveGroupDefinition.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the curve group.
   * @return the value of the property, not null
   */
  public CurveGroupName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find a repo curve by security.
   * <p>
   * This maps the security ID to a group.
   * @return the value of the property, not null
   */
  public ImmutableMap<SecurityId, RepoGroup> getRepoCurveSecurityGroups() {
    return repoCurveSecurityGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find a repo curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   * @return the value of the property, not null
   */
  public ImmutableMap<LegalEntityId, RepoGroup> getRepoCurveGroups() {
    return repoCurveGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the groups used to find an issuer curve by legal entity.
   * <p>
   * This maps the legal entity ID to a group.
   * @return the value of the property, not null
   */
  public ImmutableMap<LegalEntityId, LegalEntityGroup> getIssuerCurveGroups() {
    return issuerCurveGroups;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the configuration for building the repo curves in the group.
   * @return the value of the property, not null
   */
  public ImmutableList<RepoCurveEntry> getRepoCurveEntries() {
    return repoCurveEntries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the configuration for building the issuer curves in the group.
   * @return the value of the property, not null
   */
  public ImmutableList<IssuerCurveEntry> getIssuerCurveEntries() {
    return issuerCurveEntries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets definitions which specify how the curves are calibrated.
   * <p>
   * Curve definitions are required for curves that need to be calibrated.
   * A definition is not necessary if the curve is not built by the Strata curve calibrator.
   * @return the value of the property, not null
   */
  public ImmutableList<CurveDefinition> getCurveDefinitions() {
    return curveDefinitions;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
   * @return the value of the property
   */
  public boolean isComputeJacobian() {
    return computeJacobian;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating if present value sensitivity to market quotes should be computed and stored in metadata or not.
   * @return the value of the property
   */
  public boolean isComputePvSensitivityToMarketQuote() {
    return computePvSensitivityToMarketQuote;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      LegalEntityDiscountingCurveGroupDefinition other = (LegalEntityDiscountingCurveGroupDefinition) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(repoCurveSecurityGroups, other.repoCurveSecurityGroups) &&
          JodaBeanUtils.equal(repoCurveGroups, other.repoCurveGroups) &&
          JodaBeanUtils.equal(issuerCurveGroups, other.issuerCurveGroups) &&
          JodaBeanUtils.equal(repoCurveEntries, other.repoCurveEntries) &&
          JodaBeanUtils.equal(issuerCurveEntries, other.issuerCurveEntries) &&
          JodaBeanUtils.equal(curveDefinitions, other.curveDefinitions) &&
          (computeJacobian == other.computeJacobian) &&
          (computePvSensitivityToMarketQuote == other.computePvSensitivityToMarketQuote);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurveSecurityGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurveGroups);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoCurveEntries);
    hash = hash * 31 + JodaBeanUtils.hashCode(issuerCurveEntries);
    hash = hash * 31 + JodaBeanUtils.hashCode(curveDefinitions);
    hash = hash * 31 + JodaBeanUtils.hashCode(computeJacobian);
    hash = hash * 31 + JodaBeanUtils.hashCode(computePvSensitivityToMarketQuote);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("LegalEntityDiscountingCurveGroupDefinition{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("repoCurveSecurityGroups").append('=').append(repoCurveSecurityGroups).append(',').append(' ');
    buf.append("repoCurveGroups").append('=').append(repoCurveGroups).append(',').append(' ');
    buf.append("issuerCurveGroups").append('=').append(issuerCurveGroups).append(',').append(' ');
    buf.append("repoCurveEntries").append('=').append(repoCurveEntries).append(',').append(' ');
    buf.append("issuerCurveEntries").append('=').append(issuerCurveEntries).append(',').append(' ');
    buf.append("curveDefinitions").append('=').append(curveDefinitions).append(',').append(' ');
    buf.append("computeJacobian").append('=').append(computeJacobian).append(',').append(' ');
    buf.append("computePvSensitivityToMarketQuote").append('=').append(JodaBeanUtils.toString(computePvSensitivityToMarketQuote));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code LegalEntityDiscountingCurveGroupDefinition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<CurveGroupName> name = DirectMetaProperty.ofImmutable(
        this, "name", LegalEntityDiscountingCurveGroupDefinition.class, CurveGroupName.class);
    /**
     * The meta-property for the {@code repoCurveSecurityGroups} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<SecurityId, RepoGroup>> repoCurveSecurityGroups = DirectMetaProperty.ofImmutable(
        this, "repoCurveSecurityGroups", LegalEntityDiscountingCurveGroupDefinition.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code repoCurveGroups} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<LegalEntityId, RepoGroup>> repoCurveGroups = DirectMetaProperty.ofImmutable(
        this, "repoCurveGroups", LegalEntityDiscountingCurveGroupDefinition.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code issuerCurveGroups} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<LegalEntityId, LegalEntityGroup>> issuerCurveGroups = DirectMetaProperty.ofImmutable(
        this, "issuerCurveGroups", LegalEntityDiscountingCurveGroupDefinition.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code repoCurveEntries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<RepoCurveEntry>> repoCurveEntries = DirectMetaProperty.ofImmutable(
        this, "repoCurveEntries", LegalEntityDiscountingCurveGroupDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code issuerCurveEntries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<IssuerCurveEntry>> issuerCurveEntries = DirectMetaProperty.ofImmutable(
        this, "issuerCurveEntries", LegalEntityDiscountingCurveGroupDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code curveDefinitions} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CurveDefinition>> curveDefinitions = DirectMetaProperty.ofImmutable(
        this, "curveDefinitions", LegalEntityDiscountingCurveGroupDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code computeJacobian} property.
     */
    private final MetaProperty<Boolean> computeJacobian = DirectMetaProperty.ofImmutable(
        this, "computeJacobian", LegalEntityDiscountingCurveGroupDefinition.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code computePvSensitivityToMarketQuote} property.
     */
    private final MetaProperty<Boolean> computePvSensitivityToMarketQuote = DirectMetaProperty.ofImmutable(
        this, "computePvSensitivityToMarketQuote", LegalEntityDiscountingCurveGroupDefinition.class, Boolean.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "repoCurveSecurityGroups",
        "repoCurveGroups",
        "issuerCurveGroups",
        "repoCurveEntries",
        "issuerCurveEntries",
        "curveDefinitions",
        "computeJacobian",
        "computePvSensitivityToMarketQuote");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -1749299407:  // repoCurveSecurityGroups
          return repoCurveSecurityGroups;
        case -1279842095:  // repoCurveGroups
          return repoCurveGroups;
        case 1830129450:  // issuerCurveGroups
          return issuerCurveGroups;
        case 1389565235:  // repoCurveEntries
          return repoCurveEntries;
        case -985564678:  // issuerCurveEntries
          return issuerCurveEntries;
        case -336166639:  // curveDefinitions
          return curveDefinitions;
        case -1730091410:  // computeJacobian
          return computeJacobian;
        case -2061625469:  // computePvSensitivityToMarketQuote
          return computePvSensitivityToMarketQuote;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public LegalEntityDiscountingCurveGroupDefinition.Builder builder() {
      return new LegalEntityDiscountingCurveGroupDefinition.Builder();
    }

    @Override
    public Class<? extends LegalEntityDiscountingCurveGroupDefinition> beanType() {
      return LegalEntityDiscountingCurveGroupDefinition.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveGroupName> name() {
      return name;
    }

    /**
     * The meta-property for the {@code repoCurveSecurityGroups} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<SecurityId, RepoGroup>> repoCurveSecurityGroups() {
      return repoCurveSecurityGroups;
    }

    /**
     * The meta-property for the {@code repoCurveGroups} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<LegalEntityId, RepoGroup>> repoCurveGroups() {
      return repoCurveGroups;
    }

    /**
     * The meta-property for the {@code issuerCurveGroups} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<LegalEntityId, LegalEntityGroup>> issuerCurveGroups() {
      return issuerCurveGroups;
    }

    /**
     * The meta-property for the {@code repoCurveEntries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<RepoCurveEntry>> repoCurveEntries() {
      return repoCurveEntries;
    }

    /**
     * The meta-property for the {@code issuerCurveEntries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<IssuerCurveEntry>> issuerCurveEntries() {
      return issuerCurveEntries;
    }

    /**
     * The meta-property for the {@code curveDefinitions} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CurveDefinition>> curveDefinitions() {
      return curveDefinitions;
    }

    /**
     * The meta-property for the {@code computeJacobian} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> computeJacobian() {
      return computeJacobian;
    }

    /**
     * The meta-property for the {@code computePvSensitivityToMarketQuote} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> computePvSensitivityToMarketQuote() {
      return computePvSensitivityToMarketQuote;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).getName();
        case -1749299407:  // repoCurveSecurityGroups
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).getRepoCurveSecurityGroups();
        case -1279842095:  // repoCurveGroups
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).getRepoCurveGroups();
        case 1830129450:  // issuerCurveGroups
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).getIssuerCurveGroups();
        case 1389565235:  // repoCurveEntries
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).getRepoCurveEntries();
        case -985564678:  // issuerCurveEntries
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).getIssuerCurveEntries();
        case -336166639:  // curveDefinitions
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).getCurveDefinitions();
        case -1730091410:  // computeJacobian
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).isComputeJacobian();
        case -2061625469:  // computePvSensitivityToMarketQuote
          return ((LegalEntityDiscountingCurveGroupDefinition) bean).isComputePvSensitivityToMarketQuote();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code LegalEntityDiscountingCurveGroupDefinition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<LegalEntityDiscountingCurveGroupDefinition> {

    private CurveGroupName name;
    private Map<SecurityId, RepoGroup> repoCurveSecurityGroups = ImmutableMap.of();
    private Map<LegalEntityId, RepoGroup> repoCurveGroups = ImmutableMap.of();
    private Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups = ImmutableMap.of();
    private List<RepoCurveEntry> repoCurveEntries = ImmutableList.of();
    private List<IssuerCurveEntry> issuerCurveEntries = ImmutableList.of();
    private List<? extends CurveDefinition> curveDefinitions = ImmutableList.of();
    private boolean computeJacobian;
    private boolean computePvSensitivityToMarketQuote;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(LegalEntityDiscountingCurveGroupDefinition beanToCopy) {
      this.name = beanToCopy.getName();
      this.repoCurveSecurityGroups = beanToCopy.getRepoCurveSecurityGroups();
      this.repoCurveGroups = beanToCopy.getRepoCurveGroups();
      this.issuerCurveGroups = beanToCopy.getIssuerCurveGroups();
      this.repoCurveEntries = beanToCopy.getRepoCurveEntries();
      this.issuerCurveEntries = beanToCopy.getIssuerCurveEntries();
      this.curveDefinitions = beanToCopy.getCurveDefinitions();
      this.computeJacobian = beanToCopy.isComputeJacobian();
      this.computePvSensitivityToMarketQuote = beanToCopy.isComputePvSensitivityToMarketQuote();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -1749299407:  // repoCurveSecurityGroups
          return repoCurveSecurityGroups;
        case -1279842095:  // repoCurveGroups
          return repoCurveGroups;
        case 1830129450:  // issuerCurveGroups
          return issuerCurveGroups;
        case 1389565235:  // repoCurveEntries
          return repoCurveEntries;
        case -985564678:  // issuerCurveEntries
          return issuerCurveEntries;
        case -336166639:  // curveDefinitions
          return curveDefinitions;
        case -1730091410:  // computeJacobian
          return computeJacobian;
        case -2061625469:  // computePvSensitivityToMarketQuote
          return computePvSensitivityToMarketQuote;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (CurveGroupName) newValue;
          break;
        case -1749299407:  // repoCurveSecurityGroups
          this.repoCurveSecurityGroups = (Map<SecurityId, RepoGroup>) newValue;
          break;
        case -1279842095:  // repoCurveGroups
          this.repoCurveGroups = (Map<LegalEntityId, RepoGroup>) newValue;
          break;
        case 1830129450:  // issuerCurveGroups
          this.issuerCurveGroups = (Map<LegalEntityId, LegalEntityGroup>) newValue;
          break;
        case 1389565235:  // repoCurveEntries
          this.repoCurveEntries = (List<RepoCurveEntry>) newValue;
          break;
        case -985564678:  // issuerCurveEntries
          this.issuerCurveEntries = (List<IssuerCurveEntry>) newValue;
          break;
        case -336166639:  // curveDefinitions
          this.curveDefinitions = (List<? extends CurveDefinition>) newValue;
          break;
        case -1730091410:  // computeJacobian
          this.computeJacobian = (Boolean) newValue;
          break;
        case -2061625469:  // computePvSensitivityToMarketQuote
          this.computePvSensitivityToMarketQuote = (Boolean) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public LegalEntityDiscountingCurveGroupDefinition build() {
      return new LegalEntityDiscountingCurveGroupDefinition(
          name,
          repoCurveSecurityGroups,
          repoCurveGroups,
          issuerCurveGroups,
          repoCurveEntries,
          issuerCurveEntries,
          curveDefinitions,
          computeJacobian,
          computePvSensitivityToMarketQuote);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name of the curve group.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(CurveGroupName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the groups used to find a repo curve by security.
     * <p>
     * This maps the security ID to a group.
     * @param repoCurveSecurityGroups  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurveSecurityGroups(Map<SecurityId, RepoGroup> repoCurveSecurityGroups) {
      JodaBeanUtils.notNull(repoCurveSecurityGroups, "repoCurveSecurityGroups");
      this.repoCurveSecurityGroups = repoCurveSecurityGroups;
      return this;
    }

    /**
     * Sets the groups used to find a repo curve by legal entity.
     * <p>
     * This maps the legal entity ID to a group.
     * @param repoCurveGroups  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurveGroups(Map<LegalEntityId, RepoGroup> repoCurveGroups) {
      JodaBeanUtils.notNull(repoCurveGroups, "repoCurveGroups");
      this.repoCurveGroups = repoCurveGroups;
      return this;
    }

    /**
     * Sets the groups used to find an issuer curve by legal entity.
     * <p>
     * This maps the legal entity ID to a group.
     * @param issuerCurveGroups  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder issuerCurveGroups(Map<LegalEntityId, LegalEntityGroup> issuerCurveGroups) {
      JodaBeanUtils.notNull(issuerCurveGroups, "issuerCurveGroups");
      this.issuerCurveGroups = issuerCurveGroups;
      return this;
    }

    /**
     * Sets the configuration for building the repo curves in the group.
     * @param repoCurveEntries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurveEntries(List<RepoCurveEntry> repoCurveEntries) {
      JodaBeanUtils.notNull(repoCurveEntries, "repoCurveEntries");
      this.repoCurveEntries = repoCurveEntries;
      return this;
    }

    /**
     * Sets the {@code repoCurveEntries} property in the builder
     * from an array of objects.
     * @param repoCurveEntries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder repoCurveEntries(RepoCurveEntry... repoCurveEntries) {
      return repoCurveEntries(ImmutableList.copyOf(repoCurveEntries));
    }

    /**
     * Sets the configuration for building the issuer curves in the group.
     * @param issuerCurveEntries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder issuerCurveEntries(List<IssuerCurveEntry> issuerCurveEntries) {
      JodaBeanUtils.notNull(issuerCurveEntries, "issuerCurveEntries");
      this.issuerCurveEntries = issuerCurveEntries;
      return this;
    }

    /**
     * Sets the {@code issuerCurveEntries} property in the builder
     * from an array of objects.
     * @param issuerCurveEntries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder issuerCurveEntries(IssuerCurveEntry... issuerCurveEntries) {
      return issuerCurveEntries(ImmutableList.copyOf(issuerCurveEntries));
    }

    /**
     * Sets definitions which specify how the curves are calibrated.
     * <p>
     * Curve definitions are required for curves that need to be calibrated.
     * A definition is not necessary if the curve is not built by the Strata curve calibrator.
     * @param curveDefinitions  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curveDefinitions(List<? extends CurveDefinition> curveDefinitions) {
      JodaBeanUtils.notNull(curveDefinitions, "curveDefinitions");
      this.curveDefinitions = curveDefinitions;
      return this;
    }

    /**
     * Sets the {@code curveDefinitions} property in the builder
     * from an array of objects.
     * @param curveDefinitions  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curveDefinitions(CurveDefinition... curveDefinitions) {
      return curveDefinitions(ImmutableList.copyOf(curveDefinitions));
    }

    /**
     * Sets the flag indicating if the Jacobian matrices should be computed and stored in metadata or not.
     * @param computeJacobian  the new value
     * @return this, for chaining, not null
     */
    public Builder computeJacobian(boolean computeJacobian) {
      this.computeJacobian = computeJacobian;
      return this;
    }

    /**
     * Sets the flag indicating if present value sensitivity to market quotes should be computed and stored in metadata or not.
     * @param computePvSensitivityToMarketQuote  the new value
     * @return this, for chaining, not null
     */
    public Builder computePvSensitivityToMarketQuote(boolean computePvSensitivityToMarketQuote) {
      this.computePvSensitivityToMarketQuote = computePvSensitivityToMarketQuote;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("LegalEntityDiscountingCurveGroupDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("repoCurveSecurityGroups").append('=').append(JodaBeanUtils.toString(repoCurveSecurityGroups)).append(',').append(' ');
      buf.append("repoCurveGroups").append('=').append(JodaBeanUtils.toString(repoCurveGroups)).append(',').append(' ');
      buf.append("issuerCurveGroups").append('=').append(JodaBeanUtils.toString(issuerCurveGroups)).append(',').append(' ');
      buf.append("repoCurveEntries").append('=').append(JodaBeanUtils.toString(repoCurveEntries)).append(',').append(' ');
      buf.append("issuerCurveEntries").append('=').append(JodaBeanUtils.toString(issuerCurveEntries)).append(',').append(' ');
      buf.append("curveDefinitions").append('=').append(JodaBeanUtils.toString(curveDefinitions)).append(',').append(' ');
      buf.append("computeJacobian").append('=').append(JodaBeanUtils.toString(computeJacobian)).append(',').append(' ');
      buf.append("computePvSensitivityToMarketQuote").append('=').append(JodaBeanUtils.toString(computePvSensitivityToMarketQuote));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

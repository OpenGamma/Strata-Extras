/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.extra.repo;

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
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * A market convention for repo trades.
 * <p>
 * This defines the market convention for a repo.
 * <p>
 * The convention is defined by three dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Start date or spot date, the date on which the repo starts, for example, 1 business day after the trade date
 * <li>End date, the date on which the repo ends, for example, a number of days after the start date
 * </ul>
 * The period between the start date and the end date is specified by {@link RepoTemplate},
 * not by this convention.
 */
@BeanDefinition
public final class ImmutableRepoConvention
    implements RepoConvention, ImmutableBean, Serializable {

  /**
   * The primary currency.
   * <p>
   * This is the currency of the repo and the currency that payment is made in.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The convention name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
  /**
   * The business day adjustment to apply to the start and end date.
   * <p>
   * The start and end date will be adjusted as defined here.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment businessDayAdjustment;
  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 1 business day.
   * The start date of the repo is equal to the spot date 
   * and the end date of the repo is relative to the start date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DaysAdjustment spotDateOffset;

  //-----------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified currency, business day adjustment,
   * day count convention and spot date offset.
   * 
   * @param name  the name of the convention
   * @param currency  the currency, in which the payments are made
   * @param businessDayAdjustment  the business day adjustment to apply to the start and end date
   * @param dayCount  the day count convention, used to convert dates to a numerical value
   * @param spotDateOffset  the offset of the spot value date from the trade date
   * @return the convention
   */
  public static ImmutableRepoConvention of(
      String name,
      Currency currency,
      BusinessDayAdjustment businessDayAdjustment,
      DayCount dayCount,
      DaysAdjustment spotDateOffset) {

    return ImmutableRepoConvention.builder()
        .name(name)
        .currency(currency)
        .businessDayAdjustment(businessDayAdjustment)
        .dayCount(dayCount)
        .spotDateOffset(spotDateOffset)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public RepoTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      List<SecurityPosition> collateral,
      BuySell buySell,
      double notional,
      double rate) {

    Optional<LocalDate> tradeDate = tradeInfo.getTradeDate();
    if (tradeDate.isPresent()) {
      ArgChecker.inOrderOrEqual(tradeDate.get(), startDate, "tradeDate", "startDate");
    }
    return RepoTrade.builder()
        .info(tradeInfo)
        .product(Repo.builder()
            .collateral(collateral)
            .buySell(buySell)
            .currency(currency)
            .notional(notional)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(businessDayAdjustment)
            .rate(rate)
            .dayCount(dayCount)
            .build())
        .build();
  }

  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ImmutableRepoConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableRepoConvention.Meta meta() {
    return ImmutableRepoConvention.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ImmutableRepoConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableRepoConvention.Builder builder() {
    return new ImmutableRepoConvention.Builder();
  }

  private ImmutableRepoConvention(
      Currency currency,
      String name,
      BusinessDayAdjustment businessDayAdjustment,
      DayCount dayCount,
      DaysAdjustment spotDateOffset) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    this.currency = currency;
    this.name = name;
    this.businessDayAdjustment = businessDayAdjustment;
    this.dayCount = dayCount;
    this.spotDateOffset = spotDateOffset;
  }

  @Override
  public ImmutableRepoConvention.Meta metaBean() {
    return ImmutableRepoConvention.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary currency.
   * <p>
   * This is the currency of the repo and the currency that payment is made in.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the convention name.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment to apply to the start and end date.
   * <p>
   * The start and end date will be adjusted as defined here.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 1 business day.
   * The start date of the repo is equal to the spot date
   * and the end date of the repo is relative to the start date.
   * @return the value of the property, not null
   */
  @Override
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset;
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
      ImmutableRepoConvention other = (ImmutableRepoConvention) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableRepoConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ImmutableRepoConvention.class, Currency.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableRepoConvention.class, String.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", ImmutableRepoConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ImmutableRepoConvention.class, DayCount.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", ImmutableRepoConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "name",
        "businessDayAdjustment",
        "dayCount",
        "spotDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 3373707:  // name
          return name;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableRepoConvention.Builder builder() {
      return new ImmutableRepoConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableRepoConvention> beanType() {
      return ImmutableRepoConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((ImmutableRepoConvention) bean).getCurrency();
        case 3373707:  // name
          return ((ImmutableRepoConvention) bean).getName();
        case -1065319863:  // businessDayAdjustment
          return ((ImmutableRepoConvention) bean).getBusinessDayAdjustment();
        case 1905311443:  // dayCount
          return ((ImmutableRepoConvention) bean).getDayCount();
        case 746995843:  // spotDateOffset
          return ((ImmutableRepoConvention) bean).getSpotDateOffset();
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
   * The bean-builder for {@code ImmutableRepoConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableRepoConvention> {

    private Currency currency;
    private String name;
    private BusinessDayAdjustment businessDayAdjustment;
    private DayCount dayCount;
    private DaysAdjustment spotDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableRepoConvention beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.name = beanToCopy.getName();
      this.businessDayAdjustment = beanToCopy.getBusinessDayAdjustment();
      this.dayCount = beanToCopy.getDayCount();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 3373707:  // name
          return name;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        case 1905311443:  // dayCount
          return dayCount;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
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
    public ImmutableRepoConvention build() {
      return new ImmutableRepoConvention(
          currency,
          name,
          businessDayAdjustment,
          dayCount,
          spotDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the primary currency.
     * <p>
     * This is the currency of the repo and the currency that payment is made in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the convention name.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end date.
     * <p>
     * The start and end date will be adjusted as defined here.
     * @param businessDayAdjustment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      JodaBeanUtils.notNull(businessDayAdjustment, "businessDayAdjustment");
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date.
     * <p>
     * The offset is applied to the trade date and is typically plus 1 business day.
     * The start date of the repo is equal to the spot date
     * and the end date of the repo is relative to the start date.
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ImmutableRepoConvention.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

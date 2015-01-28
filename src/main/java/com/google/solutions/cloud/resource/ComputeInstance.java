package com.google.solutions.cloud.resource;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.appengine.api.datastore.EmbeddedEntity;

import java.util.Date;

public class ComputeInstance extends Resource {
  public static final String NAME = "name";
  public static final String CREATION_TIME = "creationTime";
  public static final String ZONE = "zone";

  private String name;
  private Date creationTime;
  private String zone;

  public String getName() {
    return this.name;
  }

  public Date getCreationTime() {
    return this.creationTime;
  }

  public String getZone() {
    return this.zone;
  }

  public ComputeInstance setName(String newName) {
    this.name = newName;
    return this;
  }

  public ComputeInstance setCreationTime(Date newCreationTime) {
    this.creationTime = newCreationTime;
    return this;
  }

  public ComputeInstance setZone(String newZone) {
    this.zone = newZone;
    return this;
  }

  @Override
  public EmbeddedEntity toDatastoreEmbeddedEntity() {
    EmbeddedEntity e = new EmbeddedEntity();
    e.setProperty(RESOURCE_TYPE, this.getResourceType().toString());
    e.setProperty(NAME, this.name);
    e.setProperty(CREATION_TIME, this.creationTime);
    e.setProperty(ZONE, this.zone);
    return e;
  }

  public static ComputeInstance fromDatastoreEmbeddedEntity(EmbeddedEntity e) {
    checkNotNull(e);

    return new ComputeInstance()
        .setName((String) e.getProperty(NAME))
        .setCreationTime((Date) e.getProperty(CREATION_TIME))
        .setZone((String) e.getProperty(ZONE));
  }

  @Override
  public ResourceType getResourceType() {
    return ResourceType.COMPUTE_INSTANCE;
  }
}

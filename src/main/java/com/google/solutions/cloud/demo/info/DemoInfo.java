package com.google.solutions.cloud.demo.info;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.base.MoreObjects;
import com.google.solutions.cloud.resource.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * "Java bean"-style class for holding information about a single demo
 * instance, within a particular demo app, specifically intended for usage
 * with GAE Datastore.
 */
public class DemoInfo {
  public static final String DEMO_KIND = "Demo";
  public static final String STATUS = "status";
  public static final String DESCRIPTION = "description";
  public static final String CREATION_TIME = "creationTime";
  public static final String RESOURCES = "resources";

  Long demoId;
  DemoStatus status;
  String description;
  Date creationTime;
  Collection<Resource> resources;

  public Long getDemoId() {
    return this.demoId;
  }

  public DemoStatus getStatus() {
    return this.status;
  }

  public String getDescription() {
    return this.description;
  }

  public Date getCreationTime() {
    return this.creationTime;
  }

  public Collection<Resource> getResources() {
    return this.resources;
  }

  public DemoInfo setDemoId(Long newDemoId) {
    this.demoId = newDemoId;
    return this;
  }

  public DemoInfo setStatus(DemoStatus newStatus) {
    this.status = newStatus;
    return this;
  }

  public DemoInfo setDescription(String newDescription) {
    this.description = newDescription;
    return this;
  }

  public DemoInfo setCreationTime(Date newCreationTime) {
    this.creationTime = newCreationTime;
    return this;
  }

  public DemoInfo setResources(Collection<Resource> newResources) {
    this.resources = newResources;
    return this;
  }

  public Entity toDatastoreEntity(Key parentKey) {
    checkNotNull(parentKey);

    Entity e;
    if (this.demoId != null) {
      e = new Entity(DEMO_KIND, this.demoId, parentKey);
    } else {
      e = new Entity(DEMO_KIND, parentKey);
    }

    // indexed
    e.setProperty(STATUS, this.status.toString());
    e.setProperty(CREATION_TIME, this.creationTime);
    // unindexed
    Collection<EmbeddedEntity> resourceEntities = new ArrayList<>();
    if (this.resources != null) {
      for (Resource resource : this.resources) {
        resourceEntities.add(resource.toDatastoreEmbeddedEntity());
      }
    }
    e.setUnindexedProperty(RESOURCES, resourceEntities);
    e.setUnindexedProperty(DESCRIPTION, this.description);

    return e;
  }

  @SuppressWarnings("unchecked")
  public static DemoInfo fromDatastoreEntity(Entity e) {
    checkNotNull(e);

    // unpack the 'resources' property, which is a nullable collection
    // of EmbeddedEntities
    Object resourcesProp = MoreObjects.firstNonNull(e.getProperty(RESOURCES),
        Arrays.asList());
    Collection<EmbeddedEntity> resourceEntities =
        (Collection<EmbeddedEntity>) resourcesProp;
    Collection<Resource> resources = new ArrayList<>();
    for (EmbeddedEntity resourceEntity : resourceEntities) {
      resources.add(Resource.fromDatastoreEmbeddedEntity(resourceEntity));
    }

    return new DemoInfo()
        .setDemoId(e.getKey().getId())
        .setStatus(DemoStatus.valueOf((String) e.getProperty(STATUS)))
        .setCreationTime((Date) e.getProperty(CREATION_TIME))
        .setDescription((String) e.getProperty(DESCRIPTION))
        .setResources(resources);
  }
}

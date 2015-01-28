package com.google.solutions.cloud.resource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.appengine.api.datastore.EmbeddedEntity;


public abstract class Resource {
  public static final String RESOURCE_KIND = "Resource";
  public static final String RESOURCE_TYPE = "resourceType";

  public abstract ResourceType getResourceType();

  public abstract EmbeddedEntity toDatastoreEmbeddedEntity();

  public static Resource fromDatastoreEmbeddedEntity(EmbeddedEntity e) {
    checkNotNull(e);
    checkArgument(e.getProperty(RESOURCE_TYPE) instanceof String);

    ResourceType resourceType = ResourceType.valueOf((String) e.getProperty(RESOURCE_TYPE));
    switch (resourceType) {
      case COMPUTE_INSTANCE:
        return ComputeInstance.fromDatastoreEmbeddedEntity(e);
      default:
        throw new IllegalArgumentException("unsupported ResourceType: " + resourceType);
    }
  }
}

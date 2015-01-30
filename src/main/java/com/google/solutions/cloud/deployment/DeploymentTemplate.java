package com.google.solutions.cloud.deployment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.solutions.cloud.demo.info.DemoInfo;

public abstract class DeploymentTemplate {
  public static String TYPE = "type";

  public abstract DeploymentType getDeploymentType();
  public abstract EmbeddedEntity toEmbeddedEntity();

  public static DeploymentTemplate fromEmbeddedEntity(EmbeddedEntity e) {
    checkNotNull(e);
    Object typeProp = e.getProperty(TYPE);
    checkArgument(typeProp instanceof String,
        "invalid deployment type value: %s", typeProp);
    DeploymentType type = DeploymentType.valueOf((String) typeProp);
    switch (type) {
      case SINGLE_INSTANCE:
        return SingleInstanceDeployment.fromEmbeddedEntity(e);
      default:
        throw new IllegalArgumentException("unsupported DeploymentType: " + type);
    }
  }

  public abstract void launch(DeploymentManager deploymentManager);
  public abstract void teardown(DeploymentManager deploymentManager);
  public abstract void updateDemoInfo(DeploymentManager deploymentManager,
      DemoInfo demoInfo);
}

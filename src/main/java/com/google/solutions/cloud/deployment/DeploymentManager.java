package com.google.solutions.cloud.deployment;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.services.compute.model.Instance;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.solutions.cloud.compute.ComputeInstanceHelper;
import com.google.solutions.cloud.compute.ComputeService;
import com.google.solutions.cloud.demo.info.DemoInfo;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class DeploymentManager {
  private static final Logger LOGGER = Logger.getLogger(DeploymentManager.class.toString());
  private final ComputeService computeService;

  public DeploymentManager(ComputeService computeService) {
    this.computeService = checkNotNull(computeService);
  }

  public void launch(SingleInstanceDeployment sid) {
    try {
      this.computeService.createInstance(sid.getInstanceTemplate());
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  public void teardown(SingleInstanceDeployment sid) {
    Instance instanceTemplate = sid.getInstanceTemplate();
    try {
      Optional<Instance> instance = this.computeService.getInstanceInformation(instanceTemplate);
      if (instance.isPresent()) {
        this.computeService.deleteInstance(instance.get());
      } else {
        LOGGER.info(String.format("instance, %s, in zone %s, not found during teardown attempt",
            instanceTemplate.getName(), instanceTemplate.getZone()));
      }
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  public void updateDemoInfo(SingleInstanceDeployment sid, DemoInfo demoInfo) {
    Instance instanceTemplate = sid.getInstanceTemplate();
    String instanceName = instanceTemplate.getName();
    Optional<Instance> fullInstance = this.computeService.getInstanceInformation(instanceTemplate);
    if (fullInstance.isPresent()) {
      demoInfo.setDeploymentStatus(fullInstance.get().getStatus());
      Map<String, String> fullDeploymentMetadata = ImmutableMap.of(
          "instanceDetailURL", ComputeInstanceHelper.getInstanceDetailUrl(instanceName),
          "sshURL", ComputeInstanceHelper.getSshUrl(instanceName));
      demoInfo.setFullDeploymentMetadta(fullDeploymentMetadata);
    }
  }

  public void launch(DeploymentTemplate unsupported) {
    throw new UnsupportedOperationException("unsupported template: " + unsupported);
  }

  public void teardown(DeploymentTemplate unsupported) {
    throw new UnsupportedOperationException("unsupported template: " + unsupported);
  }

  public void updateDemoInfo(DeploymentTemplate unsupported) {
    throw new UnsupportedOperationException("unsupported template: " + unsupported);
  }
}

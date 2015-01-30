package com.google.solutions.cloud.compute;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.AttachedDiskInitializeParams;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.google.api.services.compute.model.Scheduling;
import com.google.api.services.compute.model.ServiceAccount;
import com.google.api.services.compute.model.Tags;
import com.google.common.collect.FluentIterable;
import com.google.solutions.cloud.Constants;

import java.util.Arrays;

public final class ComputeInstanceHelper {

  static final String URL_PREFIX = "https://www.googleapis.com/compute";
  static final String API_VERSION = "v1";
  static final String PROJECT_URL = String.format("%s/%s/projects/%s",
      URL_PREFIX, API_VERSION, Constants.PROJECT_ID);

  static final String IMAGE_PROJECT_URL = String.format("%s/%s/projects/%s",
      URL_PREFIX, API_VERSION, Constants.IMAGE_PROJECT_URL);

  static final String MACHINE_TYPE_URL = String.format("%s/zones/%s/machineTypes/%s",
      PROJECT_URL, Constants.DEFAULT_ZONE, Constants.MACHINE_TYPE);

  static final String ZONE_URL = String.format("%s/zones/%s",
      PROJECT_URL, Constants.DEFAULT_ZONE);

  static final String IMAGE_URL = String.format("%s/global/images/%s",
      IMAGE_PROJECT_URL, Constants.IMAGE_NAME);

  static final String NETWORK_URL = PROJECT_URL + "/global/networks/default";

  static final String INSTANCE_DETAIL_URL_FORMAT = String.format(
      "https://console.developers.google.com/project/%s/compute/instancesDetail/zones/%s/instances/%%s",
      Constants.PROJECT_ID, Constants.DEFAULT_ZONE);

  static final String SSH_URL_FORMAT = String.format(
      "https://cloudssh.developers.google.com/projects/%s/zones/%s/instances/%%s",
      Constants.PROJECT_ID, Constants.DEFAULT_ZONE);

  public static String getInstanceDetailUrl(String instanceName) {
    checkNotNull(instanceName);
    return String.format(INSTANCE_DETAIL_URL_FORMAT, instanceName);
  }

  public static String getSshUrl(String instanceName) {
    checkNotNull(instanceName);
    return String.format(SSH_URL_FORMAT, instanceName);
  }

  public static Instance makeDefaultInstance(String name) {
    checkNotNull(name);

    return new Instance()
        .setName(name)
        .setTags(new Tags().setItems(Constants.INSTANCE_TAGS))
        .setMachineType(MACHINE_TYPE_URL)
        .setDisks(Arrays.asList(makeDefaultBootDisk()))
        .setNetworkInterfaces(Arrays.asList(makeDefaultNetworkInterface()))
        .setScheduling(makeDefaultScheduling())
        .setServiceAccounts(Arrays.asList(makeDefaultServiceAccount()));
  }

  static AttachedDisk makeDefaultBootDisk() {
    return new AttachedDisk()
        .setBoot(true)
        .setType("PERSISTENT")
        .setMode("READ_WRITE")
        .setAutoDelete(true)
        .setInitializeParams(new AttachedDiskInitializeParams()
            .setSourceImage(IMAGE_URL));
  }

  static NetworkInterface makeDefaultNetworkInterface() {
    return new NetworkInterface()
        .setNetwork(NETWORK_URL)
        .setAccessConfigs(Arrays.asList(
            new AccessConfig().setName("External NAT")
                .setType("ONE_TO_ONE_NAT"))
         );
  }

  static Scheduling makeDefaultScheduling() {
    return new Scheduling()
        .setAutomaticRestart(true)
        .setOnHostMaintenance("MIGRATE");
  }

  static ServiceAccount makeDefaultServiceAccount() {
    return new ServiceAccount()
        .setEmail("default")
        .setScopes(FluentIterable.from(Constants.ADDITONAL_SERVICE_SCOPES)
            .append("https://www.googleapis.com/auth/compute").toList());
  }
}

package com.google.solutions.cloud.compute;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Instance;
import com.google.appengine.api.utils.SystemProperty;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.solutions.cloud.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;

public class ComputeService {
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();

  private final Compute compute;

  public ComputeService() {
    this.compute = this.buildComputeService();
  }

  public void createInstance(Instance instance) throws IOException {
    checkNotNull(instance);
    this.compute.instances().insert(Constants.PROJECT_ID,
        Constants.DEFAULT_ZONE, instance).execute();
  }

  public void deleteInstance(Instance instance) throws IOException {
    checkNotNull(instance);
    this.compute.instances().delete(Constants.PROJECT_ID,
        Constants.DEFAULT_ZONE, instance.getName()).execute();
  }

  public Optional<Instance> getInstanceInformation(Instance instanceTemplate) {
    checkNotNull(instanceTemplate);
    try {
      return Optional.of(this.compute.instances().get(Constants.PROJECT_ID, Constants.DEFAULT_ZONE,
          instanceTemplate.getName()).execute());
    } catch (Exception e) {
      return Optional.absent();
    }
  }

  private Compute buildComputeService() {
    return new Compute(HTTP_TRANSPORT, JSON_FACTORY, getCredentialsForServerToServer());
  }

  private static SystemProperty.Environment.Value getEnvValue() {
    if (SystemProperty.environment == null || SystemProperty.environment.value() == null) {
      return SystemProperty.Environment.Value.Development;
    }
    return SystemProperty.environment.value();
  }

  static HttpRequestInitializer getCredentialsForServerToServer() {
    SystemProperty.Environment.Value env = getEnvValue();

    switch(env) {
      case Development:
        try {
          InputStream is = ComputeService.class.getResourceAsStream(Constants.DEV_SERVER_KEY_LOCATION);
          checkState(is != null, "\n\nis null...\n\n");
          String secretPassword = "notasecret";
          KeyStore keyStore = KeyStore.getInstance("PKCS12");
          checkState(keyStore != null, "\n\nkeyStore null...\n\n");
          keyStore.load(is, secretPassword.toCharArray());
          PrivateKey pk = (PrivateKey) keyStore.getKey("privatekey", secretPassword.toCharArray());
          checkState(pk != null, "\n\npk null...\n\n");

          return new GoogleCredential.Builder()
              .setTransport(HTTP_TRANSPORT)
              .setJsonFactory(JSON_FACTORY)
              .setServiceAccountId(Constants.SERVICE_ACCOUNT_ID)
              .setServiceAccountScopes(Arrays.asList(ComputeScopes.COMPUTE))
              .setServiceAccountPrivateKey(pk)
              .build();
        } catch (GeneralSecurityException | IOException e) {
          throw Throwables.propagate(e);
        }
      case Production:
        return new AppIdentityCredential(Arrays.asList(ComputeScopes.COMPUTE));
      default:
        throw new RuntimeException("unsupported env value: " + env);
    }
  }

  public static void main(String... args) {
    getCredentialsForServerToServer();
  }

// TODO: delete me (everything below this comment)
//  public static void main(String... args) throws InterruptedException, IOException {
//    Instance instance = ComputeInstanceHelper.makeDefaultInstance("baz");
//    DeploymentManager dm = new DeploymentManager(
//        new DatastoreDemoInfoPersistence(DatastoreServiceFactory.getDatastoreService()),
//        new ComputeService());
//
//    SingleInstanceDeployment sid = new SingleInstanceDeployment()
//        .setInstanceTemplate(instance);
//    System.out.println("creating...");
//    sid.launch(dm);
//
//    System.out.println("sleeping for a couple minutes...");
//    Thread.sleep(120_000);
//
//    System.out.println("deleting...");
//    sid.teardown(dm);
//  }
//
//  void watchOperation(Operation op) {
//    int timeoutMillis = 180 * 1000;
//    int timeEllapsedMillis = 0;
//    int tryCount = 0;
//    int delayMultiplier = 250;
//    float delayBase = 1.5F;
//
//    try {
//      while (timeEllapsedMillis < timeoutMillis) {
//        Operation zOp = this.compute.zoneOperations().get(Constants.PROJECT_ID, Constants.DEFAULT_ZONE,
//            op.getName()).execute();
//
//        String status = zOp.getStatus();
//        System.out.println("status: " + status);
//        System.out.println("progress: " + zOp.getProgress());
//        if ("DONE".equals(status)) {
//          com.google.api.services.compute.model.Operation.Error error = zOp.getError();
//          if (error != null) {
//            System.out.println(error.toPrettyString());
//          }
//          break;
//        }
//        long delayMillis = Math.min(10000, delayMultiplier * (long) Math.pow(delayBase, tryCount++));
//        System.out.format("%d try so far...sleeping %d millis...%n", tryCount, delayMillis);
//        Thread.sleep(delayMillis);
//        timeEllapsedMillis += delayMillis;
//      }
//    } catch (Exception e) {
//      Throwables.propagate(e);
//    }
//  }
}

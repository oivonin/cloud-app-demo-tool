package com.google.solutions.cloud;

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Contains the client IDs and scopes for allowed clients consuming your API.
 */
public class Constants {
  public static final String WEB_CLIENT_ID = "replace this with your web client ID";
  public static final String ANDROID_CLIENT_ID = "replace this with your Android client ID";
  public static final String IOS_CLIENT_ID = "replace this with your iOS client ID";
  public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;

  public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

  // TODO: is it reasonable to have users edit the properties below directly?
  //       or does it need to be "injected" through some "config"?
  public static final String PROJECT_ID = "cloud-app-demo-tool";
  public static final String SERVICE_ACCOUNT_ID = "336574263319-vb6ehef5q52o149duud4kv7stse02hcp@developer.gserviceaccount.com";
  public static final String DEV_SERVER_KEY_LOCATION = "dev-key.p12";

  public static final String MACHINE_TYPE = "n1-standard-2";
  public static final String DEFAULT_ZONE = "us-central1-a";
  public static final String IMAGE_PROJECT_URL = "ubuntu-os-cloud";
  public static final String IMAGE_NAME = "ubuntu-1410-utopic-v20141217";

  public static final List<String> INSTANCE_TAGS = Arrays.asList(
      // add any tags that should be applied to GCE instances here
  );

  public static final List<String> ADDITONAL_SERVICE_SCOPES = Arrays.asList(
    // "https://www.googleapis.com/auth/compute" is added by default
    // any additional scopes needed for specific apps should be added here
  );

  public static final Map<String, String> CUSTOM_METADATA = ImmutableMap.of(
      // "your-key", "your-value"
  );
}

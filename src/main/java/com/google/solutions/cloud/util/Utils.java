package com.google.solutions.cloud.util;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Utils {
  public static void checkAllParamsNotNull(Object... params) {
    for (int i = 0; i < params.length; i++) {
      checkNotNull(params[i], "param #%d out of %d is null", i, params.length);
    }
  }
}

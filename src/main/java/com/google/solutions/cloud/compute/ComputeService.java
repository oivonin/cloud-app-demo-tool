package com.google.solutions.cloud.compute;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.services.compute.Compute;

public class ComputeService {

  private final Compute compute;

  public ComputeService(Compute compute) {
    this.compute = checkNotNull(compute);
  }



}

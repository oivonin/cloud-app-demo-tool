package com.google.solutions.cloud.persistence;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.solutions.cloud.demo.info.DemoInfo;
import com.google.solutions.cloud.demo.info.DemoStatus;

import java.util.Collection;
import java.util.List;

/**
 * Persistence interface for {@link DemoInfo} records.
 */
public interface DemoInfoPersistence {

  /**
   * Create a new {@link DemoInfo} record for the specified user, within this
   * demo application.
   * @param username username for the demo.
   * @param initialDemoInfo {@link DemoInfo} record containing the initial
   * information for this {@link DemoInfo} instance (such as a description).
   * Note that any fields which should NOT be initialized at the time of
   * record creation (such as demoId) will be cleared/reassigned before this
   * record is saved.
   * @return {@link DemoInfo} record containing, at a minimum, the demoId and
   * the initial {@link DemoStatus} of the newly created demo.
   */
  DemoInfo createNewDemo(String username, DemoInfo initialDemoInfo);

  /**
   * Fetch the specified {@link DemoInfo} record.
   * @param username username for the demo.
   * @param demoId id of the demo.
   * @return {@link Optional} containing the requested {@link DemoInfo} record,
   * if any such record could be found; {@link Optional#absent()}, otherwise.
   */
  Optional<DemoInfo> get(String username, Long demoId);

  /**
   * Delete the specified {@link DemoInfo} record.
   * @param username username for the demo.
   * @param demoId id of the demo.
   */
  void delete(String username, Long demoId);

  /**
   * Update the {@link DemoStatus} of the specified {@link DemoInfo} record,
   * if such a record exists.
   *
   * @param username username for the demo.
   * @param demoId id of the demo.
   * @param newStatus new status for the demo.
   */
  void updateStatus(String username,
      Long demoId, DemoStatus newStatus);

  /**
   * Retrieve all {@link DemoInfo} records, within this demo application, which
   * have the indicated status.
   * @username name of the user, on behalf of whom these records are being
   * requested.
   * @return {@link Collection} containing all active {@link DemoInfo} records
   * which were found for the specified user, sorted
   * by creationTime in descending order.
   */
  List<DemoInfo> findAllActiveDemosForUser(String username);

  /**
   * Retrieve all {@link DemoInfo} records, within this demo application, which
   * have the indicated status.
   * @param status status to search for.
   * @return {@link Multimap} containing all of the matching {@link DemoInfo}
   * records, in which they keys are usernames and the values are the
   * {@link DemoInfo} instances for those users.
   */
  Multimap<String, DemoInfo> findAllDemosWithStatus(DemoStatus status);

  /**
   * Reserve the requested number of instance names for the specified
   * user.
   * @param username name of the user, for whom instance names are being
   * reserved.
   * @param numInstances number of instance names required.
   * @return an integer which represents the first of $numInstances sequential
   * integers which can be used to form a unique instance name, as follows:
   * $username-$integerSuffix
   */
  int reserveInstanceNames(String username, int numInstances);
}

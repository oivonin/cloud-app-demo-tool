package com.google.solutions.cloud.persistence;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Transaction;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.solutions.cloud.demo.info.DemoInfo;
import com.google.solutions.cloud.demo.info.DemoStatus;
import com.google.solutions.cloud.resource.Resource;
import com.google.solutions.cloud.user.info.UserInfo;
import com.google.solutions.cloud.util.Utils;

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

public class DatastoreDemoInfoPersistence implements DemoInfoPersistence {
  // FIXME: using a retry helper library with exponential backoff is always better...
  private static final int MAX_TRANSACTION_RETRIES = 5;
  private static final long TRANSACTION_RETRY_DELAY_MILLIS = 200;

  private final DatastoreService datastore;

  public DatastoreDemoInfoPersistence(DatastoreService datastore) {
    this.datastore = checkNotNull(datastore);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.persistence.DemoInfoPersistence#createNewDemo(java.lang.String, java.lang.String)
   */
  @Override
  public DemoInfo createNewDemo(String username, DemoInfo initialDemoInfo) {
    checkNotNull(username);

    // make sure all fields are in a "pre-creation" state
    initialDemoInfo.setDemoId(null);
    initialDemoInfo.setStatus(DemoStatus.ACTIVE);

    DateTime creationTime = DateTime.now();
    initialDemoInfo.setCreationTime(creationTime.toDate());

    if (initialDemoInfo.getDescription() == null) {
      initialDemoInfo.setDescription(String.format(
          "demo instance created by %s at %s",
          username, creationTime));
    }

    Key usernameKey = createUsernameKey(username);
    Entity entityToPut = initialDemoInfo.toDatastoreEntity(usernameKey);
    Key createdEntityKey = this.datastore.put(entityToPut);

    return initialDemoInfo.setDemoId(createdEntityKey.getId());
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.persistence.DemoInfoPersistence#get(java.lang.String, java.lang.String, java.lang.Long)
   */
  @Override
  public Optional<DemoInfo> get(String username, Long demoId) {
    Utils.checkAllParamsNotNull(username, demoId);

    Key demoKey = createDemoKey(username, demoId);
    try {
      return Optional.of(DemoInfo.fromDatastoreEntity(this.datastore.get(demoKey)));
    } catch (EntityNotFoundException e) {
      return Optional.absent();
    }
  }

  @Override
  public void delete(String username, Long demoId) {
    Utils.checkAllParamsNotNull(username, demoId);

    Key demoKey = createDemoKey(username, demoId);
    this.datastore.delete(demoKey);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.persistence.DemoInfoPersistence#updateStatus(java.lang.String, java.lang.String, java.lang.Long, com.google.solutions.cloud.demo.DemoStatus)
   */
  @Override
  public void updateStatus(String username,
      Long demoId, final DemoStatus newStatus) {
    Utils.checkAllParamsNotNull(username, demoId, newStatus);

    final Key demoKey = createDemoKey(username, demoId);
    TransactionBlock<Void> block = new TransactionBlock<Void>() {
      @Override
      public Void execute(Transaction txn) throws Exception {
        DemoInfo currentRecord = DemoInfo.fromDatastoreEntity(
            DatastoreDemoInfoPersistence.this.datastore.get(txn, demoKey));
        DemoStatus previousStatus = currentRecord.getStatus();
        checkState(previousStatus != null,
            "DemoInfo record, '%s' has null DemoStatus value",
            demoKey);
        currentRecord.setStatus(newStatus);
        DatastoreDemoInfoPersistence.this.datastore.put(txn, currentRecord.toDatastoreEntity(demoKey.getParent()));
        txn.commit();
        return null;
      }
    };

    this.tryTransaction(block);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.persistence.DemoInfoPersistence#findAllActiveDemosForUser(java.lang.String, java.lang.String)
   */
  @Override
  public List<DemoInfo> findAllActiveDemosForUser(String username) {
    checkNotNull(username);

    Key usernameKey = createUsernameKey(username);
    Multimap<String, DemoInfo> results = this.findAllDemosWithStatus(
        DemoStatus.ACTIVE, Optional.of(usernameKey));
    checkState(Sets.difference(results.keySet(), ImmutableSet.of(username)).isEmpty(),
        "queried active demos for user '%s', got multimap keys: '%s'",
        username, results.keySet());

    return FluentIterable.from(results.get(username))
        .toSortedList(CREATION_TIME_DESC_COMPARATOR);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.persistence.DemoInfoPersistence#findAllDemosWithStatus(com.google.solutions.cloud.demo.DemoStatus)
   */
  @Override
  public Multimap<String, DemoInfo> findAllDemosWithStatus(DemoStatus status) {
    checkNotNull(status);
    return this.findAllDemosWithStatus(status, Optional.<Key>absent());
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.persistence.DemoInfoPersistence#reserveInstanceNames(java.lang.String, int)
   */
  @Override
  public int reserveInstanceNames(final String username, final int numInstances) {
    checkNotNull(username);
    checkArgument(numInstances > 0,
        "numInstances must be positive. got request for %d instances from %s",
        numInstances, username);

    final Key userKey = createUsernameKey(username);
    TransactionBlock<Integer> block = new TransactionBlock<Integer>() {
      @Override
      public Integer execute(Transaction txn) throws Exception {
        UserInfo userInfo;
        int nameSuffix = 0;
        try {
          Entity e = DatastoreDemoInfoPersistence.this.datastore.get(txn, userKey);
          userInfo = UserInfo.fromDatastoreEntity(e);
          nameSuffix = userInfo.getCurrentResourceNameSuffix();
          // if a record already exists, update it so that the next available
          // name suffix is $(current suffix) + $(numInstances)
          userInfo.setCurrentResourceNameSuffix(nameSuffix + numInstances);
        } catch (EntityNotFoundException e) {
          // if we're creating a new record, initialize the name suffix to
          // $(numInstances), since we're using that many names right now
          userInfo = new UserInfo().setUserName(username).setCurrentResourceNameSuffix(numInstances);
        }

        DatastoreDemoInfoPersistence.this.datastore.put(txn, userInfo.toDatastoreEntity());
        txn.commit();
        return nameSuffix;
      }
    };

    return this.tryTransaction(block);
  }

  /* (non-Javadoc)
   * @see com.google.solutions.cloud.persistence.DemoInfoPersistence#saveResources(java.lang.String, java.lang.Long, com.google.solutions.cloud.resource.Resource[])
   */
  @Override
  public void saveResources(final String username, final Long demoId,
      final Collection<Resource> resources) {
    Utils.checkAllParamsNotNull(username, demoId);
    if (resources.isEmpty()) {
      return;
    }

    final Key demoInfoKey = createDemoKey(username, demoId);
    TransactionBlock<Void> block = new TransactionBlock<Void>() {
      @Override
      public Void execute(Transaction txn) throws Exception {
        DemoInfo demoInfo = DemoInfo.fromDatastoreEntity(DatastoreDemoInfoPersistence.this.datastore.get(txn, demoInfoKey));

        demoInfo.setResources(resources);
        DatastoreDemoInfoPersistence.this.datastore.put(txn,
            demoInfo.toDatastoreEntity(demoInfoKey.getParent()));
        txn.commit();
        return null;
      }
    };

    this.tryTransaction(block);
  }

  // comparator for sorting DemoInfo instances by creation time in descending order
  private static final Comparator<DemoInfo> CREATION_TIME_DESC_COMPARATOR =
    new Comparator<DemoInfo>() {
      @Override
      public int compare(DemoInfo d1, DemoInfo d2) {
        Utils.checkAllParamsNotNull(d1, d2);
        checkNotNull(d1.getCreationTime(),
            "DemoInfo record, (id: %d), has null creationTime value",
            d1.getDemoId());
        checkNotNull(d2.getCreationTime(),
            "DemoInfo record, (id: %d), has null creationTime value",
            d2.getDemoId());
        return d2.getCreationTime().compareTo(d1.getCreationTime());
      }
  };

  // query for all DemoInfo records with the specified status/ancestory, returning:
  // Multimap of (username -> DemoInfo)
  private Multimap<String, DemoInfo> findAllDemosWithStatus(DemoStatus status, Optional<Key> ancestorKey) {
    Utils.checkAllParamsNotNull(status, ancestorKey);

    Query query;
    if (ancestorKey.isPresent()) {
      query = new Query(DemoInfo.DEMO_KIND, ancestorKey.get());
    } else {
      query =  new Query(DemoInfo.DEMO_KIND);
    }

    query.setFilter(new Query.FilterPredicate(DemoInfo.STATUS,
        FilterOperator.EQUAL,
        status.toString()))
        .addSort(DemoInfo.CREATION_TIME, SortDirection.DESCENDING);

    PreparedQuery pq = this.datastore.prepare(query);

    Multimap<String, DemoInfo> usernamesToDemos = ArrayListMultimap.create();
    for (Entity e : pq.asIterable()) {
      Key parent = e.getParent();
      checkState(parent != null && UserInfo.USER_KIND.equals(parent.getKind()),
          "found DemoInfo, '%s', with parent '%s' (expected parent kind '%s'",
          e.getKey(), parent, UserInfo.USER_KIND);
      String username = e.getParent().getName();
      usernamesToDemos.put(username, DemoInfo.fromDatastoreEntity(e));
    }
    return usernamesToDemos;
  }

  // attempt to execute the given block in a transaction,
  // retrying only on ConcurrentModificationException and
  // InterruptedException. if any other exception is thrown
  // from the block, it will be propagated as a RuntimeException.
  private <T> T tryTransaction(TransactionBlock<T> block) {
    checkNotNull(block);

    Transaction txn = this.datastore.beginTransaction();

    int retryCount = 0;
    do {
      try {
        if (retryCount > 0) {
          Thread.sleep(TRANSACTION_RETRY_DELAY_MILLIS);
        }
        return block.execute(txn);
      } catch (ConcurrentModificationException | InterruptedException e) {
        // ConcurrentModificationException => maybe retry
        // InterruptedException => ...just swallow this one
      } catch (Exception e) {
        Throwables.propagate(e);
      } finally {
        if (txn.isActive()) {
          txn.rollback();
        }
      }
    } while (retryCount++ < MAX_TRANSACTION_RETRIES);

    throw new ConcurrentModificationException(String.format(
        "abandoning transaction after %d unsuccessful attempts",
        MAX_TRANSACTION_RETRIES));
  }

  private static interface TransactionBlock<T> {
    T execute(Transaction txn) throws Exception;
  }

  private static Key createUsernameKey(String username) {
    return KeyFactory.createKey(UserInfo.USER_KIND, username);
  }

  private static Key createDemoKey(String username, Long demoId) {
    Key usernameKey = createUsernameKey(username);
    return KeyFactory.createKey(usernameKey, DemoInfo.DEMO_KIND, demoId);
  }
}

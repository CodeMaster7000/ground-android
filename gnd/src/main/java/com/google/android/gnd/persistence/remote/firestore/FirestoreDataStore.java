/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.persistence.remote.firestore;

import com.google.android.gms.tasks.Task;
import com.google.android.gnd.Config;
import com.google.android.gnd.model.Mutation;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.model.observation.ResponseDelta;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.android.gnd.persistence.remote.NotFoundException;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.firestore.schema.GroundFirestore;
import com.google.android.gnd.persistence.sync.PhotoSyncWorkManager;
import com.google.android.gnd.rx.RxTask;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.rx.ValueOrError;
import com.google.android.gnd.ui.util.FileUtil;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.WriteBatch;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.FileNotFoundException;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class FirestoreDataStore implements RemoteDataStore {

  static final String ID_COLLECTION = "/ids";

  @Inject GroundFirestore db;
  @Inject Schedulers schedulers;
  @Inject PhotoSyncWorkManager photoSyncWorkManager;
  @Inject FileUtil fileUtil;

  @Inject
  FirestoreDataStore() {}

  @Override
  public Single<Project> loadProject(String projectId) {
    return db.projects()
        .project(projectId)
        .get()
        .switchIfEmpty(Single.error(() -> new NotFoundException("Project " + projectId)))
        .subscribeOn(schedulers.io());
  }

  @Override
  public Single<ImmutableList<ValueOrError<Observation>>> loadObservations(Feature feature) {
    return db.projects()
        .project(feature.getProject().getId())
        .observations()
        .observationsByFeatureId(feature)
        .subscribeOn(schedulers.io());
  }

  @Override
  public Single<List<Project>> loadProjectSummaries(User user) {
    return db.projects().getReadable(user).subscribeOn(schedulers.io());
  }

  @Override
  public Flowable<RemoteDataEvent<Feature>> loadFeaturesOnceAndStreamChanges(Project project) {
    return db.projects()
        .project(project.getId())
        .features()
        .loadOnceAndStreamChanges(project)
        .subscribeOn(schedulers.io());
  }

  @Override
  public Completable applyMutations(ImmutableCollection<Mutation> mutations, User user) {
    return RxTask.toCompletable(() -> applyMutationsInternal(mutations, user))
        .subscribeOn(schedulers.io());
  }

  private Task<?> applyMutationsInternal(ImmutableCollection<Mutation> mutations, User user) {
    WriteBatch batch = db.batch();
    for (Mutation mutation : mutations) {
      try {
        addMutationToBatch(mutation, user, batch);
      } catch (DataStoreException e) {
        Timber.e(e, "Skipping invalid mutation");
      }
    }
    return batch.commit();
  }

  private void addMutationToBatch(Mutation mutation, User user, WriteBatch batch)
      throws DataStoreException {
    if (mutation instanceof FeatureMutation) {
      addFeatureMutationToBatch((FeatureMutation) mutation, user, batch);
    } else if (mutation instanceof ObservationMutation) {
      addObservationMutationToBatch((ObservationMutation) mutation, user, batch);
      uploadMediaMutations(((ObservationMutation) mutation).getResponseDeltas());
    } else {
      throw new DataStoreException("Unsupported mutation " + mutation.getClass());
    }
  }

  private void addFeatureMutationToBatch(FeatureMutation mutation, User user, WriteBatch batch)
      throws DataStoreException {
    db.projects()
        .project(mutation.getProjectId())
        .features()
        .feature(mutation.getFeatureId())
        .addMutationToBatch(mutation, user, batch);
  }

  private void addObservationMutationToBatch(
      ObservationMutation mutation, User user, WriteBatch batch) throws DataStoreException {
    db.projects()
        .project(mutation.getProjectId())
        .observations()
        .observation(mutation.getObservationId())
        .addMutationToBatch(mutation, user, batch);
  }

  private boolean isPhotoTypeResponse(String response) {
    // TODO: Add field type in the response deltas as this is fragile.
    return !response.isEmpty() && response.endsWith(Config.PHOTO_EXT);
  }

  @Nullable
  private String getLocalFilePath(String remotePath) {
    try {
      return fileUtil.getLocalFileFromDestinationPath(remotePath).getPath();
    } catch (FileNotFoundException e) {
      Timber.e("Local file doesn't exist anymore: %s", remotePath);
      return null;
    }
  }

  private void uploadMediaMutations(ImmutableList<ResponseDelta> responseDeltas) {
    for (ResponseDelta responseDelta : responseDeltas) {
      responseDelta
          .getNewResponse()
          .map(Object::toString) // TODO: getter in Response shouldn't require Field object.
          .filter(this::isPhotoTypeResponse)
          .ifPresent(
              remotePath -> {
                String localFilePath = getLocalFilePath(remotePath);
                if (localFilePath != null) {
                  photoSyncWorkManager.enqueueSyncWorker(localFilePath, remotePath);
                }
              });
    }
  }
}

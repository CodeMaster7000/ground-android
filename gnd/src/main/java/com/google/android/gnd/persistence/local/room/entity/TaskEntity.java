/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.persistence.local.room.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.android.gnd.model.task.Element;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.persistence.local.room.relations.FieldEntityAndRelations;
import com.google.android.gnd.persistence.local.room.relations.TaskEntityAndRelations;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableList;

@AutoValue
@Entity(
    tableName = "task",
    foreignKeys =
    @ForeignKey(
        entity = LayerEntity.class,
        parentColumns = "id",
        childColumns = "layer_id",
        onDelete = ForeignKey.CASCADE),
    indices = {@Index("layer_id")})
public abstract class TaskEntity {

  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "title")
  public abstract String getTitle();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "layer_id")
  public abstract String getLayerId();

  public static TaskEntity fromTask(String layerId, Task task) {
    return TaskEntity.builder().setId(task.getId()).setLayerId(layerId).build();
  }

  static Task toTask(TaskEntityAndRelations taskEntityAndRelations) {
    TaskEntity taskEntity = taskEntityAndRelations.taskEntity;
    Task.Builder taskBuilder = Task.newBuilder().setId(taskEntity.getId());

    ImmutableList.Builder<Element> listBuilder = ImmutableList.builder();
    for (FieldEntityAndRelations fieldEntityAndRelations :
        taskEntityAndRelations.fieldEntityAndRelations) {
      listBuilder.add(FieldEntity.toElement(fieldEntityAndRelations));
    }

    return taskBuilder.setElements(listBuilder.build()).build();
  }

  public static TaskEntity create(String id, String title, String layerId) {
    return builder().setId(id).setTitle(title).setLayerId(layerId).build();
    form
  }

  public static Builder builder() {
    return new AutoValue_TaskEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setTitle(String title);

    public abstract Builder setLayerId(String layerId);

    public abstract TaskEntity build();
  }
}

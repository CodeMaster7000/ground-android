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
import com.google.android.gnd.model.task.Element.Type;
import com.google.android.gnd.model.task.Field;
import com.google.android.gnd.persistence.local.room.models.ElementEntityType;
import com.google.android.gnd.persistence.local.room.models.FieldEntityType;
import com.google.android.gnd.persistence.local.room.relations.FieldEntityAndRelations;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import java.util.List;
import timber.log.Timber;

@AutoValue
@Entity(
    tableName = "field",
    foreignKeys =
    @ForeignKey(
        entity = TaskEntity.class,
        parentColumns = "id",
        childColumns = "task_id",
        onDelete = ForeignKey.CASCADE),
    indices = {@Index("task_id")})
public abstract class FieldEntity {

  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @ColumnInfo(name = "index")
  public abstract int getIndex();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "element_type")
  public abstract ElementEntityType getElementType();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "field_type")
  public abstract FieldEntityType getFieldType();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "label")
  public abstract String getLabel();

  @CopyAnnotations
  @ColumnInfo(name = "is_required")
  public abstract boolean isRequired();

  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "task_id")
  public abstract String getTaskId();

  public static FieldEntity fromField(String taskId, Type elementType, Field field) {
    return FieldEntity.builder()
        .setId(field.getId())
        .setIndex(field.getIndex())
        .setLabel(field.getLabel())
        .setRequired(field.isRequired())
        .setElementType(ElementEntityType.fromElementType(elementType))
        .setFieldType(FieldEntityType.fromFieldType(field.getType()))
        .setTaskId(taskId)
        .build();
  }

  static Element toElement(FieldEntityAndRelations fieldEntityAndRelations) {
    return fieldEntityAndRelations.fieldEntity.getElementType().toElementType() == Type.FIELD
        ? Element.ofField(FieldEntity.toField(fieldEntityAndRelations))
        : Element.ofUnknown();
  }

  private static Field toField(FieldEntityAndRelations fieldEntityAndRelations) {
    FieldEntity fieldEntity = fieldEntityAndRelations.fieldEntity;
    Field.Builder fieldBuilder =
        Field.newBuilder()
            .setId(fieldEntity.getId())
            .setIndex(fieldEntity.getIndex())
            .setLabel(fieldEntity.getLabel())
            .setRequired(fieldEntity.isRequired())
            .setType(fieldEntity.getFieldType().toFieldType());

    List<MultipleChoiceEntity> multipleChoiceEntities =
        fieldEntityAndRelations.multipleChoiceEntities;

    if (!multipleChoiceEntities.isEmpty()) {
      if (multipleChoiceEntities.size() > 1) {
        Timber.e("More than 1 multiple choice found for field");
      }

      fieldBuilder.setMultipleChoice(
          MultipleChoiceEntity.toMultipleChoice(
              multipleChoiceEntities.get(0), fieldEntityAndRelations.optionEntities));
    }

    return fieldBuilder.build();
  }

  public static FieldEntity create(
      String id,
      Integer index,
      ElementEntityType elementType,
      FieldEntityType fieldType,
      String label,
      boolean required,
      String taskId) {
    return builder()
        .setId(id)
        .setIndex(index)
        .setElementType(elementType)
        .setFieldType(fieldType)
        .setLabel(label)
        .setRequired(required)
        .setTaskId(taskId)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_FieldEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setIndex(int id);

    public abstract Builder setElementType(ElementEntityType elementType);

    public abstract Builder setFieldType(FieldEntityType fieldType);

    public abstract Builder setLabel(String label);

    public abstract Builder setRequired(boolean required);

    public abstract Builder setTaskId(String taskId);

    public abstract FieldEntity build();
  }
}

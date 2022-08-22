/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.model

import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Task
import com.google.common.collect.ImmutableList
import com.google.firebase.firestore.GeoPoint

/**
 * Helper methods for building model objects for use in tests. Method return builders with required
 * fields set to placeholder values. Rather than depending on these values, tests that test for
 * specific values should explicitly set them in relevant test methods or during test setup.
 */
object TestModelBuilders {
    private fun newGeoPoint(): GeoPoint = GeoPoint(0.0, 0.0)

    @JvmStatic
    fun newGeoPointPolygonVertices(): ImmutableList<GeoPoint> = ImmutableList.builder<GeoPoint>()
        .add(newGeoPoint())
        .add(newGeoPoint())
        .add(newGeoPoint())
        .build()

    @JvmStatic
    @JvmOverloads
    fun newTask(
        id: String = "",
        type: Task.Type = Task.Type.TEXT,
        multipleChoice: MultipleChoice? = null
    ): Task = Task(id, 0, type, "", false, multipleChoice)
}
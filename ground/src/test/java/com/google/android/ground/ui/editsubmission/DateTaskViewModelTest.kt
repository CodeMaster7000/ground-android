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
package com.google.android.ground.ui.editsubmission

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.submission.DateResponse.Companion.fromDate
import com.google.android.ground.rx.Nil
import com.google.common.truth.Truth.assertThat
import com.sharedtest.TestObservers.observeUntilFirstChange
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DateTaskViewModelTest : BaseHiltTest() {
    @Inject
    lateinit var dateTaskViewModel: DateTaskViewModel

    @Test
    fun testUpdateResponse() {
        dateTaskViewModel.updateResponse(TEST_DATE)

        observeUntilFirstChange(dateTaskViewModel.response)
        assertThat(dateTaskViewModel.response.value).isEqualTo(fromDate(TEST_DATE))
    }

    @Test
    fun testDialogClick() {
        val testObserver = dateTaskViewModel.showDialogClicks.test()

        dateTaskViewModel.onShowDialogClick()

        testObserver.assertNoErrors().assertNotComplete().assertValue(Nil.NIL)
    }

    companion object {
        // Date represented in milliseconds for date: 2021-09-24T16:40+0000.
        private val TEST_DATE = Date(1632501600000L)
    }
}
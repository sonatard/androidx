/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class OnBackPressedHandlerTest {

    lateinit var dispatcher: OnBackPressedDispatcher

    @Before
    fun setup() {
        dispatcher = OnBackPressedDispatcher()
    }

    @UiThreadTest
    @Test
    fun testAddCallback() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testRemoveCallback() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        onBackPressedCallback.removeCallback()
        assertWithMessage("Handler should return false when no OnBackPressedCallbacks " +
                "are registered")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        // Check that the count still equals 1
        assertWithMessage("Count shouldn't be incremented after removal")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testRemoveCallbackInCallback() {
        val onBackPressedCallback = object : CountingOnBackPressedCallback() {
            override fun handleOnBackPressed() {
                super.handleOnBackPressed()
                removeCallback()
            }
        }

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        assertWithMessage("Handler should return false when no OnBackPressedCallbacks " +
                "are registered")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
        dispatcher.onBackPressed()
        // Check that the count still equals 1
        assertWithMessage("Count shouldn't be incremented after removal")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testMultipleCalls() {
        val onBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        assertWithMessage("Handler should return true once a callback is added")
            .that(dispatcher.hasEnabledCallbacks())
            .isTrue()
        dispatcher.onBackPressed()
        dispatcher.onBackPressed()
        assertWithMessage("Count should be incremented after each onBackPressed")
            .that(onBackPressedCallback.count)
            .isEqualTo(2)
    }

    @UiThreadTest
    @Test
    fun testMostRecentGetsPriority() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val mostRecentOnBackPressedCallback = CountingOnBackPressedCallback()

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(mostRecentOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Most recent callback should be incremented")
            .that(mostRecentOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent callback should be incremented")
            .that(onBackPressedCallback.count)
            .isEqualTo(0)
    }

    @UiThreadTest
    @Test
    fun testDisabledListener() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val disabledOnBackPressedCallback = CountingOnBackPressedCallback(enabled = false)

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(disabledOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Disabled callbacks should not be incremented")
            .that(disabledOnBackPressedCallback.count)
            .isEqualTo(0)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "were disabled")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testPassthroughListener() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val passThroughOnBackPressedCallback = object : CountingOnBackPressedCallback() {
            override fun handleOnBackPressed() {
                super.handleOnBackPressed()
                // Trigger the next listener
                isEnabled = false
                dispatcher.onBackPressed()
            }
        }

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(passThroughOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Most recent callback should be incremented")
            .that(passThroughOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "disabled itself and called onBackPressed()")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallback() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOnBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOwner = object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this)

            override fun getLifecycle() = lifecycleRegistry
        }

        dispatcher.addCallback(onBackPressedCallback)
        dispatcher.addCallback(lifecycleOwner, lifecycleOnBackPressedCallback)
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(0)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "aren't started")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        // Now start the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        dispatcher.onBackPressed()
        assertWithMessage("Once the callbacks is started, the count should increment")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Only the most recent callback should be incremented")
            .that(onBackPressedCallback.count)
            .isEqualTo(1)

        // Now stop the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "aren't started")
            .that(onBackPressedCallback.count)
            .isEqualTo(2)

        // Now destroy the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dispatcher.onBackPressed()
        assertWithMessage("Non-started callbacks shouldn't have their count incremented")
            .that(lifecycleOnBackPressedCallback.count)
            .isEqualTo(1)
        assertWithMessage("Previous callbacks should be incremented if more recent callbacks " +
                "aren't started")
            .that(onBackPressedCallback.count)
            .isEqualTo(3)
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallbackDestroyed() {
        val onBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOwner = object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this)

            override fun getLifecycle() = lifecycleRegistry
        }
        // Start the Lifecycle at CREATED
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        dispatcher.addCallback(lifecycleOwner, onBackPressedCallback)
        assertWithMessage("Non-started callbacks shouldn't appear as an enabled dispatcher")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()

        // Now destroy the Lifecycle
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertWithMessage("Destroyed callbacks shouldn't appear as an enabled dispatcher")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()

        // Now start the Lifecycle - this wouldn't happen in a real Lifecycle since DESTROYED
        // is terminal but serves as a good test to make sure the Observer is cleaned up
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertWithMessage("Previously destroyed callbacks shouldn't appear as an enabled " +
                "dispatcher")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }

    @UiThreadTest
    @Test
    fun testLifecycleCallback_whenDestroyed() {
        val lifecycleOnBackPressedCallback = CountingOnBackPressedCallback()
        val lifecycleOwner = object : LifecycleOwner {
            val lifecycleRegistry = LifecycleRegistry(this)

            override fun getLifecycle() = lifecycleRegistry
        }
        // Start the Lifecycle as DESTROYED
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        dispatcher.addCallback(lifecycleOwner, lifecycleOnBackPressedCallback)

        assertWithMessage("Handler should return false when no OnBackPressedCallbacks " +
                "are registered")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()

        // Now start the Lifecycle - this wouldn't happen in a real Lifecycle since DESTROYED
        // is terminal but serves as a good test to make sure no lingering Observer exists
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertWithMessage("Previously destroyed callbacks shouldn't appear as an enabled " +
                "dispatcher")
            .that(dispatcher.hasEnabledCallbacks())
            .isFalse()
    }
}

open class CountingOnBackPressedCallback(
    enabled: Boolean = true
) : OnBackPressedCallback(enabled) {
    var count = 0

    override fun handleOnBackPressed() {
        count++
    }
}

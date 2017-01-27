/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

import android.support.annotation.NonNull;

import com.android.support.apptoolkit.internal.SafeIterableMap;

import java.util.Map;

/**
 * An implementation of {@link Lifecycle} that can handle multiple observers.
 * <p>
 * It is used by Fragments and Support Library Activities. You can also directly use it if you have
 * a custom LifecycleProvider.
 */
@SuppressWarnings("WeakerAccess")
public class LifecycleRegistry implements Lifecycle {
    /**
     * Custom list that keeps observers and can handle removals / additions during traversal.
     */
    private SafeIterableMap<LifecycleObserver, GenericLifecycleObserver> mObserverSet =
            new SafeIterableMap<>();
    /**
     * Current state
     */
    @State
    private int mState;
    /**
     * Latest event that was provided via {@link #handleLifecycleEvent(int)}.
     */
    @Event
    private int mLastEvent;

    /**
     * The provider that owns this Lifecycle.
     */
    private final LifecycleProvider mLifecycleProvider;

    /**
     * Creates a new LifecycleRegistry for the given provider.
     * <p>
     * You should usually create this inside your LifecycleProvider class's constructor and hold
     * onto the same instance.
     *
     * @param provider The owner LifecycleProvider
     */
    public LifecycleRegistry(@NonNull LifecycleProvider provider) {
        mLifecycleProvider = provider;
        mState = INITIALIZED;
    }

    /**
     * Sets the current state and notifies the observers.
     * <p>
     * Note that if the {@code currentState} is the same state as the last call to this method,
     * calling this method has no effect.
     *
     * @param event The event that was received
     */
    public void handleLifecycleEvent(@Event int event) {
        if (mLastEvent == event) {
            return;
        }
        mLastEvent = event;
        // TODO fake intermediate events
        mState = getStateAfter(event);
        for (Map.Entry<LifecycleObserver, GenericLifecycleObserver> entry: mObserverSet) {
            entry.getValue().onStateChanged(mLifecycleProvider, mLastEvent);
        }
    }

    @Override
    public void addObserver(LifecycleObserver observer) {
        mObserverSet.putIfAbsent(observer, Lifecycling.getCallback(observer));
    }

    @Override
    public void removeObserver(LifecycleObserver observer) {
        mObserverSet.remove(observer);
    }

    /**
     * The number of observers.
     *
     * @return The number of observers.
     */
    public int size() {
        return mObserverSet.size();
    }

    @Override
    @State
    public int getCurrentState() {
        return mState;
    }

    @Lifecycle.State
    static int getStateAfter(@Event int event) {
        // TODO do some masking logic to return this fast.
        switch (event) {
            case ON_CREATE:
            case ON_STOP:
                return STOPPED;
            case ON_START:
            case ON_PAUSE:
                return STARTED;
            case ON_RESUME:
                return RESUMED;
            case ON_DESTROY:
                return DESTROYED;
            case Lifecycle.ANY:
                break;
        }
        throw new RuntimeException("Unexpected state value");
    }
}

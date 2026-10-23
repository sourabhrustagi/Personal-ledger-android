package com.ledger.personal.ui.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundLockTest {

    private val dummyLifecycleOwner = object : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = throw UnsupportedOperationException("Not required for event dispatch")
    }

    @Test
    fun locksWhenBackgroundedAndForegrounded() {
        var lockCount = 0
        var isChangingConfig = false
        val observer = createBackgroundLockObserver(
            isChangingConfigurations = { isChangingConfig },
            onLock = { lockCount++ },
        )

        // Initial launch - ON_START should not trigger lock
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_START)
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_RESUME)
        assertEquals(0, lockCount)

        // App goes to background
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_STOP)
        assertEquals(1, lockCount)

        // App comes back to foreground
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_START)
        assertEquals(2, lockCount)
    }

    @Test
    fun doesNotLockOnConfigurationChange() {
        var lockCount = 0
        var isChangingConfig = true
        val observer = createBackgroundLockObserver(
            isChangingConfigurations = { isChangingConfig },
            onLock = { lockCount++ },
        )

        // Screen rotation triggers STOP with isChangingConfigurations = true
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_STOP)
        assertEquals(0, lockCount)

        // Configuration change completes, new activity starts
        isChangingConfig = false
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_START)
        assertEquals(0, lockCount)
    }

    @Test
    fun ignoresOtherLifecycleEvents() {
        var lockCount = 0
        val observer = createBackgroundLockObserver(
            isChangingConfigurations = { false },
            onLock = { lockCount++ },
        )

        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_CREATE)
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_RESUME)
        observer.onStateChanged(dummyLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        assertEquals(0, lockCount)
    }
}

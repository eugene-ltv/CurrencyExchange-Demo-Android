package com.saiferwp.currencyexchange.utils

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

inline fun <T : ViewBinding> Fragment.viewBinding(
    crossinline bindingInflater: (View) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(requireView())
    }

internal fun Fragment.launchAndRepeatOnLifecycleStarted(block: suspend CoroutineScope.() -> Unit): Job {
    return lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            block()
        }
    }
}
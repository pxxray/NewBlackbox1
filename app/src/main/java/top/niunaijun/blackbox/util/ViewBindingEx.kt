package top.niunaijun.blackbox.util

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

object ViewBindingEx {
    inline fun <reified T : ViewBinding> Activity.inflate(): Lazy<T> = lazy {
        inflateBinding(layoutInflater)
    }

    inline fun <reified T : ViewBinding> Fragment.inflate(): Lazy<T> = lazy {
        inflateBinding(layoutInflater)
    }

    inline fun <reified T : ViewBinding> Dialog.inflate(): Lazy<T> = lazy {
        inflateBinding(layoutInflater)
    }

    inline fun <reified T : ViewBinding> inflateBinding(layoutInflater: LayoutInflater): T {
        val method = T::class.java.getMethod("inflate", LayoutInflater::class.java)
        return method.invoke(null, layoutInflater) as T
    }
}

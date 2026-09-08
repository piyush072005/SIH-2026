package com.isro.itantra.ui

import android.app.Activity
import android.os.Bundle

/**
 * UI entry point. Backend wiring should register this activity in the manifest.
 * TODO(backend): provide the contract implementations through the app component.
 */
class ITantraActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ITantraView(this))
    }
}

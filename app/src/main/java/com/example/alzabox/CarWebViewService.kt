package com.example.alzabox

import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

class CarWebViewService : CarActivityService() {
    override fun getCarActivity(): Class<out CarActivity> {
        return CarWebViewActivity::class.java
    }
}

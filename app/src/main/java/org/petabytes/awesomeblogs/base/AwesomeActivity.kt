package org.petabytes.awesomeblogs.base

import android.content.Context
import org.petabytes.awesomeblogs.AwesomeBlogsApp
import org.petabytes.coordinator.Activity
import org.petabytes.coordinator.ActivityLayoutBinder
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

/**
 * Created by cyc1007 on 2017-12-04.
 */
abstract class AwesomeActivity : Activity() {

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(context))
    }

    override fun createActivityLayoutBinder() = AwesomeBlogsApp.get().activityLayoutBinder()!!
}

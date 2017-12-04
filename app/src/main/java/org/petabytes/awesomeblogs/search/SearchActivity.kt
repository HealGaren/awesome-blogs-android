package org.petabytes.awesomeblogs.search

import android.content.Context
import android.content.Intent

import org.petabytes.awesomeblogs.R
import org.petabytes.awesomeblogs.base.AwesomeActivity
import org.petabytes.coordinator.ActivityGraph

class SearchActivity : AwesomeActivity() {

    override fun createActivityGraph(): ActivityGraph = ActivityGraph.Builder()
                .layoutResId(R.layout.search)
                .coordinator(R.id.container, SearchCoordinator(this, ::finish ))
                .build()

    companion object {
        @JvmStatic fun intent(context: Context) = Intent(context, SearchActivity::class.java)
    }
}

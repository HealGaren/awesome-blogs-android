package org.petabytes.awesomeblogs.search

import android.content.Context
import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView

import com.annimon.stream.function.Supplier
import com.jakewharton.rxrelay.PublishRelay
import com.squareup.coordinators.Coordinators

import org.jsoup.Jsoup
import org.petabytes.api.source.local.Entry
import org.petabytes.awesomeblogs.AwesomeBlogsApp
import org.petabytes.awesomeblogs.R
import org.petabytes.awesomeblogs.summary.SummaryActivity
import org.petabytes.awesomeblogs.util.Analytics
import org.petabytes.awesomeblogs.util.Strings
import org.petabytes.awesomeblogs.util.Views
import org.petabytes.coordinator.Coordinator
import org.petabytes.coordinator.RecyclerAdapter

import java.util.HashMap
import java.util.concurrent.TimeUnit

import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged
import io.realm.RealmResults
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action0
import rx.schedulers.Schedulers

internal class SearchCoordinator(private val context: Context, private val closeAction: () -> Unit) : Coordinator() {

    @BindView(R.id.search)
    lateinit var searchView: EditText
    @BindView(R.id.placeholder)
    lateinit var placeholderView: TextView
    @BindView(R.id.recycler)
    lateinit var recyclerView: RecyclerView
    private var adapter: RecyclerAdapter<Entry>? = null
    private val keywordRelay: PublishRelay<String> = PublishRelay.create()

    override fun attach(view: View) {
        super.attach(view)
        RecyclerAdapter<Entry> {
            val v = LayoutInflater.from(context).inflate(R.layout.search_item, null, false)
            val coordinator = SearchItemCoordinator(context, Supplier { searchView.text.toString() })
            Coordinators.bind(v) { coordinator }
            RecyclerAdapter.ViewHolder(v, coordinator)
        }.apply {
            adapter = this
            recyclerView.adapter = this
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    Views.hideSoftInput(searchView)
                }
            }
        })

        bind<RealmResults<Entry>>(
                keywordRelay
                        .debounce(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                        .flatMap<RealmResults<Entry>> { keyword -> AwesomeBlogsApp.get().api().search(keyword) }
        ) { entries ->
            if (entries.isEmpty()) {
                placeholderView.setText(R.string.empty_search_results)
                Views.setVisibleOrGone(placeholderView, true)
                Views.setVisibleOrGone(recyclerView, false)
            } else {
                recyclerView.scrollToPosition(0)
            }
            adapter!!.setItems(entries)
        }

        Analytics.event(Analytics.Event.VIEW_SEARCH)
    }

    @OnTextChanged(R.id.search)
    fun onSearchChanged(keyword: Editable) {
        keywordRelay.call(keyword.toString().trim { it <= ' ' })
        placeholderView.setText(R.string.search_3)
        Views.setVisibleOrGone(placeholderView, keyword.isEmpty())
        Views.setVisibleOrGone(recyclerView, keyword.isNotEmpty())
    }

    @OnClick(R.id.close)
    fun onCloseClick() {
        closeAction()
    }

    internal class SearchItemCoordinator(private val context: Context, private val keywordSupplier: Supplier<String>) : Coordinator(), RecyclerAdapter.OnBindViewHolderListener<Entry> {

        @BindView(R.id.title)
        lateinit var titleView: TextView
        @BindView(R.id.summary)
        lateinit var summaryView: TextView
        @BindView(R.id.author)
        lateinit var authorView: TextView

        override fun onBindViewHolder(entry: Entry, position: Int) {
            titleView.setTypeface(titleView.typeface, Typeface.BOLD)
            titleView.text = Strings.colorizeBackground(entry.title,
                    keywordSupplier.get(), context.resources.getColor(R.color.search), true)
            authorView.text = Strings.colorizeBackground(Entry.getFormattedAuthorUpdatedAt(entry),
                    keywordSupplier.get(), context.resources.getColor(R.color.search), true)

            bind(
                    Observable.just(entry.summary)
                    .map { summary -> Jsoup.parse(summary).text() }
                    .map { summary -> summary.substring(0, Math.min(200, summary.length)) }
                    .subscribeOn(Schedulers.io())
            ) { summary ->
                summaryView.text = summary.trim { it <= ' ' }
                titleView.post { summaryView.maxLines = 4 - titleView.lineCount }
            }

            view.setOnClickListener {
                context.startActivity(SummaryActivity.intent(context, entry.link, Analytics.Param.SEARCH))
                Analytics.event(Analytics.Event.VIEW_SEARCH_ITEM, object : HashMap<String, String>(2) {
                    init {
                        put(Analytics.Param.TITLE, entry.title)
                        put(Analytics.Param.LINK, entry.link)
                    }
                })
            }
            view.setBackgroundResource(if (position % 2 == 0) R.color.white else R.color.background_row)
        }
    }
}

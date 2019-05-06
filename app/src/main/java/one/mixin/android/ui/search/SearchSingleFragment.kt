package one.mixin.android.ui.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_search_single.*
import kotlinx.android.synthetic.main.view_head_search_single.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.addFragment
import one.mixin.android.extension.hideKeyboard
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.conversation.ConversationActivity
import one.mixin.android.ui.search.SearchFragment.Companion.SEARCH_DEBOUNCE
import one.mixin.android.ui.wallet.WalletActivity
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.User
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SearchSingleFragment : BaseFragment() {
    companion object {
        const val TAG = "SearchSingleFragment"
        const val ARGS_LIST = "args_list"
        const val ARGS_QUERY = "args_query"

        fun newInstance(
            list: ArrayList<Parcelable>,
            query: String
        ) = SearchSingleFragment().withArgs {
            putParcelableArrayList(ARGS_LIST, list)
            putString(ARGS_QUERY, query)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val searchViewModel: SearchViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)
    }

    private val data by lazy {
        arguments!!.getParcelableArrayList<Parcelable>(ARGS_LIST)
    }

    private val query by lazy {
        arguments!!.getString(ARGS_QUERY)
    }

    private var compositeDisposable = CompositeDisposable()

    private val type by lazy {
        when (data!![0]) {
            is AssetItem -> TypeAsset
            is ChatMinimal -> TypeChat
            is User -> TypeUser
            else -> TypeMessage
        }
    }

    private val adapter by lazy {
        SearchSingleAdapter(type).apply { query = this@SearchSingleFragment.query }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_search_single, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        back_ib.setOnClickListener {
            search_et.hideKeyboard()
            requireActivity().onBackPressed()
        }
        search_rv.layoutManager = LinearLayoutManager(requireContext())
        val header = LayoutInflater.from(requireContext()).inflate(R.layout.view_head_search_single, search_rv, false)
        val text = when (type) {
            TypeAsset -> requireContext().getString(R.string.search_title_assets)
            TypeUser -> requireContext().getText(R.string.search_title_contacts)
            TypeChat -> requireContext().getText(R.string.search_title_chat)
            TypeMessage -> requireContext().getText(R.string.search_title_messages)
        }
        header.title_tv.text = text
        adapter.headerView = header
        search_rv.adapter = adapter
        adapter.data = data
        adapter.onItemClickListener = object : SearchFragment.OnSearchClickListener {
            override fun onTipClick() {
            }

            override fun onAsset(assetItem: AssetItem) {
                activity?.let { WalletActivity.show(it, assetItem) }
            }

            @SuppressLint("CheckResult")
            override fun onMessageClick(message: SearchMessageItem) {
                search_rv.hideKeyboard()
                val f = SearchMessageFragment.newInstance(message, adapter.query)
                requireActivity().addFragment(this@SearchSingleFragment, f, SearchMessageFragment.TAG, R.id.root_view)
            }

            override fun onChatClick(chatMinimal: ChatMinimal) {
                search_rv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, chatMinimal.conversationId) }
            }

            override fun onUserClick(user: User) {
                search_rv.hideKeyboard()
                context?.let { ctx -> ConversationActivity.show(ctx, null, user.userId) }
            }
        }

        clear_ib.setOnClickListener { search_et.setText("") }
        search_et.hint = text
        search_et.setText(query)
        compositeDisposable.add(search_et.textChanges().debounce(SEARCH_DEBOUNCE, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                clear_ib.isVisible = it.isNotEmpty()
                if (it == adapter.query) return@subscribe

                adapter.query = it.toString()
                onTextChanged(it.toString())
            }, {}))
    }

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()
    }

    private fun onTextChanged(s: String) {
        searchViewModel.viewModelScope.launch(Dispatchers.Default) {
            val list: List<Parcelable>? = when (type) {
                TypeAsset -> searchViewModel.fuzzySearchAsync<AssetItem>(s)
                TypeUser -> searchViewModel.fuzzySearchAsync<User>(s)
                TypeChat -> searchViewModel.fuzzySearchAsync<ChatMinimal>(s)
                TypeMessage -> searchViewModel.fuzzySearchAsync<SearchMessageItem>(s, -1)
            }.await()

            withContext(Dispatchers.Main) {
                adapter.data = list
                adapter.notifyDataSetChanged()
            }
        }
    }
}
package one.mixin.android.ui.conversation.markdown

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import io.noties.markwon.recycler.MarkwonAdapter
import io.noties.markwon.recycler.SimpleEntry
import io.noties.markwon.recycler.table.TableEntry
import kotlinx.android.synthetic.main.view_web_bottom.view.*
import one.mixin.android.R
import one.mixin.android.databinding.ActivityMarkdownBinding
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.WebControlView
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.FencedCodeBlock

class MarkdownActivity : BaseActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMarkdownBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.control.mode = isNightMode()
        binding.control.callback = object : WebControlView.Callback {
            override fun onMoreClick() {
                showBottomSheet()
            }

            override fun onCloseClick() {
                finish()
            }
        }
        val adapter = MarkwonAdapter.builder(
            R.layout.layout_markdown_item,
            R.id.text
        ).include(
            FencedCodeBlock::class.java,
            SimpleEntry.create(
                R.layout.item_markdown_code_block,
                R.id.text
            )
        ).include(
            TableBlock::class.java,
            TableEntry.create { builder: TableEntry.Builder ->
                builder
                    .tableLayout(R.layout.item_markdown_table_block, R.id.table_layout)
                    .textLayoutIsRoot(R.layout.item_markdown_cell)
            }
        ).build()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        val markwon = MarkwonUtil.getMarkwon(this, { link ->
            LinkBottomSheetDialogFragment.newInstance(link)
                .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        }, { link ->
            WebBottomSheetDialogFragment.newInstance(link, intent.getStringExtra(CONVERSATION_ID))
                .showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
        })
        val markdown = intent.getStringExtra(CONTENT) ?: return
        adapter.setMarkdown(markwon, markdown)
        adapter.notifyDataSetChanged()
    }

    private fun showBottomSheet() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(
            ContextThemeWrapper(this, R.style.Custom),
            R.layout.view_markdown,
            null
        )
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.forward.setOnClickListener {
            val markdown = intent.getStringExtra(CONTENT) ?: return@setOnClickListener
            ForwardActivity.show(this, arrayListOf(ForwardMessage(ForwardCategory.POST.name, content = markdown)))
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    companion object {
        private const val CONTENT = "content"
        private const val CONVERSATION_ID = "conversation_id"
        fun show(context: Context, content: String, conversationId: String? = null) {
            context.startActivity(Intent(context, MarkdownActivity::class.java).apply {
                putExtra(CONTENT, content)
                putExtra(CONVERSATION_ID, conversationId)
            })
        }
    }
}

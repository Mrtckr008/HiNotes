package com.huawei.references.hinotes.ui.todolist.adapter

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.huawei.references.hinotes.R
import com.huawei.references.hinotes.data.item.model.Item
import com.huawei.references.hinotes.ui.notedetail.DetailNoteActivity
import com.huawei.references.hinotes.ui.todolistdetail.TodoListDetailActivity
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters


class TodoListSectionAdapter(
    private val todoListTitleList: String, var list: List<Item>
) : Section(
    SectionParameters.builder()
        .itemResourceId(R.layout.todo_list_item_list)
        .headerResourceId(R.layout.todo_list_header_list)
        .build()
) {


    override fun getContentItemsTotal(): Int {
        return list.size
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        return TodoListItemViewHolder(
            view
        )
    }

    override fun onBindItemViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val todoListItemHolder: TodoListItemViewHolder = holder as TodoListItemViewHolder
        val todoListItem: Item = list[position]
        todoListItemHolder.todoListSubText.text = todoListItem.title
        todoListItemHolder.todoListSubCheckbox.isChecked = todoListItem.isChecked!!

        todoListItemHolder.rootView.setOnClickListener { v ->
            val intent = Intent(v.context, TodoListDetailActivity::class.java)
            intent.putExtra("clickedItemData", todoListItem)
            ContextCompat.startActivity(v.context, intent, null)
        }
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return TodoListSectionHeaderViewHolder(
            view
        )
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        val todoListSectionHeaderHolder: TodoListSectionHeaderViewHolder = holder as TodoListSectionHeaderViewHolder
        todoListSectionHeaderHolder.todoListSectionHeader.text = todoListTitleList
    }

}

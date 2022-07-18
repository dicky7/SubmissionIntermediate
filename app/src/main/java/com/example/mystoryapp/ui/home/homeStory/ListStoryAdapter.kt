package com.example.mystoryapp.ui.home.homeStory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mystoryapp.R
import com.example.mystoryapp.data.remote.response.ListStoryItem
import com.example.mystoryapp.databinding.ItemListStoryBinding
import com.example.mystoryapp.ui.home.homeStory.ListStoryAdapter.MyViewHolder
import com.example.mystoryapp.utlis.generateDateFormat

class ListStoryAdapter: ListAdapter<ListStoryItem, MyViewHolder>(DIFF_CALLBACK) {
    private lateinit var onItemCLickCallback: OnItemCLickCallback
    fun setOnItemClickCallback(onItemCLickCallback: OnItemCLickCallback){
        this.onItemCLickCallback = onItemCLickCallback
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ListStoryItem>() {
            override fun areItemsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ListStoryItem, newItem: ListStoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class MyViewHolder (private val binding: ItemListStoryBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(listStoryItem: ListStoryItem){
            with(binding){
                listStoryItem.apply {
                    storyUsername.text = name
                    storyDatePost.generateDateFormat(createdAt)
                    storyDesc.text = description
                    Glide.with(itemView.context)
                        .load(listStoryItem.photoUrl)
                        .apply(RequestOptions.placeholderOf(R.drawable.ic_loading).error(R.drawable.ic_error))
                        .into(storyImage)

                    //when  card click set binding with current transition name
                    storyUserAvatar.transitionName = "avatar_profile"
                    storyUsername.transitionName = name
                    storyDesc.transitionName = description
                    storyDatePost.transitionName = createdAt
                    storyImage.transitionName = photoUrl

                    cardviewStory.setOnClickListener {
                        onItemCLickCallback.onItemCLicked(listStoryItem, binding)
                    }
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemListStoryBinding = ItemListStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(itemListStoryBinding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val data = getItem(position)
        holder.bind(data)
    }

    interface OnItemCLickCallback{
        fun onItemCLicked(listStoryItem: ListStoryItem,binding: ItemListStoryBinding)

    }

}
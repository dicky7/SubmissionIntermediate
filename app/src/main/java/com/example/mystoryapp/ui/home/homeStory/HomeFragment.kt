package com.example.mystoryapp.ui.home.homeStory

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mystoryapp.MainActivity
import com.example.mystoryapp.R
import com.example.mystoryapp.data.local.entity.StoryEntity
import com.example.mystoryapp.databinding.FragmentHomeBinding
import com.example.mystoryapp.databinding.ItemListStoryBinding
import com.example.mystoryapp.ui.home.homeStory.HomeFragmentDirections.actionHomeFragmentToDetailStoryFragment
import com.example.mystoryapp.utlis.ViewModelFactory
import kotlinx.coroutines.launch


class HomeFragment : Fragment(), ListStoryAdapter.OnItemCLickCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var listStoryAdapter: ListStoryAdapter


    private var token = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater,container,false)
        setupActionBar()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listStoryAdapter = ListStoryAdapter()
        listStoryAdapter.setOnItemClickCallback(this)
        setViewModel()
        action()
    }

    /**
     * to show custom menu to action bar
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    /**
     * handle action when menu selected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.setting_language->{
                activity?.startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
                true
            }
            R.id.logout->{
                viewModelLogout()
                true
            }
            else-> super.onOptionsItemSelected(item)
        }
    }

    /**
     * for setup actionBar fullScreen
     */
    private fun setupActionBar(){
        setHasOptionsMenu(true) // Add this!
        (activity as AppCompatActivity?)!!.setSupportActionBar(binding.toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    /**
     * action function to handle input from user
     */
    private fun action(){
        with(binding){
            addStory.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToAddStoryActivity())
            }

            //get the SwipeRefreshLayout state
            swipeRefreshStory.setOnRefreshListener {
                viewModelGetStory()
                swipeRefreshStory.isRefreshing = false
            }
        }
    }

    /**
     * setup viewModel before used
     */
    private fun setViewModel(){
        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(requireContext()))[HomeViewModel::class.java]
        viewModelGetToken()
        viewModelGetStory()

    }

    /**
     * function to handle viewModel getStories and token login
     */
    private fun viewModelGetToken(){
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getAuthToken().observe(viewLifecycleOwner) {
                token = it
            }
        }
    }

    /**
     * function to handle viewModel logout
     */
    private fun viewModelLogout(){
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            launch {
                viewModel.setIsLoggedIn(false)
                val intent = Intent(context, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
    }

    /**
     * getListStory from api
     *
     * @param result: Result<List<ListStoryItem>>
     * @return Unit
     */
    private fun viewModelGetStory(){
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED){
                viewModel.getStories(token).observe(viewLifecycleOwner) { result ->
                    listStoryAdapter.submitData(lifecycle, result)
                }
            }
        }
        recyclerViewer()
        showProgressBar()
    }

    /**
     * prepolute recyclviewer
     *
     * @param state Booleand
     * @return Unit
     */
    private fun recyclerViewer() {
        listStoryAdapter = ListStoryAdapter().apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (positionStart == 0) {
                        binding.rvStory.smoothScrollToPosition(0)
                    }
                }
            })
        }
        listStoryAdapter.setOnItemClickCallback(this)
        listStoryAdapter.refresh()
        binding.rvStory.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = listStoryAdapter.withLoadStateFooter(
                footer = LoadingStateAdapter{
                    listStoryAdapter.retry()
                }
            )
        }
    }

    /**
     * Determine loading indicator is visible or not
     *
     * @return Boolean
     */
    private fun showProgressBar(){
        with(binding){
            // Pager LoadState listener
            listStoryAdapter.addLoadStateListener { loadState ->
                if ((loadState.source.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached && listStoryAdapter.itemCount < 1) || loadState.source.refresh is LoadState.Error) {
                    // List empty or error
                    layoutError.root.isVisible = true
                    rvStory.isVisible = false
                    progressBar.isVisible = false
                } else {
                    // List not empty
                    layoutError.root.isVisible = false
                    rvStory.isVisible = true
                    progressBar.isVisible = false
                }
            }
        }
    }


    /**
     * destroy fragment
     */
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    override fun onItemCLicked(storyEntity: StoryEntity, binding: ItemListStoryBinding) {
        //When we click on an item, we need to know which binding has been clicked and its transition name to set it in the extras object.
        val extras = FragmentNavigatorExtras(
            binding.storyUserAvatar to "avatar_profile",
            binding.storyImage to storyEntity.photoUrl,
            binding.storyUsername to storyEntity.name,
            binding.storyDesc to storyEntity.description,
            binding.storyDatePost to storyEntity.createdAt
        )
        val toDetail = actionHomeFragmentToDetailStoryFragment(storyEntity)
        toDetail.storyDetailParcelable= storyEntity
        findNavController().navigate(toDetail, extras)
    }

}
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mystoryapp.MainActivity
import com.example.mystoryapp.R
import com.example.mystoryapp.data.Result
import com.example.mystoryapp.data.remote.response.ListStoryItem
import com.example.mystoryapp.databinding.FragmentHomeBinding
import com.example.mystoryapp.databinding.ItemListStoryBinding
import com.example.mystoryapp.utlis.ViewModelFactory
import com.google.android.material.snackbar.Snackbar
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.getStories(token).observe(viewLifecycleOwner){result->
                        when(result){
                            is Result.Loading->{
                                true.showProgressBar()
                            }
                            is Result.Success->{
                                false.showProgressBar()
                                listStoryAdapter.submitList(result.data)
                                recyclerViewer()
                            }
                            is Result.Error->{
                                false.showProgressBar()
                                Snackbar.make(
                                    binding.root,
                                    result.error,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * prepolute recyclviewer
     *
     * @param state Booleand
     * @return Unit
     */
    private fun recyclerViewer() {
        binding.rvStory.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = listStoryAdapter
        }
    }

    /**
     * Determine loading indicator is visible or not
     *
     * @return Boolean
     */
    private fun Boolean.showProgressBar(){
        with(binding){
            progressBar.isVisible = this@showProgressBar
        }
    }


    /**
     * destroy fragment
     */
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    override fun onItemCLicked(listStoryItem: ListStoryItem, binding: ItemListStoryBinding) {
        //When we click on an item, we need to know which binding has been clicked and its transition name to set it in the extras object.
        val extras = FragmentNavigatorExtras(
            binding.storyUserAvatar to "avatar_profile",
            binding.storyImage to listStoryItem.photoUrl,
            binding.storyUsername to listStoryItem.name,
            binding.storyDesc to listStoryItem.description,
            binding.storyDatePost to listStoryItem.createdAt
        )
        val toDetail = HomeFragmentDirections.actionHomeFragmentToDetailStoryFragment(listStoryItem)
        toDetail.storyDetailParcelable= listStoryItem
        findNavController().navigate(toDetail, extras)
    }

}
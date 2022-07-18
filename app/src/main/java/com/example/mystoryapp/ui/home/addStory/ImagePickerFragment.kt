package com.example.mystoryapp.ui.home.addStory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.mystoryapp.MainActivity
import com.example.mystoryapp.R
import com.example.mystoryapp.databinding.FragmentImagePickerBinding
import org.koin.android.ext.android.bind


class ImagePickerFragment : DialogFragment(){
    companion object{
        fun newInstance() = ImagePickerFragment()
    }

    private var _binding: FragmentImagePickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentImagePickerBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        action()
    }

    private fun action(){
        binding.btCamera.setOnClickListener {
            when(activity){
                is AddStoryActivity->{
                    (activity as AddStoryActivity).startTakePhoto()
                    dismiss()
                }
            }
        }
        binding.btGallery.setOnClickListener {
            when(activity){
                is AddStoryActivity-> {
                    (activity as AddStoryActivity).startIntentGallery()
                    dismiss()
                }
            }
        }
    }



}
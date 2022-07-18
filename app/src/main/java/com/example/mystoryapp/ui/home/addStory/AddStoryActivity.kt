package com.example.mystoryapp.ui.home.addStory

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mystoryapp.R
import com.example.mystoryapp.data.Result
import com.example.mystoryapp.data.remote.response.UploadStoryResponse
import com.example.mystoryapp.databinding.ActivityAddStoryBinding
import com.example.mystoryapp.ui.home.HomeActivity
import com.example.mystoryapp.utlis.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AddStoryActivity : AppCompatActivity() {
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_PERMISSION_CODE = 10
    }

    private lateinit var viewModel: AddStoryViewModel
    private var _binding: ActivityAddStoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var mImagePickerFragment: ImagePickerFragment

    //camera
    private lateinit var currentPhotoPath: String
    //getFile
    private var getFile: File? = null
    private var token = ""

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (!allPermissionGranted()) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.permission),
                    Snackbar.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this.baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAddStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_PERMISSION_CODE
            )
        }
        setupActionBar()
        setViewModel()
        action()

    }


    /**
     * to show custom menu to action bar
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * handle action when menu selected
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.add_menu->{
                uploadStories()
                true
            }
            android.R.id.home-> {
                backPressAlertDialog()
                true
            }
            else->{
                super.onOptionsItemSelected(item)
            }
        }

    }

    /**
     * for setup actionBar fullScreen
     */
    private fun setupActionBar(){
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * action function to handle input from user
     */
    private fun action(){
        with(binding){
            pickImage.setOnClickListener {
                getImagePicker()
            }
        }
    }

    /**
     * setup viewModel before used
     */
    private fun setViewModel() {
        viewModel = ViewModelProvider(this, ViewModelFactory.getInstance(this))[AddStoryViewModel::class.java]
        lifecycleScope.launch {
            viewModel.getAuthToken().observe(this@AddStoryActivity){
                token = it
            }
        }
    }

    /**
     * upload user story to the server and checking data is valid
     */
    private fun uploadStories(){
        with(binding) {
            //check data is empty or not
            showProgressBar(true)
            var isDataValid = true
            val description = edtDescription.text.toString()

            //if description is empty return false and show an error message
            if (description.isEmpty()){
                edtDescription.error = getString(R.string.error_description)
                isDataValid = false
            }

            //if file or image is empty return false and show an error message
            if (getFile==null){
                Snackbar.make(binding.root, getString(R.string.error_pick_image), Snackbar.LENGTH_SHORT).show()
                isDataValid = false
            }

            if (isDataValid){
                val file = reduceFile(getFile as File)
                val descriptionStory = description.toRequestBody("text/plain".toMediaType())
                val requestImageFile = file.asRequestBody("image/jpg".toMediaTypeOrNull())
                val imageMultipart:MultipartBody.Part = MultipartBody.Part.createFormData(
                    "photo",
                    file.name,
                    requestImageFile
                )

                lifecycleScope.launchWhenCreated{
                    launch {
                        viewModel.uploadStories(token, imageMultipart, descriptionStory).observe(this@AddStoryActivity){result->
                            resultUploadStories(result)
                        }
                    }
                }
            }else showProgressBar(false)
        }
    }

    /**
     * Set Result for UploadStories
     *
     * @param Result<UploadStoryResponse>
     * @return Unit
     */
    private fun resultUploadStories(result: Result<UploadStoryResponse>){
        when(result){
            is Result.Loading->{
                showProgressBar(true)
            }
            is Result.Success->{
                showProgressBar(false)
                AlertDialog.Builder(this).apply {
                    setTitle(getString(R.string.success))
                    setMessage(getString(R.string.desc_success_upload_story))
                    setPositiveButton(getString(R.string.ok)){_,_->
                        val intent = Intent(context, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    create()
                    show()
                }
            }
            is Result.Error->{
                showProgressBar(false)
                Snackbar.make(
                    binding.root,
                    result.error,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }


    /**
     * for intialize image picker
     */
    private fun getImagePicker(){
        when (::mImagePickerFragment.isInitialized) {
            false-> mImagePickerFragment = ImagePickerFragment.newInstance()
        }
        when(!mImagePickerFragment.isAdded){
            true-> mImagePickerFragment.show(supportFragmentManager, mImagePickerFragment.javaClass.simpleName)
        }
    }

    /**
     * function for take photo using camera
     */
    fun startTakePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)

        createCustomTempFile(application).also {
            val photoUri : Uri = FileProvider.getUriForFile(
                this@AddStoryActivity,
                "com.example.mystoryapp",
                it
            )
            currentPhotoPath = it.absolutePath
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            launcherIntentCamera.launch(intent)
        }
    }

    fun startIntentGallery(){
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, getString(R.string.choose_picture_gallery))
        launcherIntentGallery.launch(chooser)
    }


    /**
     * untuk menampung hasil dari intent atau IntentResult camera
     */
    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val myFile = File(currentPhotoPath)

            getFile = myFile
            val result = BitmapFactory.decodeFile(getFile?.path)
            binding.addPreviewImage.setImageBitmap(result)
        }
    }

    /**
     * untuk menampung hasil dari intent atau IntentResult camera
     */
    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if (it.resultCode == RESULT_OK){
            //create temporary to collect image from gallery
            val selectedImageUri = it.data?.data as Uri

            // then using stream to write what we got from Uri (file gallery) into myFile.
            val myFile = uriToFile(selectedImageUri, this@AddStoryActivity)

            getFile = myFile
            binding.addPreviewImage.setImageURI(selectedImageUri)
        }
    }


    /**
     * Determine loading indicator is visible or not
     * @param state
     */
    private fun showProgressBar(state:Boolean){
        with(binding){
            progressBar.isVisible = state
            pickImage.isEnabled != state
            loadingView.animateVisibility(state)
            edtDescription.isEnabled != state
        }
    }

    /**
     * function to create alert dialog when backpressed
     */
    private fun backPressAlertDialog(){
        val alertDialogBuilder = AlertDialog.Builder(this)
        with(alertDialogBuilder){
            setTitle(getString(R.string.cancel))
            setMessage(getString(R.string.message_cancel))
            setPositiveButton(getString(R.string.ok)){_,_->
                finish()
            }
            setNegativeButton(getString(R.string.cancel)){dialog,_->
                dialog.cancel()
            }
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    override fun onBackPressed() {
        backPressAlertDialog()
    }
//    override fun onSupportNavigateUp(): Boolean {
//        backPressAlertDialog()
//        return true
//    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
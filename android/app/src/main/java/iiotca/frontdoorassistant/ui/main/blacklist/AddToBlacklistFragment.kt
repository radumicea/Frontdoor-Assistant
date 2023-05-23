package iiotca.frontdoorassistant.ui.main.blacklist

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.afterTextChanged
import iiotca.frontdoorassistant.databinding.FragmentAddToBlacklistBinding
import iiotca.frontdoorassistant.ui.main.MainViewModel
import iiotca.frontdoorassistant.ui.main.MainViewModelFactory
import iiotca.frontdoorassistant.ui.main.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths


class AddToBlacklistFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentAddToBlacklistBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private val paths = ObservableArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddToBlacklistBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.add_to_blacklist_menu, menu)
                menu.findItem(R.id.action_done).isVisible =
                    (paths.size > 0 && binding.name.editText!!.text.isNotEmpty())
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_done) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.addToBlacklist(paths, binding.name.editText!!.text.toString())
                    }
                    return true
                }

                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        paths.addOnListChangedCallback(object :
            ObservableList.OnListChangedCallback<ObservableList<String>>() {
            override fun onChanged(sender: ObservableList<String>?) {
            }

            override fun onItemRangeChanged(
                sender: ObservableList<String>?, positionStart: Int, itemCount: Int
            ) {
            }

            override fun onItemRangeInserted(
                sender: ObservableList<String>?, positionStart: Int, itemCount: Int
            ) {
                requireActivity().invalidateOptionsMenu()
            }

            override fun onItemRangeMoved(
                sender: ObservableList<String>?, fromPosition: Int, toPosition: Int, itemCount: Int
            ) {
            }

            override fun onItemRangeRemoved(
                sender: ObservableList<String>?, positionStart: Int, itemCount: Int
            ) {
                requireActivity().invalidateOptionsMenu()
            }
        })

        binding.name.editText!!.afterTextChanged {
            requireActivity().invalidateOptionsMenu()
        }

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        viewModel = ViewModelProvider(
            this, MainViewModelFactory(sharedViewModel)
        )[MainViewModel::class.java]
        viewModel.init()

        sharedViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.nameTw.visibility = View.GONE
                binding.name.visibility = View.GONE
                binding.photosTw.visibility = View.GONE
                binding.photosCv.visibility = View.GONE
                binding.selectPhotos.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.nameTw.visibility = View.VISIBLE
                binding.name.visibility = View.VISIBLE
                binding.photosCv.visibility = View.VISIBLE
                binding.photosTw.visibility = View.VISIBLE
                binding.selectPhotos.visibility = View.VISIBLE
            }
        }

        viewModel.addError.observe(viewLifecycleOwner) { errorId ->
            if (errorId == null) {
                findNavController().navigateUp()
            } else {
                Snackbar.make(binding.root, errorId, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.photos.adapter = PhotosAdapter()

        binding.selectPhotos.setOnClickListener {
            askImagePermissionAndHandleSelectImages()
        }
    }

    private fun askImagePermissionAndHandleSelectImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                getMultipleContentsLauncher.launch("image/*")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                getMultipleContentsLauncher.launch("image/*")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getMultipleContentsLauncher.launch("image/*")
            } else {
                Toast.makeText(context, R.string.please_allow_storage, Toast.LENGTH_LONG).show()
            }
        }

    private val getMultipleContentsLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            requireContext().cacheDir.deleteRecursively()

            val filePaths = cacheFiles(uris)

            paths.clear()
            paths.addAll(filePaths)

            (binding.photos.adapter as PhotosAdapter).setItems(filePaths)
        }

    private fun cacheFiles(uris: List<Uri>): List<String> {
        val cacheDir = requireContext().cacheDir
        val contentResolver = requireContext().contentResolver
        val filePaths = mutableListOf<String>()

        uris.forEach { uri ->
            val fileName = getFileName(uri)
            val file = File(cacheDir, fileName)
            filePaths.add(file.canonicalPath)

            FileOutputStream(file).use { outStream ->
                contentResolver.openInputStream(uri)!!.use { inStream ->
                    outStream.write(inStream.readBytes())
                }
            }
        }

        return filePaths
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor.use {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }

        return result!!
    }

    inner class PhotosAdapter : RecyclerView.Adapter<PhotosAdapter.FieldViewHolder>() {
        private var paths = listOf<String>()
        private val imageCache = mutableMapOf<String, Bitmap>()
        private val fileNameCache = mutableMapOf<String, String>()

        @SuppressLint("NotifyDataSetChanged")
        fun setItems(newPaths: List<String>) {
            paths = newPaths
            imageCache.clear()
            fileNameCache.clear()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.photo_card, parent, false)
            return FieldViewHolder(v)
        }

        override fun getItemCount(): Int {
            return paths.size
        }

        override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
            val item = paths[position]

            holder.fileName.text = getCachedFileName(item)
            holder.photo.setImageBitmap(getCachedImage(item))
            holder.photo.adjustViewBounds = true
        }

        private fun getCachedFileName(path: String): String {
            var x = fileNameCache[path]
            if (x == null) {
                x = path.substring(path.lastIndexOf(File.separator) + 1)
                fileNameCache[path] = x
            }

            return x
        }

        private fun getCachedImage(path: String): Bitmap {
            var x = imageCache[path]
            if (x == null) {
                val data = Files.readAllBytes(Paths.get(path))
                x = BitmapFactory.decodeByteArray(data, 0, data.size)
                imageCache[path] = x
            }

            return x!!
        }

        inner class FieldViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val fileName: TextView
            val photo: ImageView

            init {
                fileName = itemView.findViewById(R.id.file_name)
                photo = itemView.findViewById(R.id.photo)
            }
        }
    }
}
package iiotca.frontdoorassistant.ui.main.history

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.data.dto.HistoryItem
import iiotca.frontdoorassistant.databinding.FragmentHistoryBinding
import iiotca.frontdoorassistant.ui.main.MainViewModel
import iiotca.frontdoorassistant.ui.main.MainViewModelFactory
import iiotca.frontdoorassistant.ui.main.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class HistoryFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentHistoryBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private var showUnknown = true
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.history_menu, menu)
                menu.findItem(R.id.action_filter).isVisible =
                    this@HistoryFragment::adapter.isInitialized
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_filter) {
                    showUnknown = !showUnknown
                    adapter.filter()
                    menuItem.title = if (showUnknown) {
                        getString(R.string.hide_unknown)
                    } else {
                        getString(R.string.show_unknown)
                    }

                    return true
                }

                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        viewModel = ViewModelProvider(
            this, MainViewModelFactory(sharedViewModel)
        )[MainViewModel::class.java]
        viewModel.init()

        sharedViewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                binding.history.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.history.visibility = View.VISIBLE
            }
        }

        viewModel.getHistoryResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val dividerItemDecoration = DividerItemDecoration(
                        binding.history.context,
                        (binding.history.layoutManager as LinearLayoutManager).orientation
                    )
                    binding.history.addItemDecoration(dividerItemDecoration)

                    adapter = HistoryAdapter(result.data)
                    requireActivity().invalidateOptionsMenu()
                    binding.history.adapter = adapter
                }

                is Result.Error -> {
                    Snackbar.make(binding.root, result.code, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getHistory()
        }
    }

    inner class HistoryAdapter(private val original: List<HistoryItem>) :
        RecyclerView.Adapter<HistoryAdapter.FieldViewHolder>() {
        private var history = original
        private val blacklisted = original.filter { !it.name.equals("Unknown", ignoreCase = true) }
        private val dateTimeCache = mutableMapOf<Long, String>()
        private val imageCache = mutableMapOf<ByteArray, Bitmap>()

        @SuppressLint("NotifyDataSetChanged")
        fun filter() {
            history = if (showUnknown) {
                original
            } else {
                blacklisted
            }

            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
            val v =
                LayoutInflater.from(parent.context).inflate(R.layout.history_card, parent, false)
            return FieldViewHolder(v)
        }

        override fun getItemCount(): Int {
            return history.size
        }

        override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
            val item = history[position]

            holder.name.text = item.name
            holder.date.text = getCachedDateTime(item.timeStamp)
            holder.photo.setImageBitmap(getCachedImage(item.data))
            holder.photo.adjustViewBounds = true
        }

        private fun getCachedDateTime(timeStamp: Long): String {
            var s = dateTimeCache[timeStamp]
            if (s == null) {
                s = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(timeStamp))
                dateTimeCache[timeStamp] = s
            }

            return s!!
        }

        private fun getCachedImage(data: ByteArray): Bitmap {
            var x = imageCache[data]
            if (x == null) {
                x = BitmapFactory.decodeByteArray(data, 0, data.size)
                imageCache[data] = x
            }

            return x!!
        }

        inner class FieldViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val name: TextView
            val date: TextView
            val photo: ImageView

            init {
                name = itemView.findViewById(R.id.person_name)
                date = itemView.findViewById(R.id.date)
                photo = itemView.findViewById(R.id.history_photo)
            }
        }
    }
}
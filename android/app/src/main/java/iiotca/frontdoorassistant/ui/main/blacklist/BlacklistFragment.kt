package iiotca.frontdoorassistant.ui.main.blacklist

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import iiotca.frontdoorassistant.R
import iiotca.frontdoorassistant.data.Result
import iiotca.frontdoorassistant.databinding.FragmentBlacklistBinding
import iiotca.frontdoorassistant.ui.main.MainViewModel
import iiotca.frontdoorassistant.ui.main.MainViewModelFactory
import iiotca.frontdoorassistant.ui.main.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlacklistFragment : Fragment() {
    private lateinit var inflater: LayoutInflater
    private lateinit var binding: FragmentBlacklistBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var adapter: BlacklistAdapter
    private val markedItems = ObservableArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentBlacklistBinding.inflate(inflater, container, false)
        this.inflater = inflater
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.blacklist_menu, menu)
                menu.findItem(R.id.action_remove_from_blacklist).isVisible = markedItems.size > 0
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_remove_from_blacklist) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.removeFromBlacklist(markedItems)
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
                binding.blacklist.visibility = View.GONE
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
                binding.blacklist.visibility = View.VISIBLE
            }
        }

        viewModel.getNamesResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    markedItems.addOnListChangedCallback(object :
                        ObservableList.OnListChangedCallback<ObservableList<String>>() {
                        override fun onChanged(sender: ObservableList<String>) {
                        }

                        override fun onItemRangeChanged(
                            sender: ObservableList<String>, positionStart: Int, itemCount: Int
                        ) {
                        }

                        override fun onItemRangeInserted(
                            sender: ObservableList<String>, positionStart: Int, itemCount: Int
                        ) {
                            requireActivity().invalidateOptionsMenu()
                        }

                        override fun onItemRangeMoved(
                            sender: ObservableList<String>,
                            fromPosition: Int,
                            toPosition: Int,
                            itemCount: Int
                        ) {
                        }

                        override fun onItemRangeRemoved(
                            sender: ObservableList<String>, positionStart: Int, itemCount: Int
                        ) {
                            requireActivity().invalidateOptionsMenu()
                        }
                    })
                    adapter = BlacklistAdapter(result.data)
                    binding.blacklist.adapter = adapter
                }

                is Result.Error -> {
                    Snackbar.make(binding.root, result.code, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.removeError.observe(viewLifecycleOwner) { errorId ->
            if (errorId != null) {
                Snackbar.make(binding.root, errorId, Snackbar.LENGTH_SHORT).show()
            } else {
                adapter.blacklist.removeAll(markedItems)
                markedItems.clear()
                adapter.notifyDataSetChanged()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getBlacklistNames()
        }
    }

    inner class BlacklistAdapter(val blacklist: MutableList<String>) :
        RecyclerView.Adapter<BlacklistAdapter.FieldViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.person_card, parent, false)
            return FieldViewHolder(v)
        }

        override fun getItemCount(): Int {
            return blacklist.size
        }

        override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
            val item = blacklist[position]

            holder.name.text = item

            val idx = markedItems.binarySearch(item)

            holder.cv.isChecked = (idx >= 0)

            holder.cv.setOnLongClickListener {
                if (holder.adapterPosition != position) {
                    return@setOnLongClickListener false
                }

                val idx = markedItems.binarySearch(item)

                if (idx < 0) {
                    markedItems.add(-(idx + 1), item)
                    holder.cv.isChecked = true
                } else {
                    markedItems.removeAt(idx)
                    holder.cv.isChecked = false
                }

                true
            }
        }

        inner class FieldViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val cv: MaterialCardView
            val name: TextView

            init {
                cv = itemView.findViewById(R.id.person_card)
                name = itemView.findViewById(R.id.person_name)
            }
        }
    }
}
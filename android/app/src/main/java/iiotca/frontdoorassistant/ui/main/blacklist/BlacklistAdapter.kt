package iiotca.frontdoorassistant.ui.main.blacklist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iiotca.frontdoorassistant.R

class BlacklistAdapter(private val blacklist: MutableList<String>) :
    RecyclerView.Adapter<FieldViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.person_card, parent, false)
        return FieldViewHolder(v)
    }

    override fun getItemCount(): Int {
        return blacklist.size
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        holder.name.text = blacklist[position]
    }
}

class FieldViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name: TextView

    init {
        name = itemView.findViewById(R.id.name)
    }
}
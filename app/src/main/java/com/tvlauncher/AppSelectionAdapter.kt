package com.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AppSelectionAdapter(
    private val selectedApps: MutableSet<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit,
    private val appManager: AppManager? = null,
    private val iconSizePx: Int = 0
) : ListAdapter<AppInfo, AppSelectionAdapter.AppSelectionViewHolder>(DIFF_CALLBACK) {

    class AppSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppSelectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return AppSelectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppSelectionViewHolder, position: Int) {
        val app = getItem(position)
        holder.appIcon.setImageDrawable(
            app.getIcon(holder.itemView.context.packageManager, iconSizePx)
        )
        holder.appName.text = app.getDisplayName()

        holder.checkBox.isFocusable = false
        holder.checkBox.isFocusableInTouchMode = false

        val appIdentifier = appManager?.getAppIdentifier(app) ?: AppIdentifier.encode(app)

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedApps.contains(appIdentifier)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                onSelectionChanged(appIdentifier, isChecked)
            }
        }

        holder.itemView.setOnClickListener {
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                holder.checkBox.isChecked = !holder.checkBox.isChecked
            }
        }
    }

    fun updateSelectionState() {
        notifyItemRangeChanged(0, itemCount)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
                return AppIdentifier.encode(oldItem) == AppIdentifier.encode(newItem)
            }

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
                return oldItem.packageName == newItem.packageName &&
                    oldItem.appName == newItem.appName &&
                    oldItem.shortcutId == newItem.shortcutId &&
                    oldItem.shortcutLabel == newItem.shortcutLabel
            }
        }
    }
}

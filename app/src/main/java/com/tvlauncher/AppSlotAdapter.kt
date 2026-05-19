package com.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSlotAdapter(
    var slots: MutableList<AppSlot>,
    private val onSlotClick: () -> Unit,
    private val onSlotLongClick: (Int) -> Unit,
    private val slotSizePx: Int,
    private val iconSizePx: Int = 0
) : RecyclerView.Adapter<AppSlotAdapter.AppSlotViewHolder>() {

    data class AppSlot(
        val appInfo: AppInfo? = null,
        val isEmpty: Boolean = true
    )

    class AppSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val plusButton: TextView = itemView.findViewById(R.id.plusButton)
        val addText: TextView = itemView.findViewById(R.id.addText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_slot, parent, false)
        val layoutParams = view.layoutParams as RecyclerView.LayoutParams
        layoutParams.width = slotSizePx
        layoutParams.height = slotSizePx
        view.layoutParams = layoutParams
        return AppSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppSlotViewHolder, position: Int) {
        val slot = slots[position]
        holder.itemView.setOnLongClickListener(null)

        if (slot.isEmpty) {
            holder.appIcon.visibility = View.GONE
            holder.appName.visibility = View.GONE
            holder.plusButton.visibility = View.VISIBLE
            holder.addText.visibility = View.VISIBLE

            holder.itemView.setOnClickListener { onSlotClick() }
        } else {
            slot.appInfo?.let { app ->
                holder.appIcon.setImageDrawable(
                    app.getIcon(holder.itemView.context.packageManager, iconSizePx)
                )
                holder.appName.text = app.getDisplayName()

                holder.appIcon.visibility = View.VISIBLE
                holder.appName.visibility = View.VISIBLE
                holder.plusButton.visibility = View.GONE
                holder.addText.visibility = View.GONE

                holder.itemView.setOnClickListener {
                    if (app.isShortcut) {
                        ShortcutHelper.launchShortcut(
                            holder.itemView.context,
                            app.packageName,
                            app.shortcutId!!
                        )
                    } else {
                        ShortcutHelper.launchApp(holder.itemView.context, app.packageName)
                    }
                }
                holder.itemView.setOnLongClickListener {
                    onSlotLongClick(holder.bindingAdapterPosition)
                    true
                }
            }
        }
    }

    override fun getItemCount(): Int = slots.size

    fun updateSlots(newSlots: List<AppSlot>) {
        slots.clear()
        slots.addAll(newSlots)
        notifyDataSetChanged()
    }
}

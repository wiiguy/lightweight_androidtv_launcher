package com.tvlauncher

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AppSlotAdapter(
    private val onSlotClick: (Int) -> Unit,
    private val onSlotLongClick: (Int) -> Unit,
    private val slotSizePx: Int,
    private val iconSizePx: Int = 0
) : ListAdapter<AppSlotAdapter.AppSlot, AppSlotAdapter.AppSlotViewHolder>(DIFF_CALLBACK) {

    data class AppSlot(
        val appInfo: AppInfo? = null,
        val isEmpty: Boolean = true
    )

    class AppSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
        val plusButton: TextView = itemView.findViewById(R.id.plusButton)
        val addText: TextView = itemView.findViewById(R.id.addText)

        init {
            itemView.setOnFocusChangeListener { view, hasFocus ->
                animateFocus(view, hasFocus)
            }
        }

        private fun animateFocus(view: View, hasFocus: Boolean) {
            val scale = if (hasFocus) 1.08f else 1.0f
            val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, scale)
            val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, scale)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                duration = 150
                interpolator = DecelerateInterpolator()
                start()
            }
            if (hasFocus) {
                view.elevation = 8f
            } else {
                view.elevation = 0f
            }
        }
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
        val slot = getItem(position)
        holder.itemView.setOnLongClickListener(null)

        if (slot.isEmpty) {
            holder.appIcon.visibility = View.GONE
            holder.appName.visibility = View.GONE
            holder.plusButton.visibility = View.VISIBLE
            holder.addText.visibility = View.VISIBLE

            holder.itemView.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onSlotClick(pos)
                }
            }
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
                    val pos = holder.bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onSlotLongClick(pos)
                    }
                    true
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppSlot>() {
            override fun areItemsTheSame(oldItem: AppSlot, newItem: AppSlot): Boolean {
                if (oldItem.isEmpty && newItem.isEmpty) return true
                if (oldItem.isEmpty != newItem.isEmpty) return false
                val oldApp = oldItem.appInfo ?: return false
                val newApp = newItem.appInfo ?: return false
                return AppIdentifier.encode(oldApp) == AppIdentifier.encode(newApp)
            }

            override fun areContentsTheSame(oldItem: AppSlot, newItem: AppSlot): Boolean {
                if (oldItem.isEmpty && newItem.isEmpty) return true
                val oldApp = oldItem.appInfo ?: return false
                val newApp = newItem.appInfo ?: return false
                return oldApp.packageName == newApp.packageName &&
                    oldApp.appName == newApp.appName &&
                    oldApp.shortcutId == newApp.shortcutId &&
                    oldApp.shortcutLabel == newApp.shortcutLabel
            }
        }
    }
}

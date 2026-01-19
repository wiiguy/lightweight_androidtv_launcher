package com.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSlotAdapter(
    var slots: MutableList<AppSlot>,
    private val onSlotClick: (Int) -> Unit,
    private val slotSizePx: Int
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
        val layoutParams = (view.layoutParams as RecyclerView.LayoutParams)
        layoutParams.width = slotSizePx
        layoutParams.height = slotSizePx
        view.layoutParams = layoutParams
        return AppSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppSlotViewHolder, position: Int) {
        val slot = slots[position]
        
        if (slot.isEmpty) {
            holder.appIcon.visibility = View.GONE
            holder.appName.visibility = View.GONE
            holder.plusButton.visibility = View.VISIBLE
            holder.addText.visibility = View.VISIBLE
            
            holder.itemView.setOnClickListener {
                onSlotClick(position)
            }
        } else {
            slot.appInfo?.let { app ->
                // Lazy load icon only when visible
                holder.appIcon.setImageDrawable(app.getIcon(holder.itemView.context.packageManager))
                holder.appName.text = app.getDisplayName()
                
                holder.appIcon.visibility = View.VISIBLE
                holder.appName.visibility = View.VISIBLE
                holder.plusButton.visibility = View.GONE
                holder.addText.visibility = View.GONE
                
                holder.itemView.setOnClickListener {
                    if (app.isShortcut) {
                        launchShortcut(holder.itemView.context, app.packageName, app.shortcutId!!)
                    } else {
                        launchApp(holder.itemView.context, app.packageName)
                    }
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
    
    companion object {
        const val MAX_SLOTS = 8
    }
    
    fun getMaxSlots(): Int = MAX_SLOTS

    private fun launchApp(context: android.content.Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                return
            }
        } catch (e: Exception) {
            // Continue to next method
        }
        
        try {
            val leanbackIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            leanbackIntent.addCategory(android.content.Intent.CATEGORY_LEANBACK_LAUNCHER)
            leanbackIntent.setPackage(packageName)
            
            val resolveInfos = context.packageManager.queryIntentActivities(leanbackIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                val activityInfo = resolveInfos[0].activityInfo
                val intent = android.content.Intent().apply {
                    setClassName(activityInfo.packageName, activityInfo.name)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            // Continue to next method
        }
        
        try {
            val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            mainIntent.setPackage(packageName)
            
            val resolveInfos = context.packageManager.queryIntentActivities(mainIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                val activityInfo = resolveInfos[0].activityInfo
                val intent = android.content.Intent().apply {
                    setClassName(activityInfo.packageName, activityInfo.name)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            // All methods failed
        }
    }
    
    private fun launchShortcut(context: android.content.Context, packageName: String, shortcutId: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val launcherApps = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                if (launcherApps != null) {
                    try {
                        // Try direct API first
                        val query = android.content.pm.LauncherApps.ShortcutQuery()
                        query.setPackage(packageName)
                        query.setShortcutIds(listOf(shortcutId))
                        query.setQueryFlags(
                            android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                                android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                        )

                        val shortcuts = launcherApps.getShortcuts(query, android.os.Process.myUserHandle())
                        shortcuts?.firstOrNull { it.id == shortcutId }?.let { shortcut ->
                            launcherApps.startShortcut(shortcut, null, null)
                            return
                        }
                    } catch (e: Exception) {
                        // Direct API failed, fall back to reflection
                        try {
                            val shortcutQueryClass = Class.forName("android.content.pm.LauncherApps\$ShortcutQuery")
                            val query = shortcutQueryClass.getDeclaredConstructor().newInstance()

                            val setPackageMethod = shortcutQueryClass.methods.firstOrNull {
                                it.name == "setPackage" && it.parameterTypes.size == 1
                            } ?: shortcutQueryClass.methods.firstOrNull {
                                it.name == "setPackageName" && it.parameterTypes.size == 1
                            }
                            val setShortcutIdsMethod = shortcutQueryClass.methods.firstOrNull {
                                it.name == "setShortcutIds" && it.parameterTypes.size == 1
                            }
                            val setQueryFlagsMethod = shortcutQueryClass.methods.firstOrNull {
                                it.name == "setQueryFlags" && it.parameterTypes.size == 1
                            }

                            if (setPackageMethod != null && setQueryFlagsMethod != null) {
                                setPackageMethod.invoke(query, packageName)
                                setShortcutIdsMethod?.invoke(query, listOf(shortcutId))
                                setQueryFlagsMethod.invoke(
                                    query,
                                    android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                                        android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                                )

                                val getShortcutsMethod = launcherApps.javaClass.getMethod(
                                    "getShortcuts",
                                    shortcutQueryClass,
                                    android.os.UserHandle::class.java
                                )
                                val shortcuts = getShortcutsMethod.invoke(
                                    launcherApps,
                                    query,
                                    android.os.Process.myUserHandle()
                                ) as? List<*>

                                shortcuts?.firstOrNull { shortcut ->
                                    val getIdMethod = shortcut?.javaClass?.getMethod("getId")
                                    getIdMethod?.invoke(shortcut) == shortcutId
                                }?.let { shortcut ->
                                    val startShortcutMethod = launcherApps.javaClass.getMethod(
                                        "startShortcut",
                                        android.content.pm.ShortcutInfo::class.java,
                                        android.graphics.Rect::class.java,
                                        android.os.Bundle::class.java
                                    )
                                    startShortcutMethod.invoke(launcherApps, shortcut, null, null)
                                    return
                                }
                            }
                        } catch (e2: Exception) {
                            // Reflection failed, fall through to app launch
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to regular app launch
        }
        
        // Fallback: launch the app normally if shortcut launch fails
        launchApp(context, packageName)
    }
}

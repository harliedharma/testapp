package com.example.testapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_item.view.*
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = Adapter(this)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
        )
        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        adapter.onItemClickListener = {
            selectDirectory(it.file)
        }

        openButton.setOnClickListener {
            selectDirectory(File(currentPathText.text.toString()))
        }

        selectDirectory(filesDir)
    }

    fun selectDirectory(directory: File?): Boolean {
        if(directory?.isDirectory == true) {
            currentPathText.setText(directory.absolutePath)
            adapter.directory = directory
            return true
        }
        Toast.makeText(this, "Cannot open directory: ${directory?.absolutePath}", Toast.LENGTH_SHORT).show()
        return false
    }
}
typealias OnItemClickListener = ((Adapter.Item) -> Unit)
class Adapter(
        val context: Context
): RecyclerView.Adapter<Adapter.ViewHolder>() {
    private var items: List<Item> = listOf()
    var directory: File? = null
        set(value) {
            field = value
            items = value?.takeIf { it.isDirectory }?.let {
                listOfNotNull(
                        it.parentFile?.let { Item(it, true) }
                ) + it.listFiles()
                        ?.filterNotNull()
                        ?.map { Item(it, false) }
                        .orEmpty()
            }.orEmpty()
            notifyDataSetChanged()
        }

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClickListener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val defaultTextColor = itemView.contentText.currentTextColor
        val errorTextColor = Color.RED
        fun bind(item: Item, onItemClickListener: OnItemClickListener?) {
            itemView.setOnClickListener { onItemClickListener?.invoke(item) }
            if(item.parent) {
                itemView.nameText.text = ".."
                itemView.pathText.visibility = View.GONE
                itemView.sizeText.visibility = View.GONE
                itemView.contentText.visibility = View.GONE
            } else {
                itemView.nameText.text = item.file.name
                itemView.pathText.visibility = View.VISIBLE
                itemView.pathText.text = item.file.absolutePath
                itemView.sizeText.visibility = View.VISIBLE
                itemView.sizeText.text = readableFileSize(item.file.length())
                itemView.contentText.visibility = View.VISIBLE
                var canRead = item.file.isFile && item.file.canRead()
                val stringBuilder = StringBuilder()
                if (canRead) {
                    try {
                        val reader = item.file.reader()
                        for (i in 0 until 10) {
                            val x = reader.read()
                            if (x == -1) break
                            stringBuilder.append(x.toChar())
                        }
                        if (reader.read() != -1) {
                            stringBuilder.append("...")
                        }
                    } catch (ignored: Exception) {
                        canRead = false
                    }
                }
                if (canRead) {
                    itemView.contentText.setTextColor(defaultTextColor)
                    itemView.contentText.text = "File: ${stringBuilder.toString()}"
                } else {
                    if(item.file.isDirectory) {
                        itemView.contentText.setTextColor(defaultTextColor)
                        itemView.contentText.text = "Directory"
                    } else {
                        itemView.contentText.setTextColor(errorTextColor)
                        itemView.contentText.text = "File: Can't read file"
                    }
                }
            }
        }

        private fun readableFileSize(size: Long): String {
            if (size <= 0) return "0"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
            return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())).toString() + " " + units[digitGroups]
        }

    }

    class Item(
            val file: File,
            val parent: Boolean = false
    )
}

package com.peanut.ted.ed

import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.peanut.ted.ed.Unities.gone
import com.peanut.ted.ed.Unities.name
import com.peanut.ted.ed.Unities.resolveUrl
import com.peanut.ted.ed.Unities.round
import com.peanut.ted.ed.Unities.visible
import com.squareup.picasso.Picasso

/**
 * 显示海报墙
 */
class AlbumAdapter(
        val context: Context,
        private val albums: MutableList<String>,
        private val scores: MutableList<Int>
) : RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            MyViewHolder(LayoutInflater.from(context).inflate(R.layout.card, parent, false))

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.tv.text = albums[position].name()
        val server = SettingManager.getValue("ip", "192.168.10.208").resolveUrl()
        loadImg("$server/getCover?cover=${Uri.encode(albums[position])}&" +
                "token=${SettingManager.getValue("token", "")}", holder.iv)
        holder.iv.round(4.dp)
        holder.score.text = scores[position].toString()
        holder.include.visible()
        holder.date.gone()
        holder.actionPlay.gone()
        holder.card.setOnClickListener {
            context.startActivity(Intent(context, DetailActivity::class.java).putExtra("ALBUM", albums[position]))
        }
    }


    inline val Double.dp: Float get() = run {
        return toFloat().dp
    }
    inline val Int.dp: Float get() = run {
        return toFloat().dp
    }
    inline val Float.dp: Float get() = run {
        val scale: Float = context.resources.displayMetrics.density
        return (this * scale + 0.5f)
    }

    override fun getItemCount(): Int {
        return albums.size
    }

    private fun loadImg(url: String,iv: ImageView) {
        Picasso.get().load(url).priority(Picasso.Priority.HIGH).error(R.mipmap.cover)
            .placeholder(R.mipmap.cover).into(iv)
    }
}
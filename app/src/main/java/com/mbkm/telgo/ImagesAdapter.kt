package com.mbkm.telgo

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide

class ImagesAdapter(
    private val imagesList: ArrayList<ImageModel>
) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageNameTextView: TextView = itemView.findViewById(R.id.tvImageName)
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.ivThumbnail)

        fun bind(image: ImageModel) {
            imageNameTextView.text = image.name

            // If image is null, display placeholder
            if (image.path == null) {
                thumbnailImageView.setImageResource(R.drawable.ic_arrow_back) // Placeholder
                thumbnailImageView.alpha = 0.5f
            } else {
                // Load image from Firebase Storage using Glide
                val storage = FirebaseStorage.getInstance()
                // Fix: Use the non-null path after null check
                val imageRef = storage.reference.child(image.path!!) // Add !! to assert non-null

                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(itemView.context)
                        .load(uri)
                        .placeholder(R.drawable.ic_visibility)
                        .error(R.drawable.ic_visibility)
                        .into(thumbnailImageView)
                }.addOnFailureListener {
                    thumbnailImageView.setImageResource(R.drawable.ic_visibility)
                }


                // Setup click listener to show full image
                thumbnailImageView.setOnClickListener {
                    showFullImage(image)
                }
            }
        }

        private fun showFullImage(image: ImageModel) {
            // Only show full image if path is not null
            if (image.path == null) return

            val dialog = Dialog(itemView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            dialog.setContentView(R.layout.dialog_full_image)

            val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
            val closeButton = dialog.findViewById<ImageView>(R.id.btnCloseFullImage)
            val imageTitle = dialog.findViewById<TextView>(R.id.tvFullImageTitle)

            imageTitle.text = image.name

            // Load full-size image
            val storage = FirebaseStorage.getInstance()
            val imageRef = storage.reference.child(image.path!!) // Add !! for non-null assertion

            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(itemView.context)
                    .load(uri)
                    .into(fullImageView)
            }


            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imagesList[position])
    }

    override fun getItemCount(): Int {
        return imagesList.size
    }
}
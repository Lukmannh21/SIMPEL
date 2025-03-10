package com.mbkm.telgo

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.firebase.storage.FirebaseStorage

class ImagesAdapter(
    private val imagesList: ArrayList<ImageModel>
) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    // Store a reference to active Glide request managers
    private var glideRequestManager: RequestManager? = null

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageNameTextView: TextView = itemView.findViewById(R.id.tvImageName)
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.ivThumbnail)

        fun bind(image: ImageModel) {
            imageNameTextView.text = image.name

            // If image is null, display placeholder
            if (image.path == null) {
                thumbnailImageView.setImageResource(R.drawable.ic_image_placeholder) // Placeholder
                thumbnailImageView.alpha = 0.5f
            } else {
                // Load image from Firebase Storage using Glide
                val storage = FirebaseStorage.getInstance()
                // Fix: Use the non-null path after null check
                val imageRef = storage.reference.child(image.path!!) // Add !! to assert non-null

                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Check if the adapter is still attached to a valid context
                    if (thumbnailImageView.context != null && isContextValid(thumbnailImageView.context)) {
                        try {
                            // Using applicationContext to prevent activity leaks
                            val context = thumbnailImageView.context.applicationContext

                            // Use the stored RequestManager if available
                            val glide = glideRequestManager ?: Glide.with(context)
                            glide.load(uri)
                                .placeholder(R.drawable.ic_image_loading)
                                .error(R.drawable.ic_image_error)
                                .into(thumbnailImageView)
                        } catch (e: Exception) {
                            // Log or safely handle any Glide exceptions
                            e.printStackTrace()
                        }
                    }
                }.addOnFailureListener {
                    // Only update UI if view is still attached to window
                    if (thumbnailImageView.isAttachedToWindow) {
                        thumbnailImageView.setImageResource(R.drawable.ic_visibility)
                    }
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

            try {
                val context = itemView.context
                // Check if context is still valid
                if (!isContextValid(context)) return

                val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.setContentView(R.layout.dialog_full_image)

                val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
                val closeButton = dialog.findViewById<ImageView>(R.id.btnCloseFullImage)
                val imageTitle = dialog.findViewById<TextView>(R.id.tvFullImageTitle)

                imageTitle.text = image.name

                // Load full-size image
                val storage = FirebaseStorage.getInstance()
                val imageRef = storage.reference.child(image.path!!)

                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Check if dialog is still showing before loading image
                    if (dialog.isShowing && isContextValid(context)) {
                        try {
                            Glide.with(context.applicationContext)
                                .load(uri)
                                .into(fullImageView)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                closeButton.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    // Method to check if a context is still valid (not destroyed)
    private fun isContextValid(context: android.content.Context): Boolean {
        if (context is android.app.Activity) {
            return !context.isFinishing && !context.isDestroyed
        }
        return true
    }

    // Method to be called in onStart of activity
    fun onStart(activity: android.app.Activity) {
        glideRequestManager = Glide.with(activity)
    }

    // Method to be called in onStop of activity
    fun onStop() {
        glideRequestManager = null
    }
}
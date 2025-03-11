package com.mbkm.telgo

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImagesAdapter(
    private val imagesList: ArrayList<ImageModel>,
    private val activity: AppCompatActivity? = null
) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    private val TAG = "ImagesAdapter"
    private var glideRequestManager: RequestManager? = null

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageNameTextView: TextView = itemView.findViewById(R.id.tvImageName)
        val thumbnailImageView: ImageView = itemView.findViewById(R.id.ivThumbnail)

        fun bind(image: ImageModel) {
            imageNameTextView.text = image.name

            // If image is null, display placeholder
            if (image.path == null) {
                thumbnailImageView.setImageResource(R.drawable.ic_image_placeholder)
                thumbnailImageView.alpha = 0.5f
                // Remove click listener for null images
                thumbnailImageView.setOnClickListener(null)
            } else {
                // Reset alpha (in case this is a recycled view)
                thumbnailImageView.alpha = 1.0f

                // Load image from Firebase Storage using Glide
                val storage = FirebaseStorage.getInstance()
                val imageRef = storage.reference.child(image.path!!)

                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    try {
                        // Make sure context is still valid
                        val context = thumbnailImageView.context
                        if (context != null && isContextValid(context)) {
                            // Use the stored RequestManager if available
                            val glide = glideRequestManager ?: Glide.with(context.applicationContext)
                            glide.load(uri)
                                .placeholder(R.drawable.ic_image_loading)
                                .error(R.drawable.ic_image_error)
                                .into(thumbnailImageView)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading thumbnail: ${e.message}")
                        e.printStackTrace()
                    }
                }.addOnFailureListener { e ->
                    // Handle failure
                    Log.e(TAG, "Failed to get download URL: ${e.message}")
                    if (thumbnailImageView.isAttachedToWindow) {
                        thumbnailImageView.setImageResource(R.drawable.ic_visibility)
                    }
                }

                // Setup click listener to show full image
                thumbnailImageView.setOnClickListener {
                    Log.d(TAG, "Image clicked: ${image.name}")
                    showFullImage(image)
                }
            }
        }

        private fun showFullImage(image: ImageModel) {
            if (image.path == null) {
                Log.e(TAG, "Cannot show full image: Path is null")
                return
            }

            try {
                val context = itemView.context
                if (!isContextValid(context)) {
                    Log.e(TAG, "Cannot show dialog: Context is invalid")
                    return
                }

                Log.d(TAG, "Creating dialog for image: ${image.name}")

                // Create and configure the dialog
                val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.setContentView(R.layout.dialog_full_image)

                // Initialize views from the dialog
                val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
                val closeButton = dialog.findViewById<ImageView>(R.id.btnCloseFullImage)
                // Change this line to use ImageView instead of Button
                val downloadButton = dialog.findViewById<ImageView>(R.id.btnDownloadImage)
                val imageTitle = dialog.findViewById<TextView>(R.id.tvFullImageTitle)

                imageTitle.text = image.name

                // Load full-size image
                val storage = FirebaseStorage.getInstance()
                val imageRef = storage.reference.child(image.path!!)

                Log.d(TAG, "Loading full image from path: ${image.path}")

                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    try {
                        // Check if dialog is still showing
                        if (dialog.isShowing && isContextValid(context)) {
                            Log.d(TAG, "Image download URL retrieved successfully")

                            // Load image with Glide
                            Glide.with(context.applicationContext)
                                .load(uri)
                                .into(fullImageView)

                            // Set up download button
                            downloadButton.setOnClickListener {
                                downloadImage(fullImageView, image.name, context)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading full image: ${e.message}")
                        e.printStackTrace()
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get full image download URL: ${e.message}")
                    Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                }

                // Set close button action
                closeButton.setOnClickListener {
                    dialog.dismiss()
                }

                // Show the dialog
                Log.d(TAG, "Showing full image dialog")
                dialog.show()

            } catch (e: Exception) {
                Log.e(TAG, "Error showing full image: ${e.message}")
                e.printStackTrace()
                Toast.makeText(itemView.context, "Error displaying image", Toast.LENGTH_SHORT).show()
            }
        }

        // Method to download the image from ImageView
        private fun downloadImage(imageView: ImageView, imageName: String, context: android.content.Context) {
            try {
                // Get bitmap from ImageView
                val drawable = imageView.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    saveImageToGallery(bitmap, imageName, context)
                } else {
                    Toast.makeText(context, "Cannot download image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error downloading image: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        // Method to save the bitmap to the gallery
        private fun saveImageToGallery(bitmap: Bitmap, imageName: String, context: android.content.Context) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "TelGo_${imageName.replace(" ", "_")}_$timestamp.jpg"

            var fos: OutputStream? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10 (API 29) and above, use MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TelGo")
                    }

                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        fos = context.contentResolver.openOutputStream(uri)
                    }
                } else {
                    // For older Android versions
                    val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/TelGo")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    val file = File(directory, fileName)
                    fos = FileOutputStream(file)

                    // Add the file to the gallery
                    val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    val contentUri = android.net.Uri.fromFile(file)
                    mediaScanIntent.data = contentUri
                    context.sendBroadcast(mediaScanIntent)
                }

                // Save the bitmap
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not save image", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                fos?.close()
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
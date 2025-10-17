package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: View
    private lateinit var emptyText: TextView
    private lateinit var adapter: UserAdapter
    private lateinit var firestore: FirebaseFirestore
    private var userStatus: String = "unverified"
    private var allUsers: List<UserModel> = emptyList()

    companion object {
        private const val ARG_STATUS = "arg_status"

        fun newInstance(status: String): UserListFragment {
            val fragment = UserListFragment()
            val args = Bundle()
            args.putString(ARG_STATUS, status)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userStatus = it.getString(ARG_STATUS) ?: "unverified"
        }

        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_list, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewUsers)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.emptyView)
        emptyText = view.findViewById(R.id.emptyText)

        setupRecyclerView()
        loadUsers()

        return view
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter { user ->
            // Handle user click - navigate to user details
            val intent = Intent(requireContext(), UserDetailActivity::class.java)
            intent.putExtra("USER_ID", user.uid)
            intent.putExtra("USER_STATUS", user.status)
            intent.putExtra("USER_ROLE", user.role) // NEW: Pass role to detail activity
            startActivity(intent)
            activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE

        // FIXED: Load users based on status only, not role
        val query = if (userStatus == "verified") {
            // For verified tab: Show users with status="verified"
            // EXCLUDE admin role (show both "user" and "editor" roles)
            firestore.collection("users")
                .whereEqualTo("status", "verified")
                .orderBy("registrationDate", Query.Direction.DESCENDING)
        } else {
            // For unverified tab: Show users with status="unverified"
            // EXCLUDE admin role
            firestore.collection("users")
                .whereEqualTo("status", "unverified")
                .orderBy("registrationDate", Query.Direction.DESCENDING)
        }

        query.get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                // Filter out admin users in the app layer (since Firestore doesn't support NOT EQUAL easily)
                allUsers = documents.mapNotNull { doc ->
                    try {
                        val role = doc.getString("role") ?: "user"

                        // Only include users that are NOT admin
                        if (role != "admin") {
                            UserModel(
                                uid = doc.getString("uid") ?: doc.id,
                                fullName = doc.getString("fullName") ?: "",
                                email = doc.getString("email") ?: "",
                                nik = doc.getString("nik") ?: "",
                                companyName = doc.getString("companyName") ?: "",
                                unit = doc.getString("unit") ?: "",
                                position = doc.getString("position") ?: "",
                                phone = doc.getString("phone") ?: "",
                                role = role,
                                status = doc.getString("status") ?: "unverified",
                                registrationDate = doc.getLong("registrationDate") ?: 0,
                                lastLoginDate = doc.getLong("lastLoginDate") ?: 0,
                                createdAt = doc.getString("createdAt") ?: "",
                                updatedAt = doc.getString("updatedAt") ?: "",
                                createdBy = doc.getString("createdBy") ?: ""
                            )
                        } else {
                            null // Exclude admin users
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                adapter.setUsers(allUsers)
                updateEmptyState(allUsers.isEmpty())
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyText.text = "Error: ${e.message}"
            }
    }

    // Method to update search from activity
    fun updateSearch(query: String) {
        adapter.filterUsers(query)
        updateEmptyState(adapter.getUsers().isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.visibility = View.VISIBLE
            emptyText.text = if (userStatus == "verified") {
                if (adapter.getUsers().isEmpty() && allUsers.isNotEmpty()) {
                    "No users match your search"
                } else {
                    "No verified users found"
                }
            } else {
                if (adapter.getUsers().isEmpty() && allUsers.isNotEmpty()) {
                    "No users match your search"
                } else {
                    "No pending verification requests"
                }
            }
        } else {
            emptyView.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload users when returning to the fragment
        loadUsers()
    }
}
package com.example.saisevatourstravels

import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth

data class User(
    var username: String? = "",
    var contact: String? = "",
    var email: String? = "",
    var password: String? = ""
) {
    companion object {
        private val database = FirebaseDatabase.getInstance()
        private val userRef = database.getReference("users")

        // Save user to Firebase
        fun saveUser(userId: String, user: User, callback: (Boolean, String) -> Unit) {
            // Reference to the "users" node in Firebase Database
            val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            // Save the user object under the userId
            databaseRef.setValue(user).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Callback on success
                    callback(true, "User data saved successfully")
                } else {
                    // Callback on failure
                    callback(false, "Failed to save user data: ${task.exception?.message}")
                }
            }
        }

        // Get user from Firebase
        fun getUser(userId: String, callback: (User?, String?) -> Unit) {
            userRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    callback(user, null)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null, error.message)
                }
            })
        }

        // Update user in Firebase
        fun updateUser(userId: String, updates: Map<String, Any?>, onComplete: (Boolean, String?) -> Unit) {
            userRef.child(userId).updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, "User data updated successfully!")
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
        }

        // Delete user from Firebase
        fun deleteUser(userId: String, onComplete: (Boolean, String?) -> Unit) {
            userRef.child(userId).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, "User data deleted successfully!")
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
        }

        // Check if email exists in Firebase Authentication
        fun checkEmailExists(email: String, callback: (Boolean) -> Unit) {
            val auth = FirebaseAuth.getInstance()
            auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val emailExists = task.result?.signInMethods?.isNotEmpty() ?: false
                    callback(emailExists)
                } else {
                    callback(false)
                }
            }
        }

        // Create user in Firebase Authentication and save to database
        fun createUserInAuthAndSaveToDatabase(email: String, password: String, user: User, onComplete: (Boolean, String?) -> Unit) {
            val auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    saveUser(userId, user) { success, message ->
                        if (success) {
                            onComplete(true, "User created and data saved successfully!")
                        } else {
                            auth.currentUser?.delete() // Rollback user creation if data save fails
                            onComplete(false, message)
                        }
                    }
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
        }
    }
}

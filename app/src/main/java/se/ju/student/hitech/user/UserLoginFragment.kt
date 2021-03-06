package se.ju.student.hitech.user

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import se.ju.student.hitech.MainActivity
import se.ju.student.hitech.MainActivity.Companion.TAG_FRAGMENT_EVENTS
import se.ju.student.hitech.MainActivity.Companion.TAG_FRAGMENT_NEWS
import se.ju.student.hitech.MainActivity.Companion.TAG_USER_PAGE
import se.ju.student.hitech.R
import se.ju.student.hitech.chat.ChatRepository
import se.ju.student.hitech.user.UserRepository.Companion.userRepository

class UserLoginFragment : Fragment() {
    private lateinit var progressBar: ProgressBar
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 120
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_user_login, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient((context as MainActivity), gso)

        val emailInput =
            view?.findViewById<TextInputEditText>(R.id.admin_login_emailTextInputEditText)
        val passwordInput =
            view?.findViewById<TextInputEditText>(R.id.admin_login_passwordTextInputEditText)
        val loginButton = view?.findViewById<Button>(R.id.admin_login_loginButton)

        progressBar = view?.findViewById(R.id.admin_login_progressBar)!!

        loginButton?.setOnClickListener {
            userRepository.userLogout()
            if (verifyLoginInputs(
                    emailInput?.text.toString().trim(),
                    passwordInput?.text.toString()
                )
            ) {
                progressBar.visibility = View.VISIBLE
                userLogin(emailInput?.text.toString().trim(), passwordInput?.text.toString())
            }
        }

        val signInGoogle = view?.findViewById<Button>(R.id.sign_in_google)

        signInGoogle?.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            signInGoogle()
        }

        val forgotPassword = view?.findViewById<TextView>(R.id.admin_login_forgotPassword)

        forgotPassword?.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            resetPassword(emailInput?.text.toString().trim())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            userRepository.loginWithGoogle(data, (context as MainActivity)) { result ->
                when (result) {
                    "successful" -> {
                        ChatRepository().subscribeToNewChatNotifications { result2 ->
                            when (result2) {
                                "successful" -> {
                                    (context as MainActivity).makeToast(getString(R.string.loginSuccessful))
                                    // reload fragments where UI changes when logged in
                                    (context as MainActivity).reloadFragment(TAG_FRAGMENT_EVENTS)
                                    (context as MainActivity).reloadFragment(TAG_FRAGMENT_NEWS)
                                    (context as MainActivity).reloadFragment(TAG_USER_PAGE)
                                    (context as MainActivity).changeToFragment(TAG_USER_PAGE)
                                }
                                "internalError" -> {
                                    (context as MainActivity).makeToast(getString(R.string.internalError))
                                    UserRepository().userLogout()
                                }
                            }
                            progressBar.visibility = View.GONE
                        }
                    }
                    "needsVerification" -> {
                        (context as MainActivity).makeToast(getString(R.string.user_not_verified))
                        progressBar.visibility = View.GONE
                    }
                    "internalError" -> {
                        (context as MainActivity).makeToast(getString(R.string.internalError))
                        UserRepository().userLogout()
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun resetPassword(email: String) {

        if (email.isEmpty()) {
            progressBar.visibility = View.GONE
            view?.findViewById<TextInputLayout>(R.id.admin_login_emailTextInputLayout)?.error =
                getString(R.string.resetPasswordInputError)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            progressBar.visibility = View.GONE
            view?.findViewById<TextInputLayout>(R.id.admin_login_emailTextInputLayout)?.error =
                getString(R.string.invalidEmail)
        } else {
            AlertDialog.Builder(context as MainActivity)
                .setTitle(getString(R.string.user_page_resetPassword))
                .setMessage(getString(R.string.resetPasswordConfirmation) + " $email?")
                .setPositiveButton(
                    getString(R.string.yes)
                ) { _, _ ->
                    userRepository.sendPasswordReset(email) { result ->
                        progressBar.visibility = View.GONE
                        when (result) {
                            "successful" -> (context as MainActivity).makeToast(getString(R.string.resetConfirmed) + " $email!")
                            "internalError" -> (context as MainActivity).makeToast(getString(R.string.internalError))
                        }
                    }
                }
                .setNegativeButton(
                    getString(R.string.no)
                ) { _, _ ->
                    progressBar.visibility = View.GONE
                    //Don't delete
                }
                .show()
        }
    }

    private fun verifyLoginInputs(email: String, password: String): Boolean {
        val emailInputLayout =
            view?.findViewById<TextInputLayout>(R.id.admin_login_emailTextInputLayout)
        val passwordInputLayout =
            view?.findViewById<TextInputLayout>(R.id.admin_login_passwordTextInputLayout)
        emailInputLayout?.error = ""
        passwordInputLayout?.error = ""

        if (email.isEmpty()) {
            emailInputLayout?.error = getString(R.string.emailEmpty)
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout?.error = getString(R.string.invalidEmail)
            return false
        }

        if (password.isEmpty()) {
            passwordInputLayout?.error = getString(R.string.passwordEmpty)
            return false

        }
        return true
    }

    private fun userLogin(email: String, password: String) {
        userRepository.userLogin(email, password) { result ->
            progressBar.visibility = View.GONE
            when (result) {
                "successful" -> {
                    ChatRepository().subscribeToNewChatNotifications { result2 ->
                        when (result2) {
                            "successful" -> {
                                (context as MainActivity).makeToast(getString(R.string.loginSuccessful))
                                // reload fragments where UI changes when logged in
                                (context as MainActivity).reloadFragment(TAG_FRAGMENT_EVENTS)
                                (context as MainActivity).reloadFragment(TAG_FRAGMENT_NEWS)
                                (context as MainActivity).reloadFragment(TAG_USER_PAGE)
                                (context as MainActivity).changeToFragment(TAG_USER_PAGE)
                            }
                            "internalError" -> {
                                (context as MainActivity).makeToast(getString(R.string.internalError))
                                UserRepository().userLogout()
                            }
                        }
                    }
                }
                "emailNotFound" -> (context as MainActivity).makeToast(getString(R.string.emailNotFound))
                "invalidPassword" -> (context as MainActivity).makeToast(getString(R.string.invalidPassword))
                "emailNotVerified" -> (context as MainActivity).makeToast(getString(R.string.emailNotVerified))
                "internalError" -> (context as MainActivity).makeToast(getString(R.string.internalError))
            }
        }
    }
}
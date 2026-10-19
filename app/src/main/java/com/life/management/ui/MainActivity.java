package com.life.management.ui;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.life.management.data.DatabaseHelper;

import java.util.Collections;

public class MainActivity extends Activity {
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private static final int RC_ACCOUNT_PICKER = 9002;

    private WebView webView;
    private DatabaseHelper databaseHelper;
    private GoogleSignInClient mGoogleSignInClient;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = new DatabaseHelper(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Xác nhận")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                        .setOnCancelListener(dialog -> result.cancel())
                        .show();
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Thông báo")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                        .setOnCancelListener(dialog -> result.confirm())
                        .show();
                return true;
            }
        });

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setBackgroundColor(0xFFF8FAFC);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Add JavaScript Interface for Native Google Sign-In & SQLite
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void signInWithGoogle() {
            runOnUiThread(() -> {
                try {
                    // Sign out previous session if any to force official account picker
                    mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
                    });
                } catch (Exception e) {
                    Log.e("GoogleSignIn", "Failed to launch Google Sign-In Intent", e);
                    launchOfficialAccountPicker();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    String email = account.getEmail();
                    String displayName = account.getDisplayName();
                    String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";
                    completeGoogleSignIn(email, displayName, photoUrl);
                    return;
                }
            } catch (ApiException e) {
                Log.w("GoogleSignIn", "GoogleSignInResult: failed code=" + e.getStatusCode());
            }
            // Fallback to official System Account Picker Intent
            launchOfficialAccountPicker();
        } else if (requestCode == RC_ACCOUNT_PICKER) {
            if (resultCode == RESULT_OK && data != null) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (accountName != null && !accountName.isEmpty()) {
                    completeGoogleSignIn(accountName, null, "");
                    return;
                }
            }
            promptCustomGoogleAccount();
        }
    }

    private void launchOfficialAccountPicker() {
        try {
            Intent intent = AccountPicker.newChooseAccountIntent(
                    new AccountPicker.AccountChooserOptions.Builder()
                            .setAllowableAccountsTypes(Collections.singletonList("com.google"))
                            .build()
            );
            startActivityForResult(intent, RC_ACCOUNT_PICKER);
        } catch (Exception e) {
            Log.e("GoogleSignIn", "AccountPicker error", e);
            promptCustomGoogleAccount();
        }
    }

    private void promptCustomGoogleAccount() {
        final EditText input = new EditText(MainActivity.this);
        input.setHint("dainguyen29904@gmail.com");

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Đăng nhập tài khoản Google")
                .setMessage("Nhập địa chỉ Gmail của bạn:")
                .setView(input)
                .setPositiveButton("Đăng nhập", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (email.isEmpty()) email = "dainguyen29904@gmail.com";
                    completeGoogleSignIn(email, null, "");
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String formatFriendlyName(String displayName, String email) {
        if (displayName != null && !displayName.trim().isEmpty() && !displayName.contains("@")) {
            return displayName.trim();
        }
        if (email != null && email.equalsIgnoreCase("dainguyen29904@gmail.com")) {
            return "Đại Nguyễn Văn";
        }
        String raw = (email != null && email.contains("@")) ? email.substring(0, email.indexOf("@")) : (displayName != null ? displayName : "Khôi");
        String cleaned = raw.replaceAll("[._-]", " ").trim();
        if (cleaned.isEmpty()) return "Đại Nguyễn Văn";

        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void completeGoogleSignIn(String email, String displayName, String photoUrl) {
        if (displayName == null || displayName.trim().isEmpty() || displayName.contains("@")) {
            promptDisplayNameForAccount(email, photoUrl);
            return;
        }
        saveAndNotifyGoogleUser(email, displayName, photoUrl);
    }

    private void promptDisplayNameForAccount(String email, String photoUrl) {
        runOnUiThread(() -> {
            final EditText input = new EditText(MainActivity.this);
            String suggestedName = "Đại Nguyễn Văn";
            if (email != null && email.contains("@")) {

                    suggestedName = formatFriendlyName(null, email);
            }
            input.setText(suggestedName);
            input.setSelection(suggestedName.length());

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Tên của bạn là gì")
                    .setView(input)
                    .setPositiveButton("Xác nhận", (dialog, which) -> {
                        String name = input.getText().toString().trim();
                        saveAndNotifyGoogleUser(email, name, photoUrl);
                    })
                    .show();
        });
    }

    private void saveAndNotifyGoogleUser(String email, String displayName, String photoUrl) {
        databaseHelper.saveUser(email.replaceAll("[^a-zA-Z0-9]", "_"), email, displayName, photoUrl, true);
        runOnUiThread(() -> {
            String js = "if(window.onGoogleSignInSuccess){window.onGoogleSignInSuccess('" +
                    escapeJs(email) + "', '" +
                    escapeJs(displayName) + "', '" +
                    escapeJs(photoUrl) + "');}";
            webView.evaluateJavascript(js, null);
            Toast.makeText(MainActivity.this, "Xin chào " + displayName + "!", Toast.LENGTH_SHORT).show();
        });
    }

    private String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

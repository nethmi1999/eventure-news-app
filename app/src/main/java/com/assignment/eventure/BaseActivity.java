package com.assignment.eventure;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected MaterialToolbar toolbar;
    private Window window;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureStatusBar();
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.status_bar_color));
        }

        int color = ContextCompat.getColor(this, R.color.status_bar_color); // Replace with your color
        setStatusBarColorWithInsets(this, color);

    }


    public void setStatusBarColorWithInsets(Activity activity, int color) {
        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // API 34+
            decorView.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars());
                v.setBackgroundColor(color);

                // Adjust padding to prevent overlap
                v.setPadding(0, statusBarInsets.top, 0, 0);

                return insets;
            });
        } else {
            // For Android 14 and below
            window.setStatusBarColor(color);
        }
    }
    protected void showPopupMenu(View anchor) {
        try {
            Log.d(TAG, "Creating popup menu");
            PopupMenu popupMenu = new PopupMenu(this, anchor);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                popupMenu.setGravity(Gravity.END);
            }

            popupMenu.getMenuInflater().inflate(R.menu.top_dropdown_menu, popupMenu.getMenu());
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || !"admin@gmail.com".equals(user.getEmail())) {
                popupMenu.getMenu().removeItem(R.id.menu_add_news);
            }

            // Set rounded background to the internal ListView of the PopupMenu
            popupMenu.setOnDismissListener(menu -> Log.d(TAG, "Popup menu dismissed"));

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                Log.d(TAG, "Popup menu item clicked: " + id);

                if (id == R.id.menu_user_info) {
                    // Open UserInfoActivity instead of showing toast
                    Intent intent = new Intent(this, UserInfoActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.menu_dev_info) {
                    Intent intent = new Intent(this, DeveloperInfoActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.menu_add_news) {
                    Intent intent = new Intent(this, AddNewsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.menu_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return true;
                }

                return false;
            });

            // Show the popup first, so the internal ListView can be accessed
            popupMenu.show();

            // Access the popup's ListView and set custom background
            // Warning: This uses reflection and internal API, which might break in future Android versions
            try {
                java.lang.reflect.Field mPopupField = popupMenu.getClass().getDeclaredField("mPopup");
                mPopupField.setAccessible(true);
                Object mPopup = mPopupField.get(popupMenu);

                if (mPopup != null) {
                    java.lang.reflect.Method getListViewMethod = mPopup.getClass().getDeclaredMethod("getListView");
                    getListViewMethod.setAccessible(true);
                    ListView listView = (ListView) getListViewMethod.invoke(mPopup);

                    if (listView != null) {
                        Drawable bg = getResources().getDrawable(R.drawable.popup_menu_background);
                        listView.setBackground(bg);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to set popup menu background with rounded corners", e);
            }

            Log.d(TAG, "Popup menu shown successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error showing popup menu", e);
            Toast.makeText(this, "Error showing menu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void setupToolbarWithUsername() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String username = documentSnapshot.getString("username");
                        if (username == null || username.isEmpty()) {
                            username = firebaseUser.getEmail().split("@")[0];
                        }
                        addRightAlignedUsername(username);
                    })
                    .addOnFailureListener(e -> addRightAlignedUsername("User"));
        } else {
            addRightAlignedUsername("User");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void addRightAlignedUsername(String username) {
        TextView textView = new TextView(this);
        textView.setText(username);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(18);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); // Ensure text inside view aligns right

        Drawable avatarDrawable = ContextCompat.getDrawable(this, R.drawable.account_circle);
        if (avatarDrawable != null) {
            int size = dpToPx(24);
            avatarDrawable.setBounds(0, 0, size, size); // Scale it manually if needed
            textView.setCompoundDrawables(null, null, avatarDrawable, null);
            textView.setCompoundDrawablePadding(8);
        }

        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;

        textView.setLayoutParams(layoutParams);
        textView.setPadding(0, 0, 24, 0);
        textView.setOnClickListener(v -> showPopupMenu(v));

        toolbar.addView(textView);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }


    public void setStatusBarColor(Activity activity, int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.setStatusBarColor(ContextCompat.getColor(activity, colorResId));
        }
    }
    public void configureStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                setStatusBarColor(this, R.color.status_bar_color);
            } catch (Exception e) {
                try {
                    setStatusBarColor(this, R.color.primary_color);
                } catch (Exception e2) {
                    getWindow().setStatusBarColor(Color.parseColor("#4CAF50")); // Green color
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

            Log.d(TAG, "Status bar color configured successfully");
        }
    }
}
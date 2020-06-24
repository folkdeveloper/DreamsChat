package com.dreams.chat.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreams.chat.BaseApplication;
import com.dreams.chat.R;
import com.dreams.chat.adapters.MenuUsersRecyclerAdapter;
import com.dreams.chat.fragments.UserSelectDialogFragment;
import com.dreams.chat.interfaces.OnUserGroupItemClick;
import com.dreams.chat.interfaces.UserGroupSelectionDismissListener;
import com.dreams.chat.models.Contact;
import com.dreams.chat.models.Group;
import com.dreams.chat.models.Message;
import com.dreams.chat.models.Status;
import com.dreams.chat.models.User;
import com.dreams.chat.services.FetchMyUsersService;
import com.dreams.chat.utils.ConfirmationDialogFragment;
import com.dreams.chat.utils.Helper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

public class ContactActivity extends BaseActivity implements OnUserGroupItemClick, UserGroupSelectionDismissListener {

    private RecyclerView menuRecyclerView;
    private SwipeRefreshLayout swipeMenuRecyclerView;
    private MenuUsersRecyclerAdapter menuUsersRecyclerAdapter;
    // private EditText searchContact;
    private SearchView searchView;
    private ArrayList<User> myUsers = new ArrayList<>();
    private final int CONTACTS_REQUEST_CODE = 321;
    private static String USER_SELECT_TAG = "userselectdialog";
    private static final int REQUEST_CODE_CHAT_FORWARD = 99;
    private UserSelectDialogFragment userSelectDialogFragment;
    private ArrayList<Message> messageForwardList = new ArrayList<>();
    private ImageView backImage;
    private TextView user_name;

    @Override
    void myUsersResult(ArrayList<User> myUsers) {
        helper.setCacheMyUsers(myUsers);
        this.myUsers.clear();
        this.myUsers.addAll(myUsers);
        refreshUsers(-1);
        try {
            menuUsersRecyclerAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        swipeMenuRecyclerView.setRefreshing(false);
    }

    @Override
    void myContactsResult(ArrayList<Contact> myContacts) {

    }

    @Override
    void userAdded(User valueUser) {

    }

    @Override
    void groupAdded(Group valueGroup) {

    }

    @Override
    void userUpdated(User valueUser) {
        int existingPos = myUsers.indexOf(valueUser);
        if (existingPos != -1) {
            valueUser.setNameInPhone(myUsers.get(existingPos).getNameInPhone());
            myUsers.set(existingPos, valueUser);
            helper.setCacheMyUsers(myUsers);
            menuUsersRecyclerAdapter.notifyItemChanged(existingPos);
            refreshUsers(existingPos);
        }
    }

    @Override
    void groupUpdated(Group valueGroup) {

    }

    @Override
    void statusAdded(Status status) {

    }

    @Override
    void statusUpdated(Status status) {

    }

    @Override
    void onSinchConnected() {

    }

    @Override
    void onSinchDisconnected() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);
        menuRecyclerView = findViewById(R.id.menu_recycler_view);
        user_name = findViewById(R.id.user_name);
        swipeMenuRecyclerView = findViewById(R.id.menu_recycler_view_swipe_refresh);
        searchView = findViewById(R.id.searchView);

        searchView.setIconified(true);
        ImageView searchIcon = searchView.findViewById(android.support.v7.appcompat.R.id.search_button);
        searchIcon.setImageDrawable(ContextCompat.getDrawable(ContactActivity.this, R.drawable.ic_search_white));
        SearchView.SearchAutoComplete searchAutoComplete =
                searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setHintTextColor(getResources().getColor(android.R.color.white));
        searchAutoComplete.setTextColor(getResources().getColor(android.R.color.white));

        backImage = findViewById(R.id.back_button);
        clickListner();

        setupMenu();
    }

    private void clickListner() {

        backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private void setupMenu() {
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuUsersRecyclerAdapter = new MenuUsersRecyclerAdapter(this, myUsers, helper.getLoggedInUser());
        menuRecyclerView.setAdapter(menuUsersRecyclerAdapter);
        swipeMenuRecyclerView.setColorSchemeResources(R.color.colorAccent);
        swipeMenuRecyclerView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchContacts();
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                menuUsersRecyclerAdapter.getFilter().filter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                menuUsersRecyclerAdapter.getFilter().filter(s);
                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                backImage.setVisibility(View.GONE);
                user_name.setVisibility(View.GONE);
            } else {
                searchView.setIconified(true);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                backImage.setVisibility(View.VISIBLE);
                user_name.setVisibility(View.VISIBLE);
            }

        });

    }

    private void fetchContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            if (!FetchMyUsersService.STARTED) {
                if (!swipeMenuRecyclerView.isRefreshing())
                    swipeMenuRecyclerView.setRefreshing(true);
                FetchMyUsersService.startMyUsersService(ContactActivity.this, userMe.getId(), "");
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CONTACTS_REQUEST_CODE:
                fetchContacts();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchContacts();
    }

    private void refreshUsers(int pos) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(USER_SELECT_TAG);
        if (frag != null) {
            userSelectDialogFragment.refreshUsers(pos);
        }
    }


    @Override
    public void OnUserClick(User user, int position, View userImage) {
//        if (userImage == null) {
//            userImage = usersImage;
//        }
        userMe = helper.getLoggedInUser();
        if (userMe.getBlockedUsersIds() != null && userMe.getBlockedUsersIds().contains(user.getId())) {
            FragmentManager manager = getSupportFragmentManager();
            Fragment frag = manager.findFragmentByTag("DELETE_TAG");
            if (frag != null) {
                manager.beginTransaction().remove(frag).commit();
            }
            unBlockAlert(user.getNameToDisplay(), userMe, ContactActivity.this,
                    helper, user.getId(), manager);
        } else {
            Intent intent = ChatActivity.newIntent(this, messageForwardList, user);
            if (Build.VERSION.SDK_INT > 21) {
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, userImage, "backImage");
                startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD, options.toBundle());
            } else {
                startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD);
                overridePendingTransition(0, 0);
            }

            if (userSelectDialogFragment != null)
                userSelectDialogFragment.dismiss();
        }
    }

    @Override
    public void OnGroupClick(Group group, int position, View userImage) {

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (REQUEST_CODE_CHAT_FORWARD):
                if (resultCode == Activity.RESULT_OK) {
                    //show forward dialog to choose users
                    messageForwardList.clear();
                    ArrayList<Message> temp = data.getParcelableArrayListExtra("FORWARD_LIST");
                    messageForwardList.addAll(temp);
                    userSelectDialogFragment = UserSelectDialogFragment.newInstance(this, myUsers);
                    FragmentManager manager = getSupportFragmentManager();
                    Fragment frag = manager.findFragmentByTag(USER_SELECT_TAG);
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    userSelectDialogFragment.show(manager, USER_SELECT_TAG);
                }
                break;
        }
    }

    private void unBlockAlert(String name, User userMe, Context context, Helper helper,
                              String userId, FragmentManager manager) {
        String UNBLOCK_TAG = "UNBLOCK_TAG";

        ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment
                .newInstance("UnBlock", String.format("Are you sure want to unblock %s",
                        name), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (userMe.getBlockedUsersIds().contains(userId)) {
                                    userMe.getBlockedUsersIds().remove(userId);
                                }

                                BaseApplication.getUserRef().child(userMe.getId()).child("blockedUsersIds")
                                        .setValue(userMe.getBlockedUsersIds())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                helper.setLoggedInUser(userMe);
                                                Toast.makeText(context, "Unblocked", Toast.LENGTH_SHORT).show();
                                                menuUsersRecyclerAdapter = new MenuUsersRecyclerAdapter(
                                                        ContactActivity.this, myUsers, userMe);
                                                menuRecyclerView.setAdapter(menuUsersRecyclerAdapter);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(context, "Unable to unblock user", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        },
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                            }
                        });
        confirmationDialogFragment.show(manager, UNBLOCK_TAG);
    }

    @Override
    public void onUserGroupSelectDialogDismiss() {

    }

    @Override
    public void selectionDismissed() {

    }
}

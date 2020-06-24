package com.dreams.chat.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreams.chat.BaseApplication;
import com.dreams.chat.R;
import com.dreams.chat.activities.MainActivity;
import com.dreams.chat.adapters.ChatAdapter;
import com.dreams.chat.interfaces.HomeIneractor;
import com.dreams.chat.models.Chat;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.ConfirmationDialogFragment;
import com.dreams.chat.utils.Helper;
import com.dreams.chat.views.MyRecyclerView;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by a_man on 30-12-2017.
 */

public class MyGroupsFragment extends Fragment {
    private MyRecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private Realm rChatDb;
    private User userMe;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private RealmResults<Chat> resultList;
    private ArrayList<Chat> chatDataList = new ArrayList<>();
    private ArrayList<Chat> tempChatDataList = new ArrayList<>();
    private static String CONFIRM_TAG = "confirmtag";
    MainActivity mainActivity;

    private RealmChangeListener<RealmResults<Chat>> chatListChangeListener = new
            RealmChangeListener<RealmResults<Chat>>() {
                @Override
                public void onChange(RealmResults<Chat> element) {
                    try {
                        if (element != null && element.isValid() && element.size() > 0) {
                            chatDataList.clear();
                            tempChatDataList.clear();
                            tempChatDataList.addAll(rChatDb.copyFromRealm(element));
                            for (Chat chat : tempChatDataList) {
                                if (chat.getGroup().getUserIds().contains(userMe.getId())) {
                                    chatDataList.add(chat);
                                }
                            }
                            chatAdapter.notifyDataSetChanged();
                        } else {
                            chatDataList.clear();
                            chatAdapter.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    private HomeIneractor homeInteractor;

    public MyGroupsFragment() {
    }

    public MyGroupsFragment(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            homeInteractor = (HomeIneractor) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement HomeIneractor");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Realm.init(getContext());
        rChatDb = Helper.getRealmInstance();
        userMe = homeInteractor.getUserMe();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_recycler, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        mySwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_lay);
        mySwipeRefreshLayout.setRefreshing(false);
        recyclerView.setEmptyView(view.findViewById(R.id.emptyView));
        recyclerView.setEmptyImageView(((ImageView) view.findViewById(R.id.emptyImage)));
        TextView emptyTextView = view.findViewById(R.id.emptyText);
        emptyTextView.setText(getString(R.string.empty_group_chat_list));
        recyclerView.setEmptyTextView(emptyTextView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    RealmQuery<Chat> query = rChatDb.where(Chat.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
                    resultList = query.isNotNull("group").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time
                } catch (Exception e) {
                    e.printStackTrace();
                }

                chatDataList.clear();
                chatDataList.addAll(rChatDb.copyFromRealm(resultList));

                chatAdapter = new ChatAdapter(getActivity(), chatDataList, userMe.getId(), "group");
                recyclerView.setAdapter(chatAdapter);

                try {
                    resultList.addChangeListener(chatListChangeListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mySwipeRefreshLayout.setRefreshing(false);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            RealmQuery<Chat> query = rChatDb.where(Chat.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
            resultList = query.isNotNull("group").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time
        } catch (Exception e) {
            e.printStackTrace();
        }

        chatDataList.clear();
        chatDataList.addAll(rChatDb.copyFromRealm(resultList));
        if (userMe != null && userMe.getId() != null) {
            chatAdapter = new ChatAdapter(getActivity(), chatDataList, userMe.getId(), "group");
            recyclerView.setAdapter(chatAdapter);
        }

        try {
            resultList.addChangeListener(chatListChangeListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
//        homeInteractor = null;
        if (resultList != null)
            resultList.removeChangeListener(chatListChangeListener);
    }

    public void deleteSelectedChats() {

        final FragmentManager manager = getActivity().getSupportFragmentManager();
        Fragment frag = manager.findFragmentByTag(CONFIRM_TAG);
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }

        // rChatDb.beginTransaction();
        rChatDb.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                boolean isExited;
                String title = "", message = "";
                for (final Chat chat : chatDataList) {
                    if (chat.isSelected()) {
                        if (chat.getGroup().getGrpExitUserIds() == null) {
                            isExited = true;
                            title = "Delete Group";
                            message = "Are you sure want to delete the group?";
                        } else if (chat.getGroup().getGrpExitUserIds() != null
                                && chat.getGroup().getGrpExitUserIds().contains(userMe.getId())) {
                            isExited = true;
                            title = "Delete Group";
                            message = "Are you sure want to delete the group?";
                        } else {
                            isExited = false;
                            title = "Exit Group";
                            message = "Are you sure want to exit from the group?";
                        }

                        final boolean finalIsExited = isExited;
                        ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment.newInstance(title,
                                message,
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        if (finalIsExited) {
                                            if (chat.getGroup().getUserIds() != null
                                                    && chat.getGroup().getUserIds().size() > 0
                                                    && chat.getGroup().getUserIds().contains(userMe.getId())) {
                                                ArrayList<String> userIds = new ArrayList<>();
                                                userIds.addAll(chat.getGroup().getUserIds());
                                                userIds.remove(userMe.getId());
                                                chat.getGroup().getGrpExitUserIds().remove(userMe.getId());
                                                chat.getGroup().setUserIds(userIds);
                                                BaseApplication.getGroupRef().child(chat.getGroupId()).setValue(chat.getGroup()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        if (chat.getGroup() != null && chat.getGroup().getId() != null) {
                                                            Helper.deleteGroupFromRealm(rChatDb, chat.getGroup().getId());
                                                        }
                                                        Toast.makeText(getContext(), "Group Deleted", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        } else {
                                            ArrayList<String> userIds = new ArrayList<>();
                                            if (chat.getGroup().getAdmin().equalsIgnoreCase(userMe.getId())) {
                                                RealmList<String> dUserIds = new RealmList<>();
                                                dUserIds.addAll(chat.getGroup().getUserIds());
                                                dUserIds.remove(userMe.getId());
                                                if (dUserIds != null && dUserIds.size() > 0) {
                                                    chat.getGroup().setAdmin(Helper.getRandomElement(dUserIds, chat.getGroup().getUserIds().size()).get(0));
                                                }
                                            }
                                            userIds.add(userMe.getId());
                                            chat.getGroup().setGrpExitUserIds(userIds);

                                            BaseApplication.getGroupRef().child(chat.getGroup().getId()).setValue(chat.getGroup()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(getContext(), "You have been exited from the group", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        mainActivity.disableContextualMode();
                                        disableContextualMode();
                                    }
                                },
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        mainActivity.disableContextualMode();
                                        disableContextualMode();
                                    }
                                });

                        confirmationDialogFragment.show(manager, CONFIRM_TAG);

//                        if (chat.getGroupId().split(Helper.GROUP_PREFIX)[1].split("_")[1].equalsIgnoreCase(userMe.getId())) {
////                            BaseApplication.getChatRef().child(chat.getGroupId()).removeValue();
////                            BaseApplication.getGroupRef().child(chat.getGroupId()).removeValue();
//                            BaseApplication.getUserRef().child(userMe.getId()).child(Helper.REF_GROUP).addListenerForSingleValueEvent(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                    Log.d("tag", dataSnapshot.toString());
//                                    ArrayList arrayList = new ArrayList();
//                                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
//                                        if (dataSnapshot1.child("id").getValue().toString().equalsIgnoreCase(chat.getGroupId())) {
//                                            dataSnapshot1.getRef().removeValue();
//                                            // Toast.makeText(context, "Removed Successfully", Toast.LENGTH_SHORT).show();
//                                        }
//                                    }
//                                }
//
//                                @Override
//                                public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                                }
//                            });
//
//
////                            Chat chatToDelete = rChatDb.where(Chat.class).equalTo("myId", userMe.getId()).equalTo("groupId", chat.getGroupId()).findFirst();
////                            if (chatToDelete != null) {
////                                RealmObject.deleteFromRealm(chatToDelete);
////                            }
//                        } else {
//                            disableContextualMode();
//                            Toast.makeText(getActivity(), "You are not an admin", Toast.LENGTH_SHORT).show();
//                        }
                    }
                }
            }
        });

        // rChatDb.commitTransaction();
        try {
            RealmQuery<Chat> query = rChatDb.where(Chat.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
            resultList = query.isNotNull("group").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time
        } catch (Exception e) {
            e.printStackTrace();
        }

        chatDataList.clear();
        chatDataList.addAll(rChatDb.copyFromRealm(resultList));

        chatAdapter = new ChatAdapter(getActivity(), chatDataList, userMe.getId(), "group");
        recyclerView.setAdapter(chatAdapter);
    }

    public void disableContextualMode() {
        chatAdapter.disableContextualMode();
    }
}

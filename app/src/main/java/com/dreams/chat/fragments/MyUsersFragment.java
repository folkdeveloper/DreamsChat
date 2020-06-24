package com.dreams.chat.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.dreams.chat.BaseApplication;
import com.dreams.chat.R;
import com.dreams.chat.activities.MainActivity;
import com.dreams.chat.adapters.ChatAdapter;
import com.dreams.chat.interfaces.HomeIneractor;
import com.dreams.chat.models.Chat;
import com.dreams.chat.models.Contact;
import com.dreams.chat.models.User;
import com.dreams.chat.models.solochat;
import com.dreams.chat.utils.ConfirmationDialogFragment;
import com.dreams.chat.utils.Helper;
import com.dreams.chat.views.MyRecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MyUsersFragment extends Fragment {
    private MyRecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private Realm rChatDb;
    private User userMe;
    private RealmResults<Chat> resultList;
    private ArrayList<Chat> chatDataList = new ArrayList<>();
    private static String CONFIRM_TAG = "confirmtag";
    MainActivity mainActivity;
    private long timeStamp;

    private RealmChangeListener<RealmResults<Chat>> chatListChangeListener = new RealmChangeListener<RealmResults<Chat>>() {
        @Override
        public void onChange(RealmResults<Chat> element) {
            if (element != null && element.isValid() && element.size() > 0) {
                chatDataList.clear();
                chatDataList.addAll(rChatDb.copyFromRealm(element));
                setUserNamesAsInPhone();
            } else {
                chatDataList.clear();
                chatAdapter.notifyDataSetChanged();
            }
        }
    };
    private HomeIneractor homeInteractor;
    private Helper helper;

    public MyUsersFragment() {
    }

    public MyUsersFragment(MainActivity mainActivity) {
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
        helper = new Helper(getContext());
        userMe = homeInteractor.getUserMe();
        Realm.init(getContext());
        rChatDb = Helper.getRealmInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_recycler, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        mySwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_lay);
        mySwipeRefreshLayout.setRefreshing(false);
        recyclerView.setEmptyView(view.findViewById(R.id.emptyView));
        recyclerView.setEmptyImageView(((ImageView) view.findViewById(R.id.emptyImage)));
        recyclerView.setEmptyTextView(((TextView) view.findViewById(R.id.emptyText)));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {

                    RealmQuery<Chat> query = rChatDb.where(Chat.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
                    resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time

                    chatDataList.clear();
                    chatDataList.addAll(rChatDb.copyFromRealm(resultList));
                    chatAdapter = new ChatAdapter(getActivity(), chatDataList, userMe.getId(), "chat");
                    recyclerView.setAdapter(chatAdapter);

                    resultList.addChangeListener(chatListChangeListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mySwipeRefreshLayout.setRefreshing(false);
                setUserNamesAsInPhone();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onStop() {
        super.onStop();
//        homeInteractor = null;
        if (resultList != null)
            resultList.removeChangeListener(chatListChangeListener);
    }

    //Display user's name as saved in phone!
    public void setUserNamesAsInPhone() {
        try {
            if (homeInteractor != null && chatDataList != null) {
                for (Chat chat : chatDataList) {
                    User user = chat.getUser();
                    if (user != null) {
                        if (helper.getCacheMyUsers() != null && helper.getCacheMyUsers().containsKey(user.getId())) {
                            user.setNameInPhone(helper.getCacheMyUsers().get(user.getId()).getNameToDisplay());
                        } else {
                            for (Contact savedContact : homeInteractor.getLocalContacts()) {
                                if (Helper.contactMatches(user.getId(), savedContact.getPhoneNumber())) {
                                    if (user.getNameInPhone() == null || !user.getNameInPhone().equals(savedContact.getName())) {
                                        user.setNameInPhone(savedContact.getName());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (chatAdapter != null)
                chatAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteSelectedChats() {

        final FragmentManager manager = getActivity().getSupportFragmentManager();
        Fragment frag = manager.findFragmentByTag(CONFIRM_TAG);
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }

        final ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment.newInstance("Delete chat",
                "Continue deleting selected chats?",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String chatID = "";
                        for (final Chat chat : chatDataList) {
                            if (chat.isSelected()) {
                                final String[] chatChild = {userMe.getId() + "-" + chat.getUserId()};
                                chatID = chat.getUserId();
                                final String finalChatID = chatID;
                                BaseApplication.getChatRef().addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (!dataSnapshot.hasChild(chatChild[0])) {
                                            chatChild[0] = chat.getUserId() + "-" + userMe.getId();
                                        }

                       /* BaseApplication.getChatRef().child(chatChild[0]).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.hasChild("chatDelete")) {
                                    if (!dataSnapshot.child("chatDelete").getValue().equals("")) {
                                        rChatDb.beginTransaction();
                                        BaseApplication.getChatRef().child(chatChild[0]).removeValue();
                                        Chat chatToDelete = rChatDb.where(Chat.class).equalTo("myId",
                                                userMe.getId()).equalTo("userId", chat.getUserId()).findFirst();
                                        if (chatToDelete != null) {
                                            RealmObject.deleteFromRealm(chatToDelete);
                                        }
                                        rChatDb.commitTransaction();
                                    }
                                } else {
                                    BaseApplication.getChatRef().child(chatChild[0]).child("chatDelete").setValue(userMe.getId());
                                    rChatDb.beginTransaction();
                                    Chat chatToDelete = rChatDb.where(Chat.class).equalTo("myId",
                                            userMe.getId()).equalTo("userId", chat.getUserId()).findFirst();
                                    if (chatToDelete != null) {
                                        RealmObject.deleteFromRealm(chatToDelete);
                                    }
                                    rChatDb.commitTransaction();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });*/
                                        // rChatDb.beginTransaction();
                                        /*BaseApplication.getChatRef().child(chatChild[0]).removeValue();
                                        Chat chatToDelete = rChatDb.where(Chat.class).equalTo("myId",
                                                userMe.getId()).equalTo("userId", chat.getUserId()).findFirst();
                                        if (chatToDelete != null) {
                                            RealmObject.deleteFromRealm(chatToDelete);
                                        }*/
                                        try {
                                            final Chat chatSoloIds = rChatDb.where(Chat.class)
                                                    .equalTo("myId", userMe.getId()).findFirst();

// BaseApplication.getChatRef().child(chatChild[0]).removeValue();
                                            Chat chatToDelete = rChatDb.where(Chat.class).equalTo("myId",
                                                    userMe.getId()).equalTo("userId", chat.getUserId()).findFirst();
                                            if (chatToDelete.getMessages().size() > 0) {
                                                timeStamp = chatToDelete.getMessages()
                                                        .get(chatToDelete.getMessages().size() - 1).getDate();
                                            }
                                            rChatDb.executeTransaction(new Realm.Transaction() {
                                                @Override
                                                public void execute(Realm realm) {
                                                    ArrayList<solochat> userIds = new ArrayList<>();

                                                    if (chatToDelete.getUser().getSolochat().size() > 0)
                                                        userIds.addAll(chatToDelete.getUser().getSolochat());

                                                    if (userIds.size() > 0) {
                                                        for (int i = 0; i < userIds.size(); i++) {
                                                            if (userIds.get(i).getPhoneNo().equalsIgnoreCase(finalChatID)) {
                                                                userIds.get(i).setTimeStamp(timeStamp);
                                                            } else if (!userIds.toString().contains(finalChatID)) {
                                                                solochat solochat = new solochat();
                                                                solochat.setTimeStamp(timeStamp);
                                                                solochat.setPhoneNo(finalChatID);
                                                                userIds.add(solochat);
                                                            }
                                                        }
                                                    } else {
                                                        solochat solochat = new solochat();
                                                        solochat.setTimeStamp(timeStamp);
                                                        solochat.setPhoneNo(finalChatID);
                                                        userIds.add(solochat);
                                                    }


                                                    BaseApplication.getUserRef().child(userMe.getId()).child("solochat")
                                                            .setValue(userIds);
                                                    if (chatToDelete != null) {
                                                        RealmObject.deleteFromRealm(chatToDelete);
                                                    }
                                                }
                                            });

                                            //   rChatDb.commitTransaction();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        }

                        try {
                            RealmQuery<Chat> query = rChatDb.where(Chat.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
                            resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time

                            chatDataList.clear();
                            chatDataList.addAll(rChatDb.copyFromRealm(resultList));
                            chatAdapter = new ChatAdapter(getActivity(), chatDataList, userMe.getId(), "chat");
                            recyclerView.setAdapter(chatAdapter);

                            resultList.addChangeListener(chatListChangeListener);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        setUserNamesAsInPhone();
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
    }


    public void disableContextualMode() {
        chatAdapter.disableContextualMode();
    }


    @Override
    public void onResume() {
        super.onResume();
        try {
            RealmQuery<Chat> query = rChatDb.where(Chat.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
            resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time

            chatDataList.clear();
            chatDataList.addAll(rChatDb.copyFromRealm(resultList));
            chatAdapter = new ChatAdapter(getActivity(), chatDataList, userMe.getId(), "chat");
            recyclerView.setAdapter(chatAdapter);

            resultList.addChangeListener(chatListChangeListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setUserNamesAsInPhone();
    }
}

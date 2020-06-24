package com.dreams.chat.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dreams.chat.R;
import com.dreams.chat.activities.MainActivity;
import com.dreams.chat.adapters.LogCallAdapter;
import com.dreams.chat.interfaces.HomeIneractor;
import com.dreams.chat.models.Contact;
import com.dreams.chat.models.LogCall;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.Helper;
import com.dreams.chat.views.MyRecyclerView;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MyCallsFragment extends Fragment {
    private MyRecyclerView recyclerView, missedRecyclerView;
    private LogCallAdapter chatAdapter;
    private LogCallAdapter missedCallAdapter;
    // private SwipeRefreshLayout mySwipeRefreshLayout;
    private Realm rChatDb;
    private User userMe;
    private RealmResults<LogCall> resultList;
    private ArrayList<LogCall> callDataList = new ArrayList<>();
    private ArrayList<LogCall> logCallDataList = new ArrayList<>();
    private ArrayList<LogCall> missedCallDataList = new ArrayList<>();
    private LinearLayout emptyView;
    private TextView missedText;
    private TextView otherCallText;
    private ImageView emptyImage;
    private TextView emptyText;
    private NestedScrollView nestedScrollView;
    private FragmentManager manager;
    private Helper helper;

    private RealmChangeListener<RealmResults<LogCall>> chatListChangeListener = new RealmChangeListener<RealmResults<LogCall>>() {
        @Override
        public void onChange(RealmResults<LogCall> element) {
            if (element != null && element.isValid() && element.size() > 0) {

                callDataList.clear();
                callDataList.addAll(rChatDb.copyFromRealm(element));

                for (int i = 0; i < callDataList.size(); i++) {
                    if (callDataList.get(i).getStatus().equalsIgnoreCase("CANCELED") ||
                            callDataList.get(i).getStatus().equalsIgnoreCase("DENIED")) {
                        missedCallDataList.add(callDataList.get(i));
                    } else {
                        logCallDataList.add(callDataList.get(i));
                    }
                }
                setUserNamesAsInPhone();
            }
        }
    };
    private HomeIneractor homeInteractor;

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
    public void onStop() {
        super.onStop();
//        homeInteractor = null;
       /* if (resultList != null)
            resultList.removeChangeListener(chatListChangeListener);*/
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        helper = new Helper(getContext());
        userMe = homeInteractor.getUserMe();
        Realm.init(getContext());
        rChatDb = Helper.getRealmInstance();
        manager = getChildFragmentManager();
        Fragment frag = manager.findFragmentByTag("DELETE_TAG");
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        missedRecyclerView = view.findViewById(R.id.missedRecyclerView);
        missedText = view.findViewById(R.id.missedText);
        otherCallText = view.findViewById(R.id.otherCallText);
        emptyView = view.findViewById(R.id.emptyView);
        emptyImage = view.findViewById(R.id.emptyImage);
        nestedScrollView = view.findViewById(R.id.scroll);
        emptyText = view.findViewById(R.id.emptyText);
        emptyImage.setBackgroundResource(R.drawable.ic_call_green_24dp);
        emptyText.setText(getString(R.string.empty_log_call_list));
      /*  mySwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_lay);
        mySwipeRefreshLayout.setRefreshing(false);*/
    /*    recyclerView.setEmptyView(view.findViewById(R.id.emptyView));
        recyclerView.setEmptyImageView(((ImageView) view.findViewById(R.id.emptyImage)));
        recyclerView.setEmptyTextView(((TextView) view.findViewById(R.id.emptyText)));
        recyclerView.setEmptyImage(R.drawable.ic_call_green_24dp);
        recyclerView.setEmptyText(getString(R.string.empty_log_call_list));*/

        recyclerView.setNestedScrollingEnabled(false);
        missedRecyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        missedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


      /*  mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                try {
                    RealmQuery<LogCall> query = rChatDb.where(LogCall.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
                    resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time

                    logCallDataList.clear();
                    missedCallDataList.clear();
                    callDataList.clear();
                    callDataList.addAll(rChatDb.copyFromRealm(resultList));

                    for (int i = 0; i < callDataList.size(); i++) {
                        if (callDataList.get(i).getStatus().equalsIgnoreCase("CANCELED") ||
                                callDataList.get(i).getStatus().equalsIgnoreCase("DENIED")) {
                            missedCallDataList.add(callDataList.get(i));
                        } else {
                            logCallDataList.add(callDataList.get(i));
                        }
                    }
                    chatAdapter = new LogCallAdapter(getActivity(), logCallDataList);
                    recyclerView.setAdapter(chatAdapter);
                    missedCallAdapter = new LogCallAdapter(getActivity(), missedCallDataList);
                    missedRecyclerView.setAdapter(missedCallAdapter);

                    resultList.addChangeListener(chatListChangeListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setUserNamesAsInPhone();
                mySwipeRefreshLayout.setRefreshing(false);
            }
        });*/
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
            RealmQuery<LogCall> query = rChatDb.where(LogCall.class).equalTo("myId", userMe.getId());//Query from chats whose owner is logged in user
            resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();//ignore forward list of messages and get rest sorted according to time

            logCallDataList.clear();
            missedCallDataList.clear();
            callDataList.clear();
            callDataList.addAll(rChatDb.copyFromRealm(resultList));

            for (int i = 0; i < callDataList.size(); i++) {
                if (callDataList.get(i).getStatus().equalsIgnoreCase("CANCELED") ||
                        callDataList.get(i).getStatus().equalsIgnoreCase("DENIED")) {
                    missedCallDataList.add(callDataList.get(i));
                } else {
                    logCallDataList.add(callDataList.get(i));
                }
            }

            chatAdapter = new LogCallAdapter(getActivity(), logCallDataList, MainActivity.myUsers,
                    helper.getLoggedInUser(), manager, helper);
            recyclerView.setAdapter(chatAdapter);
            missedCallAdapter = new LogCallAdapter(getActivity(), missedCallDataList,
                    MainActivity.myUsers, helper.getLoggedInUser(), manager, helper);
            missedRecyclerView.setAdapter(missedCallAdapter);

            resultList.addChangeListener(chatListChangeListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setUserNamesAsInPhone();
    }

    public void setUserNamesAsInPhone() {
        try {
            ArrayList<LogCall> tempList = new ArrayList<>();
            tempList.addAll(logCallDataList);
            tempList.addAll(missedCallDataList);
            if (homeInteractor != null && tempList != null) {
                for (LogCall logCall : tempList) {
                    User user = logCall.getUser();
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

            if (missedCallDataList.size() > 0) {
                missedText.setVisibility(View.VISIBLE);
            } else
                missedText.setVisibility(View.GONE);

            if (logCallDataList.size() > 0) {
                otherCallText.setVisibility(View.VISIBLE);
            } else
                otherCallText.setVisibility(View.GONE);

            if (missedCallDataList.size() == 0 && logCallDataList.size() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                nestedScrollView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                nestedScrollView.setVisibility(View.VISIBLE);
            }

            if (chatAdapter != null)
                chatAdapter.notifyDataSetChanged();

            if (missedCallAdapter != null)
                missedCallAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            try {
                RealmQuery<LogCall> query = rChatDb.where(LogCall.class).equalTo("myId", userMe.getId());
                resultList = query.isNotNull("user").sort("timeUpdated", Sort.DESCENDING).findAll();

                logCallDataList.clear();
                missedCallDataList.clear();
                callDataList.clear();
                callDataList.addAll(rChatDb.copyFromRealm(resultList));

                for (int i = 0; i < callDataList.size(); i++) {
                    if (callDataList.get(i).getStatus().equalsIgnoreCase("CANCELED") ||
                            callDataList.get(i).getStatus().equalsIgnoreCase("DENIED")) {
                        missedCallDataList.add(callDataList.get(i));
                    } else {
                        logCallDataList.add(callDataList.get(i));
                    }
                }

                chatAdapter = new LogCallAdapter(getActivity(), logCallDataList, MainActivity.myUsers,
                        helper.getLoggedInUser(), manager, helper);
                recyclerView.setAdapter(chatAdapter);
                missedCallAdapter = new LogCallAdapter(getActivity(), missedCallDataList,
                        MainActivity.myUsers, helper.getLoggedInUser(), manager, helper);
                missedRecyclerView.setAdapter(missedCallAdapter);

                resultList.addChangeListener(chatListChangeListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            setUserNamesAsInPhone();
        }
    }
}

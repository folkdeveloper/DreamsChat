package com.dreams.chat.adapters;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreams.chat.R;
import com.dreams.chat.activities.CallScreenActivity;
import com.dreams.chat.activities.MainActivity;
import com.dreams.chat.interfaces.OnUserGroupItemClick;
import com.dreams.chat.models.LogCall;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.Helper;
import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

public class LogCallAdapter extends RecyclerView.Adapter<LogCallAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<LogCall> dataList;
    private OnUserGroupItemClick itemClickListener;
    private ArrayList<User> myUsers;
    private User userMe, user;
    private FragmentManager manager;
    private Helper helper;
    protected String[] permissionsSinch = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_PHONE_STATE};

    public LogCallAdapter(Context context, ArrayList<LogCall> dataList, ArrayList<User> myUsers,
                          User loggedInUser, FragmentManager manager, Helper helper) {
        this.context = context;
        this.dataList = dataList;
        this.userMe = loggedInUser;
        this.myUsers = myUsers;
        this.manager = manager;
        this.helper = helper;
        if (context instanceof OnUserGroupItemClick) {
            this.itemClickListener = (OnUserGroupItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_log_call, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.setData(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView userImage, aCallLogImg, callTypeImg;
        private TextView time, duration, userName;

        MyViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userImage);
            time = itemView.findViewById(R.id.time);
            duration = itemView.findViewById(R.id.duration);
            userName = itemView.findViewById(R.id.userName);
            aCallLogImg = itemView.findViewById(R.id.img_calllog);
            callTypeImg = itemView.findViewById(R.id.callTypeImg);

           /* itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
                    if (pos != -1) {
                        itemClickListener.OnUserClick(dataList.get(pos).getUser(), pos, userImage);
                    }
                }
            });*/
        }

        public void setData(final LogCall logCall) {
//            Glide.with(context).load(logCall.getUser().getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_placeholder)).into(userImage);
            if (myUsers.size() != 0) {
                for (int i = 0; i < myUsers.size(); i++) {
                    if (myUsers.get(i).getId().equalsIgnoreCase(logCall.getUserId()))
                        user = myUsers.get(i);
                }
//            Glide.with(context).load(logCall.getUser().getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_placeholder)).into(userImage);
                if (user.getImage() != null && !user.getImage().isEmpty())
                    if (user.getBlockedUsersIds() != null && !user.getBlockedUsersIds().contains(MainActivity.userId))
                        Picasso.get()
                                .load(user.getImage())
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage);
                    else
                        Picasso.get()
                                .load(R.drawable.ic_avatar)
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage);
                else
                    Picasso.get()
                            .load(R.drawable.ic_avatar)
                            .tag(this)
                            .error(R.drawable.ic_avatar)
                            .placeholder(R.drawable.ic_avatar)
                            .into(userImage);
            }

            userName.setText(logCall.getUser().getNameToDisplay());
            time.setText(Helper.getDateTime(logCall.getTimeUpdated()));
//            time.setCompoundDrawablesWithIntrinsicBounds(logCall.getStatus().equals("CANCELED") ? R.drawable.ic_call_missed_24dp : logCall.getStatus().equals("DENIED") || logCall.getStatus().equals("IN") ? R.drawable.ic_call_received_24dp : logCall.getStatus().equals("OUT") ? R.drawable.ic_call_made_24dp : 0, 0, 0, 0);
            if (logCall.getStatus().equalsIgnoreCase("CANCELED")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_missed_24dp));
            } else if (logCall.getStatus().equalsIgnoreCase("DENIED")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_call_missed_24dp));
            } else if (logCall.getStatus().equalsIgnoreCase("IN")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_in));
            } else if (logCall.getStatus().equalsIgnoreCase("OUT")) {
                aCallLogImg.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_out));
            }

            duration.setText(formatTimespan(logCall.getTimeDuration()));

            if (logCall.isVideo()) {
                callTypeImg.setBackgroundResource(R.drawable.ic_videocam_white_24dp);
            } else {
                callTypeImg.setBackgroundResource(R.drawable.ic_call_white_24dp);
            }

            callTypeImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (logCall.isVideo()) {
                        makeCall(true, logCall.getUserId(), user);
                    } else {
                        makeCall(false, logCall.getUserId(), user);
                    }
                }
            });
        }

        private String formatTimespan(int totalSeconds) {
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private void makeCall(boolean b, String userId, User user) {
        if (user != null && userMe != null && userMe.getBlockedUsersIds() != null
                && userMe.getBlockedUsersIds().contains(user.getId())) {
            Helper.unBlockAlert(user.getNameToDisplay(), userMe, context,
                    helper, user.getId(), manager);
        } else
            placeCall(b, userId);
    }

    private void placeCall(boolean isVideoCall, String userId) {
        if (permissionsAvailable(permissionsSinch)) {
            try {
                Call call = isVideoCall ? ((MainActivity) context).getSinchRef().callUserVideo(userId)
                        : ((MainActivity) context).getSinchRef().callUser(userId);
                if (call == null) {
                    // Service failed for some reason, show a Toast and abort
                    Toast.makeText(context, "Service is not started. Try stopping the service and starting it again before placing a call.", Toast.LENGTH_LONG).show();
                    return;
                }
                String callId = call.getCallId();

                for (User user : MainActivity.myUsers) {
                    if (user != null && user.getId() != null && user.getId().equalsIgnoreCase(userId)) {
                        context.startActivity(CallScreenActivity.newIntent(context, user, callId, "OUT"));
                    }
                }

            } catch (Exception e) {
                Log.e("CHECK", e.getMessage());
                //ActivityCompat.requestPermissions(this, new String[]{e.getRequiredPermission()}, 0);
            }
        } else {
            ActivityCompat.requestPermissions((Activity) context, permissionsSinch, 69);
        }
    }

    protected boolean permissionsAvailable(String[] permissions) {
        boolean granted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }
        return granted;
    }
}

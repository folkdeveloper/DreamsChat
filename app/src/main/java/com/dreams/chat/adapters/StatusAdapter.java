package com.dreams.chat.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreams.chat.R;
import com.dreams.chat.fragments.MyStatusFragment;
import com.dreams.chat.interfaces.ContextualModeInteractor;
import com.dreams.chat.interfaces.OnUserGroupItemClick;
import com.dreams.chat.models.Chat;
import com.dreams.chat.models.Group;
import com.dreams.chat.models.Status;
import com.dreams.chat.models.StatusImage;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.Helper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.realm.RealmList;
import io.realm.RealmResults;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<Status> dataList;
    private OnUserGroupItemClick itemClickListener;
    private ContextualModeInteractor contextualModeInteractor;
    private int selectedCount = 0;
    MyStatusFragment myStatusFragment;
    boolean isCacheEnabled = true;
    boolean isImmersiveEnabled = true;
    boolean isTextEnabled = false;
    long storyDuration = 3000L;


    private final String[] resources = new String[]{
            "https://firebasestorage.googleapis.com/v0/b/firebase-satya.appspot.com/o/images%2Fi00001.jpg?alt=media&token=460667e4-e084-4dc5-b873-eefa028cec32",
    };

    public StatusAdapter(Context context, ArrayList<Status> dataList, MyStatusFragment aStatusFragment) {
        this.context = context;
        this.dataList = dataList;
        this.myStatusFragment = aStatusFragment;
        if (context instanceof OnUserGroupItemClick) {
            this.itemClickListener = (OnUserGroupItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }

        if (context instanceof ContextualModeInteractor) {
            this.contextualModeInteractor = (ContextualModeInteractor) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ContextualModeInteractor");
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {
        holder.setData(dataList.get(position));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myStatusFragment.navigateStatusStories(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView status, name, lastMessage, time;
        private ImageView image, myUserImageOnline;
        private RelativeLayout user_details_container;

        MyViewHolder(View itemView) {
            super(itemView);
            status = itemView.findViewById(R.id.emotion);
            name = itemView.findViewById(R.id.user_name);
            time = itemView.findViewById(R.id.time);
            lastMessage = itemView.findViewById(R.id.message);
            image = itemView.findViewById(R.id.user_image);
            user_details_container = itemView.findViewById(R.id.user_details_container);
            myUserImageOnline = itemView.findViewById(R.id.user_image_online);
        }

        private void setData(Status chat) {
            User chatUser = chat.getUser();
            Group chatGroup = chat.getGroup();
            if (chatUser != null && chatUser.getImage() != null && !chatUser.getImage().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chatUser.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .into(image);

            } else if (chatGroup != null && chatGroup.getImage() != null && !chatGroup.getImage().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chatGroup.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .into(image);
            }

            name.setText(chatUser != null ? chatUser.getNameToDisplay() : chatGroup.getName());
            name.setCompoundDrawablesWithIntrinsicBounds(0, 0, !chat.isRead() ? R.drawable.ring_blue : 0, 0);
            status.setText(chatUser != null ? chatUser.getStatus() : chatGroup.getStatus());
            time.setText(Helper.getTimeAgo(chat.getTimeUpdated(), context));
            lastMessage.setText(chat.getLastMessage());
            lastMessage.setTextColor(ContextCompat.getColor(context, !chat.isRead() ? R.color.textColorPrimary : R.color.textColorSecondary));

            user_details_container.setBackgroundColor(ContextCompat.getColor(context, (chat.isSelected() ? R.color.bg_gray : R.color.colorIcon)));

            try {
                if (chatUser != null && chatUser.isOnline()) {
                    myUserImageOnline.setVisibility(View.VISIBLE);
                    lastMessage.setCompoundDrawablesWithIntrinsicBounds(0, 0, chatUser.isOnline() ? R.drawable.ring_green : 0, 0);
                } else {
                    myUserImageOnline.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

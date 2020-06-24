package com.dreams.chat.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreams.chat.R;
import com.dreams.chat.interfaces.OnUserGroupItemClick;
import com.dreams.chat.models.AttachmentTypes;
import com.dreams.chat.models.Chat;
import com.dreams.chat.models.Group;
import com.dreams.chat.models.Message;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.Helper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.realm.RealmList;

public class SearchChatAdapter extends RecyclerView.Adapter<SearchChatAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Chat> nameChatDataList;
    private String userId, from;
    private OnUserGroupItemClick itemClickListener;


    public SearchChatAdapter(Context context, ArrayList<Chat> nameChatDataList, String userId, String from) {
        this.context = context;
        this.nameChatDataList = nameChatDataList;
        this.userId = userId;
        this.from = from;

        if (context instanceof OnUserGroupItemClick) {
            this.itemClickListener = (OnUserGroupItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_user, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.setData(nameChatDataList.get(i));
    }

    @Override
    public int getItemCount() {
        return nameChatDataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name, lastMessage, time;
        private ImageView image, myUserImageOnline, img;
        private RelativeLayout user_details_container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.user_name);
            time = itemView.findViewById(R.id.time);
            lastMessage = itemView.findViewById(R.id.message);
            image = itemView.findViewById(R.id.user_image);
            img = itemView.findViewById(R.id.img);
            user_details_container = itemView.findViewById(R.id.user_details_container);
            myUserImageOnline = itemView.findViewById(R.id.user_image_online);

            user_details_container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != -1) {
                        Chat chat = nameChatDataList.get(pos);
                        if (chat.getUser() != null)
                            itemClickListener.OnUserClick(chat.getUser(), pos, image);
                        else if (chat.getGroup() != null)
                            itemClickListener.OnGroupClick(chat.getGroup(), pos, image);
                    }
                }
            });
        }

        private void setData(final Chat chat) {
            final User chatUser = chat.getUser();
            final Group chatGroup = chat.getGroup();
            RealmList<Message> message = chat.getMessages();
            if (chatUser != null && chatUser.getImage() != null && !chatUser.getImage().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chatUser.getImage())
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(image);
            } else if (chatGroup != null && chatGroup.getImage() != null && !chatGroup.getImage().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chatGroup.getImage())
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(image);
            } else {
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(image);
            }


            name.setText(chatUser != null ? chatUser.getNameToDisplay() : chatGroup.getName());
            time.setText(Helper.getChatFormattedDate(chat.getTimeUpdated()));
            if (chatUser != null) {
                lastMessage.setText(chat.getLastMessage());
            } else if (chatGroup != null) {
                if (message.size() > 0) {
                    lastMessage.setText(chat.getLastMessage());
                } else {
                    try {
                        lastMessage.setText("Created on " + Helper.getDateTime(chatGroup.getDate()));
                    } catch (Exception e) {
                        lastMessage.setText("");
                    }
                }
            }

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

            try {
                if (message != null && message.size() > 0) {
                    if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.AUDIO) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                        lastMessage.setText(context.getString(R.string.audio));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.RECORDING) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                        lastMessage.setText(context.getString(R.string.recording));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.VIDEO) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_videocam_gray);
                        lastMessage.setText(context.getString(R.string.video));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.IMAGE) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_wallpaper_gray);
                        lastMessage.setText(context.getString(R.string.image));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.CONTACT) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_contact_gray);
                        lastMessage.setText(context.getString(R.string.contact));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.LOCATION) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_location_gray);
                        lastMessage.setText(context.getString(R.string.location));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.DOCUMENT) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_insert_gray);
                        lastMessage.setText(context.getString(R.string.document));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.NONE_TEXT) {
                        img.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


}

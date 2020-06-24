package com.dreams.chat.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreams.chat.R;
import com.dreams.chat.activities.ChatActivity;
import com.dreams.chat.interfaces.OnUserGroupItemClick;
import com.dreams.chat.models.AttachmentTypes;
import com.dreams.chat.models.Message;
import com.dreams.chat.models.SearchMessageModel;
import com.dreams.chat.utils.Helper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class SearchMessageAdapter extends RecyclerView.Adapter<SearchMessageAdapter.ViewHolder> {

    private Context context;
    private ArrayList<SearchMessageModel> messageChatDataList;
    private String userId, from;
    private OnUserGroupItemClick itemClickListener;


    public SearchMessageAdapter(Context context, ArrayList<SearchMessageModel> messageChatDataList, String userId, String from) {
        this.context = context;
        this.messageChatDataList = messageChatDataList;
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
        viewHolder.setData(messageChatDataList.get(i));
    }

    @Override
    public int getItemCount() {
        return messageChatDataList.size();
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

                    Intent intent;
                 /*   if (pos != -1) {
                        SearchMessageModel chat = messageChatDataList.get(pos);
                        if (chat.getUser() != null)
                            intent = ChatActivity.fromSearch(context, new ArrayList<Message>(), chat.getUser(), chat.getMessageId());
                        else
                            intent = ChatActivity.newIntent(context, new ArrayList<Message>(), chat.getGroup());
                        context.startActivity(intent);
                    }*/
                }
            });
        }

        private void setData(final SearchMessageModel chat) {
            if (chat != null && chat.getProfileImg() != null && !chat.getProfileImg().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chat.getProfileImg())
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


            name.setText(chat.getName());
            time.setText(Helper.getChatFormattedDate(chat.getTime()));
            lastMessage.setText(chat.getLastMessage());

            try {
                if (chat.getAttachmentType() == AttachmentTypes.AUDIO) {
                    img.setVisibility(View.VISIBLE);
                    img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                    lastMessage.setText(context.getString(R.string.audio));
                } else if (chat.getAttachmentType() == AttachmentTypes.RECORDING) {
                    img.setVisibility(View.VISIBLE);
                    img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                    lastMessage.setText(context.getString(R.string.recording));
                } else if (chat.getAttachmentType() == AttachmentTypes.VIDEO) {
                    img.setVisibility(View.VISIBLE);
                    img.setBackgroundResource(R.drawable.ic_videocam_gray);
                    lastMessage.setText(context.getString(R.string.video));
                } else if (chat.getAttachmentType() == AttachmentTypes.IMAGE) {
                    img.setVisibility(View.VISIBLE);
                    img.setBackgroundResource(R.drawable.ic_wallpaper_gray);
                    lastMessage.setText(context.getString(R.string.image));
                } else if (chat.getAttachmentType() == AttachmentTypes.CONTACT) {
                    img.setVisibility(View.VISIBLE);
                    img.setBackgroundResource(R.drawable.ic_contact_gray);
                    lastMessage.setText(context.getString(R.string.contact));
                } else if (chat.getAttachmentType() == AttachmentTypes.LOCATION) {
                    img.setVisibility(View.VISIBLE);
                    img.setBackgroundResource(R.drawable.ic_location_gray);
                    lastMessage.setText(context.getString(R.string.location));
                } else if (chat.getAttachmentType() == AttachmentTypes.DOCUMENT) {
                    img.setVisibility(View.VISIBLE);
                    img.setBackgroundResource(R.drawable.ic_insert_gray);
                    lastMessage.setText(context.getString(R.string.document));
                } else if (chat.getAttachmentType() == AttachmentTypes.NONE_TEXT) {
                    img.setVisibility(View.GONE);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


}

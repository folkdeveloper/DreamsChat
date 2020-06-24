package com.dreams.chat.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreams.chat.R;
import com.dreams.chat.interfaces.ContextualModeInteractor;
import com.dreams.chat.interfaces.OnUserGroupItemClick;
import com.dreams.chat.models.AttachmentTypes;
import com.dreams.chat.models.Chat;
import com.dreams.chat.models.Group;
import com.dreams.chat.models.Message;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.Helper;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import io.realm.RealmList;

/**
 * Created by a_man on 5/10/2017.
 */

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<Chat> dataList;
    private OnUserGroupItemClick itemClickListener;
    private ContextualModeInteractor contextualModeInteractor;
    private int selectedCount = 0;
    private String userId, from;
    private ImageView groupImg1, groupImg2, groupImg3, groupImg4;
    private TextView groupImgCount;
    protected Helper helper;
    private HashMap<String, User> myUsersNameInPhoneMap;

    public ChatAdapter(Context context, ArrayList<Chat> dataList, String userId, String from) {
        this.context = context;
        this.dataList = dataList;
        this.userId = userId;
        this.from = from;
        this.helper = new Helper(context);
        this.myUsersNameInPhoneMap = helper.getCacheMyUsers();
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
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.setData(dataList.get(position));
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView status, name, lastMessage, time, msgCount, msgCountGroup;
        private ImageView image, myUserImageOnline, img, readImg;
        private RelativeLayout user_details_container;


        MyViewHolder(View itemView) {
            super(itemView);
            status = itemView.findViewById(R.id.emotion);
            name = itemView.findViewById(R.id.user_name);
            time = itemView.findViewById(R.id.time);
            readImg = itemView.findViewById(R.id.readImg);
            lastMessage = itemView.findViewById(R.id.message);
            image = itemView.findViewById(R.id.user_image);
            msgCount = itemView.findViewById(R.id.msgCount);
            msgCountGroup = itemView.findViewById(R.id.msgCountGroup);
            img = itemView.findViewById(R.id.img);
            user_details_container = itemView.findViewById(R.id.user_details_container);
            myUserImageOnline = itemView.findViewById(R.id.user_image_online);

            groupImg1 = itemView.findViewById(R.id.groupImg1);
            groupImg2 = itemView.findViewById(R.id.groupImg2);
            groupImg3 = itemView.findViewById(R.id.groupImg3);
            groupImg4 = itemView.findViewById(R.id.groupImg4);
            groupImgCount = itemView.findViewById(R.id.groupImgCount);

            user_details_container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (contextualModeInteractor.isContextualMode()) {
                        toggleSelection(dataList.get(getAdapterPosition()), getAdapterPosition());
                    } else {
                        int pos = getAdapterPosition();
                        if (pos != -1) {
                            Chat chat = dataList.get(pos);
                            if (chat.getUser() != null)
                                itemClickListener.OnUserClick(chat.getUser(), pos, image);
                            else if (chat.getGroup() != null)
                                itemClickListener.OnGroupClick(chat.getGroup(), pos, image);
                        }
                    }
                }
            });
            user_details_container.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    contextualModeInteractor.enableContextualMode();
                    toggleSelection(dataList.get(getAdapterPosition()), getAdapterPosition());
                    return true;
                }
            });
        }

        private void showDialog(final String image, String profileName) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View dialogView = li.inflate(R.layout.dialog_image, null);
            ImageView profileImg = dialogView.findViewById(R.id.profileImg);
            TextView name = dialogView.findViewById(R.id.name);
            name.setText(profileName);
            if (!image.isEmpty()) {
                Picasso.get()
                        .load(image)
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .networkPolicy(NetworkPolicy.OFFLINE)
                        .into(profileImg);

               /* BitmapDrawable drawable = (BitmapDrawable) profileImg.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                float imageWidthInPX = (float)profileImg.getWidth();
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(Math.round(imageWidthInPX),
                        Math.round(imageWidthInPX * (float)bitmap.getHeight() / (float)bitmap.getWidth()));
                profileImg.setLayoutParams(layoutParams);*/
            } else {
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(profileImg);
            }


            builder.setView(dialogView).create().show();
        }

        private void setData(final Chat chat) {
            final User chatUser = chat.getUser();
            final Group chatGroup = chat.getGroup();
            RealmList<Message> message = chat.getMessages();
//            Glide.with(context).load(chatUser != null ? chatUser.getImage() : chatGroup.getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_placeholder)).into(image);

            if (chatUser != null && chatUser.getImage() != null && !chatUser.getImage().equalsIgnoreCase("")) {
                if (chatUser.getBlockedUsersIds() != null && !chatUser.getBlockedUsersIds().contains(userId))
                    Picasso.get()
                            .load(chatUser.getImage())
                            .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                            .centerInside()
                            .into(image);
                else
                    Picasso.get()
                            .load(R.drawable.ic_avatar)
                            .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                            .centerInside()
                            .into(image);

            } else if (chatGroup != null && chatGroup.getImage() != null &&
                    !chatGroup.getImage().equalsIgnoreCase("")) {
                /*Picasso.get()
                        .load(chatGroup.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .into(image);*/
                Picasso.get()
                        .load(chatGroup.getImage())
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(image);
            } else {
             /*   Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(image);*/
                Picasso.get()
                        .load(R.drawable.ic_avatar)
                        .resizeDimen(R.dimen._40sdp, R.dimen._40sdp)
                        .centerInside()
                        .into(image);
            }

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String profileName = name.getText().toString();
                    if (chatUser != null && chatUser.getImage() != null && !chatUser.getImage().equalsIgnoreCase("")) {
                        if (chatUser.getBlockedUsersIds() != null && !chatUser.getBlockedUsersIds().contains(userId))
                            showDialog(chatUser.getImage(), profileName);
                    } else if (chatGroup != null && chatGroup.getImage() != null && !chatGroup.getImage().equalsIgnoreCase("")) {
                        showDialog(chatGroup.getImage(), profileName);
                    } /*else {
                        showDialog(v, "", profileName);
                    }*/
                }
            });

            name.setText(chatUser != null ? chatUser.getNameToDisplay() : chatGroup.getName());
            if (from.equalsIgnoreCase("group")
                    && !chat.isRead()
                    && chatGroup.getUserIds().contains(userId)
                    && !chatGroup.getGrpExitUserIds().contains(userId)) {
                msgCountGroup.setVisibility(View.VISIBLE);
            } else {
                msgCountGroup.setVisibility(View.GONE);
            }
            //name.setCompoundDrawablesWithIntrinsicBounds(0, 0, !chat.isRead() ? R.drawable.ring_blue : 0, 0);
            status.setText(chatUser != null ? chatUser.getStatus() : chatGroup.getStatus());
            //time.setText(Helper.getTimeAgo(chat.getTimeUpdated(), context));
            if (chat.getTimeUpdated() == 0) {
                time.setVisibility(View.GONE);
            } else {
                time.setVisibility(View.VISIBLE);
                time.setText(Helper.getChatFormattedDate(chat.getTimeUpdated()));
            }
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

            //      user_details_container.setBackgroundColor(ContextCompat.getColor(context, (chat.isSelected() ? R.color.bg_gray : R.color.colorIcon)));

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
                    if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.AUDIO
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                        lastMessage.setText(context.getString(R.string.audio));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.RECORDING
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_audiotrack_gray);
                        lastMessage.setText(context.getString(R.string.recording));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.VIDEO
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_videocam_gray);
                        lastMessage.setText(context.getString(R.string.video));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.IMAGE
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_wallpaper_gray);
                        lastMessage.setText(context.getString(R.string.image));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.CONTACT
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_contact_gray);
                        lastMessage.setText(context.getString(R.string.contact));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.LOCATION
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_location_gray);
                        lastMessage.setText(context.getString(R.string.location));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.DOCUMENT
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.VISIBLE);
                        img.setBackgroundResource(R.drawable.ic_insert_gray);
                        lastMessage.setText(context.getString(R.string.document));
                    } else if (message != null && message.get(message.size() - 1).getAttachmentType() == AttachmentTypes.NONE_TEXT
                            && message.get(message.size() - 1).getDelete().isEmpty()) {
                        img.setVisibility(View.GONE);
                    }
                    try {
                        if (!from.equalsIgnoreCase("group")) {
                            if (!message.get(message.size() - 1).getRecipientId().equalsIgnoreCase(userId)
                                    && message.get(message.size() - 1).getDelete().isEmpty()) {
                                readImg.setVisibility(View.VISIBLE);
                                readImg.setBackgroundResource(message.get(message.size() - 1).isSent()
                                        ? (message.get(message.size() - 1).isDelivered()
                                        ? (message.get(message.size() - 1).isReadMsg()
                                        ? R.drawable.ic_done_all_blue : R.drawable.ic_done_all_black)
                                        : R.drawable.ic_done_black) : R.drawable.ic_waiting);
                            } else {
                                readImg.setVisibility(View.GONE);
                            }
                            groupImg1.setVisibility(View.GONE);
                            groupImg2.setVisibility(View.GONE);
                            groupImg3.setVisibility(View.GONE);
                            groupImg4.setVisibility(View.GONE);
                            groupImgCount.setVisibility(View.GONE);
                        } else {
                            readImg.setVisibility(View.GONE);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (chatUser != null) {
                        msgCountGroup.setVisibility(View.GONE);
                        int count = 0;
                        for (int i = 0; i < message.size(); i++) {
                            if (!message.get(i).isReadMsg() && !message.get(i).getSenderId().equalsIgnoreCase(userId)
                                    && !message.get(i).isBlocked())
                                count++;
                        }

                        if (count > 99) {
                            msgCount.setVisibility(View.VISIBLE);
                            msgCount.setText("+99");
                        } else if (count > 0) {
                            msgCount.setVisibility(View.VISIBLE);
                            msgCount.setText("" + count);
                        } else {
                            msgCount.setVisibility(View.GONE);
                        }
                    }
                }
                if (from.equalsIgnoreCase("group") && chatGroup != null) {
                    loadImages(chatGroup);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void loadImages(Group chatGroup) {
        RealmList<String> userIds = new RealmList<>();

        for (String ids : chatGroup.getUserIds()) {
            if (!chatGroup.getGrpExitUserIds().contains(ids))
                userIds.add(ids);
        }
        if (userIds.size() > 4) {
            groupImg1.setVisibility(View.VISIBLE);
            groupImg2.setVisibility(View.VISIBLE);
            groupImg3.setVisibility(View.VISIBLE);
            groupImg4.setVisibility(View.VISIBLE);
            groupImgCount.setVisibility(View.VISIBLE);
            groupImgCount.setText(String.valueOf(userIds.size()));

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg1);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg2);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(2)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(2)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg3);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(3)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(3)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg4);
        } else if (userIds.size() == 4) {
            groupImg1.setVisibility(View.VISIBLE);
            groupImg2.setVisibility(View.VISIBLE);
            groupImg3.setVisibility(View.VISIBLE);
            groupImg4.setVisibility(View.VISIBLE);
            groupImgCount.setVisibility(View.GONE);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg1);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg2);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(2)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(2)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg3);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(3)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(3)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg4);
        } else if (userIds.size() == 3) {
            groupImg1.setVisibility(View.VISIBLE);
            groupImg2.setVisibility(View.VISIBLE);
            groupImg3.setVisibility(View.VISIBLE);
            groupImg4.setVisibility(View.GONE);
            groupImgCount.setVisibility(View.GONE);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg1);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg2);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(2)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(2)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg3);
        } else if (userIds.size() == 2) {
            groupImg1.setVisibility(View.VISIBLE);
            groupImg2.setVisibility(View.VISIBLE);
            groupImg3.setVisibility(View.GONE);
            groupImg4.setVisibility(View.GONE);
            groupImgCount.setVisibility(View.GONE);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg1);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(1)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(1)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg2);

        } else if (userIds.size() == 1) {
            groupImg1.setVisibility(View.VISIBLE);
            groupImg2.setVisibility(View.GONE);
            groupImg3.setVisibility(View.GONE);
            groupImg4.setVisibility(View.GONE);
            groupImgCount.setVisibility(View.GONE);

            Picasso.get()
                    .load(myUsersNameInPhoneMap.get(userIds.get(0)).getImage().isEmpty() ? "a"
                            : (myUsersNameInPhoneMap.get(userIds.get(0)).getImage()))
                    .placeholder(R.drawable.ic_avatar)
                    .error(R.drawable.ic_avatar)
                    .into(groupImg1);
        }
    }

    private void toggleSelection(Chat chat, int position) {
        chat.setSelected(!chat.isSelected());
        notifyItemChanged(position);

        if (chat.isSelected())
            selectedCount++;
        else
            selectedCount--;

        contextualModeInteractor.updateSelectedCount(selectedCount);
    }

    public void disableContextualMode() {
        selectedCount = 0;
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).isSelected()) {
                dataList.get(i).setSelected(false);
                notifyItemChanged(i);
            }
        }
    }

}

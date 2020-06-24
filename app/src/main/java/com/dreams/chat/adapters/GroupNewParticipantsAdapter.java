package com.dreams.chat.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dreams.chat.BaseApplication;
import com.dreams.chat.R;
import com.dreams.chat.fragments.ChatDetailFragment;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.Helper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by a_man on 31-12-2017.
 */

public class GroupNewParticipantsAdapter extends RecyclerView.Adapter<GroupNewParticipantsAdapter.MyViewHolder> {
    private User userMe;
    private GroupNewParticipantsAdapter groupNewParticipantsAdapter;
    private Context context;
    private ArrayList<User> dataList;
    private boolean staggered;
    private ParticipantClickListener participantClickListener;
    private String groupId, groupAdmin;
    ChatDetailFragment chatDetailFragment;
    Helper helper;

    public GroupNewParticipantsAdapter(Fragment fragment, ArrayList<User> selectedUsers, User userMe) {
        this.context = fragment.getActivity();
        this.dataList = selectedUsers;
        this.staggered = true;
        this.userMe = userMe;
        helper = new Helper(context);
    }

    public GroupNewParticipantsAdapter(Fragment fragment, ArrayList<User> selectedUsers,
                                       GroupNewParticipantsAdapter groupNewParticipantsAdapter, User userMe) {
        if (fragment instanceof ParticipantClickListener) {
            this.participantClickListener = (ParticipantClickListener) fragment;
        } else {
            throw new RuntimeException(fragment.toString() + " must implement ParticipantClickListener");
        }

        this.context = fragment.getActivity();
        this.dataList = selectedUsers;
        this.staggered = false;
        this.groupNewParticipantsAdapter = groupNewParticipantsAdapter;
        this.userMe = userMe;
        helper = new Helper(context);
    }

    public GroupNewParticipantsAdapter(Fragment fragment, ArrayList<User> selectedUsers, boolean staggered, User userMe) {
        this.context = fragment.getActivity();
        this.dataList = selectedUsers;
        this.staggered = staggered;
        this.userMe = userMe;
        helper = new Helper(context);
    }

    public GroupNewParticipantsAdapter(Fragment fragment, ChatDetailFragment chatDetailFragment,
                                       ArrayList<User> selectedUsers, User userMe, String groupId, String groupAdmin) {
        if (fragment instanceof ParticipantClickListener) {
            this.participantClickListener = (ParticipantClickListener) fragment;
        } else {
            throw new RuntimeException(fragment.toString() + " must implement ParticipantClickListener");
        }

        this.context = fragment.getActivity();
        this.dataList = selectedUsers;
        this.staggered = false;
        this.userMe = userMe;
        this.groupId = groupId;
        this.groupAdmin = groupAdmin;
        this.chatDetailFragment = chatDetailFragment;
        helper = new Helper(context);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(staggered ? R.layout.item_selected_user
                : R.layout.item_menu_user, parent, false));
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
        private TextView userName, adminLabel, status;

        private AppCompatRadioButton userSelected;
        private ImageView userImage, removeUser;

        public MyViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            status = itemView.findViewById(R.id.status);
            if (!staggered) {
                userImage = itemView.findViewById(R.id.user_image);
                userSelected = itemView.findViewById(R.id.userSelected);
                if (userMe != null) {
                    removeUser = itemView.findViewById(R.id.removeUser);
                    adminLabel = itemView.findViewById(R.id.adminLabel);
                    removeUser.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int pos = getAdapterPosition();
                            participantClickListener.onParticipantClick(pos, dataList.get(pos));
                        }
                    });
                }
                if (groupNewParticipantsAdapter != null) {
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int pos = getAdapterPosition();
                            User user = dataList.get(pos);
                            userMe = helper.getLoggedInUser();
                            if (userMe.getBlockedUsersIds() != null
                                    && !userMe.getBlockedUsersIds().contains(user.getId())) {
                                user.setSelected(!user.isSelected());
                                notifyItemChanged(pos);

                                int index = groupNewParticipantsAdapter.getDataList().indexOf(user);
                                if (index == -1) {
                                    groupNewParticipantsAdapter.getDataList().add(user);
                                    groupNewParticipantsAdapter.notifyItemInserted(groupNewParticipantsAdapter.getDataList().size() - 1);
                                } else {
                                    groupNewParticipantsAdapter.getDataList().remove(index);
                                    groupNewParticipantsAdapter.notifyItemRemoved(index);
                                }
                            } else {
                                showDialog(user.getNameToDisplay(), user.getId(), pos);
                            }
                            /*for (int i = 0; i < groupNewParticipantsAdapter.getDataList().size(); i++) {
                                if (groupNewParticipantsAdapter.getDataList().get(i).getImage() != null
                                        && !groupNewParticipantsAdapter.getDataList().get(i).getImage().isEmpty()) {
                                    Picasso.get()
                                            .load(groupNewParticipantsAdapter.getDataList().get(i).getImage())
                                            .tag(this)
                                            .error(R.drawable.ic_logo_)
                                            .placeholder(R.drawable.ic_logo_)
                                            .into(userImage);
                                    notifyItemChanged(i);
                                } else {
                                    userImage.setBackgroundResource(R.drawable.ic_logo_);
                                    notifyItemChanged(i);
                                }
                            }*/
                        }


                    });
                }
            }
        }

        public void setData(User user) {
            userName.setText(user.getNameToDisplay());
            if (!staggered) {
                if (groupNewParticipantsAdapter != null) {
                    userSelected.setVisibility(View.VISIBLE);
                    userSelected.setChecked(user.isSelected());
                }
//                Glide.with(context).load(user.getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_placeholder)).into(userImage);
                if (user.getImage() != null && !user.getImage().isEmpty())
                    if (user.getBlockedUsersIds() != null && !user.getBlockedUsersIds().contains(userMe.getId())) {
                        Picasso.get()
                                .load(user.getImage())
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage);

                    } else {
                        Picasso.get()
                                .load(R.drawable.ic_avatar)
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage);

                    }
                else
                    Picasso.get()
                            .load(R.drawable.ic_placeholder)
                            .tag(this)
                            .error(R.drawable.ic_placeholder)
                            .placeholder(R.drawable.ic_placeholder)
                            .into(userImage);


            }

            userMe = helper.getLoggedInUser();
            if (userMe.getBlockedUsersIds() != null && userMe.getBlockedUsersIds().contains(user.getId())) {
                if (status != null)
                    status.setVisibility(View.VISIBLE);
            } else {
                if (status != null)
                    status.setVisibility(View.GONE);
            }

            if (groupId != null && groupId.startsWith(Helper.GROUP_PREFIX)) {
                if (chatDetailFragment.group.getAdmin() != null && chatDetailFragment.group.getAdmin().equalsIgnoreCase(user.getId())) {
                    adminLabel.setVisibility(View.VISIBLE);
                }else {
                    adminLabel.setVisibility(View.INVISIBLE);
                }

                if (chatDetailFragment.group.getAdmin() != null && chatDetailFragment.group.getAdmin().equalsIgnoreCase(userMe.getId())) {
                    removeUser.setVisibility(View.VISIBLE);
                } else if (userMe.getId().equalsIgnoreCase(user.getId())) {
                    removeUser.setVisibility(View.VISIBLE);
                } else {
                    removeUser.setVisibility(View.GONE);
                }

            } else {
                if (removeUser != null)
                    removeUser.setVisibility((userMe.getId().equals(user.getId()) || userMe.getId().equals(dataList.get(0).getId()))
                            ? View.VISIBLE : View.GONE);
            }


//            if (groupId != null && groupId.startsWith(Helper.GROUP_PREFIX)) {
//                if (groupId.split(Helper.GROUP_PREFIX)[1].split("_")[1].equalsIgnoreCase(userMe.getId())) {
//                    if (removeUser != null)
//                        removeUser.setVisibility(View.VISIBLE);
//                } else if (userMe.getId().equals(user.getId())) {
//                    if (removeUser != null)
//                        removeUser.setVisibility(View.VISIBLE);
//                }
//
//                if (groupId.split(Helper.GROUP_PREFIX)[1].split("_")[1].equalsIgnoreCase(user.getId())) {
//                    if (adminLabel != null && removeUser != null) {
//                        adminLabel.setVisibility(View.VISIBLE);
//                        removeUser.setVisibility(View.GONE);
//                    } else
//                        adminLabel.setVisibility(View.GONE);
//                }
//            } else {
//                if (removeUser != null)
//                    removeUser.setVisibility((userMe.getId().equals(user.getId()) || userMe.getId().equals(dataList.get(0).getId())) ? View.VISIBLE : View.GONE);
//            }
        }
    }

    public ArrayList<User> getDataList() {
        return dataList;
    }

    public interface ParticipantClickListener {
        void onParticipantClick(int pos, User participant);
    }

   /* @Override
    public void onViewAttachedToWindow(final MyViewHolder holder) {
        if (holder instanceof MyViewHolder) {
            holder.setIsRecyclable(false);
        }
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(final MyViewHolder holder) {
        if (holder instanceof MyViewHolder) {
            holder.setIsRecyclable(true);
        }
        super.onViewDetachedFromWindow(holder);
    }*/

    private void showDialog(String name, String userId, int pos) {
        userMe = helper.getLoggedInUser();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_confirmation, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        ((TextView) view.findViewById(R.id.title)).setText("Unblock");
        ((TextView) view.findViewById(R.id.message)).setText(String.format("Are you sure want to unblock %s",
                name));
        view.findViewById(R.id.yes).setOnClickListener(new View.OnClickListener() {
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
                                notifyItemChanged(pos);
                                Toast.makeText(context, "Unblocked", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Unable to unblock user", Toast.LENGTH_LONG).show();
                            }
                        });
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

}

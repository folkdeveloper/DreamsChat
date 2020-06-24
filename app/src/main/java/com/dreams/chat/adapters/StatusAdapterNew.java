package com.dreams.chat.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreams.chat.R;
import com.dreams.chat.fragments.MyStatusFragmentNew;
import com.dreams.chat.interfaces.ContextualModeInteractor;
import com.dreams.chat.interfaces.OnUserGroupItemClick;
import com.dreams.chat.models.Group;
import com.dreams.chat.models.StatusImageNew;
import com.dreams.chat.models.StatusNew;
import com.dreams.chat.models.User;
import com.dreams.chat.utils.CircularStatusView;
import com.dreams.chat.utils.Helper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.realm.RealmList;

public class StatusAdapterNew extends RecyclerView.Adapter<StatusAdapterNew.MyViewHolder> {
    private Context context;
    private ArrayList<StatusNew> dataList;
    private OnUserGroupItemClick itemClickListener;
    private ContextualModeInteractor contextualModeInteractor;
    private int selectedCount = 0;
    MyStatusFragmentNew myStatusFragment;
    boolean isCacheEnabled = true;
    boolean isImmersiveEnabled = true;
    boolean isTextEnabled = false;
    long storyDuration = 3000L;


    private final String[] resources = new String[]{
            "https://firebasestorage.googleapis.com/v0/b/firebase-satya.appspot.com/o/images%2Fi00001.jpg?alt=media&token=460667e4-e084-4dc5-b873-eefa028cec32",
    };

    public StatusAdapterNew(Context context, ArrayList<StatusNew> dataList, MyStatusFragmentNew aStatusFragment) {
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
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.adapter_item_status, parent, false));
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
        private ImageView myUserImageOnline, statusImage;
        private CircularStatusView image;
        private RelativeLayout user_details_container;
      //  private RecyclerView statusImageRecycler;
       // private StatusItemAdapter adapter;

        MyViewHolder(View itemView) {
            super(itemView);
            status = itemView.findViewById(R.id.emotion);
            name = itemView.findViewById(R.id.user_name);
            time = itemView.findViewById(R.id.time);
            lastMessage = itemView.findViewById(R.id.message);
            image = itemView.findViewById(R.id.user_image);
            statusImage = itemView.findViewById(R.id.statusImage);
         //   statusImageRecycler = itemView.findViewById(R.id.statusImageRecycler);


            image.setPortionsColor(context.getResources().getColor(R.color.status_border));
            user_details_container = itemView.findViewById(R.id.user_details_container);
            myUserImageOnline = itemView.findViewById(R.id.user_image_online);
        }

        private void setData(StatusNew chat) {
            User chatUser = chat.getUser();
            Group chatGroup = chat.getGroup();
            RealmList<StatusImageNew> statusImagesList = chat.getStatusImages();

            for (int i = 0; i < statusImagesList.size(); i++) {
                image.setPortionsCount(statusImagesList.get(i).getAttachment().getUrlList().size());
                if (statusImagesList.get(i).getAttachment().getUrlList().size() > 0) {
                    Picasso.get()
                            .load(statusImagesList.get(i).getAttachment().getUrlList()
                                    .get(statusImagesList.get(i).getAttachment().getUrlList().size() - 1).getUrl())
                            .tag(this)
                            .placeholder(R.drawable.ic_avatar)
                            .into(statusImage);

                   /* LinearLayoutManager mLayoutManager = new LinearLayoutManager(context,
                            LinearLayoutManager.HORIZONTAL, false);
                    statusImageRecycler.setLayoutManager(mLayoutManager);

                    adapter = new StatusItemAdapter(context, statusImagesList.get(i).getAttachment().getUrlList(), chat);
                    statusImageRecycler.setAdapter(adapter);*/
                }
            }


           /* if (chatUser != null && chatUser.getImage() != null && !chatUser.getImage().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chatUser.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_logo_)
                        .into(image);

            } else if (chatGroup != null && chatGroup.getImage() != null && !chatGroup.getImage().equalsIgnoreCase("")) {
                Picasso.get()
                        .load(chatGroup.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_logo_)
                        .into(image);
            }*/

            name.setText(chatUser != null ? chatUser.getNameToDisplay() : chatGroup.getName());
            name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            //  name.setCompoundDrawablesWithIntrinsicBounds(0, 0, !chat.isRead() ? R.drawable.ring_blue : 0, 0);
            status.setText(chatUser != null ? chatUser.getStatus() : chatGroup.getStatus());
            time.setText(Helper.getFormattedDate(chat.getTimeUpdated()));
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

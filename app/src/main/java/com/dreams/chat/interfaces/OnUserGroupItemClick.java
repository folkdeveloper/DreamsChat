package com.dreams.chat.interfaces;

import android.view.View;

import com.dreams.chat.models.Group;
import com.dreams.chat.models.User;


public interface OnUserGroupItemClick {
    void OnUserClick(User user, int position, View userImage);
    void OnGroupClick(Group group, int position, View userImage);
}

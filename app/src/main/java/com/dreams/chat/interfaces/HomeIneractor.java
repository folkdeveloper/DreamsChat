package com.dreams.chat.interfaces;

import com.dreams.chat.models.Contact;
import com.dreams.chat.models.User;

import java.util.ArrayList;

/**
 * Created by a_man on 01-01-2018.
 */

public interface HomeIneractor {
    User getUserMe();

    ArrayList<Contact> getLocalContacts();

}

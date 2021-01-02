package com.wirelesscontenttransferexercise;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.ViewHolder> {

    private List<Contact> contactList;


    public ContactListAdapter(List<Contact> contactList) {
        this.contactList = contactList;
    }

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public  TextView nameTv, phoneTv;
        public ImageView imageIv;
        public ViewHolder(View view) {
            super(view);





            nameTv =  view.findViewById(R.id.name_tv);
            phoneTv =  view.findViewById(R.id.phone_tv);
            imageIv =  view.findViewById(R.id.image_iv);
        }


    }


    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.contact_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.setIsRecyclable(false);


        viewHolder.nameTv.setText(contactList.get(position).getName());
        viewHolder.phoneTv.setText(contactList.get(position).getPhoneNumber());
        viewHolder.imageIv.setImageBitmap(getPhotoBitmap(contactList.get(position).getImage()));

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return contactList.size();
    }
    public Bitmap getPhotoBitmap(String imageBase64) {
        byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

}

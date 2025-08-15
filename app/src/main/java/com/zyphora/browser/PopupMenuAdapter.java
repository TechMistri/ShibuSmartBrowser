package com.zyphora.browser;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class PopupMenuAdapter extends ArrayAdapter<MenuItem> {

    public PopupMenuAdapter(Context context, List<MenuItem> menuItems) {
        super(context, 0, menuItems);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the data item for this position
        MenuItem menuItem = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.popup_menu_item, parent, false);
        }

        // Lookup view for data population
        ImageView icon = convertView.findViewById(R.id.icon);
        TextView text = convertView.findViewById(R.id.text);

        // Populate the data into the template view using the data object
        icon.setImageResource(menuItem.getIcon());
        text.setText(menuItem.getText());

        // Return the completed view to render on screen
        return convertView;
    }
}

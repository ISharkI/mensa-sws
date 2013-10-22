package de.mmerg.mensa;

import java.util.List;

import de.mgbc.mensa.R;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MealAdapter extends ArrayAdapter<Meal> {

    private LayoutInflater mInflater;

    public MealAdapter(Context context, int textViewResourceId, List<Meal> meals) {
        super(context, textViewResourceId, meals);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;

        Meal meal = getItem(position);

        if (v == null) {
            v = mInflater.inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder();
            holder.divider = (View) v.findViewById(R.id.divider);
            holder.name = (TextView) v.findViewById(R.id.name);
            holder.price = (TextView) v.findViewById(R.id.price);
            holder.icon = (ImageView) v.findViewById(R.id.bio_icon);
            holder.typ = (TextView) v.findViewById(R.id.typ);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }

        holder.name.setText(meal.getName());
        holder.price.setText("Student  " + meal.getPriceStudent() + "0 €"
                + "     Gast  " + meal.getPriceGuest() + "0 €");

        holder.typ.setText(meal.getTyp());
        if (position != 0
                && meal.getTyp().equals(getItem(position - 1).getTyp())) {
            holder.typ.setVisibility(View.GONE);
        } else {
            holder.typ.setVisibility(View.VISIBLE);
        }

        if (meal.getTyp().equals("Renner")) {
            holder.typ.setTextColor(Color.WHITE);
            holder.typ.setBackgroundColor(Color.rgb(153, 0, 153));
            holder.divider.setBackgroundColor(Color.rgb(153, 0, 153));
        } else if (meal.getTyp().equals("Premium line")) {
            holder.typ.setTextColor(Color.WHITE);
            holder.typ.setBackgroundColor(Color.rgb(204, 153, 0));
            holder.divider.setBackgroundColor(Color.rgb(204, 153, 0));
        } else {
            holder.typ.setTextColor(Color.BLACK);
            holder.typ.setBackgroundColor(Color.rgb(204, 204, 204));
            holder.divider.setBackgroundColor(Color.rgb(204, 204, 204));
        }

        if (meal.isBio()) {
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setVisibility(View.GONE);
        }

        return v;

    }

    private class ViewHolder {
        TextView typ;
        TextView name;
        TextView price;
        ImageView icon;
        View divider;
    }
}

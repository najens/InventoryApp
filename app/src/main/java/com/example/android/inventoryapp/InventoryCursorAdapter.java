package com.example.android.inventoryapp;

/**
 * Created by Nate on 11/4/2017.
 */

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.data.InventoryContract;

import java.text.DecimalFormat;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of inventory data as its data source. This adapter knows
 * how to create list items for each row of inventory data in the {@link Cursor}.
 */
public class InventoryCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    /**
     * This method binds the inventory data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        // Find the columns of item attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry._ID);
        final int quantityColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY);
        int nameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME);
        int priceColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_PRICE);

        // Read the item attributes from the Cursor for the current item
        int id = cursor.getInt(idColumnIndex);
        final Uri currentItemUri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, id);
        int itemQuantity = cursor.getInt(quantityColumnIndex);
        String itemName = cursor.getString(nameColumnIndex);
        int itemPrice = cursor.getInt(priceColumnIndex);
        double price = itemPrice;
        price = price / 100;
        DecimalFormat df = new DecimalFormat("0.00");
        String priceString = df.format(price);

        // Update the TextViews with the attributes for the current item
        viewHolder.quantityTextView.setText(Integer.toString(itemQuantity));
        viewHolder.nameTextView.setText(itemName);
        viewHolder.priceTextView.setText("$" + priceString);
        viewHolder.saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantity = Integer.valueOf(viewHolder.quantityTextView.getText().toString());
                if (quantity > 0) {
                    quantity -= 1;
                }
                Log.v("LOG", "Quantity = " + quantity);

                ContentValues values = new ContentValues();
                values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY, quantity);


                ContentResolver resolver = view.getContext().getContentResolver();
                resolver.update(currentItemUri, values, null, null);
            }
        });
    }

    // Use a ViewHolder to get the views
    public class ViewHolder {
        public final TextView quantityTextView;
        public final TextView nameTextView;
        public final TextView priceTextView;
        public final Button saleButton;

        public ViewHolder(View view) {
            quantityTextView = (TextView) view.findViewById(R.id.quantity_text_view);
            nameTextView = (TextView) view.findViewById(R.id.name_text_view);
            priceTextView = (TextView) view.findViewById(R.id.price_text_view);
            saleButton = (Button) view.findViewById(R.id.sale_button);
        }
    }
}

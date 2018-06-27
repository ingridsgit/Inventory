package com.example.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventory.data.InventoryContract.Product;

import java.text.DecimalFormat;

public class ProductCursorAdapter extends CursorAdapter {

    ProductCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        TextView productNameView = view.findViewById(R.id.list_view_name);
        String productName = cursor.getString(cursor.getColumnIndex(Product.COLUMN_NAME));
        productNameView.setText(productName);

        final TextView quantityView = view.findViewById(R.id.list_view_quantity);
        final int quantity = cursor.getInt(cursor.getColumnIndex(Product.COLUMN_QUANTITY));
        quantityView.setText(R.string.quantity_available);
        quantityView.append(" ");
        quantityView.append(String.valueOf(quantity));
        final int id = cursor.getInt(cursor.getColumnIndex(Product.PRODUCT_ID));

        final Button saleButton = view.findViewById(R.id.sale_button);
        saleButton.setText(R.string.sale);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Uri productUri = ContentUris.withAppendedId(Product.PRODUCT_CONTENT_URI, id);
                if (quantity > 0) {
                    int newQuantity = quantity - 1;
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(Product.COLUMN_QUANTITY, newQuantity);
                    context.getContentResolver().update(productUri, contentValues, null, null);
                }
            }
        });

        TextView priceView = view.findViewById(R.id.list_view_price);
        double price = cursor.getDouble(cursor.getColumnIndex(Product.COLUMN_PRICE));
        priceView.setText(R.string.price);
        DecimalFormat priceFormat = new DecimalFormat("#.00");
        priceView.append(priceFormat.format(price));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }
}

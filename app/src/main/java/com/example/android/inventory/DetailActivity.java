package com.example.android.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.Product;
import com.example.android.inventory.data.InventoryContract.Supplier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER_ID = 0;
    private static final int SUPPLIER_LOADER_ID = 1;
    private static final String PRODUCT_PROJECTION[] = new String[]
            {Product.PRODUCT_ID,
                    Product.COLUMN_NAME,
                    Product.COLUMN_PRICE,
                    Product.COLUMN_QUANTITY,
                    Product.COLUMN_SUPPLIER,
                    Product.COLUMN_PICTURE};
    private static final String[] SUPPLIER_PROJECTION = new String[]
            {Supplier.SUPPLIER_ID, Supplier.COLUMN_NAME, Supplier.COLUMN_CONTACT};
    private static final int RESULT_LOAD_IMAGE = 1;
    private Uri currentProductUri;
    private EditText productNameField;
    private EditText quantityField;
    private EditText amountField;
    private EditText priceField;
    private Spinner supplierSpinner;
    private TextView supplierContactView;
    private ImageView pictureView;
    private SimpleCursorAdapter simpleCursorAdapter;
    private String supplierContact;
    private Uri selectedImageUri;
    private boolean productHasChanged;
    private final View.OnTouchListener clickListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            productHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        productNameField = findViewById(R.id.product_name_field);
        productNameField.setOnTouchListener(clickListener);

        pictureView = findViewById(R.id.picture_view);
        Button downloadPictureButton = findViewById(R.id.download_picture_button);
        downloadPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                productHasChanged = true;
                Intent downloadIntent;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    downloadIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    downloadIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    downloadIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                } else {
                    downloadIntent = new Intent(Intent.ACTION_GET_CONTENT);
                }
                downloadIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                downloadIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                downloadIntent.setType("image/*");
                startActivityForResult(downloadIntent, RESULT_LOAD_IMAGE);
            }
        });

        quantityField = findViewById(R.id.edit_quantity_field);
        quantityField.setOnTouchListener(clickListener);

        amountField = findViewById(R.id.enter_amount_field);

        Button decreaseButton = findViewById(R.id.decrease_button);
        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                productHasChanged = true;
                String amountEntered = amountField.getText().toString().trim();
                int amount = 1;
                if (!TextUtils.isEmpty(amountEntered)) {
                    try {
                        amount = Integer.valueOf(amountEntered);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Toast.makeText(DetailActivity.this, R.string.incorrect_amount, Toast.LENGTH_LONG).show();
                    }
                }
                String currentQuantityString = quantityField.getText().toString().trim();
                Integer currentQuantity;
                if (!currentQuantityString.isEmpty()) {
                    currentQuantity = Integer.valueOf(currentQuantityString);
                } else {
                    currentQuantity = 0;
                }
                int quantity = currentQuantity - amount;
                if (quantity > 0) {
                    quantityField.setText(String.valueOf(quantity));
                } else {
                    quantityField.setText("0");
                }
            }
        });

        Button increaseButton = findViewById(R.id.increase_button);
        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                productHasChanged = true;
                String amountEntered = amountField.getText().toString().trim();
                int amount = 1;
                if (!TextUtils.isEmpty(amountEntered)) {
                    try {
                        amount = Integer.valueOf(amountEntered);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Toast.makeText(DetailActivity.this, R.string.incorrect_amount, Toast.LENGTH_LONG).show();
                    }
                }
                String currentQuantityString = quantityField.getText().toString().trim();
                Integer currentQuantity;
                if (!TextUtils.isEmpty(currentQuantityString)) {
                    currentQuantity = Integer.valueOf(currentQuantityString);
                } else {
                    currentQuantity = 0;
                }
                int quantity = currentQuantity + amount;
                quantityField.setText(String.valueOf(quantity));
            }

        });

        priceField = findViewById(R.id.enter_price_field);
        priceField.setOnTouchListener(clickListener);

        setUpSpinner();
        Button editSupplierButton = findViewById(R.id.edit_supplier_button);
        editSupplierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editSupplier = new Intent(DetailActivity.this, SupplierActivity.class);
                Uri selectedSupplierUri = ContentUris.withAppendedId(Supplier.SUPPLIER_CONTENT_URI, supplierSpinner.getSelectedItemId());
                editSupplier.setData(selectedSupplierUri);
                startActivity(editSupplier);
            }
        });
        Button addSupplierButton = findViewById(R.id.add_a_supplier_button);
        addSupplierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addSupplier = new Intent(DetailActivity.this, SupplierActivity.class);
                startActivity(addSupplier);
            }
        });
        supplierContactView = findViewById(R.id.supplier_contact_view);

        Button orderButton = findViewById(R.id.order_button);
        orderButton.setText(R.string.action_order);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Intent order = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + supplierContact));
                    startActivity(order);

                } catch (IllegalArgumentException e) {
                    Toast.makeText(DetailActivity.this, R.string.invalid_email, Toast.LENGTH_LONG).show();
                }

            }
        });

        Intent intent = getIntent();
        currentProductUri = intent.getData();
        if (currentProductUri == null) {
            setTitle(R.string.add_product);
        } else {
            setTitle(R.string.update_product);
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER_ID, null, this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap pictureBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                pictureView.setImageBitmap(pictureBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                pictureView.setImageResource(R.drawable.ic_insert_photo);
                Toast.makeText(DetailActivity.this, R.string.error_loading, Toast.LENGTH_LONG).show();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(selectedImageUri, takeFlags);
            }
        }
    }

    private void setUpSpinner() {
        getLoaderManager().initLoader(SUPPLIER_LOADER_ID, null, this);
        supplierSpinner = findViewById(R.id.spinner);
        supplierSpinner.setOnTouchListener(clickListener);
        simpleCursorAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1,
                null, new String[]{Supplier.COLUMN_NAME},
                new int[]{android.R.id.text1}, 0);
        supplierSpinner.setAdapter(simpleCursorAdapter);

        supplierSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    supplierContact = cursor.getString(cursor.getColumnIndex(Supplier.COLUMN_CONTACT));
                    supplierContactView.setText(supplierContact);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private int getItemPosition(Spinner spinner, String supplierName) {
        int spinnerCount = spinner.getCount();
        int i;
        for (i = 0; i < spinnerCount; i++) {
            Cursor line = (Cursor) spinner.getItemAtPosition(i);
            if (line != null) {
                String matchingSupplier = line.getString(line.getColumnIndex(Supplier.COLUMN_NAME));
                if (supplierName.equalsIgnoreCase(matchingSupplier)) {
                    return i;
                }
            }
        }
        return i;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentProductUri == null) {
            getMenuInflater().inflate(R.menu.menu_save, menu);
            return true;
        } else {
            getMenuInflater().inflate(R.menu.menu_detail, menu);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.delete_message);
                builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int deletedRows = getContentResolver().delete(currentProductUri, null, null);
                        if (deletedRows == 1) {
                            Toast.makeText(DetailActivity.this, R.string.delete_successful, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(DetailActivity.this, R.string.delete_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                break;
            case R.id.action_save:
                String productName = productNameField.getText().toString().trim();
                String quantityString = quantityField.getText().toString().trim();
                int quantityAvailable = 0;
                try {
                    if (!TextUtils.isEmpty(quantityString)) {
                        quantityAvailable = Integer.parseInt(quantityString);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(DetailActivity.this, R.string.invalid_number, Toast.LENGTH_LONG).show();
                    break;
                }
                String priceString = priceField.getText().toString().trim();
                double price = 0;
                try {
                    if (!TextUtils.isEmpty(priceString)) {
                        price = Double.parseDouble(priceString);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(DetailActivity.this, R.string.invalid_number, Toast.LENGTH_LONG).show();
                    break;
                }

                Cursor cursor = (Cursor) supplierSpinner.getSelectedItem();
                String supplier = null;
                if (cursor != null) {
                    supplier = cursor.getString(cursor.getColumnIndex(Supplier.COLUMN_NAME));
                }

                if (productName.isEmpty() &&
                        selectedImageUri == null &&
                        quantityString.isEmpty() &&
                        priceString.isEmpty()) {
                    finish();
                }
                ContentValues contentValues = new ContentValues();

                if (selectedImageUri != null) {
                    String selectedImageUriString = selectedImageUri.toString();
                    contentValues.put(Product.COLUMN_PICTURE, selectedImageUriString);
                }

                contentValues.put(Product.COLUMN_NAME, productName);
                contentValues.put(Product.COLUMN_QUANTITY, quantityAvailable);
                contentValues.put(Product.COLUMN_PRICE, price);
                contentValues.put(Product.COLUMN_SUPPLIER, supplier);
                try {
                    if (currentProductUri == null) {
                        Uri addedProductUri = getContentResolver().insert(Product.PRODUCT_CONTENT_URI, contentValues);
                        Toast.makeText(DetailActivity.this, getString(R.string.product_saved) + " " + addedProductUri, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        int updatedProduct = getContentResolver().update(currentProductUri, contentValues, null, null);
                        if (updatedProduct == 1) {
                            Toast.makeText(DetailActivity.this, R.string.product_updated, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(DetailActivity.this, R.string.error_updating, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Toast.makeText(DetailActivity.this, R.string.incorrect_entries, Toast.LENGTH_LONG).show();
                }
                break;
            case android.R.id.home:
                if (!productHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }
                showUnsavedChangesDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    }
                });
                break;
        }
        return true;
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_message);
        builder.setPositiveButton(R.string.quit, discardButtonClickListener);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public void onBackPressed() {
        if (!productHasChanged) {
            super.onBackPressed();
            return;
        }
        showUnsavedChangesDialog(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case SUPPLIER_LOADER_ID:
                return new CursorLoader(this, Supplier.SUPPLIER_CONTENT_URI, SUPPLIER_PROJECTION, null, null, null);
            case EXISTING_PRODUCT_LOADER_ID:
                return new CursorLoader(this, currentProductUri, PRODUCT_PROJECTION, null, null, null);
            default:
                throw new IllegalArgumentException("Unknown Cursor Loader");
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case SUPPLIER_LOADER_ID:
                simpleCursorAdapter.swapCursor(data);
                break;
            case EXISTING_PRODUCT_LOADER_ID:
                if (data.moveToFirst()) {
                    String name = data.getString(data.getColumnIndex(Product.COLUMN_NAME));
                    String pictureUriString = data.getString(data.getColumnIndex(Product.COLUMN_PICTURE));
                    final Uri pictureUri = Uri.parse(pictureUriString);
                    pictureView.post(new Runnable() {
                        @Override
                        public void run() {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            int viewWidth = pictureView.getWidth();
                            int viewHeight = pictureView.getHeight();
                            int pictureWidth = options.outWidth;
                            int pictureHeight = options.outHeight;
                            int scaleFactor = Math.min(pictureWidth / viewWidth, pictureHeight / viewHeight);
                            options.inJustDecodeBounds = false;
                            options.inSampleSize = scaleFactor;
                            Bitmap bitmap = null;
                            try {
                                bitmap = BitmapFactory.decodeStream(getBaseContext().getContentResolver().openInputStream(pictureUri), null, options);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                pictureView.setImageResource(R.drawable.ic_insert_photo);
                                Toast.makeText(DetailActivity.this, R.string.file_not_found, Toast.LENGTH_LONG).show();
                            }
                            pictureView.setImageBitmap(bitmap);
                        }
                    });

                    int quantity = data.getInt(data.getColumnIndex(Product.COLUMN_QUANTITY));
                    double price = data.getDouble(data.getColumnIndex(Product.COLUMN_PRICE));
                    final String supplierName = data.getString(data.getColumnIndex(Product.COLUMN_SUPPLIER));

                    productNameField.setText(name);
                    quantityField.setText(String.valueOf(quantity));
                    DecimalFormat priceFormat = new DecimalFormat("#.00");
                    priceField.setText(priceFormat.format(price));
                    supplierSpinner.post(new Runnable() {
                        @Override
                        public void run() {
                            int supplierPosition = getItemPosition(supplierSpinner, supplierName);
                            supplierSpinner.setSelection(supplierPosition);
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case SUPPLIER_LOADER_ID:
                simpleCursorAdapter.swapCursor(null);
                break;
            case EXISTING_PRODUCT_LOADER_ID:
                productNameField.setText("");
                pictureView.setImageResource(R.drawable.ic_insert_photo);
                quantityField.setText("");
                amountField.setText("");
                priceField.setText("");
                break;
        }
    }

}

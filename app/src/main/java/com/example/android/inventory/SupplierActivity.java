package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.Supplier;

public class SupplierActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int EXISTING_SUPPLIER_LOADER_ID = 3;
    private static final String[] EDIT_SUPPLIER_PROJECTION = new String[]{
            Supplier.SUPPLIER_ID, Supplier.COLUMN_NAME, Supplier.COLUMN_CONTACT};
    private Uri currentSupplierUri;
    private EditText supplierNameField;
    private EditText supplierContactField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier);

        Intent intent = getIntent();
        currentSupplierUri = intent.getData();
        if (currentSupplierUri == null) {
            setTitle(R.string.action_add_supplier);
        } else {
            setTitle(R.string.edit_supplier);
            getLoaderManager().initLoader(EXISTING_SUPPLIER_LOADER_ID, null, this);
        }

        supplierNameField = findViewById(R.id.supplier_name_field);
        supplierContactField = findViewById(R.id.supplier_contact_field);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currentSupplierUri == null) {
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
                int deletedRows = getContentResolver().delete(currentSupplierUri, null, null);
                if (deletedRows == 1) {
                    Toast.makeText(this, R.string.delete_successful, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.action_save:
                String nameString = supplierNameField.getText().toString().trim();
                String contactString = supplierContactField.getText().toString().trim();
                if (TextUtils.isEmpty(nameString) && TextUtils.isEmpty(contactString)) {
                    finish();
                    return true;
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put(Supplier.COLUMN_NAME, nameString);
                contentValues.put(Supplier.COLUMN_CONTACT, contactString);
                try {
                    if (currentSupplierUri == null) {
                        Uri addedSupplierUri = getContentResolver().insert(Supplier.SUPPLIER_CONTENT_URI, contentValues);
                        if (addedSupplierUri == null) {
                            Toast.makeText(this, R.string.error_saving, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(this, R.string.supplier_saved, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        int updatedRows = getContentResolver().update(currentSupplierUri, contentValues, null, null);
                        if (updatedRows != 1) {
                            Toast.makeText(this, R.string.error_saving, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(this, R.string.supplier_saved, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, R.string.incorrect_entries, Toast.LENGTH_LONG).show();
                }
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, currentSupplierUri, EDIT_SUPPLIER_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            supplierNameField.setText(data.getString(data.getColumnIndex(Supplier.COLUMN_NAME)));
            supplierContactField.setText(data.getString(data.getColumnIndex(Supplier.COLUMN_CONTACT)));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        supplierNameField.setText("");
        supplierContactField.setText("");

    }
}

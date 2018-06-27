package com.example.android.inventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.inventory.data.InventoryContract.Product;
import com.example.android.inventory.data.InventoryContract.Supplier;

public class InventoryContentProvider extends ContentProvider {

    private static final String LOG_TAG = InventoryContentProvider.class.getSimpleName();
    private static final int SUPPLIER = 100;
    private static final int SUPPLIER_ID = 101;
    private static final int PRODUCT = 200;
    private static final int PRODUCT_ID = 201;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_SUPPLIER, SUPPLIER);
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_SUPPLIER + "/#", SUPPLIER_ID);
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCT, PRODUCT);
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCT + "/#", PRODUCT_ID);
    }

    private InventoryOpenHelper inventoryOpenHelper;

    @Override
    public boolean onCreate() {
        inventoryOpenHelper = new InventoryOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = inventoryOpenHelper.getReadableDatabase();
        Cursor cursor;
        final int match = uriMatcher.match(uri);
        switch (match) {
            case SUPPLIER:
                cursor = database.query(Supplier.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case SUPPLIER_ID:
                selection = Supplier.SUPPLIER_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(Supplier.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCT:
                cursor = database.query(Product.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCT_ID:
                selection = Product.PRODUCT_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(Product.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query, unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = uriMatcher.match(uri);
        switch (match) {
            case SUPPLIER:
                return Supplier.CONTENT_LIST_TYPE;
            case SUPPLIER_ID:
                return Supplier.CONTENT_ITEM_TYPE;
            case PRODUCT:
                return Product.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return Product.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        ContentValues contentValues = new ContentValues(values);
        final int match = uriMatcher.match(uri);
        SQLiteDatabase database = inventoryOpenHelper.getWritableDatabase();
        switch (match) {
            case SUPPLIER:
                return insertSupplier(database, uri, contentValues);
            case PRODUCT:
                return insertProduct(database, uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertSupplier(SQLiteDatabase database, Uri uri, ContentValues values) {
        String name = values.getAsString(Supplier.COLUMN_NAME);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Supplier requires a name");
        }
        String contact = values.getAsString(Supplier.COLUMN_CONTACT);
        if (contact == null || contact.isEmpty() || !contact.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }

        long insertedRowId = database.insert(Supplier.TABLE_NAME, null, values);
        if (insertedRowId == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, insertedRowId);
    }

    private Uri insertProduct(SQLiteDatabase database, Uri uri, ContentValues values) {
        String name = values.getAsString(Product.COLUMN_NAME);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Product requires a name");
        }
        Double price = values.getAsDouble(Product.COLUMN_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Price requires a positive value");
        }
        Integer quantity = values.getAsInteger(Product.COLUMN_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity requires a positive value");
        }
        String supplier = values.getAsString(Product.COLUMN_SUPPLIER);
        if (supplier == null || supplier.isEmpty() || !supplierIsValid(supplier)) {
            throw new IllegalArgumentException("Invalid supplier");
        }
        String picture = values.getAsString(Product.COLUMN_PICTURE);
        if (picture == null || picture.isEmpty()) {
            throw new IllegalArgumentException("Invalid picture");
        }

        long insertedRowId = database.insert(Product.TABLE_NAME, null, values);
        if (insertedRowId == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, insertedRowId);

    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = uriMatcher.match(uri);
        int deletedRows;
        SQLiteDatabase database = inventoryOpenHelper.getWritableDatabase();
        switch (match) {
            case SUPPLIER_ID:
                selection = Supplier.SUPPLIER_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                deletedRows = database.delete(Supplier.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                selection = Product.PRODUCT_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                deletedRows = database.delete(Product.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Invalid Uri for deletion");
        }
        if (deletedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return deletedRows;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        ContentValues contentValues = new ContentValues(values);
        SQLiteDatabase database = inventoryOpenHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        int updatedRows;
        switch (match) {
            case SUPPLIER:
                updatedRows = updateSupplier(database, uri, contentValues, selection, selectionArgs);
                break;
            case SUPPLIER_ID:
                selection = Supplier.SUPPLIER_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                updatedRows = updateSupplier(database, uri, contentValues, selection, selectionArgs);
                break;
            case PRODUCT:
                updatedRows = updateProduct(database, uri, contentValues, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                selection = Product.PRODUCT_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                updatedRows = updateProduct(database, uri, contentValues, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        return updatedRows;
    }

    private int updateSupplier(SQLiteDatabase database, Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(Supplier.COLUMN_NAME)) {
            String name = values.getAsString(Supplier.COLUMN_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Supplier requires a name");
            }
        }
        if (values.containsKey(Supplier.COLUMN_CONTACT)) {
            String contact = values.getAsString(Supplier.COLUMN_CONTACT);
            if (contact == null || !contact.contains("@")) {
                throw new IllegalArgumentException("Invalid email address");
            }
        }

        if (values.size() == 0) {
            return 0;
        }

        int updatedRows = database.update(Supplier.TABLE_NAME, values, selection, selectionArgs);
        if (updatedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updatedRows;
    }

    private int updateProduct(SQLiteDatabase database, Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(Product.COLUMN_NAME)) {
            String name = values.getAsString(Product.COLUMN_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }
        if (values.containsKey(Product.COLUMN_PRICE)) {
            Double price = values.getAsDouble(Product.COLUMN_PRICE);
            if (price == null || price < 0) {
                throw new IllegalArgumentException("Price requires a positive value");
            }
        }
        if (values.containsKey(Product.COLUMN_QUANTITY)) {
            Integer quantity = values.getAsInteger(Product.COLUMN_QUANTITY);
            if (quantity == null || quantity < 0) {
                throw new IllegalArgumentException("Quantity requires a positive value");
            }
        }
        if (values.containsKey(Product.COLUMN_SUPPLIER)) {
            String supplier = values.getAsString(Product.COLUMN_SUPPLIER);
            if (supplier == null || !supplierIsValid(supplier)) {
                throw new IllegalArgumentException("Invalid supplier");
            }
        }
        if (values.containsKey(Product.COLUMN_PICTURE)) {
            String picture = values.getAsString(Product.COLUMN_PICTURE);
            if (picture == null || picture.isEmpty()) {
                throw new IllegalArgumentException("Invalid picture");
            }
        }

        if (values.size() == 0) {
            return 0;
        }
        int updatedRows = database.update(Product.TABLE_NAME, values, selection, selectionArgs);
        if (updatedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updatedRows;
    }

    private boolean supplierIsValid(String supplier) {
        SQLiteDatabase database = inventoryOpenHelper.getReadableDatabase();
        String[] projection = new String[]{Supplier.SUPPLIER_ID, Supplier.COLUMN_NAME};
        String[] selectionArgs = new String[]{supplier};
        Cursor cursor = database.query(Supplier.TABLE_NAME, projection, Supplier.COLUMN_NAME + "= ?", selectionArgs, null, null, null);
        boolean isValid = cursor.moveToFirst();
        cursor.close();
        return isValid;
    }


}

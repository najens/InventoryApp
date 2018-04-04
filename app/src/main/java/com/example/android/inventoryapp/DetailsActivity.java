package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
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
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailsActivity.class.getSimpleName();

    private static final int EXISTING_INVENTORY_LOADER = 0;
    private static final int PICK_IMAGE_REQUEST = 0;
    private static final String STATE_URI = "STATE_URI";

    private Uri mCurrentItemUri;
    private Uri mImageUri;

    private EditText mItemNameEditText;
    private EditText mItemPriceEditText;
    private EditText mItemSupplierEditText;
    private EditText mSupplierEmailEditText;
    private EditText mItemQuantityEditText;
    private EditText mAdjustmentValueEditText;

    private View mParentLayout;
    private View mIdContainer;
    private View mQuantityAdjustmentContainer;
    private View mEditImageContainer;

    private ImageView mItemImageView;

    private TextView mItemNameTextView;
    private TextView mItemIdTextView;
    private TextView mItemPriceTextView;
    private TextView mItemSupplierTextView;
    private TextView mSupplierEmailTextView;
    private TextView mItemQuantityTextView;

    private String stringUri;

    private double mPrice;

    private int mPriceInt;
    private int mQuantity;

    private boolean mItemHasChanged = false;
    private boolean edit = false;

    public static boolean isValidEmail(CharSequence target) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private Button mSaveOrderButton;
    private Button mIncrementButton;
    private Button mDecrementButton;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mItemHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Find all relevant views that we will need
        mItemNameEditText = (EditText) findViewById(R.id.item_name_edit_text);
        mItemPriceEditText = (EditText) findViewById(R.id.item_price_edit_text);
        mItemSupplierEditText = (EditText) findViewById(R.id.item_supplier_edit_text);
        mSupplierEmailEditText = (EditText) findViewById(R.id.supplier_email_edit_text);
        mItemQuantityEditText = (EditText) findViewById(R.id.item_quantity_edit_text);
        mAdjustmentValueEditText = (EditText) findViewById(R.id.adjustment_value_edit_text);

        mParentLayout = findViewById(R.id.parent_layout);
        mIdContainer = findViewById(R.id.id_container);
        mEditImageContainer = findViewById(R.id.edit_image_container);
        mQuantityAdjustmentContainer = findViewById(R.id.quantity_adjustment_container);

        mItemImageView = (ImageView) findViewById(R.id.item_image_view);

        mItemNameTextView = (TextView) findViewById(R.id.item_name_text_view);
        mItemIdTextView = (TextView) findViewById(R.id.item_id_text_view);
        mItemPriceTextView = (TextView) findViewById(R.id.item_price_text_view);
        mItemSupplierTextView = (TextView) findViewById(R.id.item_supplier_text_view);
        mSupplierEmailTextView = (TextView) findViewById(R.id.supplier_email_text_view);
        mItemQuantityTextView = (TextView) findViewById(R.id.item_quantity_text_view);

        mSaveOrderButton = (Button) findViewById(R.id.save_order_button);
        mIncrementButton = (Button) findViewById(R.id.increment_button);
        mDecrementButton = (Button) findViewById(R.id.decrement_button);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new item or editing an existing one.
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // Set an onTouch Listener on the parent layout and exit keyboard when screen is touched
        mParentLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(DetailsActivity.this.getWindow().getDecorView().
                        getRootView().getWindowToken(), 0);
                return true;
            }
        });

        // Set an onClickListener on the edit image container to start a new intent to find an
        // image to set on the ImageView
        mEditImageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent;
                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                } else {
                    galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
                }

                galleryIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(galleryIntent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        // If the item Uri is not null and contains edit, set the edit boolean to true and remove
        // the edit extension from the Uri.
        if (mCurrentItemUri != null) {
            stringUri = mCurrentItemUri.toString();

            if (stringUri.contains("edit")) {
                edit = true;
                stringUri = stringUri.substring(0, stringUri.lastIndexOf('/'));
            }

            mCurrentItemUri = Uri.parse(stringUri);
        }

        Log.i(LOG_TAG, "Edit =" + edit);
        Log.i(LOG_TAG, "Current Uri = " + mCurrentItemUri);

        // If the intent DOES NOT contain an item content URI, then we know that we are
        // creating a new item.
        if (mCurrentItemUri == null) {
            // This is a new item, so change the app bar to say "Add an Item"
            setTitle(getString(R.string.title_new_item));

            mSaveOrderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Save item to database
                    saveItem();
                }
            });

            // Invalidate the options menu, so the "Edit" and "Delete" menu options can be hidden.
            invalidateOptionsMenu();

            // If the intent contains an item content URI and edit = true, then we know that we are
            // editing an existing item
        } else if (mCurrentItemUri != null && edit == true) {
            // This is an existing item, so change the app bar to say "Edit Item"
            setTitle(getString(R.string.title_edit_item));

            mSaveOrderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Save item to database
                    saveItem();
                }
            });

            // Initialize a loader to read the inventory data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);

        } else {
            // Otherwise load the item details and set app bar Title to say "Edit Pet"
            setTitle(getString(R.string.title_details));

            // Hide the views we don't want to see on the details activity
            mItemNameEditText.setVisibility(View.GONE);
            mItemPriceEditText.setVisibility(View.GONE);
            mItemSupplierEditText.setVisibility(View.GONE);
            mSupplierEmailEditText.setVisibility(View.GONE);
            mItemQuantityEditText.setVisibility(View.GONE);
            mEditImageContainer.setVisibility(View.GONE);

            // Show the views we want to see
            mItemNameTextView.setVisibility(View.VISIBLE);
            mItemIdTextView.setVisibility(View.VISIBLE);
            mItemPriceTextView.setVisibility(View.VISIBLE);
            mItemSupplierTextView.setVisibility(View.VISIBLE);
            mSupplierEmailTextView.setVisibility(View.VISIBLE);
            mItemQuantityTextView.setVisibility(View.VISIBLE);
            mIdContainer.setVisibility(View.VISIBLE);
            mQuantityAdjustmentContainer.setVisibility(View.VISIBLE);

            // Set the default adjustment value to 1 and move cursor to end of EditText
            mAdjustmentValueEditText.setText("1");
            mAdjustmentValueEditText.setSelection(mAdjustmentValueEditText.getText().length());

            // Change the text of the button to say Order instead of Save
            mSaveOrderButton.setText(R.string.order);

            // Set an onClickListener on the increment button and update quantity by the adjustment
            // value
            mIncrementButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String quantity = mItemQuantityTextView.getText().toString();
                    mQuantity = parseInt(quantity);
                    String adjustmentValueString = mAdjustmentValueEditText.getText().toString().trim();
                    final int adjustmentValueInteger = parseInt(adjustmentValueString);
                    mQuantity = mQuantity + adjustmentValueInteger;
                    updateQuantity();
                }
            });

            // Set an onClickListener on the decrement button and update quantity by the adjustment
            // value
            mDecrementButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String quantity = mItemQuantityTextView.getText().toString();
                    mQuantity = parseInt(quantity);
                    if (mQuantity == 0) {
                        return;
                    }
                    String adjustmentValueString = mAdjustmentValueEditText.getText().toString().trim();
                    final int adjustmentValueInteger = parseInt(adjustmentValueString);
                    mQuantity = mQuantity - adjustmentValueInteger;
                    if (mQuantity < 0) {
                        mQuantity = 0;
                    }
                    updateQuantity();
                }
            });

            // Set an onClickListener on the Order button to send item details to email intent
            mSaveOrderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    submitOrder();
                }
            });

            // Initialize a loader to read the item data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them.
        mEditImageContainer.setOnTouchListener(mTouchListener);
        mItemNameEditText.setOnTouchListener(mTouchListener);
        mItemPriceEditText.setOnTouchListener(mTouchListener);
        mItemSupplierEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);
        mItemQuantityEditText.setOnTouchListener(mTouchListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Edit" and "Delete" menu options.
        MenuItem menuItem = menu.findItem(R.id.action_delete);
        MenuItem menuItem1 = menu.findItem(R.id.action_edit);
        if (mCurrentItemUri == null) {
            menuItem.setVisible(false);
            menuItem1.setVisible(false);
        }
        if (edit == true) {
            menuItem1.setVisible(false);
        }
        return true;
    }

    /**
     * This method is called when the user clicks on a menu item
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Edit" menu option
            case R.id.action_edit:
                // Create a new intent to load DetailsActivity with a new Uri to open the edit item
                // screen
                Intent intent = new Intent(DetailsActivity.this, DetailsActivity.class);

                String edit = "edit";
                Uri editUri = Uri.withAppendedPath(mCurrentItemUri, edit);
                Log.v("LOG", "currentItemUri= " + editUri);

                intent.setData(editUri);

                startActivity(intent);
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link InventoryActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(DetailsActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                InventoryContract.InventoryEntry._ID,
                InventoryContract.InventoryEntry.COLUMN_ITEM_NAME,
                InventoryContract.InventoryEntry.COLUMN_ITEM_IMAGE,
                InventoryContract.InventoryEntry.COLUMN_ITEM_PRICE,
                InventoryContract.InventoryEntry.COLUMN_ITEM_SUPPLIER,
                InventoryContract.InventoryEntry.COLUMN_SUPPLIER_EMAIL,
                InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int idColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry._ID);
            int nameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME);
            int imageColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_IMAGE);
            int priceColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_PRICE);
            int supplierColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_SUPPLIER);
            int supplierEmailColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_SUPPLIER_EMAIL);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY);

            // Extract out the value from the Cursor for the given column index
            int id = cursor.getInt(idColumnIndex);
            String name = cursor.getString(nameColumnIndex);
            String image = cursor.getString(imageColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);
            mPriceInt = cursor.getInt(priceColumnIndex);
            mQuantity = cursor.getInt(quantityColumnIndex);
            mImageUri = Uri.parse(image);
            Log.i(LOG_TAG, "Image Uri = " + mImageUri);
            // Convert price to a double value and then string and set decimal to hundredths column
            mPrice = mPriceInt;
            mPrice = mPrice / 100;
            DecimalFormat df = new DecimalFormat("0.00");
            String price = df.format(mPrice);

            ViewTreeObserver viewTreeObserver = mItemImageView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mItemImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mItemImageView.setImageBitmap(getBitmapFromUri(mImageUri));
                }
            });

            // Update the views on the screen with the values from the database
            mItemNameTextView.setText(name);
            mItemNameEditText.setText(name);
            mItemIdTextView.setText(Integer.toString(id));

            mItemPriceTextView.setText("$" + price);
            mItemPriceEditText.setText(price);
            mItemSupplierTextView.setText(supplier);
            mItemSupplierEditText.setText(supplier);
            mSupplierEmailTextView.setText(supplierEmail);
            mSupplierEmailEditText.setText(supplierEmail);
            mItemQuantityTextView.setText(Integer.toString(mQuantity));
            mItemQuantityEditText.setText(Integer.toString(mQuantity));

            // Set the default cursor position to end of text when EditText is clicked
            mItemNameEditText.setSelection(mItemNameEditText.getText().length());
            mItemPriceEditText.setSelection(mItemPriceEditText.getText().length());
            mItemSupplierEditText.setSelection(mItemSupplierEditText.getText().length());
            mSupplierEmailEditText.setSelection(mSupplierEmailEditText.getText().length());
            mItemQuantityEditText.setSelection(mItemQuantityEditText.getText().length());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mItemNameEditText.setText("");
        mItemPriceEditText.setText("");
        mItemSupplierEditText.setText("");
        mSupplierEmailEditText.setText("");
        mItemQuantityEditText.setText("");
        mImageUri = null;
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this item.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Get user input from editor and save item into database.
     */
    private void saveItem() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mItemNameEditText.getText().toString().trim();
        String imageString = "";
        String priceString = mItemPriceEditText.getText().toString().trim();
        String supplierString = mItemSupplierEditText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();
        String quantityString = mItemQuantityEditText.getText().toString().trim();

        // If mImageUri is not null set imageString to Uri value to verify that an image is present
        if (mImageUri != null) {
            imageString = mImageUri.toString();
        }

        // If a price is entered format it to the hundredths decimal place and then convert it to an
        // integer to store in the database
        if (!TextUtils.isEmpty(priceString)) {
            mPrice = parseDouble(priceString);
            DecimalFormat df = new DecimalFormat("0.00");
            priceString = df.format(mPrice);
            mPrice = parseDouble(priceString.replace(",", "."));
            mPrice = mPrice * 100;
            String price = String.valueOf((int) mPrice);
            mPriceInt = parseInt(price);
            Log.i(LOG_TAG, "mPrice = " + mPrice);
        }

        // If the quantity is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        if (!TextUtils.isEmpty(quantityString)) {
            mQuantity = parseInt(quantityString);
        }

        // Check if this is supposed to be a new item
        // and check if all the fields in the editor are blank
        if (mCurrentItemUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(imageString)
                && TextUtils.isEmpty(priceString) && TextUtils.isEmpty(supplierString)
                && TextUtils.isEmpty(supplierEmailString) && TextUtils.isEmpty(quantityString)) {
            // Since no fields were modified, we can return early without creating a new item.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            Toast.makeText(this, "All item attributes are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if any items are missing and return a toast rather than update the database
        if (imageString == "") {
            Toast.makeText(this, "Item image is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, "Item name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(priceString) || mPrice <= 0) {
            Toast.makeText(this, "Item price must be valid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(supplierString)) {
            Toast.makeText(this, "Item supplier is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(supplierEmailString) || !isValidEmail(supplierEmailString)) {
            Toast.makeText(this, "Supplier's Email must be valid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(quantityString) || mQuantity < 0) {
            Toast.makeText(this, "Item quantity must be valid", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and item attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_NAME, nameString);
        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_IMAGE, imageString);
        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_PRICE, mPriceInt);
        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_SUPPLIER, supplierString);
        values.put(InventoryContract.InventoryEntry.COLUMN_SUPPLIER_EMAIL, supplierEmailString);
        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY, mQuantity);

        // Determine if this is a new or existing item by checking if mCurrentItemUri is null or not
        if (mCurrentItemUri == null) {
            // This is a NEW item, so insert a new item into the provider,
            // returning the content URI for the new item.
            Uri newUri = getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_item_successful),
                        Toast.LENGTH_SHORT).show();
                // Exit activity
                finish();
            }
        } else {
            // Otherwise this is an EXISTING item, so update the item with content URI: mCurrentItemUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentItemUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_item_successful),
                        Toast.LENGTH_SHORT).show();
                // Exit activity
                finish();
            }
        }
    }

    /**
     * Update the quantity when user presses on the sale, increment, or decrement buttons
     */
    private void updateQuantity() {
        Log.v("LOG", "Quantity = " + mQuantity);

        ContentValues values = new ContentValues();

        values.put(InventoryContract.InventoryEntry.COLUMN_ITEM_QUANTITY, mQuantity);

        // Otherwise this is an EXISTING item, so update the item with content URI: mCurrentItemUri
        // and pass in the new ContentValues. Pass in null for the selection and selection args
        // because mCurrentItemUri will already identify the correct row in the database that
        // we want to modify.
        int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

        // Show a toast message depending on whether or not the update was successful.
        if (rowsAffected == 0) {
            // If no rows were affected, then there was an error with the update.
            Toast.makeText(DetailsActivity.this, getString(R.string.editor_update_item_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(DetailsActivity.this, getString(R.string.editor_update_item_successful),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        // Only perform the delete if this is an existing item.
        if (mCurrentItemUri != null) {
            // Call the ContentResolver to delete the item at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentItemUri
            // content URI already identifies the pet that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_item_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_item_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        NavUtils.navigateUpFromSameTask(this);
    }

    /**
     * This method is called when the order button is clicked.
     */
    public void submitOrder() {
        String supplier = mItemSupplierTextView.getText().toString();
        String supplierEmail = mSupplierEmailEditText.getText().toString();
        String item = mItemNameTextView.getText().toString();
        String quantityString = mAdjustmentValueEditText.getText().toString();
        int quantity;

        // Only parse the quantity string if adjusment value is not empty, otherwise set the
        // quantity value and quantity string to 0
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = parseInt(quantityString);
        } else {
            quantityString = "0";
            quantity = 0;
        }
        String priceString = mItemPriceTextView.getText().toString();
        priceString = priceString.replace("$", "");
        double price = parseDouble(priceString);

        // Calculate the total price of the order an set the priceString to proper format
        double totalPrice = quantity * price;
        DecimalFormat df = new DecimalFormat("0.00");
        priceString = df.format(totalPrice);

        // Call the priceMessage method and pass in the proper values
        String priceMessage = createOrderSummary(supplier, item, quantityString, priceString);

        // Create a new intent to pass the item details into an email
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + supplierEmail)); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject, item));
        intent.putExtra(Intent.EXTRA_TEXT, priceMessage);
        // FOLLOWING STATEMENT CHECKS WHETHER THERE IS ANY APP THAT CAN HANDLE OUR EMAIL INTENT
        startActivity(Intent.createChooser(intent,
                "Send Email Using: "));
    }

    /**
     * Create summary of the order.
     *
     * @param supplier of the item
     * @param item     is the name of purchase item
     * @param quantity is the quantity of the item being ordered
     * @param price    is the price of the order
     * @return text summary
     */
    private String createOrderSummary(String supplier, String item, String quantity, String price) {
        String PriceMessage = getString(R.string.order_summary_supplier, supplier);
        PriceMessage += "\n" + getString(R.string.order_summary_item, item);
        PriceMessage += "\n" + getString(R.string.order_summary_quantity, quantity);
        PriceMessage += "\n" + getString(R.string.order_summary_price, "$" + price);
        PriceMessage += "\n" + "\n" + getString(R.string.thank_you);
        return PriceMessage;
    }

    /**
     * If selected image is valid pass in the location to the Image Uri and then set the Bitmap
     * from Uri on the item ImageView
     *
     * @param requestCode
     * @param resultCode
     * @param resultData
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mImageUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mImageUri.toString());

                mItemImageView.setImageBitmap(getBitmapFromUri(mImageUri));
            }
        }
    }

    /**
     * This method will return a bitmap from uri
     *
     * @param uri
     * @return
     */
    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mItemImageView.getWidth();
        int targetH = mItemImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mImageUri != null)
            outState.putString(STATE_URI, mImageUri.toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_URI) &&
                !savedInstanceState.getString(STATE_URI).equals("")) {
            mImageUri = Uri.parse(savedInstanceState.getString(STATE_URI));

            ViewTreeObserver viewTreeObserver = mItemImageView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mItemImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mItemImageView.setImageBitmap(getBitmapFromUri(mImageUri));
                }
            });
        }
    }
}

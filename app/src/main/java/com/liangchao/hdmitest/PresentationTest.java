package com.liangchao.hdmitest;
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


// Need the following import to get access to the app resources, since this
// class is in a sub-package.

//创建开发分支做第一次提交与合并

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

public class PresentationTest extends Activity
        implements OnCheckedChangeListener, OnClickListener, OnItemSelectedListener {
    private final String TAG = "PresentationActivity";

    // Key for storing saved instance state.
    private static final String PRESENTATION_KEY = "presentation";

    //config diretc show
    private static final boolean config_show = false;
    private static final boolean first_show_video = false;
    private static final boolean is_repeat_play = true;
    //when boot completed start it
    public static final boolean bol_start_presentation = false;
    // need Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGEs
    private static final boolean ifneedpermissions = false;
    // The content that we want to show on the presentation.
    private static final int[] PHOTOS = new int[] {
            R.drawable.frantic,
            R.drawable.photo1, R.drawable.photo2, R.drawable.photo3,
            R.drawable.photo4, R.drawable.photo5, R.drawable.photo6,
            R.drawable.sample_4,R.drawable.timg,
    };

    private DisplayManager mDisplayManager;
    private DisplayListAdapter mDisplayListAdapter;
    private CheckBox mShowAllDisplaysCheckbox;
    private ListView mListView;
    private int mNextImageNumber;
    private static final int MY_PERMISSION_REQUEST_CODE = 10000;

    private GestureDetectorCompat mDetector;

    // List of presentation contents indexed by displayId.
    // This state persists so that we can restore the old presentation
    // contents when the activity is paused or resumed.
    private SparseArray<DemoPresentationContents> mSavedPresentationContents;

    // List of all currently visible presentations indexed by display id.
    private final SparseArray<DemoPresentation> mActivePresentations =
            new SparseArray<DemoPresentation>();

    Intent mDisplayServer;

    /**
     * Initialization of the Activity after it is first created.  Must at least
     * call {@link android.app.Activity#setContentView setContentView()} to
     * describe what is to be displayed in the screen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        // Restore saved instance state.
        if (savedInstanceState != null) {
            mSavedPresentationContents =
                    savedInstanceState.getSparseParcelableArray(PRESENTATION_KEY);
        } else {
            mSavedPresentationContents = new SparseArray<DemoPresentationContents>();
        }

        // Get the display manager service.
        mDisplayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);

        // See assets/res/any/layout/presentation_activity.xml for this
        // view layout definition, which is being set here as
        // the content of our screen.
        setContentView(R.layout.presentation_activity);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        // Set up checkbox to toggle between showing all displays or only presentation displays.
        mShowAllDisplaysCheckbox = (CheckBox)findViewById(R.id.show_all_displays);
        mShowAllDisplaysCheckbox.setChecked(config_show);
        mShowAllDisplaysCheckbox.setOnCheckedChangeListener(this);
        // Set up the list of displays.
        mDisplayListAdapter = new DisplayListAdapter(this);
        mListView = (ListView)findViewById(R.id.display_list);
        mListView.setAdapter(mDisplayListAdapter);
        if(ifneedpermissions) {
            if (requestPower()) {
                return;
            }
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    MY_PERMISSION_REQUEST_CODE
            );
        }
    }

    public boolean requestPower() {
        boolean isAllGranted = checkPermissionAllGranted(
                new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }
        );

        return isAllGranted;
    }

    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TODO Auto-generated method stub
        mDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(TAG,"onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(TAG, "onFling: " + event1.toString()+event2.toString());
            return true;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        // Be sure to call the super class.
        super.onResume();

        // Update our list of displays on resume.
        mDisplayListAdapter.updateContents();
        if(!Settings.canDrawOverlays(PresentationTest.this)){
            //Permit drawing over other apps is closed,need to open it
            showdialog();
        }
        // Restore presentations from before the activity was paused.
        final int numDisplays = mDisplayListAdapter.getCount();
        for (int i = 0; i < numDisplays; i++) {
            final Display display = mDisplayListAdapter.getItem(i);
            DemoPresentationContents contents =
                    mSavedPresentationContents.get(display.getDisplayId());
            if (contents == null && config_show) {
                contents = new DemoPresentationContents(getNextPhoto());
                showPresentation(display, contents);
            }
        }
        mSavedPresentationContents.clear();

        // Register to receive events from the display manager.
        mDisplayManager.registerDisplayListener(mDisplayListener, null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /*
     *  show permission dialog
     */
    public void showdialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(PresentationTest.this);
        //if(config_show)builder .setCancelable(false);
        AlertDialog malertDialog = builder.setMessage(R.string.dialog_permission_text)
                .setTitle(R.string.dialog_permission_attention)
                .setPositiveButton(R.string.dialog_permission_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!Settings.canDrawOverlays(PresentationTest.this)) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 10);
                        }
                    }
                })
                .setCancelable(!config_show)
                .create();

        malertDialog.show();
    }



    @Override
    protected void onPause() {
        // Be sure to call the super class.
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Be sure to call the super class.
        super.onSaveInstanceState(outState);
        outState.putSparseParcelableArray(PRESENTATION_KEY, mSavedPresentationContents);
    }

    /**
     * Shows a {@link Presentation} on the specified display.
     */
    public static Display mDisplays;
    public static DemoPresentationContents mDemoPresentationContents;
    public static DemoPresentation presentation1;
    private void showPresentation(Display display, DemoPresentationContents contents) {
        final int displayId = display.getDisplayId();
        if (mActivePresentations.get(displayId) != null) {
            return;
        }

        Log.d(TAG, "Showing presentation photo #" + contents.photo
                + " on display #" + displayId + ".");
        if(displayId == 0) {
            DemoPresentation presentation = new DemoPresentation(this, display, contents);
            presentation.show();
            presentation.setOnDismissListener(mOnDismissListener);
            mActivePresentations.put(displayId, presentation);
        }else {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(PresentationTest.this)) {
                    showdialog();
                    return;
                }
                mDisplays = display;
                mDemoPresentationContents = contents;
                presentation1 = new DemoPresentation(this, display, contents);
                presentation1.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                presentation1.show();
                presentation1.setOnDismissListener(mOnDismissListener);
                //mDisplayServer = new Intent(PresentationTest.this,DisPalyServer.class);
                //startService(mDisplayServer);
                mActivePresentations.put(displayId, presentation1);
            }
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                if(!Settings.canDrawOverlays(PresentationTest.this) || !PresentatinUtils.checkWindowsPermission(PresentationTest.this)){
                    //Alert windows permission is false
                    showdialog();
                    return;
                }
                mDisplays = display;
                mDemoPresentationContents = contents;
                presentation1 = new DemoPresentation(this, display, contents);
                presentation1.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                presentation1.show();
                presentation1.setOnDismissListener(mOnDismissListener);
                //mDisplayServer = new Intent(PresentationTest.this,DisPalyServer.class);
                //startService(mDisplayServer);
                mActivePresentations.put(displayId, presentation1);
            }
            if(Build.VERSION.SDK_INT >= 26){
                if(!Settings.canDrawOverlays(PresentationTest.this) || !PresentatinUtils.checkWindowsPermission(PresentationTest.this)){
                    //Alert windows permission is false
                    showdialog();
                    return;
                }
                if(display == null){
                    Log.d(TAG, "Could not get valid secondary display.");
                }else{
                    mDisplays = display;
                    Log.d(TAG, "Secondary display is: " + display);
                }
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchDisplayId(mDisplays.getDisplayId());
                Intent intent = new Intent();
                //intent.setClass(this,PresentationTest.class);
                intent.setComponent(new ComponentName(
                        new String("com.android.settings"), new String("com.android.settings.Settings")));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(intent, options.toBundle());
            }
        }
    }

    /**
     * Hides a {@link Presentation} on the specified display.
     */
    private void hidePresentation(Display display) {
        final int displayId = display.getDisplayId();
        DemoPresentation presentation = mActivePresentations.get(displayId);
        if (presentation == null) {
            return;
        }

        Log.d(TAG, "Dismissing presentation on display #" + displayId + ".");
        presentation.dismiss();
        mActivePresentations.delete(displayId);
    }

    /**
     * Sets the display mode of the {@link Presentation} on the specified display
     * if it is already shown.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setPresentationDisplayMode(Display display, int displayModeId) {
        final int displayId = display.getDisplayId();
        DemoPresentation presentation = mActivePresentations.get(displayId);
        if (presentation == null) {
            return;
        }

        presentation.setPreferredDisplayMode(displayModeId);
    }

    public int getNextPhoto() {
        final int photo = mNextImageNumber;
        mNextImageNumber = (mNextImageNumber + 1) % PHOTOS.length;
        return photo;
    }

    /**
     * Called when the show all displays checkbox is toggled or when
     * an item in the list of displays is checked or unchecked.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mShowAllDisplaysCheckbox) {
            // Show all displays checkbox was toggled.
            mDisplayListAdapter.updateContents();
        } else {
            // Display item checkbox was toggled.
            final Display display = (Display)buttonView.getTag();
            if (isChecked) {
                DemoPresentationContents contents = new DemoPresentationContents(getNextPhoto());
                Log.d("andrew", "onCheckedChanged");
                showPresentation(display, contents);
                //AlertDialog mAlertDialog = PresentationDialog(display,contents,buttonView);
                //mAlertDialog.dismiss();
                //buttonView.setChecked(true);
            } else {
                hidePresentation(display);
            }
            mDisplayListAdapter.updateContents();
        }
    }

    /**
     * Called when the Info button next to a display is clicked to show information
     * about the display.
     */
    @Override
    public void onClick(View v) {
        Context context = v.getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final Display display = (Display)v.getTag();
        Resources r = context.getResources();
        AlertDialog alert = builder
                .setTitle(r.getString(
                        R.string.presentation_alert_info_text, display.getDisplayId()))
                .setMessage(display.toString())
                .setNeutralButton(R.string.presentation_alert_dismiss_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .create();
        alert.show();
    }

    /**
     * Called when a display mode has been selected.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final Display display = (Display)parent.getTag();
        final Display.Mode[] modes = display.getSupportedModes();
        setPresentationDisplayMode(display, position >= 1 && position <= modes.length ?
                modes[position - 1].getModeId() : 0);
    }

    /**
     * Called when a display mode has been unselected.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        final Display display = (Display)parent.getTag();
        setPresentationDisplayMode(display, 0);
    }

    /**
     * Listens for displays to be added, changed or removed.
     * We use it to update the list and show a new {@link Presentation} when a
     * display is connected.
     *
     * Note that we don't bother dismissing the {@link Presentation} when a
     * display is removed, although we could.  The presentation API takes care
     * of doing that automatically for us.
     */
    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                    Log.d(TAG, "Display #" + displayId + " added.");
                    mDisplayListAdapter.updateContents();
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    Log.d(TAG, "Display #" + displayId + " changed.");
                    mDisplayListAdapter.updateContents();
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                    Log.d(TAG, "Display #" + displayId + " removed.");
                    mDisplayListAdapter.updateContents();
                }
            };

    /**
     * Listens for when presentations are dismissed.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    DemoPresentation presentation = (DemoPresentation)dialog;
                    int displayId = presentation.getDisplay().getDisplayId();
                    Log.d(TAG, "Presentation on display #" + displayId + " was dismissed.");
                    mActivePresentations.delete(displayId);
                    mDisplayListAdapter.notifyDataSetChanged();
                }
            };

    /**
     * List adapter.
     * Shows information about all displays.
     */
    private final class DisplayListAdapter extends ArrayAdapter<Display> {
        final Context mContext;

        public DisplayListAdapter(Context context) {
            super(context, R.layout.presentation_list_item);
            mContext = context;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View v;
            if (convertView == null) {
                v = ((Activity) mContext).getLayoutInflater().inflate(
                        R.layout.presentation_list_item, null);
            } else {
                v = convertView;
            }

            final Display display = getItem(position);
            final int displayId = display.getDisplayId();

            DemoPresentation presentation = mActivePresentations.get(displayId);
            DemoPresentationContents contents = presentation != null ?
                    presentation.mContents : null;
            if (contents == null) {
                contents = mSavedPresentationContents.get(displayId);
            }

            CheckBox cb = (CheckBox)v.findViewById(R.id.checkbox_presentation);
            cb.setTag(display);
            cb.setOnCheckedChangeListener(PresentationTest.this);
            cb.setChecked(contents != null);

            TextView tv = (TextView)v.findViewById(R.id.display_id);
            tv.setText(v.getContext().getResources().getString(
                    R.string.presentation_display_id_text, displayId, display.getName()));

            Button b = (Button)v.findViewById(R.id.info);
            b.setTag(display);
            b.setOnClickListener(PresentationTest.this);

            Spinner s = (Spinner)v.findViewById(R.id.modes);
            Display.Mode[] modes = display.getSupportedModes();
            if (contents == null || modes.length == 1) {
                s.setVisibility(View.GONE);
                s.setAdapter(null);
            } else {
                ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(mContext,
                        android.R.layout.simple_list_item_1);
                s.setVisibility(View.VISIBLE);
                s.setAdapter(modeAdapter);
                s.setTag(display);
                s.setOnItemSelectedListener(PresentationTest.this);

                modeAdapter.add("<default mode>");

                for (Display.Mode mode : modes) {
                    modeAdapter.add(String.format("Mode %d: %dx%d/%.1ffps",
                            mode.getModeId(),
                            mode.getPhysicalWidth(), mode.getPhysicalHeight(),
                            mode.getRefreshRate()));
                    if (contents.displayModeId == mode.getModeId()) {
                        s.setSelection(modeAdapter.getCount() - 1);
                    }
                }
            }

            return v;
        }

        /**
         * Update the contents of the display list adapter to show
         * information about all current displays.
         */
        public void updateContents() {
            clear();

            String displayCategory = getDisplayCategory();
            Display[] displays = mDisplayManager.getDisplays(displayCategory);
            addAll(displays);
            Log.d(TAG, "There are currently " + displays.length + " displays connected.");
            for (Display display : displays) {
                Log.d(TAG, "  " + display);
            }
        }

        private String getDisplayCategory() {
            return mShowAllDisplaysCheckbox.isChecked() ? null :
                    DisplayManager.DISPLAY_CATEGORY_PRESENTATION;
        }
    }

    /**
     * The presentation to show on the secondary display.
     *
     * Note that the presentation display may have different metrics from the display on which
     * the main activity is showing so we must be careful to use the presentation's
     * own {@link Context} whenever we load resources.
     */
    public class DemoPresentation extends Presentation {

        final DemoPresentationContents mContents;

        private GestureDetectorCompat mDetector;

        TextView text;
        ImageView image;
        GradientDrawable drawable;
        Context mComtext;
        Button but_img,but_vid;
        VideoView mVideoview;
        SeekBar mSeekbar;
        boolean isPlaying;
        //Uri mUri = Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Shrek.mp4");
        String uuri = "android.resource://" + getPackageName() + "/" + R.raw.shrek;
        //String uuri = "file:///sdcard/Movies/shrek.mp4";
        public DemoPresentation(Context context, Display display,
                                DemoPresentationContents contents) {
            super(context, display);
            mComtext = context;
            mContents = contents;
        }


        /**
         * Sets the preferred display mode id for the presentation.
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void setPreferredDisplayMode(int modeId) {
            mContents.displayModeId = modeId;

            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.preferredDisplayModeId = modeId;
            getWindow().setAttributes(params);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // Be sure to call the super class.
            super.onCreate(savedInstanceState);
            // Get the resources for the context of the presentation.
            // Notice that we are getting the resources from the context of the presentation.
            Resources r = getContext().getResources();


            // Inflate the layout.
            setContentView(R.layout.presentation_content);

            mDetector = new GestureDetectorCompat(getApplicationContext(), new MyGestureListener());

            final Display display = getDisplay();
            final int displayId = display.getDisplayId();
            final int photo = mContents.photo;

            // Show a caption to describe what's going on.
            findviewbyid();
            text.setText(r.getString(R.string.presentation_photo_text,
                    photo, displayId, display.getName()));
            // Show a n image for visual interest.
            MediaController mMediaController = new MediaController(DemoPresentation.this.getContext());
            mVideoview.setMediaController(mMediaController);
            //mMediaController.setVisibility(View.INVISIBLE);
            mVideoview.setVideoURI(Uri.parse(uuri));
            image.setImageDrawable(r.getDrawable(PHOTOS[photo]));
            //video play completioned listenter
            mVideoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (is_repeat_play) {
                        if (mVideoview.isPlaying()) {
                            if (mVideoview != null) {
                                mVideoview.pause();
                                //mVideoview.suspend();
                            }
                        } else {
                            text.setVisibility(View.GONE);
                            image.setVisibility(View.GONE);
                            mVideoview.setVisibility(View.VISIBLE);
                            //mMediaController.setVisibility(View.VISIBLE);
                            //mSeekbar.setVisibility(View.VISIBLE);
                            mVideoview.start();
                            mVideoview.requestFocus();
                        }
                    }
                }
            });
            if(displayId == 0){
                but_img.setVisibility(View.GONE);
                but_vid.setVisibility(View.GONE);
                //image.setVisibility(View.VISIBLE);
                mVideoview.setVisibility(View.GONE);
            }else{
                image.setVisibility(View.GONE);
                mVideoview.setVisibility(View.GONE);
                text.setVisibility(View.GONE);
                mSeekbar.setVisibility(View.GONE);

                if (mVideoview != null) {
                    //mVideoview.suspend();
                    mVideoview.pause();
                    mVideoview.setVisibility(View.GONE);
                }
                text.setVisibility(View.VISIBLE);
                image.setVisibility(View.VISIBLE);
                mSeekbar.setVisibility(View.GONE);
                image.setImageDrawable(getContext().getResources().getDrawable(PHOTOS[getNextPhoto()]));
                text.setText(getContext().getResources().getString(R.string.presentation_photo_text,
                        getNextPhoto(), getDisplay().getDisplayId(), getDisplay().getName()));
                //add first show video
                if(first_show_video){
                    if(mVideoview.isPlaying()){
                        if(mVideoview!=null) {
                            mVideoview.pause();
                            //mVideoview.suspend();
                        }
                    }else {
                        text.setVisibility(View.GONE);
                        image.setVisibility(View.GONE);
                        mVideoview.setVisibility(View.VISIBLE);
                        //mMediaController.setVisibility(View.VISIBLE);
                        //mSeekbar.setVisibility(View.VISIBLE);
                        mVideoview.start();
                        mVideoview.requestFocus();

                    }
                }
            }

            but_img.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mVideoview != null) {
                        //mVideoview.suspend();
                        mVideoview.pause();
                        mVideoview.setVisibility(View.GONE);//Unable to add window
                    }
                    text.setVisibility(View.VISIBLE);
                    image.setVisibility(View.VISIBLE);
                    mSeekbar.setVisibility(View.GONE);//Unable to add window
                    image.setImageDrawable(getContext().getResources().getDrawable(PHOTOS[getNextPhoto()]));
                    text.setText(getContext().getResources().getString(R.string.presentation_photo_text,
                            getNextPhoto(), getDisplay().getDisplayId(), getDisplay().getName()));
                }

            });

            but_vid.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mVideoview.isPlaying()){
                        if(mVideoview!=null) {
                            mVideoview.pause();
                            //mVideoview.suspend();
                        }
                    }else {
                        text.setVisibility(View.GONE);
                        image.setVisibility(View.GONE);
                        mVideoview.setVisibility(View.VISIBLE);
                        //mMediaController.setVisibility(View.VISIBLE);
                        //mSeekbar.setVisibility(View.VISIBLE);
                        mVideoview.start();
                        mVideoview.requestFocus();

                    }
                }
            });
            drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);

            // Set the background to a random gradient.
            Point p = new Point();
            getDisplay().getSize(p);
            drawable.setGradientRadius(Math.max(p.x, p.y) / 2);
            drawable.setColors(mContents.colors);
            findViewById(android.R.id.content).setBackground(drawable);
        }

        private void findviewbyid(){
            image = (ImageView)findViewById(R.id.image);
            but_img = (Button)findViewById(R.id.but_img);
            but_vid = (Button) findViewById(R.id.but_vid);
            mSeekbar = (SeekBar) findViewById(R.id.mSeekbar);
            text = (TextView)findViewById(R.id.text);
            mVideoview = (VideoView)findViewById(R.id.mVideoview);
        }

        public boolean dispatchTouchEvent(MotionEvent ev) {
            // TODO Auto-generated method stub
            mDetector.onTouchEvent(ev);
            Log.d(TAG,"dispatchTouchEvent");
            return super.dispatchTouchEvent(ev);
        }

        public boolean onTouchEvent(MotionEvent event){

            this.mDetector.onTouchEvent(event);

            Log.d(TAG,"onTouchEvent");

            //change photo
            Resources r = getContext().getResources();
            final Display display = getDisplay();
            final int displayId = display.getDisplayId();
            final int photo = getNextPhoto();

            text.setText(r.getString(R.string.presentation_photo_text,
                    photo, displayId, display.getName()));

            image.setImageDrawable(r.getDrawable(PHOTOS[photo]));

            drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);

            // Set the background to a random gradient.
            Point p = new Point();
            getDisplay().getSize(p);
            drawable.setGradientRadius(Math.max(p.x, p.y) / 2);
            drawable.setColors(mContents.colors);
            findViewById(android.R.id.content).setBackground(drawable);

            return super.onTouchEvent(event);
        }

        class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
            private static final String TAG = "Gestures";

            @Override
            public boolean onDown(MotionEvent event) {
                Log.d(TAG,"onDown: " + event.toString());


                return true;
            }

            @Override
            public boolean onFling(MotionEvent event1, MotionEvent event2,
                                   float velocityX, float velocityY) {
                Log.d(TAG, "onFling: " + event1.toString()+event2.toString());
                return true;
            }
        }


    }

    /**
     * Information about the content we want to show in the presentation.
     */
    private final static class DemoPresentationContents implements Parcelable {
        final int photo;
        final int[] colors;
        int displayModeId;

        public static final Creator<DemoPresentationContents> CREATOR =
                new Creator<DemoPresentationContents>() {
                    @Override
                    public DemoPresentationContents createFromParcel(Parcel in) {
                        return new DemoPresentationContents(in);
                    }

                    @Override
                    public DemoPresentationContents[] newArray(int size) {
                        return new DemoPresentationContents[size];
                    }
                };

        public DemoPresentationContents(int photo) {
            this.photo = photo;
            colors = new int[] {
                    ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000,
                    ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000 };
        }

        private DemoPresentationContents(Parcel in) {
            photo = in.readInt();
            colors = new int[] { in.readInt(), in.readInt() };
            displayModeId = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(photo);
            dest.writeInt(colors[0]);
            dest.writeInt(colors[1]);
            dest.writeInt(displayModeId);
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mDisplayServer != null){
            stopService(mDisplayServer);
        }
        // Unregister from the display manager.
        mDisplayManager.unregisterDisplayListener(mDisplayListener);

        // Dismiss all of our presentations but remember their contents.
        Log.d(TAG, "Activity is being paused.  Dismissing all active presentation.");
        for (int i = 0; i < mActivePresentations.size(); i++) {
            DemoPresentation presentation = mActivePresentations.valueAt(i);
            int displayId = mActivePresentations.keyAt(i);
            mSavedPresentationContents.put(displayId, presentation.mContents);
            presentation.dismiss();
        }
        mActivePresentations.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}


/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.leeko.intervaltimer;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.leeko.intervaltimer.dialog.RoundsDialog;
import org.leeko.intervaltimer.dialog.TimeDialog;
import org.leeko.intervaltimer.view.SlidingTabLayout;


/**
 * to display a custom {@link android.support.v4.view.ViewPager} title strip which gives continuous feedback to the user
 * when scrolling.
 */
public class SlidingTabsBasicFragment extends Fragment {

    static final String LOG_TAG = "SlidingTabsBasicFragment";

    /**
     * A custom {@link android.support.v4.view.ViewPager} title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    private SlidingTabLayout mSlidingTabLayout;

    /**
     * A {@link android.support.v4.view.ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above.
     */
    private ViewPager mViewPager;


    // Buttons
    CustomButton buttonRest;
    CustomButton buttonWU;
    CustomButton buttonRounds;
    CustomButton buttonWork;
    ToggleButton manualRestButton;
    TextView totalText;

    /**
     * Inflates the {@link android.view.View} which will be displayed by this {@link android.support.v4.app.Fragment}, from the app's
     * resources.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }

    // BEGIN_INCLUDE (fragment_onviewcreated)
    /**
     * This is called after the {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)} has finished.
     * Here we can pick out the {@link android.view.View}s we need to configure from the content view.
     *
     * We set the {@link android.support.v4.view.ViewPager}'s adapter to be an instance of {@link SamplePagerAdapter}. The
     * {@link SlidingTabLayout} is then given the {@link android.support.v4.view.ViewPager} so that it can populate itself.
     *
     * @param view View created in {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        mViewPager.setAdapter(new SamplePagerAdapter());
        // END_INCLUDE (setup_viewpager)

        // BEGIN_INCLUDE (setup_slidingtablayout)
        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        // MR Even the tabs
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mViewPager);
        // END_INCLUDE (setup_slidingtablayout)


        // MR Restore preferences
        SharedPreferences settings = MainActivity.getInstance().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int tab = settings.getInt(MainActivity.PREFS_TAB, 0);
        mViewPager.setCurrentItem(tab);


    }
    // END_INCLUDE (fragment_onviewcreated)


    public int getCurrentTab() {
        return mViewPager.getCurrentItem();
    }

    public void setCurrentTab(int i) {

        if (i != 0) {
            mViewPager.setCurrentItem(i);
        }
    }

    public void setTabName(int tabId, String name) {
        mSlidingTabLayout.populateTabStrip();
    }


    public void updateCurrentTab() {

        int position = getCurrentTab();
        View view = mViewPager.findViewWithTag(position);

        if (view == null) {
            return;
        }

        createButtons(view, position);
    }


    private void createButtons(View view, int position) {
        // create buttons

         buttonRest = (CustomButton) view.findViewById(R.id.buttonRest);
         buttonWU = (CustomButton) view.findViewById(R.id.buttonWarmUp);
         buttonRounds = (CustomButton) view.findViewById(R.id.buttonRounds);
         buttonWork = (CustomButton) view.findViewById(R.id.buttonWork);
         manualRestButton = (ToggleButton)view.findViewById(R.id.manualRestButton);

        totalText = (TextView)view.findViewById(R.id.totalText);
        totalText.setTag("totalText"+position);

        // set values
        buttonWU.setValueText(WorkoutModel.getInstance().getWorkoutCached(position).getWarmupInString());

        if (WorkoutModel.getInstance().getWorkoutCached(position).getRoundAmount() == 0) {
            buttonRounds.setValueText("Unlimited");
        } else {
            buttonRounds.setValueText("" + WorkoutModel.getInstance().getWorkoutCached(position).getRoundAmount());
        }

        buttonWork.setValueText(WorkoutModel.getInstance().getWorkoutCached(position).getWorkInString());
        buttonRest.setValueText(WorkoutModel.getInstance().getWorkoutCached(position).getRestInString());


        // Set a tag so the button can be found later ( workaround because tab system destroys buttons)
        buttonRest.setTag("restbutton"+position);

        manualRestButton.setChecked(WorkoutModel.getInstance().getWorkoutCached(position).getManual());




    }



    /**
     * The {@link android.support.v4.view.PagerAdapter} used to display pages in this sample.
     * The individual pages are simple and just display two lines of text. The important section of
     * this class is the {@link #getPageTitle(int)} method which controls what is displayed in the
     * {@link SlidingTabLayout}.
     */
    class SamplePagerAdapter extends PagerAdapter {

        /**
         * @return the number of pages to display
         */
        @Override
        public int getCount() {
            return 6;
        }

        /**
         * @return true if the value returned from {@link #instantiateItem(android.view.ViewGroup, int)} is the
         * same object as the {@link android.view.View} added to the {@link android.support.v4.view.ViewPager}.
         */
        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        // BEGIN_INCLUDE (pageradapter_getpagetitle)
        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link SlidingTabLayout}.
         * <p>
         * Here we construct one using the position value, but for real application the title should
         * refer to the item's contents.
         */
        @Override
        public CharSequence getPageTitle(int position) {

            return WorkoutModel.getInstance().getWorkoutCached(position).getName();

        }
        // END_INCLUDE (pageradapter_getpagetitle)

        /**
         * Instantiate the {@link android.view.View} which should be displayed at {@code position}. Here we
         * inflate a layout from the apps resources and then change the text view to signify the position.
         */
        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            // Inflate a new layout from our resources

            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_editor, container, false);
            view.setTag(position);

            createButtons(view, position);


            buttonWU.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    DialogFragment newFragment = new TimeDialog.WarmUpTimeDialog();
                    Bundle args = new Bundle();
                    args.putInt("min", WorkoutModel.getInstance().getWorkoutCached(position).getWarmupMin());
                    args.putInt("sec", WorkoutModel.getInstance().getWorkoutCached(position).getWarmupSec());
                    args.putInt("id", position);
                    newFragment.setArguments(args);
                    newFragment.show(getActivity().getFragmentManager(), "tag");

                }
            });


            buttonRounds.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    DialogFragment newFragment = new RoundsDialog();
                    Bundle args = new Bundle();
                    args.putInt("rounds", WorkoutModel.getInstance().getWorkoutCached(position).getRoundAmount());
                    args.putInt("id", position);
                    newFragment.setArguments(args);
                    newFragment.show(getActivity().getFragmentManager(), "tag");
                }
            });

            buttonWork.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    DialogFragment newFragment = new TimeDialog.WorkTimeDialog();

                    Bundle args = new Bundle();
                    args.putInt("min", WorkoutModel.getInstance().getWorkoutCached(position).getWorkMin());
                    args.putInt("sec", WorkoutModel.getInstance().getWorkoutCached(position).getWorkSec());
                    args.putInt("id", position);
                    newFragment.setArguments(args);
                    newFragment.show(getActivity().getFragmentManager(), "tag");
                }

            });

            buttonRest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    DialogFragment newFragment = new TimeDialog.RestTimeDialog();

                    Bundle args = new Bundle();
                    args.putInt("min", WorkoutModel.getInstance().getWorkoutCached(position).getRestMin());
                    args.putInt("sec", WorkoutModel.getInstance().getWorkoutCached(position).getRestSec());
                    args.putInt("id", position);
                    newFragment.setArguments(args);
                    newFragment.show(getActivity().getFragmentManager(), "tag");
                }
            });



            buttonWU.setValueText(WorkoutModel.getInstance().getWorkoutCached(position).getWarmupInString());
            buttonWU.setTitleText("Warm-up time");

            if (WorkoutModel.getInstance().getWorkoutCached(position).getRoundAmount() == 0) {
                buttonRounds.setValueText("Unlimited");
            } else {
                buttonRounds.setValueText("" + WorkoutModel.getInstance().getWorkoutCached(position).getRoundAmount());
            }
            buttonRounds.setTitleText("Rounds");

            buttonWork.setValueText(WorkoutModel.getInstance().getWorkoutCached(position).getWorkInString());
            buttonWork.setTitleText("Work time");

            buttonRest.setValueText(WorkoutModel.getInstance().getWorkoutCached(position).getRestInString());
            buttonRest.setTitleText("Rest time");

            if (WorkoutModel.getInstance().getWorkoutCached(position).getManual()) {
                buttonRest.setAlpha(.5f);
                buttonRest.setClickable(false);
            } else {
                buttonRest.setClickable(true);
                buttonRest.setAlpha(255);
            }

            manualRestButton.setChecked(WorkoutModel.getInstance().getWorkoutCached(position).getManual());

            final View gg = view;

            manualRestButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Workout rs = WorkoutModel.getInstance().getWorkoutCached(position);
                    rs.setManual(isChecked);
                    WorkoutModel.getInstance().saveWorkout(rs, position);

                    CustomButton b = (CustomButton)gg.findViewWithTag("restbutton"+position);

                    if (b != null) {
                        if (isChecked) {
                            b.setAlpha(.5f);
                            b.setClickable(false);
                        } else {
                            b.setAlpha(255);
                            b.setClickable(true);
                        }

                        String total = WorkoutModel.getInstance().getWorkoutCached(position).getTotal();
                        TextView t = (TextView)gg.findViewWithTag("totalText"+position);
                        t.setText("Total duration: " + total);

                    }
                }
            });


            String total = WorkoutModel.getInstance().getWorkoutCached(position).getTotal();
            totalText.setText("Total duration: " + total);

            container.addView(view);

            // Return the View
            return view;
        }

        /**
         * Destroy the item from the {@link android.support.v4.view.ViewPager}. In our case this is simply removing the
         * {@link android.view.View}.
         */
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }
}

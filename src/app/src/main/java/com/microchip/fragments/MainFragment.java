package com.microchip.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.microchip.DataStore;
import com.microchip.R;
import com.microchip.api.Api;
import com.microchip.api.model.Status.Status;
import com.microchip.model.ResponseModel;
import com.microchip.provider.response.ResponseColumns;
import com.microchip.provider.response.ResponseCursor;
import com.microchip.utils.GGson;
import com.microchip.utils.NetworkConnection;
import com.microchip.widget.MicrochipCircleButton;
import com.microchip.widget.MicrochipCircleStatus;
import com.microchip.widget.MicrochipSwitch;

/**
 * Created by jossayjacobo on 11/4/14
 */
public class MainFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor>, CompoundButton.OnCheckedChangeListener {

    private final static int RESPONSE_CALLBACK_ID = 4231;
    private static final int POTENTIOMETER_ANIM_DURATION = 1200;

    LinearLayout buttonLabelContainer;
    LinearLayout buttonContainer;
    LinearLayout switchesLabelContainer;
    LinearLayout switchesContainer;

    TextView buttonLabel;
    MicrochipCircleButton buttonS1;
    MicrochipCircleButton buttonS2;
    MicrochipCircleButton buttonS3;
    MicrochipCircleButton buttonS4;

    TextView switchLabel;
    MicrochipSwitch switchD1;
    MicrochipSwitch switchD2;
    MicrochipSwitch switchD3;
    MicrochipSwitch switchD4;

    TextView potentiometerLabel;
    ProgressBar potentiometer;
    TextView potentiometerText;

    LinearLayout statusContainer;
    MicrochipCircleStatus statusCircle;
    TextView statusText;

    int buttonsContainerX;
    float switchesLabelsContainerX;

    MainListener listener;

    float defaultPadding;

    ResponseModel currentResponse;

    // Current States
    float currentPotentiometerValue;
    boolean currentLedSwitchValue1;
    boolean currentLedSwitchValue2;
    boolean currentLedSwitchValue3;
    boolean currentLedSwitchValue4;
    boolean currentButtonValue1;
    boolean currentButtonValue2;
    boolean currentButtonValue3;
    boolean currentButtonValue4;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        getActivity().getSupportLoaderManager().initLoader(RESPONSE_CALLBACK_ID, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_main,
                container, false);
        findViews(view);

        defaultPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());

        buttonContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    buttonContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    buttonContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                int[] position = new int[2];
                buttonContainer.getLocationOnScreen(position);
                buttonsContainerX = position[0];
            }
        });

        switchesLabelContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    switchesLabelContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    switchesLabelContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }

                int[] position = new int[2];
                switchesLabelContainer.getLocationOnScreen(position);
                switchesLabelsContainerX = position[0] + defaultPadding;

                listener.onSwitchesLabelXPosition(position[0]);
            }
        });

        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    private void setContent(ResponseModel response) {

        /**
         * Remove change listener before setting content to prevent post request to be sent
         * automatically.
         */
        switchD1.setOnCheckedChangeListener(null);
        switchD2.setOnCheckedChangeListener(null);
        switchD3.setOnCheckedChangeListener(null);
        switchD4.setOnCheckedChangeListener(null);

        Status status = null;
        if(!NetworkConnection.isConnected(getActivity())){

            statusCircle.setStatusOff();
            statusText.setText(getString(R.string.device_no_network));

        }else if(!DataStore.getAwsAmi(getActivity()).isEmpty() && !DataStore.getUuid(getActivity()).isEmpty() && response != null ){
            switch (response.statusCode){
                case ResponseModel.OK:
                    status = GGson.fromJson(response.responseStatus, Status.class);
                    statusCircle.setStatusGreen();
                    statusText.setText(getString(R.string.device_configuration));
                    break;

                case ResponseModel.BAD_REQUEST:
                case ResponseModel.INTERNAL_ERROR:
                    statusCircle.setStatusRed();
                    statusText.setText(getString(R.string.device_not_configured));
                    break;

                default:
                    statusCircle.setStatusOrange();
                    statusText.setText(getString(R.string.device_configuring));
            }
        }else{
            statusCircle.setStatusOff();
            statusText.setText(getString(R.string.device_not_configured));
        }

        if(status == null){

            currentLedSwitchValue1 = false;
            currentLedSwitchValue2 = false;
            currentLedSwitchValue3 = false;
            currentLedSwitchValue4 = false;
            currentButtonValue1 = false;
            currentButtonValue2 = false;
            currentButtonValue3 = false;
            currentButtonValue4 = false;
            currentPotentiometerValue = 0;

            switchD1.setChecked(false);
            switchD2.setChecked(false);
            switchD3.setChecked(false);
            switchD4.setChecked(false);
            buttonS1.setSelected(false);
            buttonS2.setSelected(false);
            buttonS3.setSelected(false);
            buttonS4.setSelected(false);
            setPotentiometerProgress(
                    potentiometer,
                    potentiometerText,
                    currentPotentiometerValue,
                    currentPotentiometerValue,
                    POTENTIOMETER_ANIM_DURATION);

        }else{
            if(currentLedSwitchValue1 != status.getLed1()){
                switchD1.setChecked(status.getLed1());
                currentLedSwitchValue1 = status.getLed1();
            }
            if(currentLedSwitchValue2 != status.getLed2()){
                switchD2.setChecked(status.getLed2());
                currentLedSwitchValue2 = status.getLed2();
            }
            if(currentLedSwitchValue3 != status.getLed3()){
                switchD3.setChecked(status.getLed3());
                currentLedSwitchValue3 = status.getLed3();
            }
            if(currentLedSwitchValue4 != status.getLed4()){
                switchD4.setChecked(status.getLed4());
                currentLedSwitchValue4 = status.getLed4();
            }
            if(currentButtonValue1 != status.getButton1()){
                buttonS1.setSelected(status.getButton1());
                currentButtonValue1 = status.getButton1();
            }
            if(currentButtonValue2 != status.getButton2()){
                buttonS2.setSelected(status.getButton2());
                currentButtonValue2 = status.getButton2();
            }
            if(currentButtonValue3 != status.getButton3()){
                buttonS3.setSelected(status.getButton3());
                currentButtonValue3 = status.getButton3();
            }
            if(currentButtonValue4 != status.getButton4()){
                buttonS4.setSelected(status.getButton4());
                currentButtonValue4 = status.getButton4();
            }
            if(currentPotentiometerValue != status.getPotentiometer()){
                setPotentiometerProgress(
                        potentiometer,
                        potentiometerText,
                        currentPotentiometerValue,
                        status.getPotentiometer(),
                        POTENTIOMETER_ANIM_DURATION);
                currentPotentiometerValue = status.getPotentiometer();
            }
        }

        /**
         * Re-set the check change listener to listen for user input.
         */
        switchD1.setOnCheckedChangeListener(this);
        switchD2.setOnCheckedChangeListener(this);
        switchD3.setOnCheckedChangeListener(this);
        switchD4.setOnCheckedChangeListener(this);
    }

    /**
     * Animate layouts into position using the slideOffSet of the navigation drawer
     *
     * @param slideOffset - offset percentage from 0 to 1
     */
    public void setSlideOffset(float slideOffset){

        // Shrink and Fade Switches Labels
        float scale = ((1.0f - slideOffset) * 0.1f) + 0.9f;
        switchesLabelContainer.animate().scaleX(scale).scaleY(scale).setDuration(0).start();
        switchesLabelContainer.setAlpha(1.0f - slideOffset);

        // Fade in/out the button and switch label
        buttonLabel.setAlpha(slideOffset);
        switchLabel.setAlpha(slideOffset);

        // Animate buttons into position
        buttonContainer.animate().translationX((switchesLabelsContainerX - buttonsContainerX) * slideOffset).setDuration(0).start();
        potentiometerLabel.animate().translationX((switchesLabelsContainerX - defaultPadding) * slideOffset).setDuration(0).start();

        // Animate and fade in/out into position with slight parallax
        buttonLabelContainer.animate().translationX((switchesLabelsContainerX - buttonsContainerX) * slideOffset * 0.9f).setDuration(0).start();
        buttonLabelContainer.setAlpha(1.0f - slideOffset);

        // Scale potentiometer (half it's size) and fade in/out
        float pScale = ((1.0f - slideOffset) * 0.5f) + 0.5f;
        potentiometer.animate().scaleX(pScale).scaleY(pScale).setDuration(0).start();
        potentiometer.setAlpha(1.0f - slideOffset);

        // Animate Potentiometer into position
        potentiometerText.animate().translationX((switchesLabelsContainerX * -1/6) * slideOffset).setDuration(0).start();

        // Animate Status Text into position
        statusContainer.animate().translationX((switchesLabelsContainerX - defaultPadding) * slideOffset).setDuration(0).start();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), ResponseColumns.CONTENT_URI, null, null, null,
                ResponseColumns.DEFAULT_ORDER + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        ResponseCursor responseCursor = new ResponseCursor(cursor);

        // Get the first non-post response
        if(responseCursor.moveToFirst()){
            while(!responseCursor.isAfterLast()){
                ResponseModel item = new ResponseModel(responseCursor);
                if(!item.methodType.equals(ResponseModel.POST)){
                    setContent(currentResponse);
                    currentResponse = item;
                    break;
                }
                responseCursor.moveToNext();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    public void updateContent(){
        setContent(currentResponse);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // User modified the state of a check box
        if(!DataStore.getAwsAmi(getActivity()).isEmpty() && !DataStore.getUuid(getActivity()).isEmpty()){
            Status status = new Status();
            status.setDeviceType(ResponseModel.DEVICE_TYPE_ANDROID);
            status.setUuid(DataStore.getUuid(getActivity()));
            status.setButton1(buttonS1.isSelected());
            status.setButton2(buttonS2.isSelected());
            status.setButton3(buttonS3.isSelected());
            status.setButton4(buttonS4.isSelected());
            status.setLed1(switchD1.isChecked());
            status.setLed2(switchD2.isChecked());
            status.setLed3(switchD3.isChecked());
            status.setLed4(switchD4.isChecked());
            status.setPotentiometer((int) currentPotentiometerValue);

            Api.postStatus(getActivity(), status);
        }
    }

    public interface MainListener{
        public void onSwitchesLabelXPosition(int x);
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        if(activity instanceof MainListener){
            listener = (MainListener) activity;
        }else{
            throw new ClassCastException(activity.toString() + " must implement MainListener");
        }
    }

    private void findViews(View view) {
        buttonLabelContainer = (LinearLayout) view.findViewById(R.id.buttons_label_container);
        buttonContainer = (LinearLayout) view.findViewById(R.id.buttons_container);
        switchesLabelContainer = (LinearLayout) view.findViewById(R.id.switch_label_container);
        switchesContainer = (LinearLayout) view.findViewById(R.id.switch_container);

        buttonLabel = (TextView) view.findViewById(R.id.buttons_column_label);
        buttonS1 = (MicrochipCircleButton) view.findViewById(R.id.button_1);
        buttonS2 = (MicrochipCircleButton) view.findViewById(R.id.button_2);
        buttonS3 = (MicrochipCircleButton) view.findViewById(R.id.button_3);
        buttonS4 = (MicrochipCircleButton) view.findViewById(R.id.button_4);

        switchLabel = (TextView) view.findViewById(R.id.switches_column_label);
        switchD1 = (MicrochipSwitch) view.findViewById(R.id.switch_d1);
        switchD2 = (MicrochipSwitch) view.findViewById(R.id.switch_d2);
        switchD3 = (MicrochipSwitch) view.findViewById(R.id.switch_d3);
        switchD4 = (MicrochipSwitch) view.findViewById(R.id.switch_d4);

        potentiometerLabel = (TextView) view.findViewById(R.id.potentiometer_label);
        potentiometer = (ProgressBar) view.findViewById(R.id.potentiometer_progressbar);
        potentiometerText = (TextView) view.findViewById(R.id.potentiometer_text);

        statusContainer = (LinearLayout) view.findViewById(R.id.status_container);
        statusCircle = (MicrochipCircleStatus) view.findViewById(R.id.status);
        statusText = (TextView) view.findViewById(R.id.status_text);
    }
}

package org.smartgresiter.wcaro.fragment;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.smartgresiter.wcaro.R;
import org.smartgresiter.wcaro.custom_view.HomeVisitGrowthAndNutrition;
import org.smartgresiter.wcaro.util.Constants;
import org.smartgresiter.wcaro.util.JsonFormUtils;
import org.smartgresiter.wcaro.util.Utils;
import org.smartgresiter.wcaro.util.WCAROServiceSchedule;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.immunization.domain.ServiceRecord;
import org.smartregister.immunization.domain.ServiceSchedule;
import org.smartregister.immunization.domain.ServiceWrapper;
import org.smartregister.immunization.repository.RecurringServiceRecordRepository;
import org.smartregister.util.DatePickerUtils;

import java.util.Calendar;
import java.util.Map;

@SuppressLint("ValidFragment")
public class GrowthNutritionInputFragment extends DialogFragment implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {


    public GrowthNutritionInputFragment(ServiceWrapper serviceWrapper) {
        this.serviceWrapper = serviceWrapper;

    }

    public static GrowthNutritionInputFragment getInstance(String title, String question, String type, ServiceWrapper serviceWrapper,
                                                           CommonPersonObjectClient commonPersonObjectClient) {
        GrowthNutritionInputFragment growthNutritionInputFragment = new GrowthNutritionInputFragment(serviceWrapper);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.INTENT_KEY.GROWTH_IMMUNIZATION_TYPE, type);
        bundle.putString(Constants.INTENT_KEY.GROWTH_TITLE, title);
        bundle.putString(Constants.INTENT_KEY.GROWTH_QUESTION, question);
        bundle.putSerializable(Constants.INTENT_KEY.CHILD_COMMON_PERSON, commonPersonObjectClient);
        growthNutritionInputFragment.setArguments(bundle);
        return growthNutritionInputFragment;
    }
  
    static final Map<String,Integer> imageMap=ImmutableMap.of(
            GROWTH_TYPE.VITAMIN.getValue(), R.drawable.ic_form_vitamin,
            GROWTH_TYPE.MNP.getValue(),R.drawable.ic_form_mnp,
            GROWTH_TYPE.DEWORMING.getValue(),R.drawable.ic_form_deworming
    );

    private TextView textViewTitle;
    private Button buttonSave,buttonSaveBf,buttonCancel;
    private RadioButton yesRadio,noRadio;

    private String type;
    private View layoutExclusiveFeeding, layoutVitaminBar;
    private TextView textViewVitamin;
    private DatePicker datePicker;
    private ImageView vitaminImage;
    private String isFeeding = "";
    private ServiceWrapper serviceWrapper;
    private CommonPersonObjectClient commonPersonObjectClient;
    private LinearLayout context;
    private ServiceWrapper saveService;
    String dob;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Light_NoActionBar);
    }

    @Override
    public void onStart() {
        super.onStart();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                getDialog().getWindow().setLayout(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_growth_nutrition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        textViewTitle = view.findViewById(R.id.textview_vaccine_title);
        buttonSave = view.findViewById(R.id.save_btn);
        buttonSaveBf=view.findViewById(R.id.save_bf_btn);
        buttonCancel= view.findViewById(R.id.cancel);
        yesRadio=view.findViewById(R.id.yes);
        noRadio=view.findViewById(R.id.no);
        layoutExclusiveFeeding = view.findViewById(R.id.exclusive_feeding_bar);
        layoutVitaminBar = view.findViewById(R.id.vitamin_a_bar);
        vitaminImage = view.findViewById(R.id.vitamin_image);
        textViewVitamin = view.findViewById(R.id.textview_vitamin);
        datePicker = view.findViewById(R.id.earlier_date_picker);
        DatePickerUtils.themeDatePicker(datePicker, new char[]{'d', 'm', 'y'});
        (view.findViewById(R.id.close)).setOnClickListener(this);
        buttonSaveBf.setOnClickListener(this);
        buttonSave.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);
        ((RadioGroup) view.findViewById(R.id.radio_group_exclusive)).setOnCheckedChangeListener(this);
        parseBundleAndSetData();

    }

    public void setContext(LinearLayout context) {
        this.context = context;
    }

    private void saveButtonDisable(boolean value) {
        if (value) {
            buttonSave.setAlpha(0.3f);
        } else {
            buttonSave.setAlpha(1.0f);
        }
    }

    private void parseBundleAndSetData() {
        type = getArguments().getString(Constants.INTENT_KEY.GROWTH_IMMUNIZATION_TYPE, GROWTH_TYPE.EXCLUSIVE.name());
        String title = getArguments().getString(Constants.INTENT_KEY.GROWTH_TITLE, getString(R.string.growth_and_nutrition));
        String question = getArguments().getString(Constants.INTENT_KEY.GROWTH_QUESTION, getString(R.string.growth_and_nutrition));
        question = StringUtils.capitalize(question);
        textViewTitle.setText(title);

        commonPersonObjectClient = (CommonPersonObjectClient) getArguments().getSerializable(Constants.INTENT_KEY.CHILD_COMMON_PERSON);
        dob = org.smartregister.family.util.Utils.getValue(commonPersonObjectClient.getColumnmaps(), DBConstants.KEY.DOB, false);

        if (type.equalsIgnoreCase(GROWTH_TYPE.EXCLUSIVE.getValue())) {
            if (serviceWrapper.getValue() != null && serviceWrapper.getValue().equalsIgnoreCase("yes")) {
                noRadio.setChecked(true);
                isFeeding = "yes";
                saveButtonDisable(false);
            } else if (serviceWrapper.getValue() != null && serviceWrapper.getValue().equalsIgnoreCase("no")) {
                yesRadio.setChecked(true);
                isFeeding = "no";
                saveButtonDisable(false);
            } else {
                saveButtonDisable(true);
            }
            visibleExclusiveBar();
        } else {
            updateDatePicker();
            textViewVitamin.setText(getString(R.string.vitamin_given, question));
            if (serviceWrapper.getUpdatedVaccineDate() != null) {
                datePicker.updateDate(serviceWrapper.getUpdatedVaccineDate().getYear(),
                        serviceWrapper.getUpdatedVaccineDate().getMonthOfYear() - 1, serviceWrapper.getUpdatedVaccineDate().getDayOfMonth());
            }
            visibleVitaminBar();
        }
    }

    private void updateDatePicker() {
        DateTime dateOfBirth = Utils.dobStringToDateTime(dob);
        DateTime dcToday = ServiceSchedule.standardiseDateTime(DateTime.now());
        DateTime minDate = ServiceSchedule.standardiseDateTime(dateOfBirth);
        DateTime maxDate = ServiceSchedule.standardiseDateTime(dcToday);

        datePicker.setMinDate(minDate.getMillis());
        datePicker.setMaxDate(maxDate.getMillis());

    }

    private void visibleExclusiveBar() {
        layoutExclusiveFeeding.setVisibility(View.VISIBLE);
        layoutVitaminBar.setVisibility(View.GONE);
        buttonCancel.setVisibility(View.GONE);

    }

    private void visibleVitaminBar() {
        layoutExclusiveFeeding.setVisibility(View.GONE);
        layoutVitaminBar.setVisibility(View.VISIBLE);
        buttonCancel.setVisibility(View.VISIBLE);
        vitaminImage.setImageResource(imageMap.get(type));
        saveButtonDisable(false);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {

            case R.id.yes:
                saveButtonDisable(false);
                isFeeding = "no";
                break;
            case R.id.no:
                saveButtonDisable(false);
                isFeeding = "yes";
                break;
        }
    }

    private void saveExclusiveFeedingData() {
        if (serviceWrapper == null || TextUtils.isEmpty(isFeeding)) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        DateTime dateTime = new DateTime(calendar.getTime());
        serviceWrapper.setValue(isFeeding);
        serviceWrapper.setUpdatedVaccineDate(dateTime, true);

        ServiceWrapper[] arrayTags = {serviceWrapper};
        SaveServiceTask backgroundTask = new SaveServiceTask();
        String providerId = ImmunizationLibrary.getInstance().context().allSharedPreferences().fetchRegisteredANM();

        backgroundTask.setProviderId(providerId);
        Utils.startAsyncTask(backgroundTask, arrayTags);
    }

    private void saveVitaminAData() {
        if (serviceWrapper == null) {
            return;
        }
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        DateTime dateTime = new DateTime(calendar.getTime());

        serviceWrapper.setUpdatedVaccineDate(dateTime, false);
        ServiceWrapper[] arrayTags = {serviceWrapper};
        SaveServiceTask backgroundTask = new SaveServiceTask();
        String providerId = ImmunizationLibrary.getInstance().context().allSharedPreferences().fetchRegisteredANM();

        backgroundTask.setProviderId(providerId);
        Utils.startAsyncTask(backgroundTask, arrayTags);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save_bf_btn:
            case R.id.save_btn:
                if (type.equalsIgnoreCase(GROWTH_TYPE.EXCLUSIVE.getValue())) {
                    saveExclusiveFeedingData();
                } else {
                    saveVitaminAData();
                }

                break;
            case R.id.close:
                dismiss();
                break;
            case R.id.cancel:
                if (context instanceof HomeVisitGrowthAndNutrition && serviceWrapper != null) {
                    HomeVisitGrowthAndNutrition homeVisitGrowthAndNutrition = (HomeVisitGrowthAndNutrition) context;
                    homeVisitGrowthAndNutrition.notVisitSetState(type, serviceWrapper);

                }
                dismiss();
                break;
        }
    }

    public enum GROWTH_TYPE {
        EXCLUSIVE("Exclusive breastfeeding"), MNP("MNP"), VITAMIN("Vitamin A"), DEWORMING("Deworming");
        private String value;

        GROWTH_TYPE(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public class SaveServiceTask extends AsyncTask<ServiceWrapper, Void, ServiceWrapper> {

        private String providerId;


        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }

        @Override
        protected void onPostExecute(ServiceWrapper serviceWrapper) {
            saveService = serviceWrapper;
            dismiss();
        }

        @Override
        protected ServiceWrapper doInBackground(ServiceWrapper... params) {

            //ArrayList<ServiceWrapper> list = new ArrayList<>();
            ServiceWrapper serviceWrapper = null;

            for (ServiceWrapper tag : params) {
                saveService(tag, commonPersonObjectClient.entityId(), providerId, null);
                //list.add(tag);
                //serviceId=tag.getServiceType().getId()+"";
                //tag.getDbKey();
                WCAROServiceSchedule.updateOfflineAlerts(tag.getType(), commonPersonObjectClient.entityId(), Utils.dobToDateTime(commonPersonObjectClient));
                serviceWrapper = tag;
            }
            return serviceWrapper;

//            RecurringServiceRecordRepository recurringServiceRecordRepository = ImmunizationLibrary.getInstance().recurringServiceRecordRepository();
//            List<ServiceRecord> serviceRecordList = recurringServiceRecordRepository.findByEntityId(commonPersonObjectClient.entityId());
//
//            RecurringServiceTypeRepository recurringServiceTypeRepository = ImmunizationLibrary.getInstance().recurringServiceTypeRepository();
//            List<ServiceType> serviceTypes = recurringServiceTypeRepository.fetchAll();
//            String[] alertArray = VaccinateActionUtils.allAlertNames(serviceTypes);
//
//            AlertService alertService = ImmunizationLibrary.getInstance().context().alertService();
//            List<Alert> alertList = alertService.findByEntityIdAndAlertNames(commonPersonObjectClient.entityId(), alertArray);
//
//            return Triple.of(list, serviceRecordList, alertList);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (context instanceof HomeVisitGrowthAndNutrition && saveService != null) {
            HomeVisitGrowthAndNutrition homeVisitGrowthAndNutrition = (HomeVisitGrowthAndNutrition) context;
            homeVisitGrowthAndNutrition.setState(type, saveService);

        }
    }

    public static void saveService(ServiceWrapper tag, String baseEntityId, String providerId, String locationId) {
        if (tag.getUpdatedVaccineDate() == null) {
            return;
        }

        RecurringServiceRecordRepository recurringServiceRecordRepository = ImmunizationLibrary.getInstance().recurringServiceRecordRepository();

        ServiceRecord serviceRecord = new ServiceRecord();
        if (tag.getDbKey() != null) {
            serviceRecord = recurringServiceRecordRepository.find(tag.getDbKey());
            serviceRecord.setDate(tag.getUpdatedVaccineDate().toDate());
            serviceRecord.setValue(tag.getValue());
        } else {
            serviceRecord.setDate(tag.getUpdatedVaccineDate().toDate());

            serviceRecord.setBaseEntityId(baseEntityId);
            serviceRecord.setRecurringServiceId(tag.getTypeId());
            serviceRecord.setDate(tag.getUpdatedVaccineDate().toDate());
            serviceRecord.setValue(tag.getValue());

            JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), serviceRecord);

        }

        recurringServiceRecordRepository.add(serviceRecord);
        tag.setDbKey(serviceRecord.getId());
    }
}

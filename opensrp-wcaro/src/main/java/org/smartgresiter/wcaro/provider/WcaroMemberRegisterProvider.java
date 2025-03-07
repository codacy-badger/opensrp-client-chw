package org.smartgresiter.wcaro.provider;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RelativeLayout;

import org.jeasy.rules.api.Rules;
import org.smartgresiter.wcaro.R;
import org.smartgresiter.wcaro.application.WcaroApplication;
import org.smartgresiter.wcaro.interactor.ChildProfileInteractor;
import org.smartgresiter.wcaro.util.ChildDBConstants;
import org.smartgresiter.wcaro.util.ChildUtils;
import org.smartgresiter.wcaro.util.ChildVisit;
import org.smartgresiter.wcaro.util.Constants;
import org.smartgresiter.wcaro.util.Utils;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.family.provider.FamilyMemberRegisterProvider;
import org.smartregister.family.util.DBConstants;
import org.smartregister.view.contract.SmartRegisterClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WcaroMemberRegisterProvider extends FamilyMemberRegisterProvider {
    private static final String TAG = WcaroMemberRegisterProvider.class.getCanonicalName();
    private Context context;

    public WcaroMemberRegisterProvider(Context context, CommonRepository commonRepository, Set visibleColumns, View.OnClickListener onClickListener, View.OnClickListener paginationClickListener, String familyHead, String primaryCaregiver) {
        super(context, commonRepository, visibleColumns, onClickListener, paginationClickListener, familyHead, primaryCaregiver);
        this.context = context;
    }

    @Override
    public void getView(Cursor cursor, SmartRegisterClient client, RegisterViewHolder viewHolder) {
        super.getView(cursor, client, viewHolder);

        // Update UI cutoffs
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) viewHolder.profile.getLayoutParams();
        layoutParams.width = context.getResources().getDimensionPixelSize(R.dimen.member_profile_pic_width);
        layoutParams.height = context.getResources().getDimensionPixelSize(R.dimen.member_profile_pic_width);
        viewHolder.profile.setLayoutParams(layoutParams);
        viewHolder.patientNameAge.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimensionPixelSize(R.dimen.member_profile_list_title_size));

        CommonPersonObjectClient pc = (CommonPersonObjectClient) client;

        viewHolder.statusLayout.setVisibility(View.GONE);
        viewHolder.status.setVisibility(View.GONE);

        String entityType = Utils.getValue(pc.getColumnmaps(), ChildDBConstants.KEY.ENTITY_TYPE, false);
        if (Constants.TABLE_NAME.CHILD.equals(entityType)) {
            Utils.startAsyncTask(new UpdateAsyncTask(viewHolder, pc), null);
        }

    }

    private Map<String, String> getChildDetails(String baseEntityId) {
        SmartRegisterQueryBuilder queryBUilder = new SmartRegisterQueryBuilder();
        queryBUilder.SelectInitiateMainTable(CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD), new String[]{CommonFtsObject.idColumn, ChildDBConstants.KEY.LAST_HOME_VISIT, ChildDBConstants.KEY.VISIT_NOT_DONE, ChildDBConstants.KEY.DATE_CREATED});
        String query = queryBUilder.mainCondition(String.format(" %s is null AND %s = '%s' ",
                DBConstants.KEY.DATE_REMOVED,
                CommonFtsObject.idColumn,
                baseEntityId));

        query = query.replace(CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD) + ".id as _id ,", "");

        CommonRepository commonRepository = Utils.context().commonrepository(Constants.TABLE_NAME.CHILD);
        List<Map<String, String>> res = new ArrayList<>();

        Cursor cursor = commonRepository.queryTable(query);
        try {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                int columncount = cursor.getColumnCount();
                Map<String, String> columns = new HashMap<>();
                for (int i = 0; i < columncount; i++) {
                    columns.put(cursor.getColumnName(i), cursor.getType(i) == Cursor.FIELD_TYPE_NULL ? null : String.valueOf(cursor.getString(i)));
                }
                res.add(columns);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            cursor.close();
        }

        if (res.isEmpty()) {
            return null;
        }
        return res.get(0);
    }

    private ChildVisit retrieveChildVisitList(Rules rules, CommonPersonObjectClient pc, Map<String, String> map) {
        String dob = Utils.getValue(pc.getColumnmaps(), DBConstants.KEY.DOB, false);
        String dobString = Utils.getDuration(dob);
        String lastVisitDate = map.get(ChildDBConstants.KEY.LAST_HOME_VISIT);
        String visitNotDone = map.get(ChildDBConstants.KEY.VISIT_NOT_DONE);
        String strDateCreated = map.get(ChildDBConstants.KEY.DATE_CREATED);
        long lastVisit = 0, visitNot = 0, dateCreated = 0;
        if (!TextUtils.isEmpty(lastVisitDate)) {
            lastVisit = Long.valueOf(lastVisitDate);
        }
        if (!TextUtils.isEmpty(visitNotDone)) {
            visitNot = Long.valueOf(visitNotDone);
        }
        if (!TextUtils.isEmpty(strDateCreated)) {
            dateCreated = org.smartregister.family.util.Utils.dobStringToDateTime(strDateCreated).getMillis();
        }
        return ChildUtils.getChildVisitStatus(rules, dobString, lastVisit, visitNot, dateCreated);
    }

    private void updateDueColumn(RegisterViewHolder viewHolder, ChildVisit childVisit) {
        viewHolder.statusLayout.setVisibility(View.VISIBLE);
        if (childVisit != null) {
            viewHolder.status.setVisibility(View.VISIBLE);
            if (childVisit.getVisitStatus().equalsIgnoreCase(ChildProfileInteractor.VisitType.DUE.name())) {
                viewHolder.status.setImageResource(Utils.getDueProfileImageResourceIDentifier());
            } else if (childVisit.getVisitStatus().equalsIgnoreCase(ChildProfileInteractor.VisitType.OVERDUE.name())) {
                viewHolder.status.setImageResource(Utils.getOverDueProfileImageResourceIDentifier());
            } else {
                viewHolder.status.setVisibility(View.INVISIBLE);
            }
        } else {
            viewHolder.status.setVisibility(View.INVISIBLE);
        }
    }

    ////////////////////////////////////////////////////////////////
    // Inner classes
    ////////////////////////////////////////////////////////////////

    private class UpdateAsyncTask extends AsyncTask<Void, Void, Void> {
        private final RegisterViewHolder viewHolder;
        private final CommonPersonObjectClient pc;

        private final Rules rules;

        private Map<String, String> map;
        private ChildVisit childVisit;

        private UpdateAsyncTask(RegisterViewHolder viewHolder, CommonPersonObjectClient pc) {
            this.viewHolder = viewHolder;
            this.pc = pc;
            this.rules = WcaroApplication.getInstance().getRulesEngineHelper().rules(Constants.RULE_FILE.HOME_VISIT);
        }

        @Override
        protected Void doInBackground(Void... params) {
            map = getChildDetails(pc.getCaseId());
            if (map != null) {
                childVisit = retrieveChildVisitList(rules, pc, map);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            // Update status column
            if (childVisit != null) {
                updateDueColumn(viewHolder, childVisit);
            }
        }
    }
}

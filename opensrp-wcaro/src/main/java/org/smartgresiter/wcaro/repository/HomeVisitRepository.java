package org.smartgresiter.wcaro.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.ei.drishti.dto.AlertStatus;
import org.json.JSONObject;
import org.smartgresiter.wcaro.domain.HomeVisit;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.domain.Alert;
import org.smartregister.immunization.ImmunizationLibrary;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;
import org.smartregister.service.AlertService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HomeVisitRepository extends BaseRepository {
    public static final String EVENT_TYPE = "Child Home Visit";
    public static final String NOT_DONE_EVENT_TYPE = "Visit not done";
    public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final String TAG = HomeVisitRepository.class.getCanonicalName();
    private static final String HomeVisit_SQL = "CREATE TABLE home_visit (_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,base_entity_id VARCHAR NOT NULL,name VARCHAR NOT NULL,date DATETIME NOT NULL,anmid VARCHAR NULL,location_id VARCHAR NULL,event_id VARCHAR NULL,formSubmissionId VARCHAR,sync_status VARCHAR,updated_at INTEGER NULL," +
            "formfields VARCHAR,created_at DATETIME NOT NULL,vaccine_group VARCHAR,single_vaccine VARCHAR,service VARCHAR,birth_certification VARCHAR,illness_information VARCHAR)";
    public static final String HomeVisitTABLE_NAME = "home_visit";
    public static final String ID_COLUMN = "_id";
    public static final String BASE_ENTITY_ID = "base_entity_id";
    public static final String EVENT_ID = "event_id";
    public static final String FORMSUBMISSION_ID = "formSubmissionId";
    public static final String NAME = "name";
    public static final String DATE = "date";
    public static final String ANMID = "anmid";
    public static final String LOCATIONID = "location_id";
    public static final String SYNC_STATUS = "sync_status";
    public static final String UPDATED_AT_COLUMN = "updated_at";
    public static final String FORMFIELDS = "formfields";
    public static final String CREATED_AT = "created_at";
    public static final String VACCCINE_GROUP = "vaccine_group";
    public static final String SINGLE_VACCINE = "single_vaccine";
    public static final String SERVICE = "service";
    public static final String BIRTH_CERTIFICATION = "birth_certification";
    public static final String illness_information = "illness_information";


    public static final String[] HomeVisit_TABLE_COLUMNS = {ID_COLUMN, BASE_ENTITY_ID, NAME, DATE, ANMID, LOCATIONID, SYNC_STATUS, UPDATED_AT_COLUMN, EVENT_ID, FORMSUBMISSION_ID, CREATED_AT, FORMFIELDS, VACCCINE_GROUP, SINGLE_VACCINE, SERVICE, BIRTH_CERTIFICATION, illness_information};

    private static final String BASE_ENTITY_ID_INDEX = "CREATE INDEX " + HomeVisitTABLE_NAME + "_" + BASE_ENTITY_ID + "_index ON " + HomeVisitTABLE_NAME + "(" + BASE_ENTITY_ID + " COLLATE NOCASE);";
    private static final String UPDATED_AT_INDEX = "CREATE INDEX " + HomeVisitTABLE_NAME + "_" + UPDATED_AT_COLUMN + "_index ON " + HomeVisitTABLE_NAME + "(" + UPDATED_AT_COLUMN + ");";
//
//    public static final String UPDATE_TABLE_ADD_EVENT_ID_COL = "ALTER TABLE " + COUNSELLING_TABLE_NAME + " ADD COLUMN " + EVENT_ID + " VARCHAR;";
//    public static final String EVENT_ID_INDEX = "CREATE INDEX " + COUNSELLING_TABLE_NAME + "_" + EVENT_ID + "_index ON " + COUNSELLING_TABLE_NAME + "(" + EVENT_ID + " COLLATE NOCASE);";
//
//    public static final String UPDATE_TABLE_ADD_FORMSUBMISSION_ID_COL = "ALTER TABLE " + COUNSELLING_TABLE_NAME + " ADD COLUMN " + FORMSUBMISSION_ID + " VARCHAR;";
//    public static final String FORMSUBMISSION_INDEX = "CREATE INDEX " + COUNSELLING_TABLE_NAME + "_" + FORMSUBMISSION_ID + "_index ON " + COUNSELLING_TABLE_NAME + "(" + FORMSUBMISSION_ID + " COLLATE NOCASE);";
//


    private CommonFtsObject commonFtsObject;
    private AlertService alertService;

    public HomeVisitRepository(Repository repository, CommonFtsObject commonFtsObject, AlertService alertService) {
        super(repository);
        this.commonFtsObject = commonFtsObject;
        this.alertService = alertService;
    }

    public static void createTable(SQLiteDatabase database) {
        database.execSQL(HomeVisit_SQL);
        database.execSQL(BASE_ENTITY_ID_INDEX);
        database.execSQL(UPDATED_AT_INDEX);
    }

    public void add(HomeVisit homeVisit) {
        if (homeVisit == null) {
            return;
        }

        try {


            if (StringUtils.isBlank(homeVisit.getSyncStatus())) {
                homeVisit.setSyncStatus(TYPE_Unsynced);
            }
            if (StringUtils.isBlank(homeVisit.getFormSubmissionId())) {
                homeVisit.setFormSubmissionId(generateRandomUUIDString());
            }

            if (homeVisit.getUpdatedAt() == null) {
                homeVisit.setUpdatedAt(Calendar.getInstance().getTimeInMillis());
            }

            SQLiteDatabase database = getWritableDatabase();
            if (homeVisit.getId() == null) {
                HomeVisit sameHomeVisit = findUnique(database, homeVisit);
                if (sameHomeVisit != null) {
                    homeVisit.setUpdatedAt(sameHomeVisit.getUpdatedAt());
                    homeVisit.setId(sameHomeVisit.getId());
                    update(database, homeVisit);
                } else {
                    if (homeVisit.getCreatedAt() == null) {
                        homeVisit.setCreatedAt(new Date());
                    }
                    Long id = database.insert(HomeVisitTABLE_NAME, null, createValuesFor(homeVisit));
                    homeVisit.setId(id);
                }
            } else {
                //mark the vaccine as unsynced for processing as an updated event
                homeVisit.setSyncStatus(TYPE_Unsynced);
                update(database, homeVisit);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        updateFtsSearch(homeVisit);
    }

    public void update(SQLiteDatabase database, HomeVisit homeVisit) {
        if (homeVisit == null || homeVisit.getId() == null) {
            return;
        }

        try {
            String idSelection = ID_COLUMN + " = ?";
            database.update(HomeVisitTABLE_NAME, createValuesFor(homeVisit), idSelection, new String[]{homeVisit.getId().toString()});
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public List<HomeVisit> findUnSyncedBeforeTime(int hours) {
        List<HomeVisit> homeVisits = new ArrayList<HomeVisit>();
        Cursor cursor = null;
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -hours);

            Long time = calendar.getTimeInMillis();

            cursor = getReadableDatabase().query(HomeVisitTABLE_NAME, HomeVisit_TABLE_COLUMNS, UPDATED_AT_COLUMN + " < ? AND " + SYNC_STATUS + " = ? ", new String[]{time.toString(), TYPE_Unsynced}, null, null, null, null);
            homeVisits = readAllCounsellings(cursor);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return homeVisits;
    }


    public List<HomeVisit> findByEntityId(String entityId) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(HomeVisitTABLE_NAME, HomeVisit_TABLE_COLUMNS, BASE_ENTITY_ID + " = ? " + COLLATE_NOCASE + " ORDER BY " + UPDATED_AT_COLUMN, new String[]{entityId}, null, null, null, null);
        return readAllCounsellings(cursor);
    }

    public HomeVisit find(Long caseId) {
        HomeVisit homeVisit = null;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(HomeVisitTABLE_NAME, HomeVisit_TABLE_COLUMNS, ID_COLUMN + " = ?", new String[]{caseId.toString()}, null, null, null, null);
            List<HomeVisit> vaccines = readAllCounsellings(cursor);
            if (!vaccines.isEmpty()) {
                homeVisit = vaccines.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return homeVisit;
    }

    public HomeVisit findUnique(SQLiteDatabase database, HomeVisit homeVisit) {
        if (homeVisit == null || (StringUtils.isBlank(homeVisit.getFormSubmissionId()) && StringUtils.isBlank(homeVisit.getEventId()))) {
            return null;
        }

        try {
            if (database == null) {
                database = getReadableDatabase();
            }

            String selection = null;
            String[] selectionArgs = null;
            if (StringUtils.isNotBlank(homeVisit.getFormSubmissionId()) && StringUtils.isNotBlank(homeVisit.getEventId())) {
                selection = FORMSUBMISSION_ID + " = ? " + COLLATE_NOCASE + " OR " + EVENT_ID + " = ? " + COLLATE_NOCASE;
                selectionArgs = new String[]{homeVisit.getFormSubmissionId(), homeVisit.getEventId()};
            } else if (StringUtils.isNotBlank(homeVisit.getEventId())) {
                selection = EVENT_ID + " = ? " + COLLATE_NOCASE;
                selectionArgs = new String[]{homeVisit.getEventId()};
            } else if (StringUtils.isNotBlank(homeVisit.getFormSubmissionId())) {
                selection = FORMSUBMISSION_ID + " = ? " + COLLATE_NOCASE;
                selectionArgs = new String[]{homeVisit.getFormSubmissionId()};
            }

            Cursor cursor = database.query(HomeVisitTABLE_NAME, HomeVisit_TABLE_COLUMNS, selection, selectionArgs, null, null, ID_COLUMN + " DESC ", null);
            List<HomeVisit> homeVisitList = readAllCounsellings(cursor);
            if (!homeVisitList.isEmpty()) {
                return homeVisitList.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return null;

    }


    public void deleteCounselling(Long caseId) {
        try {
            HomeVisit counselling = find(caseId);
            if (counselling != null) {
                getWritableDatabase().delete(HomeVisitTABLE_NAME, ID_COLUMN + "= ?", new String[]{caseId.toString()});

                updateFtsSearch(counselling.getBaseEntityId(), counselling.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public void close(Long caseId) {
        try {
            ContentValues values = new ContentValues();
            values.put(SYNC_STATUS, TYPE_Synced);
            getWritableDatabase().update(HomeVisitTABLE_NAME, values, ID_COLUMN + " = ?", new String[]{caseId.toString()});
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


    private List<HomeVisit> readAllCounsellings(Cursor cursor) {
        List<HomeVisit> homeVisits = new ArrayList<HomeVisit>();

        try {

            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    String vaccineName = cursor.getString(cursor.getColumnIndex(NAME));
                    if (vaccineName != null) {
                        vaccineName = removeHyphen(vaccineName);
                    }

                    Date createdAt = null;
                    String dateCreatedString = cursor.getString(cursor.getColumnIndex(CREATED_AT));
                    if (StringUtils.isNotBlank(dateCreatedString)) {
                        try {
                            createdAt = dateFormat.parse(dateCreatedString);
                        } catch (ParseException e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                    }
                    HomeVisit homeVisit = new HomeVisit(cursor.getLong(cursor.getColumnIndex(ID_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(BASE_ENTITY_ID)),
                            vaccineName,
                            new Date(cursor.getLong(cursor.getColumnIndex(DATE))),
                            cursor.getString(cursor.getColumnIndex(ANMID)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONID)),
                            cursor.getString(cursor.getColumnIndex(SYNC_STATUS)),
                            cursor.getLong(cursor.getColumnIndex(UPDATED_AT_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(EVENT_ID)),
                            cursor.getString(cursor.getColumnIndex(FORMSUBMISSION_ID)),
                            createdAt
                    );
                    int columnindex = cursor.getColumnIndex(FORMFIELDS);
                    homeVisit.setFormfields(new Gson().<Map<String, String>>fromJson(cursor.getString(cursor.getColumnIndex(FORMFIELDS)),
                            new TypeToken<Map<String, String>>() {
                            }.getType()));
                    homeVisits.add(homeVisit);
                    homeVisit.setVaccineGroupsGiven(new JSONObject(cursor.getString(cursor.getColumnIndex(VACCCINE_GROUP))));
                    homeVisit.setSingleVaccinesGiven(new JSONObject(cursor.getString(cursor.getColumnIndex(SINGLE_VACCINE))));
                    homeVisit.setServicesGiven(new JSONObject(cursor.getString(cursor.getColumnIndex(SERVICE))));
                    homeVisit.setBirthCertificationState((cursor.getString(cursor.getColumnIndex(BIRTH_CERTIFICATION))));
                    homeVisit.setIllness_information(new JSONObject(cursor.getString(cursor.getColumnIndex(illness_information))));

                    cursor.moveToNext();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return homeVisits;
    }


    private ContentValues createValuesFor(HomeVisit homeVisit) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, homeVisit.getId());
        values.put(BASE_ENTITY_ID, homeVisit.getBaseEntityId());
        values.put(NAME, homeVisit.getName() != null ? addHyphen(homeVisit.getName().toLowerCase()) : null);
        values.put(DATE, homeVisit.getDate() != null ? homeVisit.getDate().getTime() : null);
        values.put(ANMID, homeVisit.getAnmId());
        values.put(LOCATIONID, homeVisit.getLocationId());
        values.put(SYNC_STATUS, homeVisit.getSyncStatus());
        values.put(UPDATED_AT_COLUMN, homeVisit.getUpdatedAt() != null ? homeVisit.getUpdatedAt() : null);
        values.put(EVENT_ID, homeVisit.getEventId() != null ? homeVisit.getEventId() : null);
        values.put(FORMSUBMISSION_ID, homeVisit.getFormSubmissionId() != null ? homeVisit.getFormSubmissionId() : null);
        values.put(CREATED_AT, homeVisit.getCreatedAt() != null ? dateFormat.format(homeVisit.getCreatedAt()) : null);
        values.put(FORMFIELDS, new Gson().toJson(homeVisit.getFormfields()));
        values.put(VACCCINE_GROUP, homeVisit.getVaccineGroupsGiven().toString());
        values.put(SINGLE_VACCINE, homeVisit.getSingleVaccinesGiven().toString());
        values.put(SERVICE, homeVisit.getServicesGiven().toString());
        values.put(BIRTH_CERTIFICATION, homeVisit.getBirthCertificationState());
        values.put(illness_information, homeVisit.getIllness_information().toString());
        return values;
    }

    //-----------------------
    // FTS methods
    public void updateFtsSearch(HomeVisit homeVisit) {
        try {
            if (commonFtsObject != null && alertService() != null) {
                String entityId = homeVisit.getBaseEntityId();
                String vaccineName = homeVisit.getName();
                if (vaccineName != null) {
                    vaccineName = removeHyphen(vaccineName);
                }
                String scheduleName = commonFtsObject.getAlertScheduleName(vaccineName);

                String bindType = commonFtsObject.getAlertBindType(scheduleName);

                if (StringUtils.isNotBlank(bindType) && StringUtils.isNotBlank(scheduleName) && StringUtils.isNotBlank(entityId)) {
                    String field = addHyphen(scheduleName);
                    // update vaccine status
                    alertService().updateFtsSearchInACR(bindType, entityId, field, AlertStatus.complete.value());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

    }

    public void updateFtsSearch(String entityId, String vaccineName_) {
        String vaccineName = vaccineName_;
        try {
            if (commonFtsObject != null && alertService() != null) {
                if (vaccineName != null) {
                    vaccineName = removeHyphen(vaccineName);
                }

                String scheduleName = commonFtsObject.getAlertScheduleName(vaccineName);
                if (StringUtils.isNotBlank(entityId) && StringUtils.isNotBlank(scheduleName)) {
                    Alert alert = alertService().findByEntityIdAndScheduleName(entityId, scheduleName);
                    alertService().updateFtsSearch(alert, true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public AlertService alertService() {
        if (alertService == null) {
            alertService = ImmunizationLibrary.getInstance().context().alertService();
        }
        return alertService;
    }

    public static String addHyphen(String s) {
        if (StringUtils.isNotBlank(s)) {
            return s.replace(" ", "_");
        }
        return s;
    }

    public static String removeHyphen(String s) {
        if (StringUtils.isNotBlank(s)) {
            return s.replace("_", " ");
        }
        return s;
    }

    public static void migrateCreatedAt(SQLiteDatabase database) {
        try {
            String sql = "UPDATE " + HomeVisitTABLE_NAME +
                    " SET " + CREATED_AT + " = " +
                    " ( SELECT " + EventClientRepository.event_column.dateCreated.name() +
                    "   FROM " + EventClientRepository.Table.event.name() +
                    "   WHERE " + EventClientRepository.event_column.eventId.name() + " = " + HomeVisitTABLE_NAME + "." + EVENT_ID +
                    "   OR " + EventClientRepository.event_column.formSubmissionId.name() + " = " + HomeVisitTABLE_NAME + "." + FORMSUBMISSION_ID +
                    " ) " +
                    " WHERE " + CREATED_AT + " is null ";
            database.execSQL(sql);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}

package org.smartgresiter.wcaro.util;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.family.util.DBConstants;

public class ChildDBConstants {
    public static final class KEY {
        //public static final String VISIT_STATUS = "visit_status";
        public static final String VISIT_NOT_DONE = "visit_not_done";
        public static final String LAST_HOME_VISIT = "last_home_visit";
        public static final String DATE_CREATED = "date_created";
        public static final String RELATIONAL_ID = "relationalid";
        public static final String FAMILY_FIRST_NAME = "family_first_name";
        public static final String FAMILY_MIDDLE_NAME = "family_middle_name";
        public static final String FAMILY_LAST_NAME = "family_last_name";
        public static final String FAMILY_HOME_ADDRESS = "family_home_address";
        public static final String ENTITY_TYPE = "entity_type";
        public static final String CHILD_BF_HR = "early_bf_1hr";
        public static final String CHILD_PHYSICAL_CHANGE = "physically_challenged";
        public static final String BIRTH_CERT = "birth_cert";
        public static final String BIRTH_CERT_ISSUE_DATE = "birth_cert_issue_date";
        public static final String BIRTH_CERT_NUMBER = "birth_cert_num";
        public static final String BIRTH_CERT_NOTIFIICATION = "birth_notification";
        public static final String ILLNESS_DATE = "date_of_illness";
        public static final String ILLNESS_DESCRIPTION = "illness_description";
        public static final String ILLNESS_ACTION = "action_taken";
        public static final String EVENT_DATE = "event_date";
        public static final String EVENT_TYPE = "event_type";

        // Family child visit status
        //public static final String CHILD_VISIT_STATUS = "child_visit_status";
    }

    public static String childDueFilter() {
        return " ((" + ChildDBConstants.KEY.LAST_HOME_VISIT + " is null OR ((" + ChildDBConstants.KEY.LAST_HOME_VISIT + "/1000) > strftime('%s',datetime('now','start of month')))) AND ((" + ChildDBConstants.KEY.VISIT_NOT_DONE + " is null OR " + ChildDBConstants.KEY.VISIT_NOT_DONE + " = '0') OR ((" + ChildDBConstants.KEY.VISIT_NOT_DONE + "/1000) > strftime('%s',datetime('now','start of month'))))) ";
    }

    public static String childMainFilter(String mainCondition, String filters, String sort, int limit, int offset) {
        return "SELECT " + CommonFtsObject.idColumn + " FROM " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD) + " WHERE " + CommonFtsObject.idColumn + " IN " +
                " ( SELECT " + CommonFtsObject.idColumn + " FROM " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD) + " WHERE  " + mainCondition + "  AND " + CommonFtsObject.phraseColumn + matchPhrase(filters) +
                " UNION " +
                " SELECT " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD) + "." + CommonFtsObject.idColumn + " FROM " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD) +
                " JOIN " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.FAMILY) + " on " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.CHILD) + "." + CommonFtsObject.relationalIdColumn + " = " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.FAMILY) + "." + CommonFtsObject.idColumn +
                " JOIN " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.FAMILY_MEMBER) + " on " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.FAMILY_MEMBER) + "." + CommonFtsObject.idColumn + " = " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.FAMILY) + "." + DBConstants.KEY.PRIMARY_CAREGIVER +
                " WHERE  " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.FAMILY_MEMBER) + "." + mainCondition.trim() + " AND " + CommonFtsObject.searchTableName(Constants.TABLE_NAME.FAMILY_MEMBER) + "." + CommonFtsObject.phraseColumn + matchPhrase(filters) +
                ")  " + orderByClause(sort) + limitClause(limit, offset);
    }

    private static String matchPhrase(String phrase) {
        if (phrase == null) {
            phrase = "";
        }

        // Underscore does not work well in fts search
        if (phrase.contains("_")) {
            phrase = phrase.replace("_", "");
        }
        return " MATCH '" + phrase + "*' ";

    }


    private static String orderByClause(String sort) {
        if (StringUtils.isNotBlank(sort)) {
            return " ORDER BY " + sort;
        }
        return "";
    }

    private static String limitClause(int limit, int offset) {
        return " LIMIT " + offset + "," + limit;
    }

}

package com.example.lupworkmanager;

import android.provider.BaseColumns;

import java.sql.Date;

public class RegistroEsquemaBDD {

    public static abstract class RegistroEntry implements BaseColumns {
        public static final String TABLE_NAME ="lectures";

        public static final String ID = "id";
        public static final String EXECUTION_HOUR = "date";
        public static final String TEXT_DETECTED = "texto";
        public static final String EXECUTION_TIME = "time";

    }
}

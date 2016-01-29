package com.ucsf.core.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Class allowing data storage. It uses a SQLite database as internal storage.
 * Multiple connections to the database are allowed and its usage of this wrapper is thread safe.
 * See the method {@link DataManager#get(Context) get()} to have an example on how to use a
 * database connection.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public class DataManager extends AbstractConnection {
    public  static final String             KEY_ROW_ID       = "_id";
    public  static final String             KEY_PATIENT_ID   = "patient";
    public  static final String             KEY_TIMESTAMP    = "timestamp";
    public  static final String             KEY_IS_COMMITTED = "committed";
    private static final String             TAG              = "ucsf:DataManager";
    private static final int                DATABASE_VERSION = 49;
    private static final String             DATABASE_NAME    = "data.db";
    private static final Map<String, Table> mTables          = new HashMap<>();
    private static final DataManager        mInstance        = new DataManager();

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    /**
     * Returns an opened connection to the database.                                                <br/>
     * The typical way to use such a connection is the following:                                   <br/>
     *                                                                                              <br/><pre>
     * {@code
     *      try (DataManager instance = DataManager.open(context)) {
     *          // Perform operations on the database
     *      } catch (Exception e) {
     *          // Handle eventual exceptions
     *      }
     * }
     *                                                                                              </pre><br/>
     * By this way, the connection is automatically closed no matter what.
     */
    public static DataManager get(Context context) throws Exception {
        return (DataManager) mInstance.open(context);
    }

    @Override
    protected void openConnection(Context context) {
        if (mDbHelper == null)
            mDbHelper = new DatabaseHelper(context.getApplicationContext());
        mDb = mDbHelper.getWritableDatabase();
    }

    @Override
    protected void closeConnection() {
        mDb.close();
    }

    /**
     * Returns the database table register under the given tag.
     */
    public static Table getTable(String tag) {
        return mTables.get(tag);
    }

    /**
     * Returns an array containing all the registered tables. Because the application has several
     * entry points, this list may not be exhaustive.
     */
    public static Table[] getTables() {
        Table[] tables = new Table[mTables.size()];
        int i = 0;
        for (Table table : mTables.values())
            tables[i++] = table;
        return tables;
    }

    /**
     * Creates a new database table using the provided information.
     * @param tag       Unique identifier of the table.
     * @param location  Specify from which device this table is coming.
     * @param fields    List of the table fields. See {@link DataManager.TableField}
     *                  for more details.
     * @return          Returns the new created table.
     */
    public Table createTable(String tag, DeviceLocation location, TableField... fields)
            throws Exception
    {
        synchronized (mTables) {
            Table table = new Table(tag, location, fields);
            if (mTables.put(table.tag, table) != null)
                throw new Exception(String.format("A table with the tag '%s' already exists!", tag));
            checkDatabaseTable(table);
            return table;
        }
    }

    /**
     * Formats the given table field definition to a valid SQLite format.
     */
    private static String formatColumnDef(TableField field) {
        if (field.defaultValue == null)
            return String.format("%s %s", field.tag, field.type.toString());
        return String.format("%s %s DEFAULT %s", field.tag, field.type.toString(),
                field.defaultValue.toString());
    }

    /**
     * Adds the given entries associated to the database table identified by the given id.
     * See {@link DataManager.Table#add(Entry... entries)} for more details.
     * @param tag Unique identifier of the table.
     * @return Returns if the operation is successful.
     */
    public boolean add(String tag, Entry... entries) throws Exception {
        Table table = mTables.get(tag);
        return table != null && table.add(entries);
    }

    /**
     * Fetches all entries matching the given conditions for the table identified by the given id.
     * See {@link DataManager.Table#fetch(Condition... conditions)} for more details.
     * @param tag Unique identifier of the table.
     * @return Returns null if an error occurs or a cursor pointing to the requested entries.
     * @see DataManager.Cursor
     */
    public Cursor fetch(String tag, Condition... conditions) throws Exception {
        Table table = mTables.get(tag);
        if (table == null)
            return null;
        return table.fetch(conditions);
    }

    /**
     * Fetches entries of the given types matching the given conditions for the table identified by
     * the given id.
     * See {@link DataManager.Table#fetch(String[] entriesTags, Condition... conditions)} for more
     * details.
     * @param tag Unique identifier of the table.
     * @return Returns null if an error occurs or a cursor pointing to the requested entries.
     * @see DataManager.Cursor
     */
    public Cursor fetch(String tag, String[] entriesTags, Condition... conditions)
            throws Exception
    {
        Table table = mTables.get(tag);
        if (table == null)
            return null;
        return table.fetch(entriesTags, conditions);
    }

    /**
     * Fetches modified entries of the given types matching the given conditions for the table
     * identified by the given id.
     * See {@link DataManager.Table#fetch(String[] entriesTags, String[] modifiers,
     * Condition... conditions)} for more details.
     * @param tag Unique identifier of the table.
     * @return Returns null if an error occurs or a cursor pointing to the requested entries.
     * @see DataManager.Cursor
     */
    public Cursor fetch(String tag, String[] entriesTags, String[] modifiers,
                        Condition... conditions) throws Exception
    {
        Table table = mTables.get(tag);
        if (table == null)
            return null;
        return table.fetch(entriesTags, modifiers, conditions);
    }

    /**
     * Updates entries matching the given conditions with the given values for the table
     * identified by the given id.
     * See {@link DataManager.Table#update(Entry[] entries, Condition... conditions)} for more
     * details.
     * @param tag Unique identifier of the table.
     * @return Returns if the operation is successful.
     */
    public boolean update(String tag, Entry[] entries, Condition... conditions) throws Exception {
        Table table = mTables.get(tag);
        return table != null && table.update(entries, conditions);
    }

    /**
     * Fetches if the given entries exist. If true, update them. Otherwise add them.
     * identified by the given id.
     * See {@link DataManager.Table#fetchAndAdd(Entry[] conditions, Entry... entries)} for more
     * details.
     * @param tag Unique identifier of the table.
     * @return Returns if the operation is successful.
     */
    public boolean fetchAndAdd(String tag, Entry[] conditions, Entry... entries) throws Exception {
        Table table = mTables.get(tag);
        return table != null && table.fetchAndAdd(conditions, entries);
    }

    /**
     * Removes entries matching the given conditions for the table identified by the given id.
     * See {@link DataManager.Table#erase(Condition...)} for more details.
     * @param tag Unique identifier of the table.
     * @return Returns if the operation is successful.
     */
    public boolean erase(String tag, Condition... conditions) throws Exception {
        Table table = mTables.get(tag);
        return table != null && table.erase(conditions);
    }

    /**
     * Makes sure that the definition of the given table matches the database table.
     */
    private void checkDatabaseTable(Table table) {
        // Get column names and types
        android.database.Cursor cursor =
                mDb.rawQuery(String.format("PRAGMA table_info('%s')", table.tag),
                        new String[]{});

        // If the table exists, update its columns
        if (cursor != null && cursor.moveToFirst()) {
            Set<String> columnNames = new HashSet<>();
            Set<String> toDrop = new HashSet<>();
            Set<TableField> toAlter = new HashSet<>();
            Set<TableField> toAdd = new HashSet<>();

            do {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                TableField field = null;
                if (name.equals(KEY_ROW_ID)) // Skip the id column
                    continue;

                // Check if the column name is in the table
                for (TableField f : table.fields) {
                    if (f.tag.equals(name)) {
                        columnNames.add(name);
                        field = f;
                        break;
                    }
                }

                if (field != null) { // The column is still a valid column
                    // Check if the column definition has changed
                    String columnType = cursor.getString(cursor.getColumnIndex("type"));
                    if (field.type.toString().contains(columnType)) {
                        // TODO Check default value?
                    } else {
                        toAlter.add(field);
                    }
                } else  // The column is no longer present in the current table
                    toDrop.add(name);
            } while (cursor.moveToNext());

            // Insert missing columns
            for (TableField field : table.fields) {
                if (!columnNames.contains(field.tag))
                    toAdd.add(field);
            }

            // Apply changes
            if (!toAdd.isEmpty() || !toAlter.isEmpty() || !toDrop.isEmpty()) {
                if (toAlter.isEmpty() && toDrop.isEmpty()) {
                    // Add the missing columns to the table
                    for (TableField field : toAdd) {
                        Log.i(TAG, String.format("Insertion of a new column '%s' in table '%s'",
                                field.tag, table.tag));

                        String sql = String.format("ALTER TABLE %s ADD COLUMN %s", table.tag,
                                formatColumnDef(field));
                        mDb.execSQL(sql);
                    }
                } else {
                    // Some columns need to be modified, therefore we have to make a copy
                    // of the table.
                    Log.i(TAG, String.format("Redefinition of table '%s'", table.tag));

                    // First, we rename the table to a temporary name
                    String tmpName = String.format("__tmp_%s_table", table.tag);
                    mDb.execSQL(String.format("ALTER TABLE %s RENAME TO %s", table.tag, tmpName));

                    // Create the new table
                    createDatabaseTable(table);

                    // Copy each entries from the old table to the new one
                    StringBuilder sql = new StringBuilder("INSERT INTO ")
                            .append(table.tag).append(" (").append(KEY_ROW_ID);
                    for (String column : columnNames)
                        sql.append(", ").append(column);
                    sql.append(") SELECT ").append(KEY_ROW_ID);
                    for (String column : columnNames)
                        sql.append(", ").append(column);
                    sql.append(" FROM ").append(tmpName);
                    mDb.execSQL(sql.toString());

                    // Delete the old table
                    mDb.execSQL(String.format("DROP TABLE %s", tmpName));
                }
            }
        } else { // Otherwise add the table to the database
            Log.i(TAG, String.format("Insertion of a new table: '%s'", table.tag));
            createDatabaseTable(table);
        }
    }

    /**
     * Creates a new table in the database using the given definition.
     */
    private void createDatabaseTable(final Table table) {
        StringBuilder request = new StringBuilder(
                String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY ASC",
                        table.tag, KEY_ROW_ID));

        for (final TableField field : table.fields)
            request.append(", ").append(formatColumnDef(field));
        request.append(");");

        mDb.execSQL(request.toString());
    }

    /**
     * Enumeration od the possible field types in the database.
     */
    public enum Type {
        Text {
            @Override
            public String toString() {
                return "TEXT";
            }
        },
        UniqueText {
            @Override
            public String toString() {
                return "TEXT UNIQUE";
            }
        },
        Real {
            @Override
            public String toString() {
                return "REAL";
            }
        },
        Integer {
            @Override
            public String toString() {
                return "INTEGER";
            }
        },
        Boolean {
            @Override
            public String toString() {
                return "BOOLEAN";
            }
        },
        Long {
            @Override
            public String toString() {
                return "BIGINT";
            }
        },
        Blob {
            @Override
            public String toString() {
                return "BLOB";
            }
        },
    }

    /**
     * Representation of a table field. Basically, defines a database column by an unique identifier
     * and a data type (see {@link DataManager.Type}). Can also have a default value.
     */
    public static class TableField implements Serializable {
        public final String       tag;          /**< Unique identifier (for a given table) of the field. */
        public final Type         type;         /**< Data type of the field. */
        public final Serializable defaultValue; /**< Optional default value for the field. Can be null. */

        /**
         * Create a new TableField using the given information.
         */
        public TableField(String tag, Type type, Serializable defaultValue) {
            this.type = type;
            this.tag = tag;
            this.defaultValue = defaultValue;
        }

        /**
         * Create a new TableField using the given information.
         */
        public TableField(String tag, Type type) {
            this(tag, type, null);
        }
    }

    /**
     * Abstract class describing a condition to satisfy when fetching the database.
     */
    public static abstract class Condition implements Serializable {
        /**
         * Condition for which the given field has to match the given value.
         */
        public static class Equal<Type> extends Condition {
            public final String tag;    /**< Unique identifier of the field to match. */
            public final Type   value;  /**< Value to match. */

            public Equal(String tag, Type value) {
                this.tag = tag;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s=\"%s\"", tag, value.toString());
            }
        }

        /**
         * Condition for which the given field has to be smaller than the given value.
         * The order used is defined by the underlying database, here MySql.
         */
        public static class Less<Type> extends Condition {
            public final String tag;    /**< Unique identifier of the field to compare. */
            public final Type   value;  /**< Value to compare with. */

            public Less(String tag, Type value) {
                this.tag = tag;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s<\"%s\"", tag, value.toString());
            }
        }

        /**
         * Condition for which the given field has to be smaller or equal than the given value.
         * The order used is defined by the underlying database, here MySql.
         */
        public static class LessEqual<Type> extends Condition {
            public final String tag;    /**< Unique identifier of the field to compare. */
            public final Type   value;  /**< Value to compare with. */

            public LessEqual(String tag, Type value) {
                this.tag = tag;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s<=\"%s\"", tag, value.toString());
            }
        }

        /**
         * Condition for which the given field has to be greater than the given value.
         * The order used is defined by the underlying database, here MySql.
         */
        public static class Greater<Type> extends Condition {
            public final String tag;    /**< Unique identifier of the field to compare. */
            public final Type   value;  /**< Value to compare with. */

            public Greater(String tag, Type value) {
                this.tag = tag;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s>\"%s\"", tag, value.toString());
            }
        }

        /**
         * Condition for which the given field has to be greater or equal than the given value.
         * The order used is defined by the underlying database, here MySql.
         */
        public static class GreaterEqual<Type> extends Condition {
            public final String tag;    /**< Unique identifier of the field to compare. */
            public final Type   value;  /**< Value to compare with. */

            public GreaterEqual(String tag, Type value) {
                this.tag = tag;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("%s>=\"%s\"", tag, value.toString());
            }
        }

        /**
         * Condition for which the given field has to be included in the given range.
         * The order used and if the bounds are included in the result are defined by the
         * underlying database, here MySql.
         */
        public static class Range<Type> extends Condition {
            public final String tag;   /**< Unique identifier of the field to compare. */
            public final Type   first; /**< Lower bound of the range to check. */
            public final Type   last;  /**< Upper bound of the range to check. */

            public Range(String tag, Type first, Type last) {
                this.tag = tag;
                this.first = first;
                this.last = last;
            }

            @Override
            public String toString() {
                return String.format("%s BETWEEN \"%s\" AND \"%s\"", tag,
                        first.toString(), last.toString());
            }
        }
    }

    /**
     * Class allowing to iterate through requested entries. A typical use is:
     * <pre>
     * {@code
     *      Cursor cursor = instance.fetch(...);
     *
     *      // Check that the cursor is valid and that there is at least one result
     *      if (cursor != null && cursor.moveToFirst()) {
     *          do {
     *              // Access the current entry values through the cursor
     *          } while (cursor.moveToNext()); // Move to the next entry
     *      }
     * }
     * </pre>
     */
    public static class Cursor {
        private android.database.Cursor mCursor;

        private Cursor(android.database.Cursor cursor) {
            mCursor = cursor;
        }

        /**
         * Initializes the cursor to point on its first entry. Has to be called first for a
         * forward iteration.
         * @return Returns if there is at least one entry.
         */
        public boolean moveToFirst() {
            return mCursor.moveToFirst();
        }

        /**
         * Initializes the cursor to point on its last entry. Has to be called first for a
         * backward iteration.
         * @return Returns if there is at least one entry.
         */
        public boolean moveToLast() {
            return mCursor.moveToLast();
        }

        /**
         * Moves the cursor to point to the next entry.
         * @return Returns false if the underlying list of entries don't have a next element.
         */
        public boolean moveToNext() {
            return mCursor.moveToNext();
        }

        /**
         * Moves the cursor to point to the previous entry.
         * @return Returns false if the underlying list of entries don't have a previous element.
         */
        public boolean moveToPrevious() {
            return mCursor.moveToPrevious();
        }

        /**
         * Returns the underlying cursor and invalidate itself.
         */
        public android.database.Cursor delegateCursor() {
            android.database.Cursor cursor = mCursor;
            mCursor = null;
            return cursor;
        }

        /**
         * Returns the number of entries of the underlying list.
         */
        public int getCount() {
            return mCursor.getCount();
        }

        /**
         * Returns the value (as a String) of the given field.
         * @param tag Unique identifier of the desired field.
         */
        public String getString(String tag) {
            return mCursor.getString(mCursor.getColumnIndex(tag));
        }

        /**
         * Returns the value (as a double) of the given field.
         * @param index Table index of the desired field.
         */
        public double getDouble(int index) {
            return mCursor.getDouble(index);
        }

        /**
         * Returns the value (as a double) of the given field.
         * @param tag Unique identifier of the desired field.
         */
        public double getDouble(String tag) {
            return getDouble(mCursor.getColumnIndex(tag));
        }

        /**
         * Returns the value (as a long) of the given field.
         * @param tag Unique identifier of the desired field.
         */
        public long getLong(String tag) {
            return mCursor.getLong(mCursor.getColumnIndex(tag));
        }

        /**
         * Returns the value (as an integer) of the given field.
         * @param tag Unique identifier of the desired field.
         */
        public int getInt(String tag) {
            return mCursor.getInt(mCursor.getColumnIndex(tag));
        }

        /**
         * Returns the value (as a boolean) of the given field.
         * @param tag Unique identifier of the desired field.
         */
        public boolean getBoolean(String tag) {
            return getBoolean(mCursor.getColumnIndex(tag));
        }

        /**
         * Returns the value (as a boolean) of the given field.
         * @param index Table index of the desired field.
         */
        public boolean getBoolean(int index) {
            return mCursor.getInt(index) != 0 || mCursor.getString(index).equalsIgnoreCase("true");
        }

        /**
         * Returns the value (as a bytes array) of the given field.
         * @param tag Unique identifier of the desired field.
         */
        public byte[] getBlob(String tag) {
            return mCursor.getBlob(mCursor.getColumnIndex(tag));
        }

        /**
         * Returns the value of the given field. Used for complex objects that need serialization.
         * @param tag Unique identifier of the desired field.
         */
        public Serializable getSerializable(String tag) {
            try {
                byte[] bytes = mCursor.getBlob(mCursor.getColumnIndex(tag));
                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bis);
                return (Serializable) ois.readObject();
            } catch (Exception e) {
                Log.e(TAG, "Failed to read serializable object: ", e);
            }
            return null;
        }

        @Override
        public void finalize() throws Throwable {
            if (mCursor != null)
                mCursor.close();
            super.finalize();
        }
    }

    /** Class responsible of handling the database creation. */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            try {
                // Generate the database key
                KeyGenerator keygen = KeyGenerator.getInstance("AES");
                SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
                sr.setSeed("ucsf.patient_data.keyword".getBytes());
                keygen.init(128, sr);
                SecretKey key = keygen.generateKey();

                // Encode the database
                db.execSQL(String.format("PRAGMA key = '%s'", key.toString()));
            } catch (Exception e) {
                Log.e(TAG, "Failed to configure encrypted database: ", e);
            }
        }
    }

    /**
     * Class representing a database table. Tables are uniquely identified by a name and a device
     * location.
     */
    public class Table {
        public final String         tag;      /**< Unique identifier of the table. */
        public final DeviceLocation location; /**< Device from which the table is coming. */
        public final TableField[]   fields;   /**< List of fields composing the table. */

        private Table(String tag, DeviceLocation location, TableField... fields) {
            this.tag      = tag;
            this.location = location;
            this.fields   = fields;
        }

        /**
         * Add the given entries to the table. The database must have been opened before (see
         * {@link DataManager#get(Context)}).
         * @param entries List of entries to insert. An entry is composed of the field tag and the
         *                value to add. Missing field will be filled with the corresponding field
         *                default value.
         * @return Returns if the operation is successful.
         */
        public boolean add(Entry... entries) throws Exception {
            checkDb();

            return mDb.insert(tag, null, formatEntries(entries)) != -1;
        }

        /**
         * Fetch all entries matching the given conditions. The database must have been opened
         * before (see {@link DataManager#get(Context)}).
         * @param conditions List of conditions to satisfy. See {@link DataManager.Condition} for
         *                   supported conditions.
         * @return Returns null if an error occurs or a cursor pointing to the requested entries.
         */
        public Cursor fetch(Condition... conditions) throws Exception {
            return fetch(-1, conditions);
        }

        /**
         * Fetch all entries matching the given conditions up to the given limit. The database must
         * have been opened  before (see {@link DataManager#get(Context)}).
         * @param limit      Maximal number of entries to fetch.
         * @param conditions List of conditions to satisfy. See {@link DataManager.Condition} for
         *                   supported conditions.
         * @return Returns null if an error occurs or a cursor pointing to the requested entries.
         */
        public Cursor fetch(int limit, Condition... conditions) throws Exception {
            // Get the entries
            String[] entries = new String[fields.length + 1];
            for (int i = 0; i < fields.length; ++i)
                entries[i] = fields[i].tag;
            entries[fields.length] = KEY_ROW_ID;

            return fetch(limit, entries, conditions);
        }
        
        /**
         * Fetch all entries of the given types matching the given conditions. The database must
         * have been opened before (see {@link DataManager#get(Context)}).
         * @param entriesTags List of the field tags to fetch.
         * @param conditions  List of conditions to satisfy. See {@link DataManager.Condition} for
         *                    supported conditions.
         * @return Returns null if an error occurs or a cursor pointing to the requested entries.
         */
        public Cursor fetch(String[] entriesTags, Condition... conditions) throws Exception {
            return fetch(-1, entriesTags, conditions);
        }

        /**
         * Fetch all entries of the given types matching the given conditions up to the given limit.
         * The database must have been opened before (see {@link DataManager#get(Context)}).
         * @param limit      Maximal number of entries to fetch.
         * @param entriesTags List of the field tags to fetch.
         * @param conditions  List of conditions to satisfy. See {@link DataManager.Condition} for
         *                    supported conditions.
         * @return Returns null if an error occurs or a cursor pointing to the requested entries.
         */
        public Cursor fetch(int limit, String[] entriesTags, Condition... conditions)
                throws Exception
        {
            checkDb();

            // Get the internal database cursor
            android.database.Cursor cursor =
                    mDb.query(true, tag,
                            entriesTags,
                            formatConditions(conditions),
                            null, null, null, null,
                            limit > 0 ? String.valueOf(limit) : null);

            // Returns a cursor pointing to the first entry (if valid).
            if (cursor == null)
                return null;

            return new Cursor(cursor);
        }

        /**
         * Fetch all modified entries of the given types matching the given conditions. For
         * instance, the modifier "count" will return the number of entries matching the given
         * conditions. The database must have been opened before (see
         * {@link DataManager#get(Context)}).
         * @param entriesTags List of the field tags to fetch.
         * @param modifiers   Modifiers to apply to the given field tags. If a modifier is null or
         *                    and empty string, just fetch the corresponding field.
         * @param conditions  List of conditions to satisfy. See {@link DataManager.Condition} for
         *                    supported conditions.
         * @return Returns null if an error occurs or a cursor pointing to the requested entries.
         */
        public Cursor fetch(String[] entriesTags, String[] modifiers, Condition... conditions)
                throws Exception {
            return fetch(-1, entriesTags, modifiers, conditions);
        }

        /**
         * Fetch all modified entries of the given types matching the given conditions up to the
         * given limit. For instance, the modifier "count" will return the number of entries
         * matching the given conditions. The database must have been opened before (see
         * {@link DataManager#get(Context)}).
         * @param limit       Maximal number of entries to fetch.
         * @param entriesTags List of the field tags to fetch.
         * @param modifiers   Modifiers to apply to the given field tags. If a modifier is null or
         *                    and empty string, just fetch the corresponding field.
         * @param conditions  List of conditions to satisfy. See {@link DataManager.Condition} for
         *                    supported conditions.
         * @return Returns null if an error occurs or a cursor pointing to the requested entries.
         */
        public Cursor fetch(int limit, String[] entriesTags, String[] modifiers,
                            Condition... conditions) throws Exception {
            checkDb();

            String[] to_fetch = new String[entriesTags.length];
            for (int i = 0; i < entriesTags.length; ++i) {
                String modifier = modifiers[i];
                if (modifier == null || modifier.isEmpty())
                    to_fetch[i] = entriesTags[i];
                else
                    to_fetch[i] = String.format("%s(%s)", modifier, entriesTags[i]);
            }

            // Get the internal database cursor
            android.database.Cursor cursor =
                    mDb.query(true, tag,
                            to_fetch,
                            formatConditions(conditions),
                            null, null, null, null,
                            limit > 0 ? String.valueOf(limit) : null);

            // Returns a cursor pointing to the first entry (if valid).
            if (cursor == null)
                return null;

            return new Cursor(cursor);
        }

        /**
         * Update entries matching the given condition with the given values. The database must
         * have been opened before (see {@link DataManager#get(Context)}).
         * @param entries     List of entries to update. An entry is composed of the field tag and
         *                    the value to insert.
         * @param conditions  List of conditions to satisfy. See {@link DataManager.Condition} for
         *                    supported conditions.
         * @return Returns if the operation is successful.
         */
        public boolean update(Entry[] entries, Condition... conditions) throws Exception {
            checkDb();

            return mDb.update(tag, formatEntries(entries),
                    formatConditions(conditions), null) > 0;
        }

        /**
         * Fetch if the given entries exists. If true, update them. Otherwise add new entries.
         * The database must have been opened before (see {@link DataManager#get(Context)}).
         * @param entries     List of entries to update. An entry is composed of the field tag and
         *                    the value to insert.
         * @param conditions  List of entries to fetch. If no match found, those entries will be
         *                    inserted together with the other entries.
         * @return Returns if the operation is successful.
         */
        public boolean fetchAndAdd(Entry[] conditions, Entry... entries) throws Exception {
            // Create the conditions
            Condition[] cond = new Condition[conditions.length];
            for (int i = 0; i < conditions.length; ++i)
                cond[i] = new Condition.Equal<>(conditions[i].tag, conditions[i].value);

            // Search for existing entries
            Cursor cursor = fetch(cond);
            if (cursor != null && cursor.moveToFirst()) { // Update the existing entries
                return update(entries, cond);
            } else { // The entries don't exist, add entries.
                Set<Entry> allEntries = new HashSet<>(); // A set to be sure that there is no duplicates
                Collections.addAll(allEntries, conditions);
                Collections.addAll(allEntries, entries);
                return add(allEntries.toArray(new Entry[allEntries.size()]));
            }
        }

        /**
         * Removes entries matching the given conditions. The database must have been opened before
         * (see {@link DataManager#get(Context)}).
         * @param conditions  List of conditions to satisfy. See {@link DataManager.Condition} for
         *                    supported conditions.
         * @return Returns if the operation is successful.
         */
        public boolean erase(Condition... conditions) throws Exception {
            checkDb();
            return mDb.delete(tag, formatConditions(conditions), null) > 0;
        }

        /**
         * Formats the given table entries to a valid SQLite format.
         */
        private ContentValues formatEntries(Entry[] entries) {
            ContentValues values = new ContentValues();
            for (Entry entry : entries) {
                if (entry.value instanceof byte[])
                    values.put(entry.tag, (byte[]) entry.value);
                else
                    values.put(entry.tag, entry.value.toString());
            }
            return values;
        }

        /**
         * Formats the given conditions to a valid SQLite format.
         */
        private String formatConditions(Condition... conditions) {
            if (conditions.length == 0)
                return null;

            StringBuilder conditionRequest = new StringBuilder();
            conditionRequest.append(conditions[0].toString());
            for (int i = 1; i < conditions.length; ++i)
                conditionRequest.append(" AND ").append(conditions[i].toString());
            return conditionRequest.toString();
        }

        /**
         * Checks if the database is opened before executing any operation.
         */
        private void checkDb() throws Exception {
            if (mDb == null)
                throw new Exception("Database is not open!");
        }
    }

}

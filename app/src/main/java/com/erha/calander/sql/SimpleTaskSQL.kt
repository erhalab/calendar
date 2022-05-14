import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SimpleTaskSQL(context: Context, name: String, version: Int) :
    SQLiteOpenHelper(context, name, null, version) {
    //数据库第一次创建时被调用
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE `info` (`id` bigint NOT NULL,`json` varchar NOT NULL,PRIMARY KEY (`id`));")
    }

    //软件版本号发生改变时调用
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}
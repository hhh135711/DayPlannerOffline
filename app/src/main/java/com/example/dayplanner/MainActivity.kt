package com.example.dayplanner

import android.app.*
import android.os.Bundle
import android.content.*
import android.database.sqlite.*
import android.graphics.Color
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class PlannerDb(ctx: Context): SQLiteOpenHelper(ctx,"planner.db",null,1) {
 override fun onCreate(db: SQLiteDatabase) {
  db.execSQL("CREATE TABLE templates(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)")
  db.execSQL("CREATE TABLE template_items(id INTEGER PRIMARY KEY AUTOINCREMENT,template_id INTEGER NOT NULL,title TEXT NOT NULL,minutes INTEGER NOT NULL,note TEXT NOT NULL DEFAULT '')")
  db.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,day TEXT NOT NULL,start_time TEXT NOT NULL,title TEXT NOT NULL,minutes INTEGER NOT NULL,note TEXT NOT NULL DEFAULT '',done INTEGER NOT NULL DEFAULT 0)")
  db.execSQL("INSERT INTO templates(name) VALUES('日常计划')")
  listOf(Triple("早餐",30,""),Triple("专注学习",120,""),Triple("休息",30,""),Triple("运动",60,"")).forEach { (name,mins,note)->
   db.execSQL("INSERT INTO template_items(template_id,title,minutes,note) VALUES(1,?,?,?)",arrayOf(name,mins,note))
  }
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) {}
}

data class Task(val id:Long,val title:String,val minutes:Int,val note:String,val done:Boolean=false,val time:String="")
class MainActivity: Activity() {
 private lateinit var db:PlannerDb
 private lateinit var root:LinearLayout
 private var day=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
 private var start="08:00"
 private var selectedTemplate=1L
 private val bg=Color.rgb(247,249,252)
 override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState);db=PlannerDb(this);showToday()}
 private fun init(title:String) {
  val scroll=ScrollView(this);scroll.setBackgroundColor(bg)
  root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22,22,22,28)}
  scroll.addView(root);setContentView(scroll)
  text(title,26,true)
  val nav=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
  listOf("今日计划" to {showToday()},"模板管理" to {showTemplates()},"历史回顾" to {showHistory()}).forEach { (s,f)->
   val b=Button(this).apply{text=s;textSize=12f;setOnClickListener{f()}}
   nav.addView(b,LinearLayout.LayoutParams(0,-2,1f))
  };root.addView(nav)
 }
 private fun text(s:String,size:Int=16,bold:Boolean=false):TextView {val t=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(Color.rgb(35,45,60));setPadding(4,12,4,12);if(bold)setTypeface(null,1)};root.addView(t);return t}
 private fun button(s:String,click:()->Unit){root.addView(Button(this).apply{text=s;setOnClickListener{click()}})}
 private fun field(hint:String,value:String=""):EditText=EditText(this).apply{setSingleLine(true);setText(value);this.hint=hint}
 private fun ask(title:String,inputs:List<EditText>,ok:()->Unit){val layout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,8,24,0)};inputs.forEach{layout.addView(it)};AlertDialog.Builder(this).setTitle(title).setView(layout).setPositiveButton("保存"){_,_->ok()}.setNegativeButton("取消",null).show()}
 private fun templates():List<Pair<Long,String>> {val out=mutableListOf<Pair<Long,String>>();db.readableDatabase.rawQuery("SELECT id,name FROM templates ORDER BY id",null).use{while(it.moveToNext())out.add(it.getLong(0) to it.getString(1))};return out}
 private fun templateTasks(id:Long):List<Task>{val out=mutableListOf<Task>();db.readableDatabase.rawQuery("SELECT id,title,minutes,note FROM template_items WHERE template_id=? ORDER BY id",arrayOf(id.toString())).use{while(it.moveToNext())out.add(Task(it.getLong(0),it.getString(1),it.getInt(2),it.getString(3)))};return out}
 private fun dayTasks():List<Task>{val out=mutableListOf<Task>();db.readableDatabase.rawQuery("SELECT id,title,minutes,note,done,start_time FROM plans WHERE day=? ORDER BY start_time,id",arrayOf(day)).use{while(it.moveToNext())out.add(Task(it.getLong(0),it.getString(1),it.getInt(2),it.getString(3),it.getInt(4)==1,it.getString(5)))};return out}
 private fun chooseDay(next:()->Unit){val parts=day.split("-").map{it.toInt()};DatePickerDialog(this,{_,y,m,d->day="%04d-%02d-%02d".format(Locale.US,y,m+1,d);next()},parts[0],parts[1]-1,parts[2]).show()}
 private fun chooseTime(next:()->Unit){val p=start.split(":").map{it.toInt()};TimePickerDialog(this,{_,h,m->start="%02d:%02d".format(Locale.US,h,m);next()},p[0],p[1],true).show()}
 private fun showToday(){init("每日计划");button("日期：$day  · 点击切换"){chooseDay{showToday()}};button("开始时间：$start  · 点击修改"){chooseTime{showToday()}}
  val names=templates();if(names.none{it.first==selectedTemplate})selectedTemplate=names.firstOrNull()?.first?:0L
  button("选择模板：${names.find{it.first==selectedTemplate}?.second?:"无"}"){
   AlertDialog.Builder(this).setItems(names.map{it.second}.toTypedArray()){_,i->selectedTemplate=names[i].first;showToday()}.show()
  }
  button("按模板生成当天计划"){
   val tasks=templateTasks(selectedTemplate);if(tasks.isEmpty()){toast("请先添加模板任务");return@button}
   val generate={db.writableDatabase.beginTransaction();try{db.writableDatabase.delete("plans","day=?",arrayOf(day));var cursor=start.split(":").let{it[0].toInt()*60+it[1].toInt()};tasks.forEach{t->val v=ContentValues().apply{put("day",day);put("start_time","%02d:%02d".format(Locale.US,(cursor/60)%24,cursor%60));put("title",t.title);put("minutes",t.minutes);put("note",t.note)};db.writableDatabase.insert("plans",null,v);cursor+=t.minutes};db.writableDatabase.setTransactionSuccessful()}finally{db.writableDatabase.endTransaction()};showToday()}
   if(dayTasks().isNotEmpty())AlertDialog.Builder(this).setTitle("覆盖当天计划？").setMessage("重新生成会删除当天已有的任务、备注和完成状态。历史其他日期不受影响。") .setPositiveButton("覆盖"){_,_->generate()}.setNegativeButton("取消",null).show() else generate()
  }
  text("当天任务",20,true);val tasks=dayTasks();if(tasks.isEmpty())text("还没有计划，选择模板后点击生成。")
  tasks.forEach{t->val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(14,8,14,8);setBackgroundColor(Color.WHITE)}
   val check=CheckBox(this).apply{text="${t.time}  ${t.title}（${t.minutes} 分钟）";isChecked=t.done;setOnCheckedChangeListener{_,checked->val v=ContentValues().apply{put("done",if(checked)1 else 0)};db.writableDatabase.update("plans",v,"id=?",arrayOf(t.id.toString()))}}
   row.addView(check);row.addView(TextView(this).apply{text=if(t.note.isBlank())"备注：无" else "备注：${t.note}";setPadding(12,4,4,4)})
   row.addView(Button(this).apply{text="编辑任务 / 备注";setOnClickListener{editPlan(t)}});root.addView(row)
  }
  if(tasks.isNotEmpty())text("已完成 ${tasks.count{it.done}} / ${tasks.size} 项",16,true)
 }
 private fun editPlan(t:Task){val name=field("任务名称",t.title);val mins=field("持续分钟",t.minutes.toString()).apply{inputType=2};val note=field("备注",t.note);val time=field("开始时间 HH:mm",t.time)
  ask("编辑当天任务",listOf(name,mins,note,time)){
   val n=mins.text.toString().toIntOrNull();val tm=time.text.toString();if(name.text.isBlank()||n==null||n<=0||!validTime(tm)){toast("请输入有效任务、分钟数和时间");return@ask}
   val v=ContentValues().apply{put("title",name.text.toString());put("minutes",n);put("note",note.text.toString());put("start_time",tm)};db.writableDatabase.update("plans",v,"id=?",arrayOf(t.id.toString()));showToday()
  }
 }
 private fun validTime(s:String)=Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(s)
 private fun showTemplates(){init("模板管理");button("＋ 新建模板"){val name=field("模板名称");ask("新建模板",listOf(name)){if(name.text.isNotBlank()){val v=ContentValues().apply{put("name",name.text.toString())};selectedTemplate=db.writableDatabase.insert("templates",null,v);showTemplates()}}}
  templates().forEach{(id,name)->text(name,20,true);button("＋ 给「$name」添加任务"){editTemplateTask(id,null)}
   templateTasks(id).forEach{t->button("${t.title}  ·  ${t.minutes}分钟  ·  ${t.note.ifBlank{"无备注"}}  ✎"){editTemplateTask(id,t)}}
   button("删除模板「$name」") {AlertDialog.Builder(this).setMessage("确定删除模板及其中的任务？不会删除已经生成的历史计划。") .setPositiveButton("删除"){_,_->db.writableDatabase.delete("template_items","template_id=?",arrayOf(id.toString()));db.writableDatabase.delete("templates","id=?",arrayOf(id.toString()));showTemplates()}.setNegativeButton("取消",null).show()}
  }
 }
 private fun editTemplateTask(templateId:Long,t:Task?){val name=field("任务名称",t?.title?:"");val mins=field("持续分钟",t?.minutes?.toString()?:"30").apply{inputType=2};val note=field("默认备注",t?.note?:"");ask(if(t==null)"添加任务" else "修改任务",listOf(name,mins,note)){
  val n=mins.text.toString().toIntOrNull();if(name.text.isBlank()||n==null||n<=0){toast("请填写任务名称和正确时长");return@ask}
  val v=ContentValues().apply{put("title",name.text.toString());put("minutes",n);put("note",note.text.toString());put("template_id",templateId)}
  if(t==null)db.writableDatabase.insert("template_items",null,v) else db.writableDatabase.update("template_items",v,"id=?",arrayOf(t.id.toString()));showTemplates()
 }}
 private fun showHistory(){init("历史回顾");button("查看日期：$day"){chooseDay{showHistory()}};val tasks=dayTasks();if(tasks.isEmpty())text("这一天没有保存的计划。") else {text("完成 ${tasks.count{it.done}} / ${tasks.size}",20,true);tasks.forEach{text("${if(it.done)"✓" else "○"} ${it.time} ${it.title}（${it.minutes}分钟）\n备注：${it.note.ifBlank{"无"}}")}}
  button("打开这一天并编辑"){showToday()}
 }
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}

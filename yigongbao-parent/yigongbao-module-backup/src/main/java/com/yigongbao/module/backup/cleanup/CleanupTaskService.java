package com.yigongbao.module.backup.cleanup;

import org.springframework.stereotype.Service;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;

/** COS 清理线上安全闸门；实际对象删除只能由本服务签发的 ACTIVE permit 驱动。 */
@Service
public class CleanupTaskService {
 private final Map<String,Task> tasks=new ConcurrentHashMap<>(); private final Map<String,Permit> permits=new ConcurrentHashMap<>();
 public Evaluation evaluate(int olderThanDays){if(olderThanDays<0)throw new IllegalArgumentException("清理天数不能为负");return new Evaluation(UUID.randomUUID().toString(),"DRY_RUN",List.of(),0L);}
 public synchronized Task create(int olderThanDays,List<String> keys){if(olderThanDays<0||keys==null)throw new IllegalArgumentException("参数非法");String id=UUID.randomUUID().toString();Task t=new Task(id,olderThanDays,"RUNNING",List.copyOf(keys));tasks.put(id,t);return t;}
 public Task get(String id){Task t=tasks.get(id);if(t==null)throw new IllegalArgumentException("任务不存在");return t;}
 public List<Task> list(){return tasks.values().stream().sorted(Comparator.comparing(Task::id).reversed()).toList();}
 public synchronized Task pause(String id){Task t=get(id);if(!"RUNNING".equals(t.status))throw new IllegalStateException("任务不可暂停");t.status="PAUSED";return t;}
 public synchronized Task resume(String id){Task t=get(id);if(!"PAUSED".equals(t.status))throw new IllegalStateException("任务不可恢复");t.status="RUNNING";return t;}
 public synchronized Permit issuePermit(String id){Task t=get(id);if(!"RUNNING".equals(t.status)||t.keys.isEmpty())throw new IllegalStateException("任务尚未满足授权条件");Permit p=new Permit(UUID.randomUUID().toString(),id,List.copyOf(t.keys),"ACTIVE");permits.put(p.id,p);return p;}
 public synchronized Permit revoke(String id){Permit p=permits.get(id);if(p==null)throw new IllegalArgumentException("授权不存在");p.status="REVOKED";return p;}
 public record Evaluation(String taskId,String status,List<String> keys,long estimatedBytes){} public static final class Task {public final String id; public final int olderThanDays; public final List<String> keys; public String status; Task(String i,int d,String s,List<String> k){id=i;olderThanDays=d;status=s;keys=k;} public String id(){return id;} public String status(){return status;} public List<String> keys(){return keys;}}
 public static final class Permit {public final String id;public final String taskId;public final List<String> keys;public String status;Permit(String i,String t,List<String> k,String s){id=i;taskId=t;keys=k;status=s;}}
}

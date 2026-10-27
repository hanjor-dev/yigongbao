package com.yigongbao.module.backup.restore;

import org.springframework.stereotype.Service;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;

/** 恢复会话状态机参考实现；生产实现以 bk_restore_session/bk_restore_asset 事务写入为准。 */
@Service
public class InMemoryRestoreSessionService implements RestoreSessionService {
 private final Map<String,RestoreSession> sessions=new ConcurrentHashMap<>();
 public RestoreSession prepare(Long orderId,String revision,String digest){if(orderId==null||revision==null||digest==null)throw new IllegalArgumentException("恢复参数不完整");String id=UUID.randomUUID().toString();RestoreSession s=new RestoreSession(id,orderId,"PREPARED",revision,digest,List.of(),null);sessions.put(id,s);return s;}
 public RestoreSession get(String id){RestoreSession s=sessions.get(id);if(s==null)throw new IllegalArgumentException("恢复会话不存在");return s;}
 public synchronized RestoreSession commit(String id,List<UploadedAsset> uploaded){RestoreSession s=get(id);if(!"PREPARED".equals(s.status())){if("COMMITTED".equals(s.status()))return s;throw new IllegalStateException("恢复会话不可提交");}if(uploaded==null||uploaded.isEmpty())throw new IllegalArgumentException("上传证据为空");Set<String> ids=new HashSet<>();for(UploadedAsset a:uploaded){if(a==null||a.assetIdentity()==null||!ids.add(a.assetIdentity())||a.objectKey()==null||a.sha256()==null||a.size()<0)throw new IllegalArgumentException("上传证据非法");}RestoreSession done=new RestoreSession(s.id(),s.orderId(),"COMMITTED",s.manifestRevision(),s.relationDigest(),List.copyOf(uploaded),null);sessions.put(id,done);return done;}
 public synchronized RestoreSession abort(String id,String reason){RestoreSession s=get(id);if("COMMITTED".equals(s.status()))throw new IllegalStateException("已提交会话不可终止");RestoreSession aborted=new RestoreSession(s.id(),s.orderId(),"ABORTED",s.manifestRevision(),s.relationDigest(),s.uploaded(),reason);sessions.put(id,aborted);return aborted;}
}

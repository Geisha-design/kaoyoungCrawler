package smartebao.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
@TableName("crawler_scheduled_task")
public class CrawlerScheduledTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("task_key")
    private String taskKey; // 定时任务唯一标识
    
    @TableField("client_id")
    private String clientId; // 关联客户端ID
    
    @TableField("username")
    private String username; // 关联用户名
    
    @TableField("script_id")
    private String scriptId; // 关联脚本ID
    
    @TableField("domain")
    private String domain; // 目标域名正则
    
    @TableField("interval")
    private Long interval; // 执行间隔（毫秒）
    
    @TableField(value = "`enabled`")
    private Boolean enabled; // 是否启用（true=启用，false=禁用）
    
    @TableField("execute_on_idle")
    private Boolean executeOnIdle; // 是否仅在空闲时执行
    
    @TableField("task_name")
    private String taskName; // 任务名称
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("update_time")
    private Date updateTime;

    // 自动生成的Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getExecuteOnIdle() {
        return executeOnIdle;
    }

    public void setExecuteOnIdle(Boolean executeOnIdle) {
        this.executeOnIdle = executeOnIdle;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
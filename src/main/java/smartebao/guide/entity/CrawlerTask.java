package smartebao.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
@TableName("crawler_task")
public class CrawlerTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("task_id")
    private String taskId;
    
    @TableField("client_id")
    private String clientId;
    
    @TableField("task_type")
    private String taskType;
    
    @TableField("script_id")
    private String scriptId;
    
    private Integer timeout;
    
    @TableField("params")
    private String taskParams; // 任务参数（新增字段以匹配使用场景）
    
    @TableField("target_clients")
    private String targetClients; // 目标客户端（新增字段以匹配使用场景）
    
    @TableField("priority")
    private Integer priority; // 优先级（新增字段以匹配使用场景）
    
    @TableField("params")
    private String params; // 任务额外参数（JSON格式）
    
    @TableField("execute_on_idle")
    private Boolean executeOnIdle; // 是否仅在空闲时执行
    
    @TableField("create_time")
    private Date createTime;
    
    @TableField("status")
    private String status; // pending/processing/success/fail

    // 自动生成的Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getTaskParams() {
        return taskParams;
    }

    public void setTaskParams(String taskParams) {
        this.taskParams = taskParams;
    }

    public String getTargetClients() {
        return targetClients;
    }

    public void setTargetClients(String targetClients) {
        this.targetClients = targetClients;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Boolean getExecuteOnIdle() {
        return executeOnIdle;
    }

    public void setExecuteOnIdle(Boolean executeOnIdle) {
        this.executeOnIdle = executeOnIdle;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
package smartebao.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("crawler_client")
public class CrawlerClient {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String clientId;
    
    private String username;
    
    private String currentUrl;
    
    private String supportTaskTypes;
    
    private String browser;
    
    private Date connectTime;
    
    private String status;
    
    private Date lastUpdateTime;
    
    private Boolean idleStatus; // 空闲状态

    // 自动生成的Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void setCurrentUrl(String currentUrl) {
        this.currentUrl = currentUrl;
    }

    public String getSupportTaskTypes() {
        return supportTaskTypes;
    }

    public void setSupportTaskTypes(String supportTaskTypes) {
        this.supportTaskTypes = supportTaskTypes;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public Date getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(Date connectTime) {
        this.connectTime = connectTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Boolean getIdleStatus() {
        return idleStatus;
    }

    public void setIdleStatus(Boolean idleStatus) {
        this.idleStatus = idleStatus;
    }
}
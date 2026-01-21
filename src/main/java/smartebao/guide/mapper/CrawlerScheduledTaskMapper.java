package smartebao.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import smartebao.guide.entity.CrawlerScheduledTask;

import java.util.List;

@Mapper
public interface CrawlerScheduledTaskMapper extends BaseMapper<CrawlerScheduledTask> {
    
    /**
     * 查询所有启用的定时任务
     * 使用自定义查询避免MySQL保留字enabled的问题
     */
    @Select("SELECT id, task_key, client_id, username, script_id, domain, interval, `enabled`, execute_on_idle, task_name, create_time, update_time FROM crawler_scheduled_task WHERE `enabled` = 1")
    List<CrawlerScheduledTask> selectAllEnabledTasks();
    
    /**
     * 根据客户端ID查询定时任务
     */
    @Select("SELECT id, task_key, client_id, username, script_id, domain, interval, `enabled`, execute_on_idle, task_name, create_time, update_time FROM crawler_scheduled_task WHERE client_id = #{clientId}")
    List<CrawlerScheduledTask> selectByClientId(String clientId);
    
    /**
     * 根据客户端ID删除定时任务
     */
    @Delete("DELETE FROM crawler_scheduled_task WHERE client_id = #{clientId}")
    int deleteByClientId(String clientId);
}
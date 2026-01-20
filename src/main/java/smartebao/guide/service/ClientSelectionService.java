package smartebao.guide.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerClient;
import smartebao.guide.mapper.CrawlerClientMapper;

import java.util.List;
import java.util.Map;

/**
 * 客户端选择服务 - 根据不同条件筛选合适的客户端
 */
@Service
public class ClientSelectionService {

    @Autowired
    private CrawlerClientMapper crawlerClientMapper;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 根据任务类型选择合适的客户端
     *
     * @param taskType 任务类型
     * @param criteria 附加筛选条件，如公司账号、地理位置等
     * @return 符合条件的客户端ID列表
     */
    public List<String> selectClientsByTaskType(String taskType, Map<String, Object> criteria) {
        QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
        
        // 基础条件：客户端必须在线
        wrapper.eq("status", "online");
        
        // 根据任务类型筛选支持该任务类型的客户端
        if (taskType != null && !taskType.isEmpty()) {
            wrapper.like("support_task_types", taskType);
        }
        
        // 根据附加条件筛选
        if (criteria != null) {
            for (Map.Entry<String, Object> entry : criteria.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                switch (key) {
                    case "companyAccount":
                        // 假设公司账号信息存储在备注字段中
                        wrapper.and(w -> w.like("notes", value.toString())
                                          .or()
                                          .eq("username", value.toString()));
                        break;
                    case "location":
                        wrapper.eq("location", value);
                        break;
                    case "browserType":
                        wrapper.eq("browser_type", value);
                        break;
                    case "idleOnly":
                        if ((Boolean) value) {
                            wrapper.eq("idle_status", true);
                        }
                        break;
                    default:
                        // 其他自定义条件
                        wrapper.eq(key, value);
                        break;
                }
            }
        }
        
        List<CrawlerClient> clients = crawlerClientMapper.selectList(wrapper);
        return clients.stream()
                     .map(CrawlerClient::getClientId)
                     .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取空闲的客户端
     *
     * @return 空闲客户端ID列表
     */
    public List<String> getIdleClients() {
        return webSocketService.getIdleClients();
    }

    /**
     * 获取所有在线客户端
     *
     * @return 在线客户端ID列表
     */
    public List<String> getOnlineClients() {
        return webSocketService.getOnlineClients();
    }

    /**
     * 根据特定客户端ID列表获取客户端
     *
     * @param clientIds 特定客户端ID列表
     * @return 符合条件的在线客户端ID列表
     */
    public List<String> getClientsByIds(List<String> clientIds) {
        QueryWrapper<CrawlerClient> wrapper = new QueryWrapper<>();
        wrapper.in("client_id", clientIds)
               .eq("status", "online");
        
        List<CrawlerClient> clients = crawlerClientMapper.selectList(wrapper);
        return clients.stream()
                     .map(CrawlerClient::getClientId)
                     .collect(java.util.stream.Collectors.toList());
    }
}
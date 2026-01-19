package smartebao.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import smartebao.guide.entity.CrawlerScheduledTask;

@Mapper
public interface CrawlerScheduledTaskMapper extends BaseMapper<CrawlerScheduledTask> {
}
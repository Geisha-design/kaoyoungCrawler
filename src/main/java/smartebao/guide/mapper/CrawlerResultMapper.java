package smartebao.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import smartebao.guide.entity.CrawlerResult;

@Mapper
public interface CrawlerResultMapper extends BaseMapper<CrawlerResult> {
}
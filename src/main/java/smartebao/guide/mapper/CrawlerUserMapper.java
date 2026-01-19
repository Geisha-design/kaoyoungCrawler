package smartebao.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import smartebao.guide.entity.CrawlerUser;

@Mapper
public interface CrawlerUserMapper extends BaseMapper<CrawlerUser> {
}
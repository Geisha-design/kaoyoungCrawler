package smartebao.guide.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import smartebao.guide.entity.CrawlerUser;
import smartebao.guide.mapper.CrawlerUserMapper;
import smartebao.guide.service.CrawlerUserService;

@Service
public class CrawlerUserServiceImpl extends ServiceImpl<CrawlerUserMapper, CrawlerUser> implements CrawlerUserService {

    @Override
    public CrawlerUser findByUsername(String username) {
        QueryWrapper<CrawlerUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return getOne(wrapper);
    }

    @Override
    public CrawlerUser findByUsernameAndPassword(String username, String password) {
        QueryWrapper<CrawlerUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).eq("password", password);
        return getOne(wrapper);
    }
}
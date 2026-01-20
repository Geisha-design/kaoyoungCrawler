package smartebao.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import smartebao.guide.entity.CrawlerUser;

public interface CrawlerUserService extends IService<CrawlerUser> {
    /**
     * 根据用户名查找用户
     */
    CrawlerUser findByUsername(String username);

    /**
     * 根据用户名和密码查找用户
     */
    CrawlerUser findByUsernameAndPassword(String username, String password);
}
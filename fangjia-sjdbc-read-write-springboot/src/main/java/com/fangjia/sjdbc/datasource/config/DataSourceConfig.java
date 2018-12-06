package com.fangjia.sjdbc.datasource.config;

import com.fangjia.sjdbc.datasource.DataBaseMessage;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@SpringBootConfiguration
public class DataSourceConfig {

    static final String PACKAGE = "com.fangjia.sjdbc.repository";
    static final String MAPPER_LOCATION = "classpath:META-INF/mappers/*.xml";
    public static DataBaseMessage masterDataBaseMessage;
    public static DataBaseMessage dashBoardDataBaseMessage;

    @Value("${datasource.url}")
    private String url;

    @Value("${datasource.username}")
    private String user;

    @Value("${datasource.password}")
    private String password;

    @Value("${datasource.driverClassName}")
    private String driverClass;

    @Value("${dashBoard.datasource.url}")
    private String dashBoardUrl;

    @Value("${dashBoard.datasource.username}")
    private String dashBoardUser;

    @Value("${dashBoard.datasource.password}")
    private String dashBoardPassword;

    @Value("${dashBoard.datasource.driverClassName}")
    private String dashBoardDriverClass;

    @Bean(name = "dashBoardDatabaseMessage")
    public DataBaseMessage dashBoardDatabaseMessage() {
        DataBaseMessage boardDataBaseMessage = new DataBaseMessage();
        boardDataBaseMessage.setUrl(dashBoardUrl);
        boardDataBaseMessage.setUserName(dashBoardUser);
        boardDataBaseMessage.setPassword(dashBoardPassword);
        dashBoardDataBaseMessage = boardDataBaseMessage;
        return boardDataBaseMessage;
    }

    @Bean(name = "masterDatabaseMessage")
    public DataBaseMessage masterDatabaseMessage() {
        DataBaseMessage dataBaseMessage = new DataBaseMessage();
        dataBaseMessage.setUrl(url);
        dataBaseMessage.setUserName(user);
        dataBaseMessage.setPassword(password);
        masterDataBaseMessage = dataBaseMessage;
        return dataBaseMessage;
    }


}